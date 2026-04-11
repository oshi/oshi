/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.windows.Kernel32FFM.GetLastError;
import static oshi.ffm.windows.WinNTFFM.PERFORMANCE_INFORMATION;
import static oshi.ffm.windows.WindowsForeignFunctions.setupTokenPrivileges;
import static oshi.software.os.OperatingSystem.ProcessFiltering.VALID_PROCESS;
import static oshi.util.Memoizer.installedAppsExpiration;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.registry.ProcessPerformanceData;
import oshi.driver.windows.registry.ProcessWtsData;
import oshi.driver.windows.registry.ProcessWtsData.WtsInfo;
import oshi.driver.windows.registry.ThreadPerformanceData;
import oshi.ffm.windows.Advapi32FFM;
import oshi.ffm.windows.Kernel32FFM;
import oshi.ffm.windows.PsapiFFM;
import oshi.ffm.windows.WinNTFFM;
import oshi.software.os.ApplicationInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.util.GlobalConfig;
import oshi.util.Memoizer;
import oshi.util.platform.windows.Advapi32UtilFFM;
import oshi.util.platform.windows.Kernel32UtilFFM;

@ThreadSafe
public class WindowsOperatingSystemFFM extends WindowsOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOperatingSystemFFM.class);

    private static final long BOOTTIME = Advapi32UtilFFM.querySystemBootTime();

    private final Supplier<List<ApplicationInfo>> installedAppsSupplier = Memoizer
            .memoize(WindowsInstalledAppsFFM::queryInstalledApps, installedAppsExpiration());

    static {
        enableDebugPrivilege();
    }

    private static boolean enableDebugPrivilege() {

        MemorySegment hToken = null;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment hTokenPtr = arena.allocate(ADDRESS);

            Optional<MemorySegment> hProcess = Kernel32FFM.GetCurrentProcess();
            if (hProcess.isEmpty()) {
                return false;
            }
            boolean success = Advapi32FFM.OpenProcessToken(hProcess.get(),
                    WinNTFFM.TOKEN_QUERY | WinNTFFM.TOKEN_ADJUST_PRIVILEGES, hTokenPtr);
            if (!success) {
                LOG.error("OpenProcessToken failed, error: {}", GetLastError());
                return false;
            }
            hToken = hTokenPtr.get(ADDRESS, 0);

            MemorySegment luid = arena.allocate(WinNTFFM.LUID);
            success = Advapi32FFM.LookupPrivilegeValue("SeDebugPrivilege", luid, arena);
            if (!success) {
                LOG.error("LookupPrivilegeValue failed, error: {}", GetLastError());
                return false;
            }

            MemorySegment tkp = setupTokenPrivileges(arena, luid);
            success = Advapi32FFM.AdjustTokenPrivileges(hToken, tkp);
            if (!success) {
                LOG.error("AdjustTokenPrivileges failed, error: {}", GetLastError());
                return false;
            }

            return true;
        } catch (Throwable t) {
            LOG.error("enableDebugPrivilege exception: {}", t.getMessage());
            return false;
        } finally {
            if (hToken != null && hToken.address() != 0) {
                Kernel32FFM.CloseHandle(hToken);
            }
        }
    }

    @Override
    public List<ApplicationInfo> getInstalledApplications() {
        return installedAppsSupplier.get();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new WindowsInternetProtocolStatsFFM();
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new WindowsNetworkParamsFFM();
    }

    @Override
    public boolean isElevated() {
        return Advapi32UtilFFM.isCurrentProcessElevated();
    }

    @Override
    public FileSystem getFileSystem() {
        return new WindowsFileSystemFFM();
    }

    @Override
    public int getProcessId() {
        return Kernel32FFM.GetCurrentProcessId().orElse(-1);
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    @Override
    public long getSystemUptime() {
        return Kernel32UtilFFM.querySystemUptime();
    }

    @Override
    public int getProcessCount() {
        return getPerformanceInfoField("ProcessCount");
    }

    @Override
    public int getThreadCount() {
        return getPerformanceInfoField("ThreadCount");
    }

    private int getPerformanceInfoField(String fieldName) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment perfInfo = arena.allocate(PERFORMANCE_INFORMATION);
            int size = (int) PERFORMANCE_INFORMATION.byteSize();
            perfInfo.set(JAVA_INT, PERFORMANCE_INFORMATION.byteOffset(MemoryLayout.PathElement.groupElement("cb")),
                    size);
            if (!PsapiFFM.GetPerformanceInfo(perfInfo, size)) {
                LOG.error("Failed to get Performance Info. Error code: {}", GetLastError());
                return 0;
            }
            return perfInfo.get(JAVA_INT,
                    PERFORMANCE_INFORMATION.byteOffset(MemoryLayout.PathElement.groupElement(fieldName)));
        } catch (Throwable t) {
            LOG.error("Exception getting {}", fieldName, t);
            return 0;
        }
    }

    @Override
    public int getThreadId() {
        return Kernel32FFM.GetCurrentThreadId().orElse(-1);
    }

    private static final boolean USE_PROCSTATE_SUSPENDED = GlobalConfig
            .get(GlobalConfig.OSHI_OS_WINDOWS_PROCSTATE_SUSPENDED, false);

    private final Supplier<Map<Integer, ProcessPerformanceData.PerfCounterBlock>> processMapFromRegistry = Memoizer
            .memoize(WindowsOperatingSystemFFM::queryProcessMapFromRegistry, Memoizer.defaultExpiration());
    private final Supplier<Map<Integer, ProcessPerformanceData.PerfCounterBlock>> processMapFromPerfCounters = Memoizer
            .memoize(WindowsOperatingSystemFFM::queryProcessMapFromPerfCounters, Memoizer.defaultExpiration());
    private final Supplier<Map<Integer, ThreadPerformanceData.PerfCounterBlock>> threadMapFromRegistry = Memoizer
            .memoize(WindowsOperatingSystemFFM::queryThreadMapFromRegistry, Memoizer.defaultExpiration());
    private final Supplier<Map<Integer, ThreadPerformanceData.PerfCounterBlock>> threadMapFromPerfCounters = Memoizer
            .memoize(WindowsOperatingSystemFFM::queryThreadMapFromPerfCounters, Memoizer.defaultExpiration());

    @Override
    public List<OSProcess> getProcesses(Collection<Integer> pids) {
        return processMapToList(pids);
    }

    @Override
    public List<OSProcess> queryAllProcesses() {
        return processMapToList(null);
    }

    @Override
    public OSProcess getProcess(int pid) {
        List<OSProcess> procList = processMapToList(List.of(pid));
        return procList.isEmpty() ? null : procList.get(0);
    }

    private List<OSProcess> processMapToList(Collection<Integer> pids) {
        // Get data from the registry if possible
        Map<Integer, ProcessPerformanceData.PerfCounterBlock> processMap = processMapFromRegistry.get();
        // otherwise performance counters with WMI backup
        if (processMap == null || processMap.isEmpty()) {
            processMap = (pids == null) ? processMapFromPerfCounters.get()
                    : ProcessPerformanceData.buildProcessMapFromPerfCounters(pids);
        }
        Map<Integer, ThreadPerformanceData.PerfCounterBlock> threadMap = null;
        if (USE_PROCSTATE_SUSPENDED) {
            // Get data from the registry if possible
            threadMap = threadMapFromRegistry.get();
            // otherwise performance counters with WMI backup
            if (threadMap == null || threadMap.isEmpty()) {
                threadMap = (pids == null) ? threadMapFromPerfCounters.get()
                        : ThreadPerformanceData.buildThreadMapFromPerfCounters(pids);
            }
        }

        Map<Integer, WtsInfo> processWtsMap = ProcessWtsData.queryProcessWtsMap(pids);

        // Determine which PIDs to process: requested pids filtered by available data, or all available
        Set<Integer> mapKeys;
        if (pids == null) {
            // All processes: use intersection of WTS and processMap (WTS enrichment required for full data)
            mapKeys = new HashSet<>(processWtsMap.keySet());
            mapKeys.retainAll(processMap.keySet());
        } else {
            // Specific PIDs: filter by what's available in processMap
            mapKeys = new HashSet<>(pids);
            mapKeys.retainAll(processMap.keySet());
        }

        final Map<Integer, ProcessPerformanceData.PerfCounterBlock> finalProcessMap = processMap;
        final Map<Integer, ThreadPerformanceData.PerfCounterBlock> finalThreadMap = threadMap;
        return mapKeys.stream().parallel()
                .map(pid -> new WindowsOSProcessFFM(pid, this, finalProcessMap, processWtsMap, finalThreadMap))
                .filter(VALID_PROCESS).collect(Collectors.toList());
    }

    private static Map<Integer, ProcessPerformanceData.PerfCounterBlock> queryProcessMapFromRegistry() {
        return ProcessPerformanceData.buildProcessMapFromRegistry(null);
    }

    private static Map<Integer, ProcessPerformanceData.PerfCounterBlock> queryProcessMapFromPerfCounters() {
        return ProcessPerformanceData.buildProcessMapFromPerfCounters(null);
    }

    private static Map<Integer, ThreadPerformanceData.PerfCounterBlock> queryThreadMapFromRegistry() {
        return ThreadPerformanceData.buildThreadMapFromRegistry(null);
    }

    private static Map<Integer, ThreadPerformanceData.PerfCounterBlock> queryThreadMapFromPerfCounters() {
        return ThreadPerformanceData.buildThreadMapFromPerfCounters(null);
    }
}
