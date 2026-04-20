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
import static oshi.ffm.windows.WinNTFFM.TOKEN_QUERY;
import static oshi.ffm.windows.WindowsForeignFunctions.readWideString;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.registry.ProcessPerfCounterBlock;
import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;
import oshi.driver.common.windows.registry.WtsInfo;
import oshi.driver.common.windows.wmi.Win32Process.CommandLineProperty;
import oshi.driver.windows.registry.ProcessPerformanceDataFFM;
import oshi.driver.windows.registry.ProcessWtsDataFFM;
import oshi.driver.windows.registry.ThreadPerformanceDataFFM;
import oshi.driver.windows.wmi.Win32ProcessCachedFFM;
import oshi.driver.windows.wmi.Win32ProcessFFM;
import oshi.ffm.windows.Advapi32FFM;
import oshi.ffm.windows.Kernel32FFM;
import oshi.ffm.windows.NtDllFFM;
import oshi.ffm.windows.Shell32FFM;
import oshi.ffm.windows.VersionHelpersFFM;
import oshi.ffm.windows.WinErrorFFM;
import oshi.software.common.os.windows.WindowsOSProcess;
import oshi.software.os.OSThread;
import oshi.util.Constants;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtilFFM;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * FFM-based Windows OS process implementation.
 */
@ThreadSafe
public class WindowsOSProcessFFM extends WindowsOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOSProcessFFM.class);

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpersFFM.IsWindowsVistaOrGreater();
    private static final boolean IS_WINDOWS7_OR_GREATER = VersionHelpersFFM.IsWindows7OrGreater();

    public WindowsOSProcessFFM(int pid, WindowsOperatingSystemFFM os, Map<Integer, ProcessPerfCounterBlock> processMap,
            Map<Integer, WtsInfo> processWtsMap, Map<Integer, ThreadPerfCounterBlock> threadMap) {
        super(pid, os, processMap, processWtsMap, threadMap);
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
    public boolean updateAttributes() {
        Set<Integer> pids = Collections.singleton(this.getProcessID());
        // Get data from the registry if possible
        Map<Integer, ProcessPerfCounterBlock> pcbMap = ProcessPerformanceDataFFM.buildProcessMapFromRegistry(pids);
        // otherwise performance counters with WMI backup
        if (pcbMap == null || pcbMap.isEmpty()) {
            pcbMap = ProcessPerformanceDataFFM.buildProcessMapFromPerfCounters(pids);
        }
        ProcessPerfCounterBlock pcb = pcbMap == null ? null : pcbMap.get(this.getProcessID());
        if (USE_PROCSTATE_SUSPENDED) {
            // Populate name from pcb before querying threads, since the fallback path uses getName()
            if (pcb != null) {
                setName(pcb.getName());
            }
            setTcb(queryMatchingThreads(pids));
        }
        Map<Integer, WtsInfo> wts = ProcessWtsDataFFM.queryProcessWtsMap(pids);
        return updateAttributes(pcb, wts == null ? null : wts.get(this.getProcessID()));
    }

    @Override
    protected boolean updateAttributes(ProcessPerfCounterBlock pcb, WtsInfo wts) {
        if (!super.updateAttributes(pcb, wts)) {
            return false;
        }

        Optional<MemorySegment> pHandleOpt = Kernel32FFM.OpenProcess(PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandleOpt.isPresent()) {
            MemorySegment pHandle = pHandleOpt.get();
            try (Arena arena = Arena.ofConfined()) {
                // Test for 32-bit process on 64-bit windows
                if (IS_VISTA_OR_GREATER && getBitness() == 64) {
                    MemorySegment wow64 = arena.allocate(JAVA_INT);
                    if (Kernel32FFM.IsWow64Process(pHandle, wow64) && wow64.get(JAVA_INT, 0) > 0) {
                        setBitness(32);
                    }
                }
                // EXECUTABLEPATH
                if (IS_WINDOWS7_OR_GREATER) {
                    Optional<String> pathOpt = Kernel32FFM.QueryFullProcessImageName(pHandle, 0, arena);
                    if (pathOpt.isPresent()) {
                        setPath(pathOpt.get());
                    }
                }
            } finally {
                Kernel32FFM.CloseHandle(pHandle);
            }
        }

        return !getState().equals(State.INVALID);
    }

    @Override
    protected OSThread createOSThread(int pid, int tid, String procName, ThreadPerfCounterBlock pcb) {
        return new WindowsOSThreadFFM(pid, tid, procName, pcb);
    }

    @Override
    protected Map<Integer, ThreadPerfCounterBlock> queryMatchingThreads(Set<Integer> pids) {
        Map<Integer, ThreadPerfCounterBlock> threads = ThreadPerformanceDataFFM.buildThreadMapFromRegistry(pids);
        if (threads == null || threads.isEmpty()) {
            threads = ThreadPerformanceDataFFM.buildThreadMapFromPerfCounters(pids, this.getName(), -1);
        }
        return threads;
    }

    @Override
    protected String queryCommandLine() {
        String cwdCmdEnvB = getCwdCmdEnv().getB();
        if (!cwdCmdEnvB.isEmpty()) {
            return cwdCmdEnvB;
        }
        if (USE_BATCH_COMMANDLINE) {
            return Win32ProcessCachedFFM.getInstance().getCommandLine(getProcessID(), getStartTime());
        }
        var commandLineProcs = Win32ProcessFFM.queryCommandLines(Collections.singleton(getProcessID()));
        if (commandLineProcs.getResultCount() > 0) {
            return WmiUtilFFM.getString(commandLineProcs, CommandLineProperty.COMMANDLINE, 0);
        }
        return "";
    }

    @Override
    protected List<String> queryArguments() {
        String cl = getCommandLine();
        if (!cl.isEmpty()) {
            return Shell32FFM.CommandLineToArgv(cl);
        }
        return Collections.emptyList();
    }

    @Override
    protected Pair<String, String> queryUserInfo() {
        Pair<String, String> pair = null;
        Optional<MemorySegment> pHandleOpt = Kernel32FFM.OpenProcess(PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandleOpt.isPresent()) {
            MemorySegment pHandle = pHandleOpt.get();
            MemorySegment hToken = null;
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment hTokenPtr = arena.allocate(ADDRESS);
                if (Advapi32FFM.OpenProcessToken(pHandle, TOKEN_QUERY, hTokenPtr)) {
                    hToken = hTokenPtr.get(ADDRESS, 0);
                    pair = getTokenAccountInfo(hToken, TokenUser, arena);
                } else {
                    int error = Kernel32FFM.GetLastError().orElse(0);
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
        return pair != null ? pair : defaultPair();
    }

    @Override
    protected Pair<String, String> queryGroupInfo() {
        Pair<String, String> pair = null;
        Optional<MemorySegment> pHandleOpt = Kernel32FFM.OpenProcess(PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandleOpt.isPresent()) {
            MemorySegment pHandle = pHandleOpt.get();
            MemorySegment hToken = null;
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment hTokenPtr = arena.allocate(ADDRESS);
                if (Advapi32FFM.OpenProcessToken(pHandle, TOKEN_QUERY, hTokenPtr)) {
                    hToken = hTokenPtr.get(ADDRESS, 0);
                    pair = getTokenAccountInfo(hToken, TokenPrimaryGroup, arena);
                } else {
                    int error = Kernel32FFM.GetLastError().orElse(0);
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
        return pair != null ? pair : defaultPair();
    }

    private Pair<String, String> getTokenAccountInfo(MemorySegment hToken, int tokenInfoClass, Arena arena)
            throws Throwable {
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

        MemorySegment sidPtr = tokenInfo.get(ADDRESS, 0);
        if (sidPtr.address() == 0) {
            return null;
        }

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
            accountName = readWideString(nameBuffer);
        }

        String sidString = Constants.UNKNOWN;
        MemorySegment sidStringPtr = arena.allocate(ADDRESS);
        if (Advapi32FFM.ConvertSidToStringSid(sidPtr, sidStringPtr)) {
            MemorySegment strPtr = sidStringPtr.get(ADDRESS, 0);
            if (strPtr.address() != 0) {
                sidString = readWideString(strPtr.reinterpret(512));
                Kernel32FFM.LocalFree(strPtr);
            }
        }

        return new Pair<>(accountName, sidString);
    }

    @Override
    protected Triplet<String, String, Map<String, String>> queryCwdCommandlineEnvironment() {
        Optional<MemorySegment> hOpt = Kernel32FFM.OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, false,
                getProcessID());
        if (hOpt.isPresent()) {
            MemorySegment h = hOpt.get();
            try (Arena arena = Arena.ofConfined()) {
                if (WindowsOperatingSystemFFM.isX86() == isWow(h, arena)) {
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

                    MemorySegment upp = arena.allocate(RTL_USER_PROCESS_PARAMETERS);
                    if (!Kernel32FFM.ReadProcessMemory(h, processParamsAddress, upp,
                            RTL_USER_PROCESS_PARAMETERS.byteSize(), bytesRead)) {
                        return defaultCwdCommandlineEnvironment();
                    }
                    if (bytesRead.get(JAVA_LONG, 0) == 0) {
                        return defaultCwdCommandlineEnvironment();
                    }

                    MemorySegment cwdUnicodeString = upp.asSlice(UPP_CURRENT_DIRECTORY_OFFSET,
                            UNICODE_STRING.byteSize());
                    String cwd = readUnicodeString(h, cwdUnicodeString, arena);

                    MemorySegment cmdLineUnicodeString = upp.asSlice(UPP_COMMAND_LINE_OFFSET,
                            UNICODE_STRING.byteSize());
                    String cl = readUnicodeString(h, cmdLineUnicodeString, arena);

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

    private static boolean isWow(MemorySegment h, Arena arena) {
        MemorySegment wow64 = arena.allocate(JAVA_INT);
        if (Kernel32FFM.IsWow64Process(h, wow64)) {
            return wow64.get(JAVA_INT, 0) != 0;
        }
        return false;
    }
}
