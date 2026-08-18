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
import static oshi.util.LogLevel.ERROR;
import static oshi.util.Memoizer.installedAppsExpiration;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.registry.ProcessPerfCounterBlock;
import oshi.driver.common.windows.registry.ProcessPerformanceData;
import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;
import oshi.driver.common.windows.registry.ThreadPerformanceData;
import oshi.driver.common.windows.registry.WtsInfo;
import oshi.driver.common.windows.wmi.Win32OperatingSystem.OSVersionProperty;
import oshi.driver.common.windows.wmi.Win32Processor.BitnessProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.driver.windows.perfmon.PerfCounterQueryExecutorFFM;
import oshi.driver.windows.registry.HkeyUserDataFFM;
import oshi.driver.windows.registry.NetSessionDataFFM;
import oshi.driver.windows.registry.ProcessWtsDataFFM;
import oshi.driver.windows.registry.SessionWtsDataFFM;
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

                    State state = switch (currentState) {
                        case 1 -> State.STOPPED;
                        case 4 -> State.RUNNING;
                        default -> State.OTHER;
                    };
                    svcArray.add(new OSService(displayName, processId, state));
                }
                return Collections.unmodifiableList(svcArray);
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
        return proc.getThreadDetails().stream().filter(t -> t.getThreadId() == tid).findFirst()
                .orElseGet(() -> new WindowsOSThreadFFM(proc.getProcessID(), tid, null, null));
    }

    @Override
    protected @Nullable Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromRegistry(
            @Nullable Collection<Integer> pids) {
        return ProcessPerformanceData.buildProcessMapFromRegistry(PerfCounterQueryExecutorFFM.INSTANCE, pids);
    }

    @Override
    protected @Nullable Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromPerfCounters(
            @Nullable Collection<Integer> pids) {
        return ProcessPerformanceData.buildProcessMapFromPerfCounters(PerfCounterQueryExecutorFFM.INSTANCE, pids);
    }

    @Override
    protected @Nullable Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromRegistry(
            @Nullable Collection<Integer> pids) {
        return ThreadPerformanceData.buildThreadMapFromRegistry(PerfCounterQueryExecutorFFM.INSTANCE, pids);
    }

    @Override
    protected @Nullable Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromPerfCounters(
            @Nullable Collection<Integer> pids) {
        return ThreadPerformanceData.buildThreadMapFromPerfCounters(PerfCounterQueryExecutorFFM.INSTANCE, pids);
    }

    @Override
    protected Map<Integer, WtsInfo> queryProcessWtsMap(@Nullable Collection<Integer> pids) {
        return ProcessWtsDataFFM.queryProcessWtsMap(pids);
    }

    @Override
    protected OSProcess createOSProcess(int pid, Map<Integer, ProcessPerfCounterBlock> processMap,
            Map<Integer, WtsInfo> processWtsMap, @Nullable Map<Integer, ThreadPerfCounterBlock> threadMap) {
        return new WindowsOSProcessFFM(pid, this, processMap, processWtsMap, threadMap);
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

    /**
     * Unlike the JNA backend, which takes a live ToolHelp32 snapshot, this derives each process's parent from the
     * cached performance counter data rather than enumerating every process a second time.
     */
    @Override
    protected Map<Integer, Integer> queryParentPidMap() {
        Map<Integer, Integer> parentPidMap = new HashMap<>();
        for (Map.Entry<Integer, ProcessPerfCounterBlock> entry : getCachedProcessMap().entrySet()) {
            parentPidMap.put(entry.getKey(), entry.getValue().getParentProcessID());
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
