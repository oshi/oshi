/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.registry.ProcessPerfCounterBlock;
import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;
import oshi.driver.windows.registry.ProcessWtsData.WtsInfo;
import oshi.jna.ByRef.CloseableHANDLEByReference;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.ByRef.CloseableULONGptrByReference;
import oshi.jna.platform.windows.NtDll;
import oshi.jna.platform.windows.NtDll.UNICODE_STRING;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * JNA-based Windows OS process implementation.
 */
@ThreadSafe
public class WindowsOSProcessJNA extends WindowsOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOSProcessJNA.class);

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();
    private static final boolean IS_WINDOWS7_OR_GREATER = VersionHelpers.IsWindows7OrGreater();

    public WindowsOSProcessJNA(int pid, WindowsOperatingSystem os, Map<Integer, ProcessPerfCounterBlock> processMap,
            Map<Integer, WtsInfo> processWtsMap, Map<Integer, ThreadPerfCounterBlock> threadMap) {
        super(pid, os, processMap, processWtsMap, threadMap);
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
    protected boolean updateAttributes(ProcessPerfCounterBlock pcb, WtsInfo wts) {
        if (!super.updateAttributes(pcb, wts)) {
            return false;
        }

        final HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandle != null) {
            try {
                // Test for 32-bit process on 64-bit windows
                if (IS_VISTA_OR_GREATER && getBitness() == 64) {
                    try (CloseableIntByReference wow64 = new CloseableIntByReference()) {
                        if (Kernel32.INSTANCE.IsWow64Process(pHandle, wow64) && wow64.getValue() > 0) {
                            setBitness(32);
                        }
                    }
                }
                try { // EXECUTABLEPATH
                    if (IS_WINDOWS7_OR_GREATER) {
                        setPath(Kernel32Util.QueryFullProcessImageName(pHandle, 0));
                    }
                } catch (Win32Exception e) {
                    // Best-effort path lookup; leave process state and path untouched on failure
                }
            } finally {
                Kernel32.INSTANCE.CloseHandle(pHandle);
            }
        }

        return !getState().equals(State.INVALID);
    }

    @Override
    protected List<String> queryArguments() {
        String cl = getCommandLine();
        if (!cl.isEmpty()) {
            return Arrays.asList(Shell32Util.CommandLineToArgv(cl));
        }
        return Collections.emptyList();
    }

    @Override
    protected Pair<String, String> queryUserInfo() {
        Pair<String, String> pair = null;
        final HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandle != null) {
            try (CloseableHANDLEByReference phToken = new CloseableHANDLEByReference()) {
                try {
                    if (Advapi32.INSTANCE.OpenProcessToken(pHandle, WinNT.TOKEN_QUERY, phToken)) {
                        Account account = Advapi32Util.getTokenAccount(phToken.getValue());
                        pair = new Pair<>(account.name, account.sidString);
                    } else {
                        int error = Kernel32.INSTANCE.GetLastError();
                        // Access denied errors are common. Fail silently.
                        if (error != WinError.ERROR_ACCESS_DENIED) {
                            LOG.error("Failed to get process token for process {}: {}", getProcessID(), error);
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
        return pair != null ? pair : defaultPair();
    }

    @Override
    protected Pair<String, String> queryGroupInfo() {
        Pair<String, String> pair = null;
        final HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandle != null) {
            try (CloseableHANDLEByReference phToken = new CloseableHANDLEByReference()) {
                try {
                    if (Advapi32.INSTANCE.OpenProcessToken(pHandle, WinNT.TOKEN_QUERY, phToken)) {
                        Account account = Advapi32Util.getTokenPrimaryGroup(phToken.getValue());
                        pair = new Pair<>(account.name, account.sidString);
                    } else {
                        int error = Kernel32.INSTANCE.GetLastError();
                        // Access denied errors are common. Fail silently.
                        if (error != WinError.ERROR_ACCESS_DENIED) {
                            LOG.error("Failed to get process token for process {}: {}", getProcessID(), error);
                        }
                    }
                } catch (Win32Exception e) {
                    LOG.warn("Failed to query group info for process {} ({}): {}", getProcessID(), getName(),
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
        return pair != null ? pair : defaultPair();
    }

    @Override
    protected Triplet<String, String, Map<String, String>> queryCwdCommandlineEnvironment() {
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
