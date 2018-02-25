/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.software.os.windows;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Advapi32Util.Account;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;

import oshi.jna.platform.windows.Psapi;
import oshi.jna.platform.windows.Psapi.PERFORMANCE_INFORMATION;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;

public class WindowsOperatingSystem extends AbstractOperatingSystem {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOperatingSystem.class);

    // For WMI Process queries
    private static String processProperties = "Name,ExecutablePath,CommandLine,ExecutionState,ProcessID,ParentProcessId"
            + ",ThreadCount,Priority,VirtualSize,WorkingSetSize,KernelModeTime,UserModeTime,CreationDate"
            + ",ReadTransferCount,WriteTransferCount,__PATH,__PATH,HandleCount";
    private static ValueType[] processPropertyTypes = { ValueType.STRING, ValueType.STRING, ValueType.STRING,
            ValueType.UINT32, ValueType.UINT32, ValueType.UINT32, ValueType.UINT32, ValueType.UINT32, ValueType.STRING,
            ValueType.STRING, ValueType.STRING, ValueType.STRING, ValueType.DATETIME, ValueType.UINT64,
            ValueType.UINT64, ValueType.PROCESS_GETOWNER, ValueType.PROCESS_GETOWNERSID, ValueType.UINT32 };

    /*
     * Windows Execution States:
     */
    private static final int UNKNOWN = 0;
    private static final int OTHER = 1;
    private static final int READY = 2;
    private static final int RUNNING = 3;
    private static final int BLOCKED = 4;
    private static final int SUSPENDED_BLOCKED = 5;
    private static final int SUSPENDED_READY = 6;
    private static final int TERMINATED = 7;
    private static final int STOPPED = 8;
    private static final int GROWING = 9;

    /*
     * LastError
     */
    private static final int ERROR_ACCESS_DENIED = 5;

    static {
        enableDebugPrivilege();
    }

    public WindowsOperatingSystem() {
        this.manufacturer = "Microsoft";
        this.family = "Windows";
        this.version = new WindowsOSVersionInfoEx();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem() {
        return new WindowsFileSystem();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess[] getProcesses(int limit, ProcessSort sort) {
        Map<String, List<Object>> procs = WmiUtil.selectObjectsFrom(null, "Win32_Process", processProperties, null,
                processPropertyTypes);
        List<OSProcess> procList = processMapToList(procs);
        List<OSProcess> sorted = processSort(procList, limit, sort);
        return sorted.toArray(new OSProcess[sorted.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess getProcess(int pid) {
        Map<String, List<Object>> procs = WmiUtil.selectObjectsFrom(null, "Win32_Process", processProperties,
                String.format("WHERE ProcessId=%d", pid), processPropertyTypes);
        List<OSProcess> procList = processMapToList(procs);
        return procList.isEmpty() ? null : procList.get(0);
    }

    private List<OSProcess> processMapToList(Map<String, List<Object>> procs) {
        long now = System.currentTimeMillis();
        List<OSProcess> procList = new ArrayList<>();
        List<String> groupList = new ArrayList<>();
        List<String> groupIDList = new ArrayList<>();
        // All map lists should be the same length. Pick one size and iterate
        final int procCount = procs.get("Name").size();
        int myPid = getProcessId();
        for (int p = 0; p < procCount; p++) {
            OSProcess proc = new OSProcess();
            proc.setName((String) procs.get("Name").get(p));
            proc.setPath((String) procs.get("ExecutablePath").get(p));
            proc.setCommandLine((String) procs.get("CommandLine").get(p));
            proc.setProcessID(((Long) procs.get("ProcessID").get(p)).intValue());
            if (myPid == proc.getProcessID()) {
                proc.setCurrentWorkingDirectory(new File(".").getAbsolutePath());
            }
            proc.setParentProcessID(((Long) procs.get("ParentProcessId").get(p)).intValue());
            proc.setUser((String) procs.get("PROCESS_GETOWNER").get(p));
            proc.setUserID((String) procs.get("PROCESS_GETOWNERSID").get(p));
            // Fetching group information incurs significant latency.
            // Only do for single-process queries
            if (procCount == 1) {
                final HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(
                        WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ, false, proc.getProcessID());
                if (pHandle != null) {
                    final HANDLEByReference phToken = new HANDLEByReference();
                    if (Advapi32.INSTANCE.OpenProcessToken(pHandle, WinNT.TOKEN_DUPLICATE | WinNT.TOKEN_QUERY,
                            phToken)) {
                        Account[] accounts = Advapi32Util.getTokenGroups(phToken.getValue());
                        // get groups
                        groupList.clear();
                        groupIDList.clear();
                        for (Account account : accounts) {
                            groupList.add(account.name);
                            groupIDList.add(account.sidString);
                        }
                        proc.setGroup(FormatUtil.join(",", groupList));
                        proc.setGroupID(FormatUtil.join(",", groupIDList));
                    } else {
                        int error = Kernel32.INSTANCE.GetLastError();
                        // Access denied errors are common and will silently
                        // fail
                        if (error != ERROR_ACCESS_DENIED) {
                            LOG.error("Failed to get process token for process {}: {}", proc.getProcessID(),
                                    Kernel32.INSTANCE.GetLastError());
                        }
                    }
                }
                Kernel32.INSTANCE.CloseHandle(pHandle);
            }
            switch (((Long) procs.get("ExecutionState").get(p)).intValue()) {
            case READY:
            case SUSPENDED_READY:
                proc.setState(OSProcess.State.SLEEPING);
                break;
            case BLOCKED:
            case SUSPENDED_BLOCKED:
                proc.setState(OSProcess.State.WAITING);
                break;
            case RUNNING:
                proc.setState(OSProcess.State.RUNNING);
                break;
            case GROWING:
                proc.setState(OSProcess.State.NEW);
                break;
            case TERMINATED:
                proc.setState(OSProcess.State.ZOMBIE);
                break;
            case STOPPED:
                proc.setState(OSProcess.State.STOPPED);
                break;
            case UNKNOWN:
            case OTHER:
            default:
                proc.setState(OSProcess.State.OTHER);
                break;
            }
            proc.setThreadCount(((Long) procs.get("ThreadCount").get(p)).intValue());
            proc.setPriority(((Long) procs.get("Priority").get(p)).intValue());
            proc.setVirtualSize(ParseUtil.parseLongOrDefault((String) procs.get("VirtualSize").get(p), 0L));
            proc.setResidentSetSize(ParseUtil.parseLongOrDefault((String) procs.get("WorkingSetSize").get(p), 0L));
            // Kernel and User time units are 100ns
            proc.setKernelTime(ParseUtil.parseLongOrDefault((String) procs.get("KernelModeTime").get(p), 0L) / 10000L);
            proc.setUserTime(ParseUtil.parseLongOrDefault((String) procs.get("UserModeTime").get(p), 0L) / 10000L);
            proc.setStartTime((Long) procs.get("CreationDate").get(p));
            proc.setUpTime(now - proc.getStartTime());
            proc.setBytesRead((Long) procs.get("ReadTransferCount").get(p));
            proc.setBytesWritten((Long) procs.get("WriteTransferCount").get(p));
            proc.setOpenFiles((Long) procs.get("HandleCount").get(p));
            procList.add(proc);
        }
        return procList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessId() {
        return Kernel32.INSTANCE.GetCurrentProcessId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessCount() {
        PERFORMANCE_INFORMATION perfInfo = new PERFORMANCE_INFORMATION();
        if (!Psapi.INSTANCE.GetPerformanceInfo(perfInfo, perfInfo.size())) {
            LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
            return 0;
        }
        return perfInfo.ProcessCount.intValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThreadCount() {
        PERFORMANCE_INFORMATION perfInfo = new PERFORMANCE_INFORMATION();
        if (!Psapi.INSTANCE.GetPerformanceInfo(perfInfo, perfInfo.size())) {
            LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
            return 0;
        }
        return perfInfo.ThreadCount.intValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkParams getNetworkParams() {
        return new WindowsNetworkParams();
    }

    /**
     * Enables debug privileges for this process, required for OpenProcess() to
     * get processes other than the current user
     */
    private static void enableDebugPrivilege() {
        HANDLEByReference hToken = new HANDLEByReference();
        boolean success = Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(),
                WinNT.TOKEN_QUERY | WinNT.TOKEN_ADJUST_PRIVILEGES, hToken);
        if (!success) {
            LOG.error("OpenProcessToken failed. Error: {}" + Native.getLastError());
            return;
        }
        WinNT.LUID luid = new WinNT.LUID();
        success = Advapi32.INSTANCE.LookupPrivilegeValue(null, WinNT.SE_DEBUG_NAME, luid);
        if (!success) {
            LOG.error("LookupprivilegeValue failed. Error: {}" + Native.getLastError());
            return;
        }
        WinNT.TOKEN_PRIVILEGES tkp = new WinNT.TOKEN_PRIVILEGES(1);
        tkp.Privileges[0] = new WinNT.LUID_AND_ATTRIBUTES(luid, new DWORD(WinNT.SE_PRIVILEGE_ENABLED));
        success = Advapi32.INSTANCE.AdjustTokenPrivileges(hToken.getValue(), false, tkp, 0, null, null);
        if (!success) {
            LOG.error("AdjustTokenPrivileges failed. Error: {}" + Native.getLastError());
        }
        Kernel32.INSTANCE.CloseHandle(hToken.getValue());
    }
}
