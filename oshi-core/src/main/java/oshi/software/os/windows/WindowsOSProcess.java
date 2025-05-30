/*
 * Copyright 2020-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SUSPENDED;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Advapi32Util.Account;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.registry.ProcessPerformanceData;
import oshi.driver.windows.registry.ProcessWtsData;
import oshi.driver.windows.registry.ProcessWtsData.WtsInfo;
import oshi.driver.windows.registry.ThreadPerformanceData;
import oshi.driver.windows.wmi.Win32Process;
import oshi.driver.windows.wmi.Win32Process.CommandLineProperty;
import oshi.driver.windows.wmi.Win32ProcessCached;
import oshi.jna.ByRef.CloseableHANDLEByReference;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.ByRef.CloseableULONGptrByReference;
import oshi.jna.platform.windows.NtDll;
import oshi.jna.platform.windows.NtDll.UNICODE_STRING;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.util.Constants;
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * OSProcess implementation
 */
@ThreadSafe
public class WindowsOSProcess extends AbstractOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOSProcess.class);

    private static final boolean USE_BATCH_COMMANDLINE = GlobalConfig
            .get(GlobalConfig.OSHI_OS_WINDOWS_COMMANDLINE_BATCH, false);

    private static final boolean USE_PROCSTATE_SUSPENDED = GlobalConfig
            .get(GlobalConfig.OSHI_OS_WINDOWS_PROCSTATE_SUSPENDED, false);

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();
    private static final boolean IS_WINDOWS7_OR_GREATER = VersionHelpers.IsWindows7OrGreater();

    // track the OperatingSystem object that created this
    private final WindowsOperatingSystem os;

    private Supplier<Pair<String, String>> userInfo = memoize(this::queryUserInfo);
    private Supplier<Pair<String, String>> groupInfo = memoize(this::queryGroupInfo);
    private Supplier<String> currentWorkingDirectory = memoize(this::queryCwd);
    private Supplier<String> commandLine = memoize(this::queryCommandLine);
    private Supplier<List<String>> args = memoize(this::queryArguments);
    private Supplier<Triplet<String, String, Map<String, String>>> cwdCmdEnv = memoize(
            this::queryCwdCommandlineEnvironment);
    private Map<Integer, ThreadPerformanceData.PerfCounterBlock> tcb;

    private String name;
    private String path;
    private State state = INVALID;
    private int parentProcessID;
    private int threadCount;
    private int priority;
    private long virtualSize;
    private long residentSetSize;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private long bytesRead;
    private long bytesWritten;
    private long openFiles;
    private int bitness;
    private long pageFaults;

    public WindowsOSProcess(int pid, WindowsOperatingSystem os,
            Map<Integer, ProcessPerformanceData.PerfCounterBlock> processMap, Map<Integer, WtsInfo> processWtsMap,
            Map<Integer, ThreadPerformanceData.PerfCounterBlock> threadMap) {
        super(pid);
        // Save a copy of OS creating this object for later use
        this.os = os;
        // Initially set to match OS bitness. If 64 will check later for 32-bit process
        this.bitness = os.getBitness();
        // Initialize thread counters
        this.tcb = threadMap;
        updateAttributes(processMap.get(pid), processWtsMap.get(pid));
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public String getCommandLine() {
        return this.commandLine.get();
    }

    @Override
    public List<String> getArguments() {
        return args.get();
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return cwdCmdEnv.get().getC();
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return currentWorkingDirectory.get();
    }

    @Override
    public String getUser() {
        return userInfo.get().getA();
    }

    @Override
    public String getUserID() {
        return userInfo.get().getB();
    }

    @Override
    public String getGroup() {
        return groupInfo.get().getA();
    }

    @Override
    public String getGroupID() {
        return groupInfo.get().getB();
    }

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public int getParentProcessID() {
        return this.parentProcessID;
    }

    @Override
    public int getThreadCount() {
        return this.threadCount;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public long getVirtualSize() {
        return this.virtualSize;
    }

    @Override
    public long getResidentSetSize() {
        return this.residentSetSize;
    }

    @Override
    public long getKernelTime() {
        return this.kernelTime;
    }

    @Override
    public long getUserTime() {
        return this.userTime;
    }

    @Override
    public long getUpTime() {
        return this.upTime;
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public long getBytesRead() {
        return this.bytesRead;
    }

    @Override
    public long getBytesWritten() {
        return this.bytesWritten;
    }

    @Override
    public long getOpenFiles() {
        return this.openFiles;
    }

    @Override
    public long getSoftOpenFileLimit() {
        return WindowsFileSystem.MAX_WINDOWS_HANDLES;
    }

    @Override
    public long getHardOpenFileLimit() {
        return WindowsFileSystem.MAX_WINDOWS_HANDLES;
    }

    @Override
    public int getBitness() {
        return this.bitness;
    }

    @Override
    public long getAffinityMask() {
        final HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandle != null) {
            try (CloseableULONGptrByReference processAffinity = new CloseableULONGptrByReference();
                    CloseableULONGptrByReference systemAffinity = new CloseableULONGptrByReference()) {
                if (Kernel32.INSTANCE.GetProcessAffinityMask(pHandle, processAffinity, systemAffinity)) {
                    return Pointer.nativeValue(processAffinity.getValue().toPointer());
                }
            } finally {
                Kernel32.INSTANCE.CloseHandle(pHandle);
            }
        }
        return 0L;
    }

    @Override
    public long getMinorFaults() {
        return this.pageFaults;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        Map<Integer, ThreadPerformanceData.PerfCounterBlock> threads = tcb == null
                ? queryMatchingThreads(Collections.singleton(this.getProcessID()))
                : tcb;
        return threads.entrySet().stream().parallel()
                .filter(entry -> entry.getValue().getOwningProcessID() == this.getProcessID())
                .map(entry -> new WindowsOSThread(getProcessID(), entry.getKey(), this.name, entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateAttributes() {
        Set<Integer> pids = Collections.singleton(this.getProcessID());
        // Get data from the registry if possible
        Map<Integer, ProcessPerformanceData.PerfCounterBlock> pcb = ProcessPerformanceData
                .buildProcessMapFromRegistry(null);
        // otherwise performance counters with WMI backup
        if (pcb == null) {
            pcb = ProcessPerformanceData.buildProcessMapFromPerfCounters(pids);
        }
        if (USE_PROCSTATE_SUSPENDED) {
            this.tcb = queryMatchingThreads(pids);
        }
        Map<Integer, WtsInfo> wts = ProcessWtsData.queryProcessWtsMap(pids);
        return updateAttributes(pcb.get(this.getProcessID()), wts.get(this.getProcessID()));
    }

    private boolean updateAttributes(ProcessPerformanceData.PerfCounterBlock pcb, WtsInfo wts) {
        this.name = pcb.getName();
        this.path = wts.getPath(); // Empty string for Win7+
        this.parentProcessID = pcb.getParentProcessID();
        this.threadCount = wts.getThreadCount();
        this.priority = pcb.getPriority();
        this.virtualSize = wts.getVirtualSize();
        this.residentSetSize = pcb.getResidentSetSize();
        this.kernelTime = wts.getKernelTime();
        this.userTime = wts.getUserTime();
        this.startTime = pcb.getStartTime();
        this.upTime = pcb.getUpTime();
        this.bytesRead = pcb.getBytesRead();
        this.bytesWritten = pcb.getBytesWritten();
        this.openFiles = wts.getOpenFiles();
        this.pageFaults = pcb.getPageFaults();

        // There are only 3 possible Process states on Windows: RUNNING, SUSPENDED, or
        // UNKNOWN. Processes are considered running unless all of their threads are
        // SUSPENDED.
        this.state = RUNNING;
        if (this.tcb != null) {
            // If user hasn't enabled this in properties, we ignore
            int pid = this.getProcessID();
            // If any thread is NOT suspended, set running
            for (ThreadPerformanceData.PerfCounterBlock tpd : this.tcb.values()) {
                if (tpd.getOwningProcessID() == pid) {
                    if (tpd.getThreadWaitReason() == 5) {
                        this.state = SUSPENDED;
                    } else {
                        this.state = RUNNING;
                        break;
                    }
                }
            }
        }

        // Get a handle to the process for various extended info. Only gets
        // current user unless running as administrator
        final HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandle != null) {
            try {
                // Test for 32-bit process on 64-bit windows
                if (IS_VISTA_OR_GREATER && this.bitness == 64) {
                    try (CloseableIntByReference wow64 = new CloseableIntByReference()) {
                        if (Kernel32.INSTANCE.IsWow64Process(pHandle, wow64) && wow64.getValue() > 0) {
                            this.bitness = 32;
                        }
                    }
                }
                try { // EXECUTABLEPATH
                    if (IS_WINDOWS7_OR_GREATER) {
                        this.path = Kernel32Util.QueryFullProcessImageName(pHandle, 0);
                    }
                } catch (Win32Exception e) {
                    this.state = INVALID;
                }
            } finally {
                Kernel32.INSTANCE.CloseHandle(pHandle);
            }
        }

        return !this.state.equals(INVALID);
    }

    private Map<Integer, ThreadPerformanceData.PerfCounterBlock> queryMatchingThreads(Set<Integer> pids) {
        // fetch from registry
        Map<Integer, ThreadPerformanceData.PerfCounterBlock> threads = ThreadPerformanceData
                .buildThreadMapFromRegistry(pids);
        // otherwise performance counters with WMI backup
        if (threads == null) {
            threads = ThreadPerformanceData.buildThreadMapFromPerfCounters(pids, this.getName(), -1);
        }
        return threads;
    }

    private String queryCommandLine() {
        // Try to fetch from process memory
        if (!cwdCmdEnv.get().getB().isEmpty()) {
            return cwdCmdEnv.get().getB();
        }
        // If using batch mode fetch from WMI Cache
        if (USE_BATCH_COMMANDLINE) {
            return Win32ProcessCached.getInstance().getCommandLine(getProcessID(), getStartTime());
        }
        // If no cache enabled, query line by line
        WmiResult<CommandLineProperty> commandLineProcs = Win32Process
                .queryCommandLines(Collections.singleton(getProcessID()));
        if (commandLineProcs.getResultCount() > 0) {
            return WmiUtil.getString(commandLineProcs, CommandLineProperty.COMMANDLINE, 0);
        }
        return "";
    }

    private List<String> queryArguments() {
        String cl = getCommandLine();
        if (!cl.isEmpty()) {
            return Arrays.asList(Shell32Util.CommandLineToArgv(cl));
        }
        return Collections.emptyList();
    }

    private String queryCwd() {
        // Try to fetch from process memory
        if (!cwdCmdEnv.get().getA().isEmpty()) {
            return cwdCmdEnv.get().getA();
        }
        // For executing process, set CWD
        if (getProcessID() == this.os.getProcessId()) {
            String cwd = new File(".").getAbsolutePath();
            // trim off trailing "."
            if (!cwd.isEmpty()) {
                return cwd.substring(0, cwd.length() - 1);
            }
        }
        return "";
    }

    private Pair<String, String> queryUserInfo() {
        Pair<String, String> pair = null;
        final HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandle != null) {
            try (CloseableHANDLEByReference phToken = new CloseableHANDLEByReference()) {
                try {
                    if (Advapi32.INSTANCE.OpenProcessToken(pHandle, WinNT.TOKEN_DUPLICATE | WinNT.TOKEN_QUERY,
                            phToken)) {
                        Account account = Advapi32Util.getTokenAccount(phToken.getValue());
                        pair = new Pair<>(account.name, account.sidString);
                    } else {
                        int error = Kernel32.INSTANCE.GetLastError();
                        // Access denied errors are common. Fail silently.
                        if (error != WinError.ERROR_ACCESS_DENIED) {
                            LOG.error("Failed to get process token for process {}: {}", getProcessID(),
                                    Kernel32.INSTANCE.GetLastError());
                        }
                    }
                } catch (Win32Exception e) {
                    LOG.warn("Failed to query user info for process {} ({}): {}", getProcessID(), getName(),
                            e.getMessage());
                } finally {
                    final HANDLE token = phToken.getValue();
                    if (token != null) {
                        Kernel32.INSTANCE.CloseHandle(token);
                    }
                    Kernel32.INSTANCE.CloseHandle(pHandle);
                }
            }
        }
        if (pair == null) {
            return new Pair<>(Constants.UNKNOWN, Constants.UNKNOWN);
        }
        return pair;
    }

    private Pair<String, String> queryGroupInfo() {
        Pair<String, String> pair = null;
        final HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandle != null) {
            try (CloseableHANDLEByReference phToken = new CloseableHANDLEByReference()) {
                if (Advapi32.INSTANCE.OpenProcessToken(pHandle, WinNT.TOKEN_DUPLICATE | WinNT.TOKEN_QUERY, phToken)) {
                    Account account = Advapi32Util.getTokenPrimaryGroup(phToken.getValue());
                    pair = new Pair<>(account.name, account.sidString);
                } else {
                    int error = Kernel32.INSTANCE.GetLastError();
                    // Access denied errors are common. Fail silently.
                    if (error != WinError.ERROR_ACCESS_DENIED) {
                        LOG.error("Failed to get process token for process {}: {}", getProcessID(),
                                Kernel32.INSTANCE.GetLastError());
                    }
                }
                final HANDLE token = phToken.getValue();
                if (token != null) {
                    Kernel32.INSTANCE.CloseHandle(token);
                }
                Kernel32.INSTANCE.CloseHandle(pHandle);
            }
        }
        if (pair == null) {
            return new Pair<>(Constants.UNKNOWN, Constants.UNKNOWN);
        }
        return pair;
    }

    private Triplet<String, String, Map<String, String>> queryCwdCommandlineEnvironment() {
        // Get the process handle
        HANDLE h = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ, false,
                getProcessID());
        if (h != null) {
            try {
                // Can't check 32-bit procs from a 64-bit one
                if (WindowsOperatingSystem.isX86() == WindowsOperatingSystem.isWow(h)) {
                    try (CloseableIntByReference nRead = new CloseableIntByReference()) {
                        // Start by getting the address of the PEB
                        NtDll.PROCESS_BASIC_INFORMATION pbi = new NtDll.PROCESS_BASIC_INFORMATION();
                        int ret = NtDll.INSTANCE.NtQueryInformationProcess(h, NtDll.PROCESS_BASIC_INFORMATION,
                                pbi.getPointer(), pbi.size(), nRead);
                        if (ret != 0) {
                            return defaultCwdCommandlineEnvironment();
                        }
                        pbi.read();

                        // Now fetch the PEB
                        NtDll.PEB peb = new NtDll.PEB();
                        Kernel32.INSTANCE.ReadProcessMemory(h, pbi.PebBaseAddress, peb.getPointer(), peb.size(), nRead);
                        if (nRead.getValue() == 0) {
                            return defaultCwdCommandlineEnvironment();
                        }
                        peb.read();

                        // Now fetch the Process Parameters structure containing our data
                        NtDll.RTL_USER_PROCESS_PARAMETERS upp = new NtDll.RTL_USER_PROCESS_PARAMETERS();
                        Kernel32.INSTANCE.ReadProcessMemory(h, peb.ProcessParameters, upp.getPointer(), upp.size(),
                                nRead);
                        if (nRead.getValue() == 0) {
                            return defaultCwdCommandlineEnvironment();
                        }
                        upp.read();

                        // Get CWD and Command Line strings here
                        String cwd = readUnicodeString(h, upp.CurrentDirectory.DosPath);
                        String cl = readUnicodeString(h, upp.CommandLine);

                        // Fetch the Environment Strings
                        int envSize = upp.EnvironmentSize.intValue();
                        if (envSize > 0) {
                            try (Memory buffer = new Memory(envSize)) {
                                Kernel32.INSTANCE.ReadProcessMemory(h, upp.Environment, buffer, envSize, nRead);
                                if (nRead.getValue() > 0) {
                                    char[] env = buffer.getCharArray(0, envSize / 2);
                                    Map<String, String> envMap = ParseUtil.parseCharArrayToStringMap(env);
                                    // First entry in Environment is "=::=::\"
                                    envMap.remove("");
                                    return new Triplet<>(cwd, cl, Collections.unmodifiableMap(envMap));
                                }
                            }
                        }
                        return new Triplet<>(cwd, cl, Collections.emptyMap());
                    }
                }
            } finally {
                Kernel32.INSTANCE.CloseHandle(h);
            }
        }
        return defaultCwdCommandlineEnvironment();
    }

    private static Triplet<String, String, Map<String, String>> defaultCwdCommandlineEnvironment() {
        return new Triplet<>("", "", Collections.emptyMap());
    }

    private static String readUnicodeString(HANDLE h, UNICODE_STRING s) {
        if (s.Length > 0) {
            // Add space for null terminator
            try (Memory m = new Memory(s.Length + 2L); CloseableIntByReference nRead = new CloseableIntByReference()) {
                m.clear(); // really only need null in last 2 bytes but this is easier
                Kernel32.INSTANCE.ReadProcessMemory(h, s.Buffer, m, s.Length, nRead);
                if (nRead.getValue() > 0) {
                    return m.getWideString(0);
                }
            }
        }
        return "";
    }
}
