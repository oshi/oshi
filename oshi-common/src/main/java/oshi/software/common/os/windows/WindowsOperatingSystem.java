/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.windows;

import static oshi.software.os.OperatingSystem.ProcessFiltering.VALID_PROCESS;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.registry.ProcessPerfCounterBlock;
import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;
import oshi.driver.common.windows.registry.WtsInfo;
import oshi.driver.common.windows.wmi.Win32OperatingSystem.OSVersionProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.OSProcess;
import oshi.util.Constants;
import oshi.util.GlobalConfig;
import oshi.util.tuples.Pair;

/**
 * Common base class for Windows operating system implementations.
 */
@ThreadSafe
public abstract class WindowsOperatingSystem extends AbstractOperatingSystem {

    /**
     * Default constructor.
     */
    protected WindowsOperatingSystem() {
    }

    /** Whether to check thread states to determine if a process is suspended. */
    protected static final boolean USE_PROCSTATE_SUSPENDED = GlobalConfig
            .get(GlobalConfig.OSHI_OS_WINDOWS_PROCSTATE_SUSPENDED, false);

    /*
     * Cache full process stats queries. The second query only populates if the first one returns nothing.
     */
    private final Supplier<Map<Integer, ProcessPerfCounterBlock>> processMapFromRegistry = memoize(
            () -> buildProcessMapFromRegistry(null), defaultExpiration());
    private final Supplier<Map<Integer, ProcessPerfCounterBlock>> processMapFromPerfCounters = memoize(
            () -> buildProcessMapFromPerfCounters(null), defaultExpiration());
    /*
     * Cache full thread stats queries. Only used if USE_PROCSTATE_SUSPENDED is set true.
     */
    private final Supplier<Map<Integer, ThreadPerfCounterBlock>> threadMapFromRegistry = memoize(
            () -> buildThreadMapFromRegistry(null), defaultExpiration());
    private final Supplier<Map<Integer, ThreadPerfCounterBlock>> threadMapFromPerfCounters = memoize(
            () -> buildThreadMapFromPerfCounters(null), defaultExpiration());

    /**
     * Reads process performance data from {@code HKEY_PERFORMANCE_DATA}.
     *
     * @param pids An optional collection of process IDs to filter the results to, or {@code null} for all processes
     * @return A map of process ID to performance counter block, or {@code null} if the registry data is unavailable
     */
    protected abstract Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromRegistry(
            @Nullable Collection<Integer> pids);

    /**
     * Reads process performance data from performance counters, with a WMI backup.
     *
     * @param pids An optional collection of process IDs to filter the results to, or {@code null} for all processes
     * @return A map of process ID to performance counter block
     */
    protected abstract Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromPerfCounters(
            @Nullable Collection<Integer> pids);

    /**
     * Reads thread performance data from {@code HKEY_PERFORMANCE_DATA}.
     *
     * @param pids An optional collection of process IDs to filter the results to, or {@code null} for all processes
     * @return A map of thread ID to performance counter block, or {@code null} if the registry data is unavailable
     */
    protected abstract Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromRegistry(
            @Nullable Collection<Integer> pids);

    /**
     * Reads thread performance data from performance counters, with a WMI backup.
     *
     * @param pids An optional collection of process IDs to filter the results to, or {@code null} for all processes
     * @return A map of thread ID to performance counter block
     */
    protected abstract Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromPerfCounters(
            @Nullable Collection<Integer> pids);

    /**
     * Reads process information from the Windows Terminal Service, with a WMI backup.
     *
     * @param pids An optional collection of process IDs to filter the results to, or {@code null} for all processes
     * @return A map of process ID to terminal service information
     */
    protected abstract Map<Integer, WtsInfo> queryProcessWtsMap(@Nullable Collection<Integer> pids);

    /**
     * Maps every process ID to the ID of its parent.
     *
     * @return A map of process ID to parent process ID
     */
    protected abstract Map<Integer, Integer> queryParentPidMap();

    /**
     * Creates a backend-specific process object from data already fetched for the whole process list.
     *
     * @param pid           The process ID
     * @param processMap    A map of process ID to performance counter block
     * @param processWtsMap A map of process ID to terminal service information
     * @param threadMap     A map of thread ID to performance counter block, or {@code null} if thread states are not
     *                      being collected
     * @return The process
     */
    protected abstract OSProcess createOSProcess(int pid, Map<Integer, ProcessPerfCounterBlock> processMap,
            Map<Integer, WtsInfo> processWtsMap, @Nullable Map<Integer, ThreadPerfCounterBlock> threadMap);

    @Override
    public List<OSProcess> getProcesses(@Nullable Collection<Integer> pids) {
        return processMapToList(pids);
    }

    @Override
    protected List<OSProcess> queryAllProcesses() {
        return processMapToList(null);
    }

    @Override
    public @Nullable OSProcess getProcess(int pid) {
        List<OSProcess> procList = processMapToList(Collections.singletonList(pid));
        return procList.isEmpty() ? null : procList.get(0);
    }

    @Override
    protected List<OSProcess> queryChildProcesses(int parentPid) {
        return processMapToList(getChildrenOrDescendants(queryParentPidMap(), parentPid, false));
    }

    @Override
    protected List<OSProcess> queryDescendantProcesses(int parentPid) {
        return processMapToList(getChildrenOrDescendants(queryParentPidMap(), parentPid, true));
    }

    /**
     * Fetches the cached performance data for all processes, from the registry if it is available and from performance
     * counters otherwise.
     *
     * @return A map of process ID to performance counter block, empty if neither source returned data
     */
    protected Map<Integer, ProcessPerfCounterBlock> getCachedProcessMap() {
        return resolveProcessMap(null);
    }

    private Map<Integer, ProcessPerfCounterBlock> resolveProcessMap(@Nullable Collection<Integer> pids) {
        // Get data from the registry if possible
        Map<Integer, ProcessPerfCounterBlock> processMap = processMapFromRegistry.get();
        // otherwise performance counters with WMI backup
        if (processMap == null || processMap.isEmpty()) {
            processMap = (pids == null) ? processMapFromPerfCounters.get() : buildProcessMapFromPerfCounters(pids);
        }
        return processMap == null ? Collections.emptyMap() : processMap;
    }

    private List<OSProcess> processMapToList(@Nullable Collection<Integer> pids) {
        Map<Integer, ProcessPerfCounterBlock> processMap = resolveProcessMap(pids);
        Map<Integer, ThreadPerfCounterBlock> threadMap = null;
        if (USE_PROCSTATE_SUSPENDED) {
            // Get data from the registry if possible
            threadMap = threadMapFromRegistry.get();
            // otherwise performance counters with WMI backup
            if (threadMap == null || threadMap.isEmpty()) {
                threadMap = (pids == null) ? threadMapFromPerfCounters.get() : buildThreadMapFromPerfCounters(pids);
            }
        }

        Map<Integer, WtsInfo> processWtsMap = queryProcessWtsMap(pids);

        // Intersect with the WTS keys whether or not pids were requested. The perf counter map is memoized, so it can
        // name a process that has since exited; WTS is queried fresh and will not. Keying off the WTS map keeps
        // getProcess(pid) from returning a process that getProcesses() would not list.
        Set<Integer> mapKeys = new HashSet<>(processWtsMap.keySet());
        mapKeys.retainAll(processMap.keySet());

        final Map<Integer, ProcessPerfCounterBlock> finalProcessMap = processMap;
        final Map<Integer, ThreadPerfCounterBlock> finalThreadMap = threadMap;
        return mapKeys.stream().parallel()
                .map(pid -> createOSProcess(pid, finalProcessMap, processWtsMap, finalThreadMap)).filter(VALID_PROCESS)
                .collect(Collectors.toList());
    }

    @Override
    protected String queryManufacturer() {
        return "Microsoft";
    }

    /**
     * Queries WMI for the operating system version, service pack, suite mask, and build number.
     *
     * @return The {@code Win32_OperatingSystem} query result
     */
    protected abstract WmiResult<OSVersionProperty> queryOsVersion();

    @Override
    protected Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        String servicePack = "";
        int suiteMask = 0;
        String buildNumber = "";
        WmiResult<OSVersionProperty> versionInfo = queryOsVersion();
        if (versionInfo.getResultCount() > 0) {
            servicePack = WmiUtil.getString(versionInfo, OSVersionProperty.CSDVERSION, 0);
            suiteMask = WmiUtil.getUint32(versionInfo, OSVersionProperty.SUITEMASK, 0);
            buildNumber = WmiUtil.getString(versionInfo, OSVersionProperty.BUILDNUMBER, 0);
        }
        return parseVersionInfo(System.getProperty("os.name"), servicePack, suiteMask, buildNumber);
    }

    /**
     * Assembles the family and version information from the raw values reported by the JDK and by WMI.
     *
     * @param osName      The value of the {@code os.name} system property
     * @param servicePack The service pack name, empty or {@link Constants#UNKNOWN} if none
     * @param suiteMask   The suite mask bitmask
     * @param buildNumber The build number reported by WMI
     * @return A pair of the family name and the version information
     */
    static Pair<String, OSVersionInfo> parseVersionInfo(String osName, String servicePack, int suiteMask,
            String buildNumber) {
        String version = osName.startsWith("Windows ") ? osName.substring(8) : osName;
        if (!servicePack.isEmpty() && !Constants.UNKNOWN.equals(servicePack)) {
            version = version + " " + servicePack.replace("Service Pack ", "SP");
        }
        return new Pair<>("Windows",
                new OSVersionInfo(resolveVersionAlias(version, buildNumber), parseCodeName(suiteMask), buildNumber));
    }

    /**
     * Maps the version name reported by the JDK to the name of the release that actually shipped with the given build
     * number. Older JDKs predate Windows 11 and the Server releases after 2016, so {@code os.name} reports them under
     * the name of the last release the JDK knew about.
     *
     * @param version     The version name derived from {@code os.name}
     * @param buildNumber The build number reported by WMI
     * @return The version name of the release matching {@code buildNumber}
     */
    static String resolveVersionAlias(String version, String buildNumber) {
        if ("10".equals(version) && buildNumber.compareTo("22000") >= 0) {
            return "11";
        }
        if ("Server 2016".equals(version) && buildNumber.compareTo("17762") > 0) {
            version = "Server 2019";
        }
        if ("Server 2019".equals(version) && buildNumber.compareTo("20347") > 0) {
            version = "Server 2022";
        }
        if ("Server 2022".equals(version) && buildNumber.compareTo("26039") > 0) {
            version = "Server 2025";
        }
        return version;
    }

    /**
     * Gets suites available on the system and return as a codename.
     *
     * @param suiteMask The suite mask bitmask
     * @return Suites
     */
    protected static String parseCodeName(int suiteMask) {
        List<String> suites = new ArrayList<>();
        if ((suiteMask & 0x00000002) != 0) {
            suites.add("Enterprise");
        }
        if ((suiteMask & 0x00000004) != 0) {
            suites.add("BackOffice");
        }
        if ((suiteMask & 0x00000008) != 0) {
            suites.add("Communications Server");
        }
        if ((suiteMask & 0x00000080) != 0) {
            suites.add("Datacenter");
        }
        if ((suiteMask & 0x00000200) != 0) {
            suites.add("Home");
        }
        if ((suiteMask & 0x00000400) != 0) {
            suites.add("Web Server");
        }
        if ((suiteMask & 0x00002000) != 0) {
            suites.add("Storage Server");
        }
        if ((suiteMask & 0x00004000) != 0) {
            suites.add("Compute Cluster");
        }
        if ((suiteMask & 0x00008000) != 0) {
            suites.add("Home Server");
        }
        return String.join(",", suites);
    }
}
