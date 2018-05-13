/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Advapi32Util.Account;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.Psapi.PERFORMANCE_INFORMATION;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinBase.FILETIME;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.windows.Kernel32;
import oshi.jna.platform.windows.Kernel32.IO_COUNTERS;
import oshi.jna.platform.windows.Wtsapi32;
import oshi.jna.platform.windows.Wtsapi32.WTS_PROCESS_INFO_EX;
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

    // For WMI Process queries for command line
    private static final String processProperties = "ProcessID,CommandLine";
    private static final ValueType[] processPropertyTypes = { ValueType.UINT32, ValueType.STRING };

    // For WMI Process queries for private working set
    private static final String workingSetPrivateProperties = "IDProcess,WorkingSetPrivate";
    private static final ValueType[] workingSetPrivatePropertyTypes = { ValueType.UINT32, ValueType.STRING };

    // For WMI Process queries for start time, IO counters
    private static final String perfRawDataProperties = "IDProcess,ElapsedTime,Timestamp_Sys100NS,IOReadBytesPerSec,IOWriteBytesPerSec";
    private static final ValueType[] perfRawDataPropertyTypes = { ValueType.UINT32, ValueType.STRING, ValueType.STRING,
            ValueType.STRING, ValueType.STRING };
    /*
     * LastError
     */
    private static final int ERROR_ACCESS_DENIED = 5;

    static {
        enableDebugPrivilege();
    }

    /*
     * This process map will cache process info to avoid repeated calls for data
     */
    private final Map<Integer, OSProcess> processMap = new HashMap<>();

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
        List<OSProcess> procList = processMapToList(null);
        List<OSProcess> sorted = processSort(procList, limit, sort);
        return sorted.toArray(new OSProcess[sorted.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess[] getChildProcesses(int parentPid, int limit, ProcessSort sort) {
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
        return sorted.toArray(new OSProcess[sorted.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess getProcess(int pid) {
        Set<Integer> pids = new HashSet<>(1);
        pids.add(pid);
        List<OSProcess> procList = processMapToList(pids);
        return procList.isEmpty() ? null : procList.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OSProcess> getProcesses(Collection<Integer> pids) {
        return processMapToList(pids);
    }

    /**
     * Private method to do the heavy lifting for all the getProcess functions.
     * 
     * @param pids
     *            A collection of pids to query. If null, the entire process
     *            list will be queried.
     * @return A corresponding list of processes
     */
    private List<OSProcess> processMapToList(Collection<Integer> pids) {
        long now = System.currentTimeMillis();
        List<OSProcess> procList = new ArrayList<>();
        // define here to avoid object creation overhead later
        // For process times
        FILETIME lpCreationTime = new FILETIME();
        FILETIME lpExitTime = new FILETIME();
        FILETIME lpKernelTime = new FILETIME();
        FILETIME lpUserTime = new FILETIME();
        // For read/write bytes
        IO_COUNTERS lpIoCounters = new IO_COUNTERS();
        // For groups
        List<String> groupList = new ArrayList<>();
        List<String> groupIDList = new ArrayList<>();
        // Track when data fails and we need fallback
        Set<Integer> wmiDataNeeded = new HashSet<>();

        int myPid = getProcessId();
        Map<Integer, OSProcess> tempProcessMap = new HashMap<>();

        // Populate WMI process list with Private Working Set size
        Map<String, List<Object>> procs = null;
        // If there's a list of pids, filter the WHERE clause
        if (pids != null) {
            StringBuilder query = new StringBuilder("WHERE ");
            for (Integer pid : pids) {
                query.append(String.format("IDProcess=%d OR ", pid));
            }
            query.setLength(query.length() - 3);
            procs = WmiUtil.selectObjectsFrom(null, "Win32_PerfRawData_PerfProc_Process", workingSetPrivateProperties,
                    query.toString(), workingSetPrivatePropertyTypes);
        } else {
            procs = WmiUtil.selectObjectsFrom(null, "Win32_PerfRawData_PerfProc_Process", workingSetPrivateProperties,
                    null, workingSetPrivatePropertyTypes);
            // If querying the whole process list, Remove any non-existent
            // processes from the global process map/cache
            List<Object> processIDs = procs.get("IDProcess");
            List<Integer> pidsToKeep = new ArrayList<>(processIDs.size());
            for (Object pid : processIDs) {
                pidsToKeep.add(((Long) pid).intValue());
            }
            for (Integer pid : processMap.keySet()) {
                if (!pidsToKeep.contains(pid)) {
                    processMap.remove(pid);
                }
            }
        }

        // Get processes from WTS, for additional info
        final PointerByReference ppProcessInfo = new PointerByReference();
        IntByReference pCount = new IntByReference(0);
        if (!Wtsapi32.INSTANCE.WTSEnumerateProcessesEx(Wtsapi32.WTS_CURRENT_SERVER_HANDLE,
                Wtsapi32.WTSTypeProcessInfoLevel1, Wtsapi32.WTS_ANY_SESSION, ppProcessInfo, pCount)) {
            LOG.error("Failed to enumerate Processes. Error code: {}", Kernel32.INSTANCE.GetLastError());
            return procList;
        }
        // extract the pointed-to pointer
        final Pointer pProcessInfo = ppProcessInfo.getValue();
        final WTS_PROCESS_INFO_EX processInfoRef = new WTS_PROCESS_INFO_EX(pProcessInfo);
        processInfoRef.read();
        final WTS_PROCESS_INFO_EX[] processInfo = (WTS_PROCESS_INFO_EX[]) processInfoRef.toArray(pCount.getValue());

        for (WTS_PROCESS_INFO_EX procInfo : processInfo) {
            // Only consider processes passed to this method
            if (pids != null && !pids.contains(procInfo.ProcessId.intValue())) {
                continue;
            }
            OSProcess proc = new OSProcess();
            proc.setProcessID(procInfo.ProcessId.intValue());
            proc.setName(procInfo.pProcessName);
            // For my own process, set CWD
            if (myPid == proc.getProcessID()) {
                proc.setCurrentWorkingDirectory(new File(".").getAbsolutePath());
            }

            proc.setKernelTime(procInfo.KernelTime.getValue() / 10000L);
            proc.setUserTime(procInfo.UserTime.getValue() / 10000L);

            // Get a handle to the process for various extended info. Only gets
            // current user unless running as administrator
            final HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false,
                    proc.getProcessID());
            if (pHandle != null) {
                if (Kernel32.INSTANCE.GetProcessTimes(pHandle, lpCreationTime, lpExitTime, lpKernelTime, lpUserTime)) {
                    proc.setStartTime(lpCreationTime.toTime());
                    proc.setUpTime(now - proc.getStartTime());
                    // Kernel and User time units are already saved
                }
                // Read/Write bytes
                if (Kernel32.INSTANCE.GetProcessIoCounters(pHandle, lpIoCounters)) {
                    proc.setBytesRead(lpIoCounters.ReadTransferCount);
                    proc.setBytesWritten(lpIoCounters.WriteTransferCount);
                }
                // Full path
                proc.setPath(Kernel32Util.QueryFullProcessImageName(pHandle, 0));

                final HANDLEByReference phToken = new HANDLEByReference();
                if (Advapi32.INSTANCE.OpenProcessToken(pHandle, WinNT.TOKEN_DUPLICATE | WinNT.TOKEN_QUERY, phToken)) {
                    Account account = Advapi32Util.getTokenAccount(phToken.getValue());
                    proc.setUser(account.name);
                    proc.setUserID(account.sidString);
                    // Fetching group information incurs ~10ms latency per
                    // process. Skip for full process list
                    if (pids != null) {
                        Account[] accounts = Advapi32Util.getTokenGroups(phToken.getValue());
                        // get groups
                        groupList.clear();
                        groupIDList.clear();
                        for (Account a : accounts) {
                            groupList.add(a.name);
                            groupIDList.add(a.sidString);
                        }
                        proc.setGroup(FormatUtil.join(",", groupList));
                        proc.setGroupID(FormatUtil.join(",", groupIDList));
                    }
                } else {
                    int error = Kernel32.INSTANCE.GetLastError();
                    // Access denied errors are common. Fail silently.
                    if (error != ERROR_ACCESS_DENIED) {
                        LOG.error("Failed to get process token for process {}: {}", proc.getProcessID(),
                                Kernel32.INSTANCE.GetLastError());
                    }
                }
            } else {
                // Get Data from WMI
                wmiDataNeeded.add(proc.getProcessID());
            }
            Kernel32.INSTANCE.CloseHandle(pHandle);

            // TODO: There is no easy way to get ExecutuionState for a process.
            // The WMI value is null. It's possible to get thread Execution
            // State and possibly roll up.
            proc.setState(OSProcess.State.OTHER);

            proc.setThreadCount(procInfo.NumberOfThreads.intValue());
            proc.setVirtualSize(procInfo.PagefileUsage.longValue());
            // This is the Working Set, not the Private Working Set
            // Set here as a default and overwrite later with data from WMI
            proc.setResidentSetSize(procInfo.WorkingSetSize.longValue());

            proc.setOpenFiles(procInfo.HandleCount.intValue());
            tempProcessMap.put(proc.getProcessID(), proc);
        }
        // Clean up memory allocated in C
        if (!Wtsapi32.INSTANCE.WTSFreeMemoryEx(Wtsapi32.WTSTypeProcessInfoLevel1.getValue(), pProcessInfo,
                pCount.getValue())) {
            LOG.error("Failed to Free Memory for Processes. Error code: {}", Kernel32.INSTANCE.GetLastError());
            return procList;
        }

        // Get processes from ToolHelp API for parent PID and priority info
        Tlhelp32.PROCESSENTRY32.ByReference processEntry = new Tlhelp32.PROCESSENTRY32.ByReference();
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new DWORD(0));
        try {
            while (Kernel32.INSTANCE.Process32Next(snapshot, processEntry)) {
                // Only consider processes passed to this method
                if (!procs.containsKey(processEntry.th32ProcessID.intValue())) {
                    continue;
                }
                if (tempProcessMap.containsKey(processEntry.th32ProcessID.intValue())) {
                    OSProcess proc = tempProcessMap.get(processEntry.th32ProcessID.intValue());
                    proc.setThreadCount(processEntry.cntThreads.intValue());
                    proc.setParentProcessID(processEntry.th32ParentProcessID.intValue());
                    proc.setPriority(processEntry.pcPriClassBase.intValue());
                }
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }

        // Update RSS from Private Working Set value.
        // Reuse the proc object since we're done with the loop
        for (int p = 0; p < procs.get("IDProcess").size() - 1; p++) {
            // Last line "total" appears with PID 0, so iterating to size - 1
            int pwsPid = ((Long) procs.get("IDProcess").get(p)).intValue();
            if (tempProcessMap.containsKey(pwsPid)) {
                OSProcess proc = tempProcessMap.get(pwsPid);
                proc.setResidentSetSize(
                        ParseUtil.parseLongOrDefault((String) procs.get("WorkingSetPrivate").get(p), 0L));
            }
        }

        // Update start time and IO
        if (!wmiDataNeeded.isEmpty()) {
            StringBuilder query = new StringBuilder("WHERE ");
            for (Integer pid : wmiDataNeeded) {
                query.append(String.format("IDProcess=%d OR ", pid));
            }
            query.setLength(query.length() - 3);
            Map<String, List<Object>> rawDataProcs = WmiUtil.selectObjectsFrom(null,
                    "Win32_PerfRawData_PerfProc_Process", perfRawDataProperties, query.toString(),
                    perfRawDataPropertyTypes);
            for (int p = 0; p < rawDataProcs.get("IDProcess").size(); p++) {
                int pid = ((Long) rawDataProcs.get("IDProcess").get(p)).intValue();
                if (tempProcessMap.containsKey(pid)) {
                    OSProcess proc = tempProcessMap.get(pid);
                    proc.setUpTime(
                            (ParseUtil.parseLongOrDefault((String) rawDataProcs.get("Timestamp_Sys100NS").get(p), 0L)
                                    - ParseUtil.parseLongOrDefault((String) rawDataProcs.get("ElapsedTime").get(p), 0L))
                                    / 10000L);
                    proc.setStartTime(now - proc.getUpTime());

                    proc.setBytesRead(
                            ParseUtil.parseLongOrDefault((String) rawDataProcs.get("IOReadBytesPerSec").get(p), 0L));
                    proc.setBytesWritten(
                            ParseUtil.parseLongOrDefault((String) rawDataProcs.get("IOWriteBytesPerSec").get(p), 0L));
                    // If start time is newer than cached version, delete cache
                    if (processMap.containsKey(proc.getProcessID())
                            && processMap.get(proc.getProcessID()).getStartTime() < proc.getStartTime()) {
                        processMap.remove(proc.getProcessID());
                    }
                }
            }
        }
        // Update Command Line
        Set<Integer> emptyCommandLines = new HashSet<>();
        for (Integer pid : tempProcessMap.keySet()) {
            if (processMap.containsKey(pid) && processMap.get(pid).getCommandLine().isEmpty()) {
                emptyCommandLines.add(pid);
            }
        }
        if (!emptyCommandLines.isEmpty()) {
            StringBuilder query = new StringBuilder("WHERE ");
            for (Integer pid : emptyCommandLines) {
                query.append(String.format("ProcessID=%d OR ", pid));
            }
            query.setLength(query.length() - 3);
            Map<String, List<Object>> commandLineProcs = WmiUtil.selectObjectsFrom(null, "Win32_Process",
                    processProperties, query.toString(), processPropertyTypes);
            for (int p = 0; p < commandLineProcs.get("ProcessID").size(); p++) {
                int pid = ((Long) commandLineProcs.get("ProcessID").get(p)).intValue();
                if (tempProcessMap.containsKey(pid)) {
                    OSProcess proc = tempProcessMap.get(pid);
                    proc.setCommandLine((String) commandLineProcs.get("CommandLine").get(p));
                }
            }
        }

        // Update cached list
        for (OSProcess p : tempProcessMap.values()) {
            processMap.put(p.getProcessID(), p);
            procList.add(p);
        }

        return new ArrayList<>(tempProcessMap.values());
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
            LOG.error("OpenProcessToken failed. Error: {}", Native.getLastError());
            return;
        }
        WinNT.LUID luid = new WinNT.LUID();
        success = Advapi32.INSTANCE.LookupPrivilegeValue(null, WinNT.SE_DEBUG_NAME, luid);
        if (!success) {
            LOG.error("LookupprivilegeValue failed. Error: {}", Native.getLastError());
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

}
