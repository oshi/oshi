/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static oshi.ffm.ForeignFunctions.callInArenaBooleanOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaIntOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.platform.windows.WinNTFFM.PERFORMANCE_INFORMATION;
import static oshi.ffm.platform.windows.WindowsForeignFunctions.readWideString;
import static oshi.ffm.platform.windows.WindowsForeignFunctions.setupTokenPrivileges;
import static oshi.ffm.platform.windows.WindowsForeignFunctions.succeededOrLog;
import static oshi.software.os.OperatingSystem.ProcessFiltering.VALID_PROCESS;
import static oshi.util.LogLevel.ERROR;
import static oshi.util.Memoizer.installedAppsExpiration;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import oshi.driver.common.windows.registry.ProcessPerfCounterBlock;
import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;
import oshi.driver.common.windows.registry.WtsInfo;
import oshi.driver.common.windows.wmi.Win32OperatingSystem.OSVersionProperty;
import oshi.driver.common.windows.wmi.Win32Processor.BitnessProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.driver.windows.registry.HkeyUserDataFFM;
import oshi.driver.windows.registry.NetSessionDataFFM;
import oshi.driver.windows.registry.ProcessPerformanceDataFFM;
import oshi.driver.windows.registry.ProcessWtsDataFFM;
import oshi.driver.windows.registry.SessionWtsDataFFM;
import oshi.driver.windows.registry.ThreadPerformanceDataFFM;
import oshi.driver.windows.wmi.Win32OperatingSystemFFM;
import oshi.driver.windows.wmi.Win32ProcessorFFM;
import oshi.ffm.NativeHandle;
import oshi.ffm.platform.windows.Advapi32FFM;
import oshi.ffm.platform.windows.Kernel32FFM;
import oshi.ffm.platform.windows.PsapiFFM;
import oshi.ffm.platform.windows.WinNTFFM;
import oshi.ffm.util.platform.windows.Advapi32UtilFFM;
import oshi.ffm.util.platform.windows.Kernel32UtilFFM;
import oshi.software.common.os.windows.WindowsOperatingSystem;
import oshi.software.os.ApplicationInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSService.State;
import oshi.software.os.OSSession;
import oshi.software.os.OSThread;
import oshi.util.Memoizer;

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
        return callInArenaBooleanOrDefault(arena -> {
            MemorySegment hTokenPtr = arena.allocate(ADDRESS);

            Optional<MemorySegment> hProcess = Kernel32FFM.GetCurrentProcess();
            if (hProcess.isEmpty()) {
                return false;
            }
            if (!succeededOrLog(
                    Advapi32FFM.OpenProcessToken(hProcess.get(),
                            WinNTFFM.TOKEN_QUERY | WinNTFFM.TOKEN_ADJUST_PRIVILEGES, hTokenPtr),
                    LOG, "OpenProcessToken")) {
                return false;
            }
            try (NativeHandle hToken = NativeHandle.of(hTokenPtr.get(ADDRESS, 0), Kernel32FFM::CloseHandle)) {
                MemorySegment luid = arena.allocate(WinNTFFM.LUID);
                return succeededOrLog(Advapi32FFM.LookupPrivilegeValue("SeDebugPrivilege", luid, arena), LOG,
                        "LookupPrivilegeValue")
                        && succeededOrLog(
                                Advapi32FFM.AdjustTokenPrivileges(hToken.get(), setupTokenPrivileges(arena, luid)), LOG,
                                "AdjustTokenPrivileges");
            }
        }, LOG, ERROR, "enableDebugPrivilege exception", false);
    }

    @Override
    public List<OSSession> getSessions() {
        List<OSSession> whoList = HkeyUserDataFFM.queryUserSessions();
        whoList.addAll(SessionWtsDataFFM.queryUserSessions());
        whoList.addAll(NetSessionDataFFM.queryUserSessions());
        return whoList;
    }

    @Override
    public List<OSService> getServices() {
        return queryServicesFFM();
    }

    private static final int SC_MANAGER_ENUMERATE_SERVICE = 0x0004;
    private static final int SERVICE_WIN32 = 0x30;
    private static final int SERVICE_STATE_ALL = 3;
    // ENUM_SERVICE_STATUS_PROCESSW on 64-bit: 8+8+36=52, padded to 56
    private static final long ENUM_SERVICE_STATUS_PROCESS_SIZE = 56;
    private static final long DISPLAY_NAME_OFFSET = 8;
    private static final long CURRENT_STATE_OFFSET = 20;
    private static final long PROCESS_ID_OFFSET = 44;

    private static List<OSService> queryServicesFFM() {
        return callInArenaOrDefault(arena -> {
            MemorySegment hSCManager = Advapi32FFM.OpenSCManager(MemorySegment.NULL, MemorySegment.NULL,
                    SC_MANAGER_ENUMERATE_SERVICE);
            if (hSCManager == null || hSCManager.address() == 0) {
                LOG.error("Failed to open Service Control Manager");
                return Collections.emptyList();
            }
            // wrapped only to release the SCM handle on close
            try (var _ = NativeHandle.of(hSCManager, Advapi32FFM::CloseServiceHandle)) {
                // First call to get required buffer size
                MemorySegment pcbBytesNeeded = arena.allocate(JAVA_INT);
                MemorySegment lpServicesReturned = arena.allocate(JAVA_INT);
                MemorySegment lpResumeHandle = arena.allocate(JAVA_INT);
                lpResumeHandle.set(JAVA_INT, 0, 0);

                Advapi32FFM.EnumServicesStatusEx(hSCManager, 0, SERVICE_WIN32, SERVICE_STATE_ALL, MemorySegment.NULL, 0,
                        pcbBytesNeeded, lpServicesReturned, lpResumeHandle, MemorySegment.NULL);

                int bytesNeeded = pcbBytesNeeded.get(JAVA_INT, 0);
                if (bytesNeeded == 0) {
                    return Collections.emptyList();
                }

                MemorySegment lpServices = arena.allocate(bytesNeeded);
                lpResumeHandle.set(JAVA_INT, 0, 0);
                if (!succeededOrLog(
                        Advapi32FFM.EnumServicesStatusEx(hSCManager, 0, SERVICE_WIN32, SERVICE_STATE_ALL, lpServices,
                                bytesNeeded, pcbBytesNeeded, lpServicesReturned, lpResumeHandle, MemorySegment.NULL),
                        LOG, "EnumServicesStatusEx")) {
                    return Collections.emptyList();
                }

                int count = lpServicesReturned.get(JAVA_INT, 0);
                List<OSService> svcArray = new ArrayList<>(count);

                for (int i = 0; i < count; i++) {
                    long base = i * ENUM_SERVICE_STATUS_PROCESS_SIZE;
                    MemorySegment pDisplayName = lpServices.get(ADDRESS, base + DISPLAY_NAME_OFFSET).reinterpret(512);
                    String displayName = readWideString(pDisplayName);
                    int currentState = lpServices.get(JAVA_INT, base + CURRENT_STATE_OFFSET);
                    int processId = lpServices.get(JAVA_INT, base + PROCESS_ID_OFFSET);

                    State state;
                    switch (currentState) {
                        case 1:
                            state = State.STOPPED;
                            break;
                        case 4:
                            state = State.RUNNING;
                            break;
                        default:
                            state = State.OTHER;
                            break;
                    }
                    svcArray.add(new OSService(displayName, processId, state));
                }
                return svcArray;
            }
        }, LOG, ERROR, "Error enumerating services", Collections.emptyList());
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
        return callInArenaIntOrDefault(arena -> {
            MemorySegment perfInfo = arena.allocate(PERFORMANCE_INFORMATION);
            int size = (int) PERFORMANCE_INFORMATION.byteSize();
            perfInfo.set(JAVA_INT, PERFORMANCE_INFORMATION.byteOffset(MemoryLayout.PathElement.groupElement("cb")),
                    size);
            if (!succeededOrLog(PsapiFFM.GetPerformanceInfo(perfInfo, size), LOG, "GetPerformanceInfo")) {
                return 0;
            }
            return perfInfo.get(JAVA_INT,
                    PERFORMANCE_INFORMATION.byteOffset(MemoryLayout.PathElement.groupElement(fieldName)));
        }, LOG, ERROR, "Exception getting " + fieldName, 0);
    }

    @Override
    public int getThreadId() {
        return Kernel32FFM.GetCurrentThreadId().orElse(-1);
    }

    @Override
    public OSThread getCurrentThread() {
        final int tid = getThreadId();
        OSProcess proc = getCurrentProcess();
        if (proc == null) {
            return new WindowsOSThreadFFM(0, tid, null, null);
        }
        return proc.getThreadDetails().stream().filter(t -> t.getThreadId() == tid).findFirst()
                .orElseGet(() -> new WindowsOSThreadFFM(proc.getProcessID(), tid, null, null));
    }

    private final Supplier<Map<Integer, ProcessPerfCounterBlock>> processMapFromRegistry = Memoizer
            .memoize(WindowsOperatingSystemFFM::queryProcessMapFromRegistry, Memoizer.defaultExpiration());
    private final Supplier<Map<Integer, ProcessPerfCounterBlock>> processMapFromPerfCounters = Memoizer
            .memoize(WindowsOperatingSystemFFM::queryProcessMapFromPerfCounters, Memoizer.defaultExpiration());
    private final Supplier<Map<Integer, ThreadPerfCounterBlock>> threadMapFromRegistry = Memoizer
            .memoize(WindowsOperatingSystemFFM::queryThreadMapFromRegistry, Memoizer.defaultExpiration());
    private final Supplier<Map<Integer, ThreadPerfCounterBlock>> threadMapFromPerfCounters = Memoizer
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
        Map<Integer, ProcessPerfCounterBlock> processMap = processMapFromRegistry.get();
        // otherwise performance counters with WMI backup
        if (processMap == null || processMap.isEmpty()) {
            processMap = (pids == null) ? processMapFromPerfCounters.get()
                    : ProcessPerformanceDataFFM.buildProcessMapFromPerfCounters(pids);
        }
        Map<Integer, ThreadPerfCounterBlock> threadMap = null;
        if (USE_PROCSTATE_SUSPENDED) {
            // Get data from the registry if possible
            threadMap = threadMapFromRegistry.get();
            // otherwise performance counters with WMI backup
            if (threadMap == null || threadMap.isEmpty()) {
                threadMap = (pids == null) ? threadMapFromPerfCounters.get()
                        : ThreadPerformanceDataFFM.buildThreadMapFromPerfCounters(pids);
            }
        }

        Map<Integer, WtsInfo> processWtsMap = ProcessWtsDataFFM.queryProcessWtsMap(pids);

        // Intersect with the WTS keys whether or not pids were requested. The perf counter map is memoized, so it can
        // name a process that has since exited; WTS is queried fresh and will not. Keying off the WTS map keeps
        // getProcess(pid) from returning a process that getProcesses() would not list.
        Set<Integer> mapKeys = new HashSet<>(processWtsMap.keySet());
        mapKeys.retainAll(processMap.keySet());

        final Map<Integer, ProcessPerfCounterBlock> finalProcessMap = processMap;
        final Map<Integer, ThreadPerfCounterBlock> finalThreadMap = threadMap;
        return mapKeys.stream().parallel()
                .map(pid -> new WindowsOSProcessFFM(pid, this, finalProcessMap, processWtsMap, finalThreadMap))
                .filter(VALID_PROCESS).collect(Collectors.toList());
    }

    private static Map<Integer, ProcessPerfCounterBlock> queryProcessMapFromRegistry() {
        return ProcessPerformanceDataFFM.buildProcessMapFromRegistry(null);
    }

    // Package-private: only reached in production when the registry query fails, so tested directly
    static Map<Integer, ProcessPerfCounterBlock> queryProcessMapFromPerfCounters() {
        return ProcessPerformanceDataFFM.buildProcessMapFromPerfCounters(null);
    }

    private static Map<Integer, ThreadPerfCounterBlock> queryThreadMapFromRegistry() {
        return ThreadPerformanceDataFFM.buildThreadMapFromRegistry(null);
    }

    // Package-private: only reached in production when the registry query fails, so tested directly
    static Map<Integer, ThreadPerfCounterBlock> queryThreadMapFromPerfCounters() {
        return ThreadPerformanceDataFFM.buildThreadMapFromPerfCounters(null);
    }

    @Override
    protected WmiResult<OSVersionProperty> queryOsVersion() {
        return Win32OperatingSystemFFM.queryOsVersion();
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness < 64 && System.getenv("ProgramFiles(x86)") != null) {
            WmiResult<BitnessProperty> bitnessMap = Win32ProcessorFFM.queryBitness();
            if (bitnessMap.getResultCount() > 0) {
                return WmiUtil.getUint16(bitnessMap, BitnessProperty.ADDRESSWIDTH, 0);
            }
        }
        return jvmBitness;
    }

    @Override
    protected List<OSProcess> queryChildProcesses(int parentPid) {
        Map<Integer, Integer> parentPidMap = getParentPidMap();
        Set<Integer> descendantPids = getChildrenOrDescendants(parentPidMap, parentPid, false);
        return processMapToList(descendantPids);
    }

    @Override
    protected List<OSProcess> queryDescendantProcesses(int parentPid) {
        Map<Integer, Integer> parentPidMap = getParentPidMap();
        Set<Integer> descendantPids = getChildrenOrDescendants(parentPidMap, parentPid, true);
        return processMapToList(descendantPids);
    }

    private Map<Integer, Integer> getParentPidMap() {
        Map<Integer, Integer> parentPidMap = new HashMap<>();
        Map<Integer, ProcessPerfCounterBlock> processMap = processMapFromRegistry.get();
        if (processMap == null || processMap.isEmpty()) {
            processMap = processMapFromPerfCounters.get();
        }
        if (processMap != null) {
            for (Map.Entry<Integer, ProcessPerfCounterBlock> entry : processMap.entrySet()) {
                parentPidMap.put(entry.getKey(), entry.getValue().getParentProcessID());
            }
        }
        return parentPidMap;
    }

    private static final boolean X86 = isCurrentX86();

    static boolean isX86() {
        return X86;
    }

    private static boolean isCurrentX86() {
        // Query the true machine architecture via GetNativeSystemInfo, matching the JNA backend. The
        // PROCESSOR_ARCHITECTURE environment variable reflects the process (WOW64) architecture and would
        // misreport a 32-bit JVM on 64-bit Windows as x86.
        try (Arena arena = Arena.ofConfined()) {
            // SYSTEM_INFO is 48 bytes on x64; wProcessorArchitecture is a WORD at offset 0.
            MemorySegment sysInfo = arena.allocate(48);
            if (Kernel32FFM.GetNativeSystemInfo(sysInfo)) {
                // PROCESSOR_ARCHITECTURE_INTEL == 0
                return Short.toUnsignedInt(sysInfo.get(JAVA_SHORT, 0)) == 0;
            }
        }
        return false;
    }
}
