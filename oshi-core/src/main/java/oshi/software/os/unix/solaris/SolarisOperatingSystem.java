/**
 * MIT License
 *
 * Copyright (c) 2010-2019 The OSHI project team
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
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; // NOSONAR squid:S1191

import oshi.jna.platform.unix.solaris.SolarisLibc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.util.ExecutingCommand;
import oshi.util.LsofUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcUtil;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;

/**
 * Linux is a family of free operating systems most commonly used on personal
 * computers.
 */
public class SolarisOperatingSystem extends AbstractOperatingSystem {

    private static final long BOOTTIME = querySystemBootTime();

    /**
     * <p>
     * Constructor for SolarisOperatingSystem.
     * </p>
     */
    @SuppressWarnings("deprecation")
    public SolarisOperatingSystem() {
        this.version = new SolarisOSVersionInfoEx();
    }

    @Override
    public String queryManufacturer() {
        return "Oracle";
    }

    @Override
    public FamilyVersionInfo queryFamilyVersionInfo() {
        String[] split = ParseUtil.whitespaces.split(ExecutingCommand.getFirstAnswer("uname -rv"));
        String version = split[0];
        String buildNumber = null;
        if (split.length > 1) {
            buildNumber = split[1];
        }
        return new FamilyVersionInfo("SunOS", new OSVersionInfo(version, "Solaris", buildNumber));
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness == 64) {
            return 64;
        }
        return ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("isainfo -b"), 32);
    }

    @Override
    protected boolean queryElevated() {
        return System.getenv("SUDO_COMMAND") != null;
    }

    @Override
    public FileSystem getFileSystem() {
        return new SolarisFileSystem();
    }

    @Override
    public OSProcess[] getProcesses(int limit, ProcessSort sort, boolean slowFields) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -eo s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args", -1, slowFields);
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[0]);
    }

    @Override
    public OSProcess getProcess(int pid, boolean slowFields) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -o s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args -p ", pid, slowFields);
        if (procs.isEmpty()) {
            return null;
        }
        return procs.get(0);
    }

    @Override
    public OSProcess[] getChildProcesses(int parentPid, int limit, ProcessSort sort) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -eo s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args --ppid", parentPid, true);
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[0]);
    }

    private List<OSProcess> getProcessListFromPS(String psCommand, int pid, boolean slowFields) {
        Map<Integer, String> cwdMap = LsofUtil.getCwdMap(pid);
        List<OSProcess> procs = new ArrayList<>();
        List<String> procList = ExecutingCommand.runNative(psCommand + (pid < 0 ? "" : pid));
        if (procList.isEmpty() || procList.size() < 2) {
            return procs;
        }
        // remove header row
        procList.remove(0);
        // Fill list
        for (String proc : procList) {
            String[] split = ParseUtil.whitespaces.split(proc.trim(), 15);
            // Elements should match ps command order
            if (split.length < 15) {
                continue;
            }
            long now = System.currentTimeMillis();
            OSProcess sproc = new OSProcess(this);
            switch (split[0].charAt(0)) {
            case 'O':
                sproc.setState(OSProcess.State.RUNNING);
                break;
            case 'S':
                sproc.setState(OSProcess.State.SLEEPING);
                break;
            case 'R':
            case 'W':
                sproc.setState(OSProcess.State.WAITING);
                break;
            case 'Z':
                sproc.setState(OSProcess.State.ZOMBIE);
                break;
            case 'T':
                sproc.setState(OSProcess.State.STOPPED);
                break;
            default:
                sproc.setState(OSProcess.State.OTHER);
                break;
            }
            sproc.setProcessID(ParseUtil.parseIntOrDefault(split[1], 0));
            sproc.setParentProcessID(ParseUtil.parseIntOrDefault(split[2], 0));
            sproc.setUser(split[3]);
            sproc.setUserID(split[4]);
            sproc.setGroup(split[5]);
            sproc.setGroupID(split[6]);
            sproc.setThreadCount(ParseUtil.parseIntOrDefault(split[7], 0));
            sproc.setPriority(ParseUtil.parseIntOrDefault(split[8], 0));
            // These are in KB, multiply
            sproc.setVirtualSize(ParseUtil.parseLongOrDefault(split[9], 0) * 1024);
            sproc.setResidentSetSize(ParseUtil.parseLongOrDefault(split[10], 0) * 1024);
            // Avoid divide by zero for processes up less than a second
            long elapsedTime = ParseUtil.parseDHMSOrDefault(split[11], 0L);
            sproc.setUpTime(elapsedTime < 1L ? 1L : elapsedTime);
            sproc.setStartTime(now - sproc.getUpTime());
            sproc.setUserTime(ParseUtil.parseDHMSOrDefault(split[12], 0L));
            sproc.setPath(split[13]);
            sproc.setName(sproc.getPath().substring(sproc.getPath().lastIndexOf('/') + 1));
            sproc.setCommandLine(split[14]);
            sproc.setCurrentWorkingDirectory(cwdMap.getOrDefault(sproc.getProcessID(), ""));
            // bytes read/written not easily available

            if (slowFields) {
                List<String> openFilesList = ExecutingCommand.runNative(String.format("lsof -p %d", pid));
                sproc.setOpenFiles(openFilesList.size() - 1L);

                List<String> pflags = ExecutingCommand.runNative("pflags " + pid);
                for (String line : pflags) {
                    if (line.contains("data model")) {
                        if (line.contains("LP32")) {
                            sproc.setBitness(32);
                        } else if (line.contains("LP64")) {
                            sproc.setBitness(64);
                        }
                        break;
                    }
                }
            }
            procs.add(sproc);
        }
        return procs;
    }

    @Override
    public long getProcessAffinityMask(int processId) {
        long bitMask = 0L;
        String cpuset = ExecutingCommand.getFirstAnswer("pbind -q " + processId);
        // Sample output:
        // <empty string if no binding>
        // pid 101048 strongly bound to processor(s) 0 1 2 3.
        if (cpuset.isEmpty()) {
            List<String> allProcs = ExecutingCommand.runNative("psrinfo");
            for (String proc : allProcs) {
                String[] split = ParseUtil.whitespaces.split(proc);
                int bitToSet = ParseUtil.parseIntOrDefault(split[0], -1);
                if (bitToSet >= 0) {
                    bitMask |= (1L << bitToSet);
                }
            }
            return bitMask;
        } else if (cpuset.endsWith(".") && cpuset.contains("strongly bound to processor(s)")) {
            String parse = cpuset.substring(0, cpuset.length() - 1);
            String[] split = ParseUtil.whitespaces.split(parse);
            for (int i = split.length - 1; i >= 0; i--) {
                int bitToSet = ParseUtil.parseIntOrDefault(split[i], -1);
                if (bitToSet >= 0) {
                    bitMask |= (1L << bitToSet);
                } else {
                    // Once we run into the word processor(s) we're done
                    break;
                }
            }
        }
        return bitMask;
    }

    @Override
    public int getProcessId() {
        return SolarisLibc.INSTANCE.getpid();
    }

    @Override
    public int getProcessCount() {
        return ProcUtil.getPidFiles().length;
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
            Kstat ksp = kc.lookup("unix", 0, "system_misc");
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
            Kstat ksp = kc.lookup("unix", 0, "system_misc");
            if (ksp != null && kc.read(ksp)) {
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
