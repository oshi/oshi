/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.bsd;

import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Abstract base shared by the BSD-family OperatingSystem implementations (FreeBSD, OpenBSD, DragonFly, NetBSD). Holds
 * the cross-BSD-identical pieces: the {@code Unix/BSD} manufacturer, the {@code uname}-based bitness, the
 * process/child/descendant queries, the {@code ps -axo pid} process count, the {@code ps -axo nlwp} thread count, the
 * {@code /etc/rc.d} services, and the memoized system uptime/boot time. Per-platform work (the {@code ps} process
 * enumeration, the boot-time source, the current-thread factory, and the native version/file-system/network queries) is
 * provided by the subclasses.
 */
@ThreadSafe
public abstract class BsdOperatingSystem extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(BsdOperatingSystem.class);

    private final Supplier<Long> bootTime = memoize(this::queryBootTime);

    @Override
    public String queryManufacturer() {
        return "Unix/BSD";
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness < 64 && ExecutingCommand.getFirstAnswer("uname -m").indexOf("64") == -1) {
            return jvmBitness;
        }
        return 64;
    }

    @Override
    public List<OSProcess> queryAllProcesses() {
        return getProcessListFromPS(-1);
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

    @Override
    public OSProcess getProcess(int pid) {
        if (pid < 0) {
            // A negative pid is the getProcessListFromPS "all processes" sentinel, not a real process
            return null;
        }
        List<OSProcess> procs = getProcessListFromPS(pid);
        if (procs.isEmpty()) {
            return null;
        }
        return procs.get(0);
    }

    @Override
    public int getProcessCount() {
        List<String> procList = ExecutingCommand.runNative("ps -axo pid");
        if (!procList.isEmpty()) {
            // Subtract 1 for header
            return procList.size() - 1;
        }
        return 0;
    }

    @Override
    public int getThreadCount() {
        // Sum nlwp (number of LWPs) across all processes; the header row parses to 0
        int threads = 0;
        for (String proc : ExecutingCommand.runNative("ps -axo nlwp")) {
            threads += ParseUtil.parseIntOrDefault(proc.trim(), 0);
        }
        return threads;
    }

    @Override
    public long getSystemUptime() {
        return System.currentTimeMillis() / 1000 - getSystemBootTime();
    }

    @Override
    public long getSystemBootTime() {
        return bootTime.get();
    }

    @Override
    public List<OSService> getServices() {
        // Get running services
        List<OSService> services = new ArrayList<>();
        Set<String> running = new HashSet<>();
        for (OSProcess p : getChildProcesses(1, ProcessFiltering.ALL_PROCESSES, ProcessSorting.PID_ASC, 0)) {
            OSService s = new OSService(p.getName(), p.getProcessID(), RUNNING);
            services.add(s);
            running.add(p.getName());
        }
        // Get Directories for stopped services
        File dir = new File("/etc/rc.d");
        File[] listFiles;
        if (dir.exists() && dir.isDirectory() && (listFiles = dir.listFiles()) != null) {
            for (File f : listFiles) {
                String name = f.getName();
                if (!running.contains(name)) {
                    OSService s = new OSService(name, 0, STOPPED);
                    services.add(s);
                }
            }
        } else {
            LOG.error("Directory: /etc/rc.d does not exist");
        }
        return services;
    }

    /**
     * Builds the family/version info from the raw {@code kern.ostype}/{@code kern.osrelease}/{@code kern.version}
     * sysctl values. Shared by the FreeBSD, DragonFly, and OpenBSD subclasses, which differ only in how those three
     * strings are fetched (string-named vs. MIB-based sysctl).
     *
     * @param family      the {@code kern.ostype} value (e.g. {@code "FreeBSD"})
     * @param version     the {@code kern.osrelease} value
     * @param versionInfo the {@code kern.version} value
     * @return the family name paired with the parsed version info
     */
    protected static Pair<String, OSVersionInfo> buildFamilyVersionInfo(String family, String version,
            String versionInfo) {
        String buildNumber = versionInfo.split(":")[0].replace(family, "").replace(version, "").trim();
        return new Pair<>(family, new OSVersionInfo(version, null, buildNumber));
    }

    /**
     * Parses the boot time (seconds since the epoch) from the {@code sysctl -n kern.boottime} command output. Used as a
     * fallback by the BSD subclasses when the native {@code kern.boottime} sysctl is unavailable.
     *
     * @return the boot time in seconds, or the current time if it can't be parsed
     */
    protected static long queryBootTimeFromCommand() {
        // Boot time will be the first consecutive string of digits.
        return ParseUtil.parseLongOrDefault(
                ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                System.currentTimeMillis() / 1000);
    }

    /**
     * Enumerates processes (or a single process when {@code pid >= 0}) via the platform's {@code ps} command.
     *
     * @param pid the process ID, or {@code -1} for all processes
     * @return the process list
     */
    protected abstract List<OSProcess> getProcessListFromPS(int pid);

    /**
     * Queries the system boot time in seconds since the epoch.
     *
     * @return the boot time in seconds
     */
    protected abstract long queryBootTime();
}
