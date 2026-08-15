/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import static oshi.software.os.OSService.State.OTHER;
import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;
import static oshi.util.Memoizer.installedAppsExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Advapi32Util.EventLogIterator;
import com.sun.jna.platform.win32.Advapi32Util.EventLogRecord;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.W32ServiceManager;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.LUID;
import com.sun.jna.platform.win32.Winsvc;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.registry.ProcessPerfCounterBlock;
import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;
import oshi.driver.common.windows.registry.WtsInfo;
import oshi.driver.common.windows.wmi.Win32OperatingSystem.OSVersionProperty;
import oshi.driver.common.windows.wmi.Win32Processor.BitnessProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.driver.windows.EnumWindows;
import oshi.driver.windows.registry.HkeyUserData;
import oshi.driver.windows.registry.NetSessionData;
import oshi.driver.windows.registry.ProcessPerformanceDataJNA;
import oshi.driver.windows.registry.ProcessWtsData;
import oshi.driver.windows.registry.SessionWtsData;
import oshi.driver.windows.registry.ThreadPerformanceDataJNA;
import oshi.driver.windows.wmi.Win32OperatingSystemJNA;
import oshi.driver.windows.wmi.Win32ProcessorJNA;
import oshi.jna.ByRef.CloseableHANDLEByReference;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.ByRef.CloseablePROCESSENTRY32ByReference;
import oshi.jna.Struct.CloseablePerformanceInformation;
import oshi.jna.Struct.CloseableSystemInfo;
import oshi.software.common.os.windows.WindowsOperatingSystem;
import oshi.software.os.ApplicationInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSDesktopWindow;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSService.State;
import oshi.software.os.OSSession;
import oshi.software.os.OSThread;
import oshi.util.GlobalConfig;
import oshi.util.Memoizer;

/**
 * Microsoft Windows, commonly referred to as Windows, is a group of several proprietary graphical operating system
 * families, all of which are developed and marketed by Microsoft.
 */
@ThreadSafe
public class WindowsOperatingSystemJNA extends WindowsOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOperatingSystemJNA.class);

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

    /*
     * Windows event log name
     */
    private static final Supplier<String> SYSTEM_LOG = memoize(WindowsOperatingSystemJNA::querySystemLog,
            TimeUnit.HOURS.toNanos(1));

    private static final long BOOTTIME = querySystemBootTime();

    static {
        enableDebugPrivilege();
    }

    /*
     * OSProcess code will need to know bitness of current process
     */
    private static final boolean X86 = isCurrentX86();
    private static final boolean WOW = isCurrentWow();

    private final Supplier<List<ApplicationInfo>> installedAppsSupplier = Memoizer
            .memoize(WindowsInstalledAppsJNA::queryInstalledApps, installedAppsExpiration());

    @Override
    protected Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromRegistry(@Nullable Collection<Integer> pids) {
        return ProcessPerformanceDataJNA.buildProcessMapFromRegistry(pids);
    }

    @Override
    protected Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromPerfCounters(
            @Nullable Collection<Integer> pids) {
        return ProcessPerformanceDataJNA.buildProcessMapFromPerfCounters(pids);
    }

    @Override
    protected Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromRegistry(@Nullable Collection<Integer> pids) {
        return ThreadPerformanceDataJNA.buildThreadMapFromRegistry(pids);
    }

    @Override
    protected Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromPerfCounters(@Nullable Collection<Integer> pids) {
        return ThreadPerformanceDataJNA.buildThreadMapFromPerfCounters(pids);
    }

    @Override
    protected Map<Integer, WtsInfo> queryProcessWtsMap(@Nullable Collection<Integer> pids) {
        return ProcessWtsData.queryProcessWtsMap(pids);
    }

    @Override
    protected OSProcess createOSProcess(int pid, Map<Integer, ProcessPerfCounterBlock> processMap,
            Map<Integer, WtsInfo> processWtsMap, @Nullable Map<Integer, ThreadPerfCounterBlock> threadMap) {
        return new WindowsOSProcessJNA(pid, this, processMap, processWtsMap, threadMap);
    }

    @Override
    protected WmiResult<OSVersionProperty> queryOsVersion() {
        return Win32OperatingSystemJNA.queryOsVersion();
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness < 64 && System.getenv("ProgramFiles(x86)") != null && IS_VISTA_OR_GREATER) {
            WmiResult<BitnessProperty> bitnessMap = Win32ProcessorJNA.queryBitness();
            if (bitnessMap.getResultCount() > 0) {
                return WmiUtil.getUint16(bitnessMap, BitnessProperty.ADDRESSWIDTH, 0);
            }
        }
        return jvmBitness;
    }

    @Override
    public boolean isElevated() {
        return Advapi32Util.isCurrentProcessElevated();
    }

    @Override
    public FileSystem getFileSystem() {
        return new WindowsFileSystemJNA();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new WindowsInternetProtocolStatsJNA();
    }

    @Override
    public List<OSSession> getSessions() {
        List<OSSession> whoList = HkeyUserData.queryUserSessions();
        whoList.addAll(SessionWtsData.queryUserSessions());
        whoList.addAll(NetSessionData.queryUserSessions());
        return whoList;
    }

    @Override
    protected Map<Integer, Integer> queryParentPidMap() {
        Map<Integer, Integer> parentPidMap = new HashMap<>();
        // Get processes from ToolHelp API for parent PID
        try (CloseablePROCESSENTRY32ByReference processEntry = new CloseablePROCESSENTRY32ByReference()) {
            WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS,
                    new DWORD(0));
            try {
                while (Kernel32.INSTANCE.Process32Next(snapshot, processEntry)) {
                    parentPidMap.put(processEntry.th32ProcessID.intValue(),
                            processEntry.th32ParentProcessID.intValue());
                }
            } finally {
                Kernel32.INSTANCE.CloseHandle(snapshot);
            }
        }
        return parentPidMap;
    }

    @Override
    public int getProcessId() {
        return Kernel32.INSTANCE.GetCurrentProcessId();
    }

    @Override
    public int getProcessCount() {
        try (CloseablePerformanceInformation perfInfo = new CloseablePerformanceInformation()) {
            if (!Psapi.INSTANCE.GetPerformanceInfo(perfInfo, perfInfo.size())) {
                LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
                return 0;
            }
            return perfInfo.ProcessCount.intValue();
        }
    }

    @Override
    public int getThreadId() {
        return Kernel32.INSTANCE.GetCurrentThreadId();
    }

    @Override
    public OSThread getCurrentThread() {
        OSProcess proc = getCurrentProcess();
        final int tid = getThreadId();
        return proc.getThreadDetails().stream().filter(t -> t.getThreadId() == tid).findFirst()
                .orElse(new WindowsOSThreadJNA(proc.getProcessID(), tid, null, null));
    }

    @Override
    public int getThreadCount() {
        try (CloseablePerformanceInformation perfInfo = new CloseablePerformanceInformation()) {
            if (!Psapi.INSTANCE.GetPerformanceInfo(perfInfo, perfInfo.size())) {
                LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
                return 0;
            }
            return perfInfo.ThreadCount.intValue();
        }
    }

    @Override
    public long getSystemUptime() {
        return querySystemUptime();
    }

    private static long querySystemUptime() {
        // Uptime is in seconds so divide milliseconds
        // GetTickCount64 requires Vista (6.0) or later
        if (IS_VISTA_OR_GREATER) {
            return Kernel32.INSTANCE.GetTickCount64() / 1000L;
        } else {
            // 32 bit rolls over at ~ 49 days
            return Kernel32.INSTANCE.GetTickCount() / 1000L;
        }
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    private static long querySystemBootTime() {
        String eventLog = SYSTEM_LOG.get();
        if (eventLog != null) {
            try {
                EventLogIterator iter = new EventLogIterator(null, eventLog, WinNT.EVENTLOG_BACKWARDS_READ);
                // EventLogIterator is not AutoCloseable but opens a native event-log handle; close it on every path
                // (including the early returns below) to avoid leaking the handle.
                try {
                    // Get the most recent boot event (ID 12) from the Event log. If Windows "Fast
                    // Startup" is enabled we may not see event 12, so also check for most recent ID
                    // 6005 (Event log startup) as a reasonably close backup.
                    long event6005Time = 0L;
                    while (iter.hasNext()) {
                        EventLogRecord logRecord = iter.next();
                        if (logRecord.getStatusCode() == 12) {
                            // Event 12 is system boot. We want this value unless we find two 6005 events
                            // first (may occur with Fast Boot)
                            return logRecord.getRecord().TimeGenerated.longValue();
                        } else if (logRecord.getStatusCode() == 6005) {
                            // If we already found one, this means we've found a second one without finding
                            // an event 12. Return the latest one.
                            if (event6005Time > 0) {
                                return event6005Time;
                            }
                            // First 6005; tentatively assign
                            event6005Time = logRecord.getRecord().TimeGenerated.longValue();
                        }
                    }
                    // Only one 6005 found, return
                    if (event6005Time > 0) {
                        return event6005Time;
                    }
                } finally {
                    iter.close();
                }
            } catch (Win32Exception e) {
                LOG.warn("Can't open event log \"{}\".", eventLog);
            }
        }
        // If we get this far, event log reading has failed, either from no log or no
        // startup times. Subtract up time from current time as a reasonable proxy.
        return System.currentTimeMillis() / 1000L - querySystemUptime();
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new WindowsNetworkParamsJNA();
    }

    /**
     * Attempts to enable debug privileges for this process, required for OpenProcess() to get processes other than the
     * current user. Requires elevated permissions.
     *
     * @return {@code true} if debug privileges were successfully enabled.
     */
    private static boolean enableDebugPrivilege() {
        try (CloseableHANDLEByReference hToken = new CloseableHANDLEByReference()) {
            boolean success = Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(),
                    WinNT.TOKEN_QUERY | WinNT.TOKEN_ADJUST_PRIVILEGES, hToken);
            if (!success) {
                LOG.error("OpenProcessToken failed. Error: {}", Native.getLastError());
                return false;
            }
            try {
                LUID luid = new LUID();
                success = Advapi32.INSTANCE.LookupPrivilegeValue(null, WinNT.SE_DEBUG_NAME, luid);
                if (!success) {
                    LOG.error("LookupPrivilegeValue failed. Error: {}", Native.getLastError());
                    return false;
                }
                WinNT.TOKEN_PRIVILEGES tkp = new WinNT.TOKEN_PRIVILEGES(1);
                tkp.Privileges[0] = new WinNT.LUID_AND_ATTRIBUTES(luid, new DWORD(WinNT.SE_PRIVILEGE_ENABLED));
                success = Advapi32.INSTANCE.AdjustTokenPrivileges(hToken.getValue(), false, tkp, 0, null, null);
                int err = Native.getLastError();
                if (!success) {
                    LOG.error("AdjustTokenPrivileges failed. Error: {}", err);
                    return false;
                } else if (err == WinError.ERROR_NOT_ALL_ASSIGNED) {
                    LOG.debug("Debug privileges not enabled.");
                    return false;
                }
            } finally {
                Kernel32.INSTANCE.CloseHandle(hToken.getValue());
            }
        }
        return true;
    }

    @Override
    public List<OSService> getServices() {
        try (W32ServiceManager sm = new W32ServiceManager()) {
            sm.open(Winsvc.SC_MANAGER_ENUMERATE_SERVICE);
            Winsvc.ENUM_SERVICE_STATUS_PROCESS[] services = sm.enumServicesStatusExProcess(WinNT.SERVICE_WIN32,
                    Winsvc.SERVICE_STATE_ALL, null);
            List<OSService> svcArray = new ArrayList<>();
            for (Winsvc.ENUM_SERVICE_STATUS_PROCESS service : services) {
                State state;
                switch (service.ServiceStatusProcess.dwCurrentState) {
                    case 1:
                        state = STOPPED;
                        break;
                    case 4:
                        state = RUNNING;
                        break;
                    default:
                        state = OTHER;
                        break;
                }
                svcArray.add(new OSService(service.lpDisplayName, service.ServiceStatusProcess.dwProcessId, state));
            }
            return Collections.unmodifiableList(svcArray);
        } catch (com.sun.jna.platform.win32.Win32Exception ex) {
            LOG.error("Win32Exception", ex);
            return Collections.emptyList();
        }
    }

    private static String querySystemLog() {
        String systemLog = GlobalConfig.get(GlobalConfig.OSHI_OS_WINDOWS_EVENTLOG, "System");
        if (systemLog.isEmpty()) {
            // Use faster boot time approximation
            return null;
        }
        // Check whether it works
        HANDLE h = Advapi32.INSTANCE.OpenEventLog(null, systemLog);
        if (h == null) {
            LOG.warn("Unable to open configured system Event log \"{}\". Calculating boot time from uptime.",
                    systemLog);
            return null;
        }
        // Opened only to validate the configured log name; close it so the handle is not leaked.
        Advapi32.INSTANCE.CloseEventLog(h);
        return systemLog;
    }

    @Override
    public List<OSDesktopWindow> getDesktopWindows(boolean visibleOnly) {
        return EnumWindows.queryDesktopWindows(visibleOnly);
    }

    @Override
    public List<ApplicationInfo> getInstalledApplications() {
        return installedAppsSupplier.get();
    }

    /*
     * Package-private methods for use by WindowsOSProcess to limit process memory queries to processes with same
     * bitness as the current one
     */
    /**
     * Is the processor architecture x86?
     *
     * @return true if the processor architecture is Intel x86
     */
    static boolean isX86() {
        return X86;
    }

    private static boolean isCurrentX86() {
        try (CloseableSystemInfo sysinfo = new CloseableSystemInfo()) {
            Kernel32.INSTANCE.GetNativeSystemInfo(sysinfo);
            return (0 == sysinfo.processorArchitecture.pi.wProcessorArchitecture.intValue());
        }
    }

    /**
     * Is the current operating process x86 or x86-compatibility mode?
     *
     * @return true if the current process is 32-bit
     */
    static boolean isWow() {
        return WOW;
    }

    /**
     * Is the specified process x86 or x86-compatibility mode?
     *
     * @param h The handle to the processs to check
     * @return true if the process is 32-bit
     */
    static boolean isWow(HANDLE h) {
        if (X86) {
            return true;
        }
        try (CloseableIntByReference isWow = new CloseableIntByReference()) {
            Kernel32.INSTANCE.IsWow64Process(h, isWow);
            return isWow.getValue() != 0;
        }
    }

    private static boolean isCurrentWow() {
        if (X86) {
            return true;
        }
        HANDLE h = Kernel32.INSTANCE.GetCurrentProcess();
        return h != null && isWow(h);
    }
}
