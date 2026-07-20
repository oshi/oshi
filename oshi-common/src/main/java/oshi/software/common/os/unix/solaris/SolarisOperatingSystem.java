/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.solaris;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.driver.linux.proc.ProcessStat;
import oshi.util.tuples.Pair;

/**
 * Solaris is a non-free Unix operating system originally developed by Sun Microsystems. It superseded the company's
 * earlier SunOS in 1993. In 2010, after the Sun acquisition by Oracle, it was renamed Oracle Solaris.
 * <p>
 * Process enumeration, services, and version queries are shared; the native bits ({@code kstat} uptime/boot time,
 * {@code getpid}/{@code thr_self}, and the {@link OSProcess}/{@code FileSystem}/{@code NetworkParams}/{@code OSThread}
 * factories) are provided by the JNA and FFM subclasses.
 */
@ThreadSafe
public abstract class SolarisOperatingSystem extends AbstractOperatingSystem {

    private static final String VERSION;
    private static final String BUILD_NUMBER;
    static {
        String[] split = ParseUtil.whitespaces.split(ExecutingCommand.getFirstAnswer("uname -rv"));
        VERSION = split[0];
        BUILD_NUMBER = split.length > 1 ? split[1] : "";
    }

    @Override
    public String queryManufacturer() {
        return "Oracle";
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        return new Pair<>("SunOS", new OSVersionInfo(VERSION, "Solaris", BUILD_NUMBER));
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness == 64) {
            return 64;
        }
        return ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("isainfo -b"), 32);
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new SolarisInternetProtocolStats();
    }

    @Override
    public OSProcess getProcess(int pid) {
        List<OSProcess> procs = getProcessListFromProcfs(pid);
        if (procs.isEmpty()) {
            return null;
        }
        return procs.get(0);
    }

    @Override
    public List<OSProcess> queryAllProcesses() {
        return getProcessListFromProcfs(-1);
    }

    @Override
    public List<OSProcess> queryChildProcesses(int parentPid) {
        List<OSProcess> allProcs = queryAllProcesses();
        Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, false);
        return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
    }

    @Override
    public List<OSProcess> queryDescendantProcesses(int parentPid) {
        List<OSProcess> allProcs = queryAllProcesses();
        Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, true);
        return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
    }

    private List<OSProcess> getProcessListFromProcfs(int pid) {
        List<OSProcess> procs = new ArrayList<>();

        File[] numericFiles = null;
        if (pid < 0) {
            // If no pid, get process files in proc
            File directory = new File("/proc");
            numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
        } else {
            // If pid specified just find that file
            File pidFile = new File("/proc/" + pid);
            if (pidFile.exists()) {
                numericFiles = new File[1];
                numericFiles[0] = pidFile;
            }
        }
        if (numericFiles == null) {
            return procs;
        }

        // Iterate files
        for (File pidFile : numericFiles) {
            int pidNum = ParseUtil.parseIntOrDefault(pidFile.getName(), 0);
            OSProcess proc = createProcess(pidNum);
            if (proc.getState() != INVALID) {
                procs.add(proc);
            }
        }
        return procs;
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
    public List<OSService> getServices() {
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
        return parseSvcs(svcs, legacySvcs);
    }

    /**
     * Parses the output of {@code svcs -p} to build a list of OS services.
     *
     * @param svcs       the lines emitted by {@code svcs -p}
     * @param legacySvcs the list of legacy service names from {@code /etc/init.d}
     * @return a list of {@link OSService} objects
     */
    static List<OSService> parseSvcs(List<String> svcs, List<String> legacySvcs) {
        List<OSService> services = new ArrayList<>();
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
        return services;
    }

    /**
     * Creates a platform-specific {@link OSProcess} for the given pid.
     *
     * @param pid the process ID
     * @return a new OSProcess
     */
    protected abstract OSProcess createProcess(int pid);
}
