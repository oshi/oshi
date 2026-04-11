/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.windows.Advapi32FFM.TokenPrimaryGroup;
import static oshi.ffm.windows.Advapi32FFM.TokenUser;
import static oshi.ffm.windows.NtDllFFM.PBI_PEB_BASE_ADDRESS_OFFSET;
import static oshi.ffm.windows.NtDllFFM.PEB;
import static oshi.ffm.windows.NtDllFFM.PEB_PROCESS_PARAMETERS_OFFSET;
import static oshi.ffm.windows.NtDllFFM.PROCESS_BASIC_INFORMATION;
import static oshi.ffm.windows.NtDllFFM.PROCESS_BASIC_INFORMATION_STRUCT;
import static oshi.ffm.windows.NtDllFFM.RTL_USER_PROCESS_PARAMETERS;
import static oshi.ffm.windows.NtDllFFM.UNICODE_STRING;
import static oshi.ffm.windows.NtDllFFM.UPP_COMMAND_LINE_OFFSET;
import static oshi.ffm.windows.NtDllFFM.UPP_CURRENT_DIRECTORY_OFFSET;
import static oshi.ffm.windows.NtDllFFM.UPP_ENVIRONMENT_OFFSET;
import static oshi.ffm.windows.NtDllFFM.UPP_ENVIRONMENT_SIZE_OFFSET;
import static oshi.ffm.windows.NtDllFFM.readUnicodeString;
import static oshi.ffm.windows.WinNTFFM.PROCESS_QUERY_INFORMATION;
import static oshi.ffm.windows.WinNTFFM.PROCESS_VM_READ;
import static oshi.ffm.windows.WindowsForeignFunctions.readWideString;
import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SUSPENDED;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.registry.ProcessPerformanceData;
import oshi.driver.windows.registry.ProcessWtsData;
import oshi.driver.windows.registry.ProcessWtsData.WtsInfo;
import oshi.driver.windows.registry.ThreadPerformanceData;
import oshi.driver.windows.wmi.Win32Process;
import oshi.driver.windows.wmi.Win32Process.CommandLineProperty;
import oshi.driver.windows.wmi.Win32ProcessCached;
import oshi.ffm.windows.Advapi32FFM;
import oshi.ffm.windows.Kernel32FFM;
import oshi.ffm.windows.NtDllFFM;
import oshi.ffm.windows.Shell32FFM;
import oshi.ffm.windows.WinErrorFFM;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSThread;
import oshi.util.Constants;
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * OSProcess implementation using FFM (Foreign Function and Memory API)
 */
@ThreadSafe
public class WindowsOSProcessFFM extends AbstractOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOSProcessFFM.class);

    private static final boolean USE_BATCH_COMMANDLINE = GlobalConfig
            .get(GlobalConfig.OSHI_OS_WINDOWS_COMMANDLINE_BATCH, false);

    private static final boolean USE_PROCSTATE_SUSPENDED = GlobalConfig
            .get(GlobalConfig.OSHI_OS_WINDOWS_PROCSTATE_SUSPENDED, false);

    private static final boolean IS_VISTA_OR_GREATER = isWindowsVistaOrGreater();
    private static final boolean IS_WINDOWS7_OR_GREATER = isWindows7OrGreater();

    // track the OperatingSystem object that created this
    private final WindowsOperatingSystemFFM os;

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
    private long workingSetSize;
    private long privateWorkingSetSize;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private long bytesRead;
    private long bytesWritten;
    private long openFiles;
    private int bitness;
    private long pageFaults;

    public WindowsOSProcessFFM(int pid, WindowsOperatingSystemFFM os,
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

    private static boolean isWindowsVistaOrGreater() {
        String osVersion = System.getProperty("os.version");
        if (osVersion != null) {
            String[] parts = osVersion.split("\\.");
            if (parts.length >= 1) {
                int major = ParseUtil.parseIntOrDefault(parts[0], 0);
                return major >= 6;
            }
        }
        return true; // Assume modern Windows
    }

    private static boolean isWindows7OrGreater() {
        String osVersion = System.getProperty("os.version");
        if (osVersion != null) {
            String[] parts = osVersion.split("\\.");
            if (parts.length >= 2) {
                int major = ParseUtil.parseIntOrDefault(parts[0], 0);
                int minor = ParseUtil.parseIntOrDefault(parts[1], 0);
                return major > 6 || (major == 6 && minor >= 1);
            }
        }
        return true; // Assume modern Windows
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
    public long getResidentMemory() {
        return this.workingSetSize;
    }

    @Override
    public long getPrivateResidentMemory() {
        return this.privateWorkingSetSize;
    }

    /**
     * {@inheritDoc}
     * <p>
     * On Windows, delegates to {@link #getPrivateResidentMemory()} for backwards compatibility with prior behavior that
     * returned the Private Working Set.
     */
    @Deprecated
    @Override
    public long getResidentSetSize() {
        return getPrivateResidentMemory();
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
        Optional<MemorySegment> pHandleOpt = Kernel32FFM.OpenProcess(PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandleOpt.isPresent()) {
            MemorySegment pHandle = pHandleOpt.get();
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment processAffinity = arena.allocate(JAVA_LONG);
                MemorySegment systemAffinity = arena.allocate(JAVA_LONG);
                if (Kernel32FFM.GetProcessAffinityMask(pHandle, processAffinity, systemAffinity)) {
                    return processAffinity.get(JAVA_LONG, 0);
                }
            } finally {
                Kernel32FFM.CloseHandle(pHandle);
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
        try {
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
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean updateAttributes(ProcessPerformanceData.PerfCounterBlock pcb, WtsInfo wts) {
        if (pcb == null) {
            this.state = INVALID;
            return false;
        }
        this.name = pcb.getName();
        this.path = wts != null ? wts.getPath() : ""; // Empty string for Win7+
        this.parentProcessID = pcb.getParentProcessID();
        this.threadCount = wts != null ? wts.getThreadCount() : 0;
        this.priority = pcb.getPriority();
        this.virtualSize = wts != null ? wts.getVirtualSize() : 0L;
        this.workingSetSize = pcb.getWorkingSetSize();
        this.privateWorkingSetSize = pcb.getPrivateWorkingSetSize();
        this.kernelTime = wts != null ? wts.getKernelTime() : 0L;
        this.userTime = wts != null ? wts.getUserTime() : 0L;
        this.startTime = pcb.getStartTime();
        this.upTime = pcb.getUpTime();
        this.bytesRead = pcb.getBytesRead();
        this.bytesWritten = pcb.getBytesWritten();
        this.openFiles = wts != null ? wts.getOpenFiles() : 0L;
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
        Optional<MemorySegment> pHandleOpt = Kernel32FFM.OpenProcess(PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandleOpt.isPresent()) {
            MemorySegment pHandle = pHandleOpt.get();
            try (Arena arena = Arena.ofConfined()) {
                // Test for 32-bit process on 64-bit windows
                if (IS_VISTA_OR_GREATER && this.bitness == 64) {
                    MemorySegment wow64 = arena.allocate(JAVA_INT);
                    if (Kernel32FFM.IsWow64Process(pHandle, wow64) && wow64.get(JAVA_INT, 0) > 0) {
                        this.bitness = 32;
                    }
                }
                // EXECUTABLEPATH
                if (IS_WINDOWS7_OR_GREATER) {
                    Optional<String> pathOpt = Kernel32FFM.QueryFullProcessImageName(pHandle, 0, arena);
                    if (pathOpt.isPresent()) {
                        this.path = pathOpt.get();
                    }
                    // Don't set INVALID on path failure - process is still valid
                }
            } finally {
                Kernel32FFM.CloseHandle(pHandle);
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
            return Shell32FFM.CommandLineToArgv(cl);
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
        Optional<MemorySegment> pHandleOpt = Kernel32FFM.OpenProcess(PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandleOpt.isPresent()) {
            MemorySegment pHandle = pHandleOpt.get();
            MemorySegment hToken = null;
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment hTokenPtr = arena.allocate(ADDRESS);
                if (Advapi32FFM.OpenProcessToken(pHandle, 0x0008 | 0x0002, hTokenPtr)) { // TOKEN_QUERY |
                                                                                         // TOKEN_DUPLICATE
                    hToken = hTokenPtr.get(ADDRESS, 0);
                    pair = getTokenAccountInfo(hToken, TokenUser, arena);
                } else {
                    int error = Kernel32FFM.GetLastError().orElse(0);
                    // Access denied errors are common. Fail silently.
                    if (error != WinErrorFFM.ERROR_ACCESS_DENIED) {
                        LOG.error("Failed to get process token for process {}: {}", getProcessID(), error);
                    }
                }
            } catch (Throwable e) {
                LOG.warn("Failed to query user info for process {} ({}): {}", getProcessID(), getName(),
                        e.getMessage());
            } finally {
                if (hToken != null && hToken.address() != 0) {
                    Kernel32FFM.CloseHandle(hToken);
                }
                Kernel32FFM.CloseHandle(pHandle);
            }
        }
        if (pair == null) {
            return new Pair<>(Constants.UNKNOWN, Constants.UNKNOWN);
        }
        return pair;
    }

    private Pair<String, String> queryGroupInfo() {
        Pair<String, String> pair = null;
        Optional<MemorySegment> pHandleOpt = Kernel32FFM.OpenProcess(PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandleOpt.isPresent()) {
            MemorySegment pHandle = pHandleOpt.get();
            MemorySegment hToken = null;
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment hTokenPtr = arena.allocate(ADDRESS);
                if (Advapi32FFM.OpenProcessToken(pHandle, 0x0008 | 0x0002, hTokenPtr)) { // TOKEN_QUERY |
                                                                                         // TOKEN_DUPLICATE
                    hToken = hTokenPtr.get(ADDRESS, 0);
                    pair = getTokenAccountInfo(hToken, TokenPrimaryGroup, arena);
                } else {
                    int error = Kernel32FFM.GetLastError().orElse(0);
                    // Access denied errors are common. Fail silently.
                    if (error != WinErrorFFM.ERROR_ACCESS_DENIED) {
                        LOG.error("Failed to get process token for process {}: {}", getProcessID(), error);
                    }
                }
            } catch (Throwable e) {
                LOG.warn("Failed to query group info for process {} ({}): {}", getProcessID(), getName(),
                        e.getMessage());
            } finally {
                if (hToken != null && hToken.address() != 0) {
                    Kernel32FFM.CloseHandle(hToken);
                }
                Kernel32FFM.CloseHandle(pHandle);
            }
        }
        if (pair == null) {
            return new Pair<>(Constants.UNKNOWN, Constants.UNKNOWN);
        }
        return pair;
    }

    private Pair<String, String> getTokenAccountInfo(MemorySegment hToken, int tokenInfoClass, Arena arena)
            throws Throwable {
        // First call to get required size
        MemorySegment returnLength = arena.allocate(JAVA_INT);
        Advapi32FFM.GetTokenInformation(hToken, tokenInfoClass, MemorySegment.NULL, 0, returnLength);

        int size = returnLength.get(JAVA_INT, 0);
        if (size <= 0) {
            return null;
        }

        MemorySegment tokenInfo = arena.allocate(size);
        if (!Advapi32FFM.GetTokenInformation(hToken, tokenInfoClass, tokenInfo, size, returnLength)) {
            return null;
        }

        // TOKEN_USER and TOKEN_PRIMARY_GROUP both start with a SID_AND_ATTRIBUTES structure
        // which contains a pointer to the SID at offset 0
        MemorySegment sidPtr = tokenInfo.get(ADDRESS, 0);
        if (sidPtr.address() == 0) {
            return null;
        }

        // Get account name
        int maxNameLen = 256;
        MemorySegment nameBuffer = arena.allocate(JAVA_CHAR, maxNameLen);
        MemorySegment nameLen = arena.allocate(JAVA_INT);
        nameLen.set(JAVA_INT, 0, maxNameLen);

        MemorySegment domainBuffer = arena.allocate(JAVA_CHAR, maxNameLen);
        MemorySegment domainLen = arena.allocate(JAVA_INT);
        domainLen.set(JAVA_INT, 0, maxNameLen);

        MemorySegment sidUse = arena.allocate(JAVA_INT);

        String accountName = Constants.UNKNOWN;
        if (Advapi32FFM.LookupAccountSid(MemorySegment.NULL, sidPtr, nameBuffer, nameLen, domainBuffer, domainLen,
                sidUse)) {
            String name = readWideString(nameBuffer);
            String domain = readWideString(domainBuffer);
            // Build domain-qualified name if domain is available
            if (!domain.isEmpty() && !domain.equals(Constants.UNKNOWN)) {
                accountName = domain + "\\" + name;
            } else {
                accountName = name;
            }
        }

        // Get SID string
        String sidString = Constants.UNKNOWN;
        MemorySegment sidStringPtr = arena.allocate(ADDRESS);
        if (Advapi32FFM.ConvertSidToStringSid(sidPtr, sidStringPtr)) {
            MemorySegment strPtr = sidStringPtr.get(ADDRESS, 0);
            if (strPtr.address() != 0) {
                sidString = readWideString(strPtr.reinterpret(512));
                // Free the buffer allocated by ConvertSidToStringSid
                Kernel32FFM.LocalFree(strPtr);
            }
        }

        return new Pair<>(accountName, sidString);
    }

    private Triplet<String, String, Map<String, String>> queryCwdCommandlineEnvironment() {
        // Get the process handle
        Optional<MemorySegment> hOpt = Kernel32FFM.OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, false,
                getProcessID());
        if (hOpt.isPresent()) {
            MemorySegment h = hOpt.get();
            try (Arena arena = Arena.ofConfined()) {
                // Can't check 32-bit procs from a 64-bit one
                if (WindowsOperatingSystem.isX86() == isWow(h, arena)) {
                    // Start by getting the address of the PEB
                    MemorySegment pbi = arena.allocate(PROCESS_BASIC_INFORMATION_STRUCT);
                    MemorySegment nRead = arena.allocate(JAVA_INT);

                    int ret = NtDllFFM.NtQueryInformationProcess(h, PROCESS_BASIC_INFORMATION, pbi,
                            (int) PROCESS_BASIC_INFORMATION_STRUCT.byteSize(), nRead);
                    if (ret != 0) {
                        return defaultCwdCommandlineEnvironment();
                    }

                    MemorySegment pebAddress = pbi.get(ADDRESS, PBI_PEB_BASE_ADDRESS_OFFSET);
                    if (pebAddress.address() == 0) {
                        return defaultCwdCommandlineEnvironment();
                    }

                    // Now fetch the PEB
                    MemorySegment peb = arena.allocate(PEB);
                    MemorySegment bytesRead = arena.allocate(JAVA_LONG);
                    if (!Kernel32FFM.ReadProcessMemory(h, pebAddress, peb, PEB.byteSize(), bytesRead)) {
                        return defaultCwdCommandlineEnvironment();
                    }
                    if (bytesRead.get(JAVA_LONG, 0) == 0) {
                        return defaultCwdCommandlineEnvironment();
                    }

                    MemorySegment processParamsAddress = peb.get(ADDRESS, PEB_PROCESS_PARAMETERS_OFFSET);
                    if (processParamsAddress.address() == 0) {
                        return defaultCwdCommandlineEnvironment();
                    }

                    // Now fetch the Process Parameters structure containing our data
                    MemorySegment upp = arena.allocate(RTL_USER_PROCESS_PARAMETERS);
                    if (!Kernel32FFM.ReadProcessMemory(h, processParamsAddress, upp,
                            RTL_USER_PROCESS_PARAMETERS.byteSize(), bytesRead)) {
                        return defaultCwdCommandlineEnvironment();
                    }
                    if (bytesRead.get(JAVA_LONG, 0) == 0) {
                        return defaultCwdCommandlineEnvironment();
                    }

                    // Get CWD and Command Line strings here
                    MemorySegment cwdUnicodeString = upp.asSlice(UPP_CURRENT_DIRECTORY_OFFSET,
                            UNICODE_STRING.byteSize());
                    String cwd = readUnicodeString(h, cwdUnicodeString, arena);

                    MemorySegment cmdLineUnicodeString = upp.asSlice(UPP_COMMAND_LINE_OFFSET,
                            UNICODE_STRING.byteSize());
                    String cl = readUnicodeString(h, cmdLineUnicodeString, arena);

                    // Fetch the Environment Strings
                    long envSize = upp.get(JAVA_LONG, UPP_ENVIRONMENT_SIZE_OFFSET);
                    if (envSize > 0 && envSize < Integer.MAX_VALUE) {
                        MemorySegment envAddress = upp.get(ADDRESS, UPP_ENVIRONMENT_OFFSET);
                        if (envAddress.address() != 0) {
                            MemorySegment envBuffer = arena.allocate((int) envSize);
                            if (Kernel32FFM.ReadProcessMemory(h, envAddress, envBuffer, envSize, bytesRead)) {
                                if (bytesRead.get(JAVA_LONG, 0) > 0) {
                                    char[] env = new char[(int) (envSize / 2)];
                                    for (int i = 0; i < env.length; i++) {
                                        env[i] = envBuffer.get(JAVA_CHAR, (long) i * 2);
                                    }
                                    Map<String, String> envMap = ParseUtil.parseCharArrayToStringMap(env);
                                    // First entry in Environment is "=::=::\"
                                    envMap.remove("");
                                    return new Triplet<>(cwd, cl, Collections.unmodifiableMap(envMap));
                                }
                            }
                        }
                    }
                    return new Triplet<>(cwd, cl, Collections.emptyMap());
                }
            } finally {
                Kernel32FFM.CloseHandle(h);
            }
        }
        return defaultCwdCommandlineEnvironment();
    }

    private static Triplet<String, String, Map<String, String>> defaultCwdCommandlineEnvironment() {
        return new Triplet<>("", "", Collections.emptyMap());
    }

    private static boolean isWow(MemorySegment h, Arena arena) {
        MemorySegment wow64 = arena.allocate(JAVA_INT);
        if (Kernel32FFM.IsWow64Process(h, wow64)) {
            return wow64.get(JAVA_INT, 0) != 0;
        }
        return false;
    }
}
