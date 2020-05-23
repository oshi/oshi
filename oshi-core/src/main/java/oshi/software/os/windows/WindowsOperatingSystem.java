/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.software.os.windows;

import static oshi.software.os.OSService.State.OTHER;
import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util.EventLogIterator;
import com.sun.jna.platform.win32.Advapi32Util.EventLogRecord;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.Psapi.PERFORMANCE_INFORMATION;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.W32ServiceManager;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.Winsvc;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.registry.HkeyUserData;
import oshi.driver.windows.registry.NetSessionData;
import oshi.driver.windows.registry.ProcessPerformanceData;
import oshi.driver.windows.registry.ProcessPerformanceData.PerfCounterBlock;
import oshi.driver.windows.registry.ProcessWtsData;
import oshi.driver.windows.registry.ProcessWtsData.WtsInfo;
import oshi.driver.windows.registry.SessionWtsData;
import oshi.driver.windows.wmi.Win32OperatingSystem;
import oshi.driver.windows.wmi.Win32OperatingSystem.OSVersionProperty;
import oshi.driver.windows.wmi.Win32Processor;
import oshi.driver.windows.wmi.Win32Processor.BitnessProperty;
import oshi.jna.platform.windows.Kernel32;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSService.State;
import oshi.software.os.OSSession;
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;

/**
 * Microsoft Windows, commonly referred to as Windows, is a group of several
 * proprietary graphical operating system families, all of which are developed
 * and marketed by Microsoft.
 */
@ThreadSafe
public class WindowsOperatingSystem extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOperatingSystem.class);

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

    /**
     * Windows event log name
     */
    private static Supplier<String> systemLog = memoize(WindowsOperatingSystem::querySystemLog,
            TimeUnit.HOURS.toNanos(1));

    private static final long BOOTTIME = querySystemBootTime();

    static {
        enableDebugPrivilege();
    }

    /*
     * Cache full process stats queries. Second query will only populate if first
     * one returns null.
     */
    private Supplier<Map<Integer, PerfCounterBlock>> processMapFromRegistry = memoize(
            WindowsOperatingSystem::queryProcessMapFromRegistry, defaultExpiration());
    private Supplier<Map<Integer, PerfCounterBlock>> processMapFromPerfCounters = memoize(
            WindowsOperatingSystem::queryProcessMapFromPerfCounters, defaultExpiration());

    @Override
    public String queryManufacturer() {
        return "Microsoft";
    }

    @Override
    public FamilyVersionInfo queryFamilyVersionInfo() {
        WmiResult<OSVersionProperty> versionInfo = Win32OperatingSystem.queryOsVersion();
        if (versionInfo.getResultCount() < 1) {
            return new FamilyVersionInfo("Windows", new OSVersionInfo(System.getProperty("os.version"), null, null));
        }
        // Guaranteed that versionInfo is not null and lists non-empty
        // before calling the parse*() methods
        int suiteMask = WmiUtil.getUint32(versionInfo, OSVersionProperty.SUITEMASK, 0);
        String buildNumber = WmiUtil.getString(versionInfo, OSVersionProperty.BUILDNUMBER, 0);
        String version = parseVersion(versionInfo, suiteMask, buildNumber);
        String codeName = parseCodeName(suiteMask);
        return new FamilyVersionInfo("Windows", new OSVersionInfo(version, codeName, buildNumber));
    }

    private static String parseVersion(WmiResult<OSVersionProperty> versionInfo, int suiteMask, String buildNumber) {
        // Initialize a default, sane value
        String version = System.getProperty("os.version");

        // Version is major.minor.build. Parse the version string for
        // major/minor and get the build number separately
        String[] verSplit = WmiUtil.getString(versionInfo, OSVersionProperty.VERSION, 0).split("\\D");
        int major = verSplit.length > 0 ? ParseUtil.parseIntOrDefault(verSplit[0], 0) : 0;
        int minor = verSplit.length > 1 ? ParseUtil.parseIntOrDefault(verSplit[1], 0) : 0;

        // see
        // http://msdn.microsoft.com/en-us/library/windows/desktop/ms724833%28v=vs.85%29.aspx
        boolean ntWorkstation = WmiUtil.getUint32(versionInfo, OSVersionProperty.PRODUCTTYPE,
                0) == WinNT.VER_NT_WORKSTATION;
        switch (major) {
        case 10:
            if (minor == 0) {
                if (ntWorkstation) {
                    version = "10";
                } else {
                    // Build numbers greater than 17762 is Server 2019 for OS
                    // Version 10.0
                    version = (ParseUtil.parseLongOrDefault(buildNumber, 0L) > 17762) ? "Server 2019" : "Server 2016";
                }
            }
            break;
        case 6:
            if (minor == 3) {
                version = ntWorkstation ? "8.1" : "Server 2012 R2";
            } else if (minor == 2) {
                version = ntWorkstation ? "8" : "Server 2012";
            } else if (minor == 1) {
                version = ntWorkstation ? "7" : "Server 2008 R2";
            } else if (minor == 0) {
                version = ntWorkstation ? "Vista" : "Server 2008";
            }
            break;
        case 5:
            if (minor == 2) {
                if ((suiteMask & 0x00008000) != 0) { // VER_SUITE_WH_SERVER
                    version = "Home Server";
                } else if (ntWorkstation) {
                    version = "XP"; // 64 bits
                } else {
                    version = User32.INSTANCE.GetSystemMetrics(WinUser.SM_SERVERR2) != 0 ? "Server 2003"
                            : "Server 2003 R2";
                }
            } else if (minor == 1) {
                version = "XP"; // 32 bits
            } else if (minor == 0) {
                version = "2000";
            }
            break;
        default:
            break;
        }

        String sp = WmiUtil.getString(versionInfo, OSVersionProperty.CSDVERSION, 0);
        if (!sp.isEmpty() && !"unknown".equals(sp)) {
            version = version + " " + sp.replace("Service Pack ", "SP");
        }

        return version;
    }

    /**
     * Gets suites available on the system and return as a codename
     *
     * @param suiteMask
     *            The suite mask bitmask
     *
     * @return Suites
     */
    private static String parseCodeName(int suiteMask) {
        List<String> suites = new ArrayList<>();
        if ((suiteMask & 0x00000002) != 0) {
            suites.add("Enterprise");
        }
        if ((suiteMask & 0x00000004) != 0) {
            suites.add("BackOffice");
        }
        if ((suiteMask & 0x00000008) != 0) {
            suites.add("Communication Server");
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
        // 0x8000, Home Server, is included in main version name
        return String.join(",", suites);
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness < 64 && System.getenv("ProgramFiles(x86)") != null && IS_VISTA_OR_GREATER) {
            WmiResult<BitnessProperty> bitnessMap = Win32Processor.queryBitness();
            if (bitnessMap.getResultCount() > 0) {
                return WmiUtil.getUint16(bitnessMap, BitnessProperty.ADDRESSWIDTH, 0);
            }
        }
        return jvmBitness;
    }

    @Override
    public boolean queryElevated() {
        try {
            File dir = new File(System.getenv("windir") + "\\system32\\config\\systemprofile");
            return dir.isDirectory();
        } catch (SecurityException e) {
            return false;
        }
    }

    @Override
    public FileSystem getFileSystem() {
        return new WindowsFileSystem();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new WindowsInternetProtocolStats();
    }

    @Override
    public List<OSSession> getSessions() {
        List<OSSession> whoList = HkeyUserData.queryUserSessions();
        whoList.addAll(SessionWtsData.queryUserSessions());
        whoList.addAll(NetSessionData.queryUserSessions());
        return Collections.unmodifiableList(whoList);
    }

    @Override
    public List<OSProcess> getProcesses(int limit, ProcessSort sort) {
        List<OSProcess> procList = processMapToList(null);
        List<OSProcess> sorted = processSort(procList, limit, sort);
        return Collections.unmodifiableList(sorted);
    }

    @Override
    public List<OSProcess> getProcesses(Collection<Integer> pids) {
        return Collections.unmodifiableList(processMapToList(pids));
    }

    @Override
    public List<OSProcess> getChildProcesses(int parentPid, int limit, ProcessSort sort) {
        Set<Integer> childPids = new HashSet<>();
        // Get processes from ToolHelp API for parent PID
        Tlhelp32.PROCESSENTRY32.ByReference processEntry = new Tlhelp32.PROCESSENTRY32.ByReference();
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new DWORD(0));
        try {
            while (Kernel32.INSTANCE.Process32Next(snapshot, processEntry)) {
                if (processEntry.th32ParentProcessID.intValue() == parentPid) {
                    childPids.add(processEntry.th32ProcessID.intValue());
                }
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }
        List<OSProcess> procList = getProcesses(childPids);
        List<OSProcess> sorted = processSort(procList, limit, sort);
        return Collections.unmodifiableList(sorted);
    }

    @Override
    public OSProcess getProcess(int pid) {
        List<OSProcess> procList = processMapToList(Arrays.asList(pid));
        return procList.isEmpty() ? null : procList.get(0);
    }

    private List<OSProcess> processMapToList(Collection<Integer> pids) {
        // Get data from the registry if possible
        Map<Integer, PerfCounterBlock> processMap = processMapFromRegistry.get();
        // otherwise performance counters with WMI backup
        if (processMap == null) {
            processMap = (pids == null) ? processMapFromPerfCounters.get()
                    : ProcessPerformanceData.buildProcessMapFromPerfCounters(pids);
        }
        int myPid = getProcessId();

        Map<Integer, WtsInfo> processWtsMap = ProcessWtsData.queryProcessWtsMap(pids);

        Set<Integer> mapKeys = new HashSet<>(processWtsMap.keySet());
        mapKeys.retainAll(processMap.keySet());

        List<OSProcess> processList = new ArrayList<>();
        for (Integer pid : mapKeys) {
            processList.add(new WindowsOSProcess(pid, myPid, getBitness(), processMap, processWtsMap));
        }
        return processList;
    }

    private static Map<Integer, PerfCounterBlock> queryProcessMapFromRegistry() {
        return ProcessPerformanceData.buildProcessMapFromRegistry(null);
    }

    private static Map<Integer, PerfCounterBlock> queryProcessMapFromPerfCounters() {
        return ProcessPerformanceData.buildProcessMapFromPerfCounters(null);
    }

    @Override
    public int getProcessId() {
        return Kernel32.INSTANCE.GetCurrentProcessId();
    }

    @Override
    public int getProcessCount() {
        PERFORMANCE_INFORMATION perfInfo = new PERFORMANCE_INFORMATION();
        if (!Psapi.INSTANCE.GetPerformanceInfo(perfInfo, perfInfo.size())) {
            LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
            return 0;
        }
        return perfInfo.ProcessCount.intValue();
    }

    @Override
    public int getThreadCount() {
        PERFORMANCE_INFORMATION perfInfo = new PERFORMANCE_INFORMATION();
        if (!Psapi.INSTANCE.GetPerformanceInfo(perfInfo, perfInfo.size())) {
            LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
            return 0;
        }
        return perfInfo.ThreadCount.intValue();
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
        String eventLog = systemLog.get();
        if (eventLog != null) {
            try {
                EventLogIterator iter = new EventLogIterator(null, eventLog, WinNT.EVENTLOG_BACKWARDS_READ);
                // Get the most recent boot event (ID 12) from the Event log. If Windows "Fast
                // Startup" is enabled we may not see event 12, so also check for most recent ID
                // 6005 (Event log startup) as a reasonably close backup.
                long event6005Time = 0L;
                while (iter.hasNext()) {
                    EventLogRecord record = iter.next();
                    if (record.getStatusCode() == 12) {
                        // Event 12 is system boot. We want this value unless we find two 6005 events
                        // first (may occur with Fast Boot)
                        return record.getRecord().TimeGenerated.longValue();
                    } else if (record.getStatusCode() == 6005) {
                        // If we already found one, this means we've found a second one without finding
                        // an event 12. Return the latest one.
                        if (event6005Time > 0) {
                            return event6005Time;
                        }
                        // First 6005; tentatively assign
                        event6005Time = record.getRecord().TimeGenerated.longValue();
                    }
                }
                // Only one 6005 found, return
                if (event6005Time > 0) {
                    return event6005Time;
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
        return new WindowsNetworkParams();
    }

    /**
     * Enables debug privileges for this process, required for OpenProcess() to get
     * processes other than the current user
     */
    private static void enableDebugPrivilege() {
        HANDLEByReference hToken = new HANDLEByReference();
        boolean success = Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(),
                WinNT.TOKEN_QUERY | WinNT.TOKEN_ADJUST_PRIVILEGES, hToken);
        if (!success) {
            LOG.error("OpenProcessToken failed. Error: {}", Native.getLastError());
            return;
        }
        WinNT.LUID luid = new WinNT.LUID();
        success = Advapi32.INSTANCE.LookupPrivilegeValue(null, WinNT.SE_DEBUG_NAME, luid);
        if (!success) {
            LOG.error("LookupprivilegeValue failed. Error: {}", Native.getLastError());
            Kernel32.INSTANCE.CloseHandle(hToken.getValue());
            return;
        }
        WinNT.TOKEN_PRIVILEGES tkp = new WinNT.TOKEN_PRIVILEGES(1);
        tkp.Privileges[0] = new WinNT.LUID_AND_ATTRIBUTES(luid, new DWORD(WinNT.SE_PRIVILEGE_ENABLED));
        success = Advapi32.INSTANCE.AdjustTokenPrivileges(hToken.getValue(), false, tkp, 0, null, null);
        if (!success) {
            LOG.error("AdjustTokenPrivileges failed. Error: {}", Native.getLastError());
        }
        Kernel32.INSTANCE.CloseHandle(hToken.getValue());
    }

    @Override
    public OSService[] getServices() {
        try (W32ServiceManager sm = new W32ServiceManager()) {
            sm.open(Winsvc.SC_MANAGER_ENUMERATE_SERVICE);
            Winsvc.ENUM_SERVICE_STATUS_PROCESS[] services = sm.enumServicesStatusExProcess(WinNT.SERVICE_WIN32,
                    Winsvc.SERVICE_STATE_ALL, null);
            OSService[] svcArray = new OSService[services.length];
            for (int i = 0; i < services.length; i++) {
                State state;
                switch (services[i].ServiceStatusProcess.dwCurrentState) {
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
                svcArray[i] = new OSService(services[i].lpDisplayName, services[i].ServiceStatusProcess.dwProcessId,
                        state);
            }
            return svcArray;
        } catch (com.sun.jna.platform.win32.Win32Exception ex) {
            LOG.error("Win32Exception: {}", ex.getMessage());
            return new OSService[0];
        }
    }

    private static String querySystemLog() {
        String systemLog = GlobalConfig.get("oshi.os.windows.eventlog", "System");
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
        return systemLog;
    }
}
