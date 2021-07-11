/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os.unix.solaris;

import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; // NOSONAR squid:S1191

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.proc.ProcessStat;
import oshi.driver.unix.solaris.Who;
import oshi.jna.platform.unix.solaris.SolarisLibc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSSession;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;
import oshi.util.tuples.Pair;

/**
 * Solaris is a non-free Unix operating system originally developed by Sun
 * Microsystems. It superseded the company's earlier SunOS in 1993. In 2010,
 * after the Sun acquisition by Oracle, it was renamed Oracle Solaris.
 */
@ThreadSafe
public class SolarisOperatingSystem extends AbstractOperatingSystem {

    /*
     * Package-private for use by SolarisOSProcess
     */
    enum PsKeywords {
        S, PID, PPID, USER, UID, GROUP, GID, NLWP, PRI, VSZ, RSS, ETIME, TIME, COMM, ARGS; // ARGS must always be last
    }

    static final String PS_COMMAND_ARGS = Arrays.stream(PsKeywords.values()).map(Enum::name).map(String::toLowerCase)
            .collect(Collectors.joining(","));

    enum PrstatKeywords {
        PID, USERNAME, USR, SYS, TRP, TFL, DFL, LCK, SLP, LAT, VCX, ICX, SCL, SIG, PROCESS_NLWP; // prstat -v
    }

    private static final long BOOTTIME = querySystemBootTime();

    @Override
    public String queryManufacturer() {
        return "Oracle";
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        String[] split = ParseUtil.whitespaces.split(ExecutingCommand.getFirstAnswer("uname -rv"));
        String version = split[0];
        String buildNumber = null;
        if (split.length > 1) {
            buildNumber = split[1];
        }
        return new Pair<>("SunOS", new OSVersionInfo(version, "Solaris", buildNumber));
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness == 64) {
            return 64;
        }
        return ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("isainfo -b"), 32);
    }

    @Override
    public FileSystem getFileSystem() {
        return new SolarisFileSystem();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new SolarisInternetProtocolStats();
    }

    @Override
    public List<OSSession> getSessions() {
        return USE_WHO_COMMAND ? super.getSessions() : Who.queryUtxent();
    }

    @Override
    public OSProcess getProcess(int pid) {
        List<OSProcess> procs = getProcessListFromPS("ps -o " + PS_COMMAND_ARGS + " -p " + pid, pid);
        if (procs.isEmpty()) {
            return null;
        }
        return procs.get(0);
    }

    @Override
    public List<OSProcess> queryAllProcesses() {
        return queryAllProcessesFromPS();
    }

    @Override
    public List<OSProcess> queryChildProcesses(int parentPid) {
        List<OSProcess> allProcs = queryAllProcessesFromPS();
        Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, false);
        return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
    }

    @Override
    public List<OSProcess> queryDescendantProcesses(int parentPid) {
        List<OSProcess> allProcs = queryAllProcessesFromPS();
        Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, true);
        return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
    }

    private static List<OSProcess> queryAllProcessesFromPS() {
        return getProcessListFromPS("ps -eo " + PS_COMMAND_ARGS, -1);
    }

    private static List<OSProcess> getProcessListFromPS(String psCommand, int pid) {
        List<OSProcess> procs = new ArrayList<>();
        List<String> procList = ExecutingCommand.runNative(psCommand);
        if (procList.size() > 1) {
            // Get a map by pid of prstat output
            List<String> prstatList = pid < 0 ? ExecutingCommand.runNative("prstat -v 1 1")
                    : ExecutingCommand.runNative("prstat -v -p " + pid + " 1 1");
            Map<String, String> prstatRowMap = new HashMap<>();
            for (String s : prstatList) {
                String row = s.trim();
                int idx = row.indexOf(' ');
                if (idx > 0) {
                    prstatRowMap.put(row.substring(0, idx), row);
                }
            }
            // remove header row and iterate proc list
            procList.remove(0);
            for (String proc : procList) {
                Map<PsKeywords, String> psMap = ParseUtil.stringToEnumMap(PsKeywords.class, proc.trim(), ' ');
                // Check if last (thus all) value populated
                if (psMap.containsKey(PsKeywords.ARGS)) {
                    String pidStr = psMap.get(PsKeywords.PID);
                    Map<PrstatKeywords, String> prstatMap = ParseUtil.stringToEnumMap(PrstatKeywords.class,
                            prstatRowMap.getOrDefault(pidStr, ""), ' ');
                    procs.add(new SolarisOSProcess(pid < 0 ? ParseUtil.parseIntOrDefault(pidStr, 0) : pid, psMap,
                            prstatMap));
                }
            }
        }
        return procs;
    }

    @Override
    public int getProcessId() {
        return SolarisLibc.INSTANCE.getpid();
    }

    @Override
    public int getProcessCount() {
        return ProcessStat.getPidFiles().length;
    }

    @Override
    public int getThreadCount() {
        List<String> threadList = ExecutingCommand.runNative("ps -eLo pid");
        if (!threadList.isEmpty()) {
            // Subtract 1 for header
            return threadList.size() - 1;
        }
        return getProcessCount();
    }

    @Override
    public long getSystemUptime() {
        return querySystemUptime();
    }

    private static long querySystemUptime() {
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = KstatChain.lookup("unix", 0, "system_misc");
            if (ksp != null) {
                // Snap Time is in nanoseconds; divide for seconds
                return ksp.ks_snaptime / 1_000_000_000L;
            }
        }
        return 0L;
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    private static long querySystemBootTime() {
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = KstatChain.lookup("unix", 0, "system_misc");
            if (ksp != null && KstatChain.read(ksp)) {
                return KstatUtil.dataLookupLong(ksp, "boot_time");
            }
        }
        return System.currentTimeMillis() / 1000L - querySystemUptime();
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new SolarisNetworkParams();
    }

    @Override
    public OSService[] getServices() {
        List<OSService> services = new ArrayList<>();
        // Get legacy RC service name possibilities
        List<String> legacySvcs = new ArrayList<>();
        File dir = new File("/etc/init.d");
        File[] listFiles;
        if (dir.exists() && dir.isDirectory() && (listFiles = dir.listFiles()) != null) {
            for (File f : listFiles) {
                legacySvcs.add(f.getName());
            }
        }
        // Iterate service list
        List<String> svcs = ExecutingCommand.runNative("svcs -p");
        /*-
         Output:
         STATE          STIME    FRMI
         legacy_run     23:56:49 lrc:/etc/rc2_d/S47pppd
         legacy_run     23:56:49 lrc:/etc/rc2_d/S81dodatadm_udaplt
         legacy_run     23:56:49 lrc:/etc/rc2_d/S89PRESERVE
         online         23:56:25 svc:/system/early-manifest-import:default
         online         23:56:25 svc:/system/svc/restarter:default
                        23:56:24       13 svc.startd
                        ...
         */
        for (String line : svcs) {
            if (line.startsWith("online")) {
                int delim = line.lastIndexOf(":/");
                if (delim > 0) {
                    String name = line.substring(delim + 1);
                    if (name.endsWith(":default")) {
                        name = name.substring(0, name.length() - 8);
                    }
                    services.add(new OSService(name, 0, STOPPED));
                }
            } else if (line.startsWith(" ")) {
                String[] split = ParseUtil.whitespaces.split(line.trim());
                if (split.length == 3) {
                    services.add(new OSService(split[2], ParseUtil.parseIntOrDefault(split[1], 0), RUNNING));
                }
            } else if (line.startsWith("legacy_run")) {
                for (String svc : legacySvcs) {
                    if (line.endsWith(svc)) {
                        services.add(new OSService(svc, 0, STOPPED));
                        break;
                    }
                }
            }
        }
        return services.toArray(new OSService[0]);
    }
}
