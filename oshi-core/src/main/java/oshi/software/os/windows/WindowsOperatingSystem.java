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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; // NOSONAR
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Advapi32Util.Account;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.Psapi.PERFORMANCE_INFORMATION;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.windows.Kernel32;
import oshi.jna.platform.windows.Pdh;
import oshi.jna.platform.windows.WbemcliUtil.WmiQuery;
import oshi.jna.platform.windows.WbemcliUtil.WmiResult;
import oshi.jna.platform.windows.WinPerf.PERF_COUNTER_BLOCK;
import oshi.jna.platform.windows.WinPerf.PERF_COUNTER_DEFINITION;
import oshi.jna.platform.windows.WinPerf.PERF_DATA_BLOCK;
import oshi.jna.platform.windows.WinPerf.PERF_INSTANCE_DEFINITION;
import oshi.jna.platform.windows.WinPerf.PERF_OBJECT_TYPE;
import oshi.jna.platform.windows.Wtsapi32;
import oshi.jna.platform.windows.Wtsapi32.WTS_PROCESS_INFO_EX;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.util.FormatUtil;
import oshi.util.platform.windows.WmiUtil;

public class WindowsOperatingSystem extends AbstractOperatingSystem {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOperatingSystem.class);

    enum BitnessProperty {
        ADDRESSWIDTH;
    }

    enum ProcessProperty {
        PROCESSID, COMMANDLINE;
    }

    private static final String PROCESS_BASE_CLASS = "Win32_Process";
    private static final WmiQuery<ProcessProperty> PROCESS_QUERY = new WmiQuery<>(null, ProcessProperty.class);

    /*
     * Registry variables to persist
     */
    private int perfDataBufferSize = 8192;
    private int processIndex;
    private String processIndexStr;

    /*
     * Registry counter block offsets
     */
    private int priorityBaseOffset; // 92
    private int elapsedTimeOffset; // 96
    private int idProcessOffset; // 104
    private int creatingProcessIdOffset; // 108
    private int ioReadOffset; // 160
    private int ioWriteOffset; // 168
    private int workingSetPrivateOffset; // 192

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
        initRegistry();
        initBitness();
    }

    private void initRegistry() {
        // Get the title indices
        DWORDByReference index = new DWORDByReference();
        Pdh.INSTANCE.PdhLookupPerfIndexByName(null, "Process", index);
        processIndex = index.getValue().intValue();
        processIndexStr = Integer.toString(processIndex);
        Pdh.INSTANCE.PdhLookupPerfIndexByName(null, "Priority Base", index);
        int priorityBaseIndex = index.getValue().intValue();
        Pdh.INSTANCE.PdhLookupPerfIndexByName(null, "Elapsed Time", index);
        int elapsedTimeIndex = index.getValue().intValue();
        Pdh.INSTANCE.PdhLookupPerfIndexByName(null, "ID Process", index);
        int idProcessIndex = index.getValue().intValue();
        Pdh.INSTANCE.PdhLookupPerfIndexByName(null, "Creating Process ID", index);
        int creatingProcessIdIndex = index.getValue().intValue();
        Pdh.INSTANCE.PdhLookupPerfIndexByName(null, "IO Read Bytes/sec", index);
        int ioReadIndex = index.getValue().intValue();
        Pdh.INSTANCE.PdhLookupPerfIndexByName(null, "IO Write Bytes/sec", index);
        int ioWriteIndex = index.getValue().intValue();
        Pdh.INSTANCE.PdhLookupPerfIndexByName(null, "Working Set - Private", index);
        int workingSetPrivateIndex = index.getValue().intValue();

        // now load the Process registry to match up the offsets
        // Sequentially increase the buffer until everything fits.
        // Save this buffer size for later use
        IntByReference lpcbData = new IntByReference(this.perfDataBufferSize);
        Pointer pPerfData = new Memory(this.perfDataBufferSize);
        while (WinError.ERROR_MORE_DATA == Advapi32.INSTANCE.RegQueryValueEx(WinReg.HKEY_PERFORMANCE_DATA,
                processIndexStr, 0, null, pPerfData, lpcbData)) {
            this.perfDataBufferSize += 4096;
            lpcbData.setValue(this.perfDataBufferSize);
            pPerfData = new Memory(this.perfDataBufferSize);
        }

        PERF_DATA_BLOCK perfData = new PERF_DATA_BLOCK(pPerfData.share(0));

        // See format at
        // https://msdn.microsoft.com/en-us/library/windows/desktop/aa373105(v=vs.85).aspx
        // [ ] Object Type
        // [ ][ ][ ] Multiple counter definitions
        // Then multiple:
        // [ ] Instance Definition
        // [ ] Instance name
        // [ ] Counter Block
        // [ ][ ][ ] Counter data for each definition above

        long perfObjectOffset = perfData.HeaderLength;

        // Iterate object types. For Process should only be one here
        for (int obj = 0; obj < perfData.NumObjectTypes; obj++) {
            PERF_OBJECT_TYPE perfObject = new PERF_OBJECT_TYPE(pPerfData.share(perfObjectOffset));
            // Identify where counter definitions start
            long perfCounterOffset = perfObjectOffset + perfObject.HeaderLength;
            // If this isn't the Process object, ignore
            if (perfObject.ObjectNameTitleIndex == processIndex) {
                for (int counter = 0; counter < perfObject.NumCounters; counter++) {
                    PERF_COUNTER_DEFINITION perfCounter = new PERF_COUNTER_DEFINITION(
                            pPerfData.share(perfCounterOffset));
                    if (perfCounter.CounterNameTitleIndex == priorityBaseIndex) {
                        this.priorityBaseOffset = perfCounter.CounterOffset;
                    } else if (perfCounter.CounterNameTitleIndex == elapsedTimeIndex) {
                        this.elapsedTimeOffset = perfCounter.CounterOffset;
                    } else if (perfCounter.CounterNameTitleIndex == creatingProcessIdIndex) {
                        this.creatingProcessIdOffset = perfCounter.CounterOffset;
                    } else if (perfCounter.CounterNameTitleIndex == idProcessIndex) {
                        this.idProcessOffset = perfCounter.CounterOffset;
                    } else if (perfCounter.CounterNameTitleIndex == ioReadIndex) {
                        this.ioReadOffset = perfCounter.CounterOffset;
                    } else if (perfCounter.CounterNameTitleIndex == ioWriteIndex) {
                        this.ioWriteOffset = perfCounter.CounterOffset;
                    } else if (perfCounter.CounterNameTitleIndex == workingSetPrivateIndex) {
                        this.workingSetPrivateOffset = perfCounter.CounterOffset;
                    }
                    // Increment for next Counter
                    perfCounterOffset += perfCounter.ByteLength;
                }
                // We're done, break the loop
                break;
            }
            // Increment for next object (should never need this)
            perfObjectOffset += perfObject.TotalByteLength;
        }
    }

    private void initBitness() {
        // If bitness is 64 we are 64 bit.
        // If 32 test if we are on 64-bit OS
        if (bitness < 64) {
            // Try the easy way
            if (System.getenv("ProgramFiles(x86)") != null) {
                this.bitness = 64;
            } else {
                WmiQuery<BitnessProperty> bitnessQuery = new WmiQuery<>("Win32_Processor", BitnessProperty.class);
                WmiResult<BitnessProperty> bitnessMap = WmiUtil.queryWMI(bitnessQuery);
                if (bitnessMap.getResultCount() > 0) {
                    this.bitness = WmiUtil.getUint16(bitnessMap, BitnessProperty.ADDRESSWIDTH, 0);
                }
            }
        }
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
        // Get data from the registry to update cache
        updateProcessMapFromRegistry(pids);

        // define here to avoid object repeated creation overhead later
        List<String> groupList = new LinkedList<>();
        List<String> groupIDList = new LinkedList<>();
        int myPid = getProcessId();

        // Get processes from WTS
        final PointerByReference ppProcessInfo = new PointerByReference();
        IntByReference pCount = new IntByReference(0);
        if (!Wtsapi32.INSTANCE.WTSEnumerateProcessesEx(Wtsapi32.WTS_CURRENT_SERVER_HANDLE,
                new IntByReference(Wtsapi32.WTS_PROCESS_INFO_LEVEL_1), Wtsapi32.WTS_ANY_SESSION, ppProcessInfo,
                pCount)) {
            LOG.error("Failed to enumerate Processes. Error code: {}", Kernel32.INSTANCE.GetLastError());
            return new ArrayList<>(0);
        }
        // extract the pointed-to pointer and create array
        final Pointer pProcessInfo = ppProcessInfo.getValue();
        final WTS_PROCESS_INFO_EX processInfoRef = new WTS_PROCESS_INFO_EX(pProcessInfo);
        final WTS_PROCESS_INFO_EX[] processInfo = (WTS_PROCESS_INFO_EX[]) processInfoRef.toArray(pCount.getValue());

        // Store a subset of processes in a list to later return.
        List<OSProcess> processList = new LinkedList<>();

        for (WTS_PROCESS_INFO_EX procInfo : processInfo) {
            // Skip if only updating a subset of pids, or if not in cache.
            // (Cache should have just been updated from registry so this will
            // only occur in a race condition for a just-started process.)
            // However, when the cache is empty, there was a problem with
            // filling
            // the cache using performance information. When this happens, we
            // ignore
            // the cache completely.

            int pid = procInfo.ProcessId;
            OSProcess proc = null;
            if (this.processMap.isEmpty()) {
                if (pids != null && !pids.contains(pid)) {
                    continue;
                }
                proc = new OSProcess();
                proc.setProcessID(pid);
                proc.setName(procInfo.pProcessName);
            } else {
                proc = this.processMap.get(pid);
                if (proc == null || (pids != null && !pids.contains(pid))) {
                    continue;
                }
            }
            // For my own process, set CWD
            if (pid == myPid) {
                String cwd = new File(".").getAbsolutePath();
                // trim off trailing "."
                proc.setCurrentWorkingDirectory(cwd.isEmpty() ? "" : cwd.substring(0, cwd.length() - 1));
            }

            proc.setKernelTime(procInfo.KernelTime.getValue() / 10000L);
            proc.setUserTime(procInfo.UserTime.getValue() / 10000L);

            // Get a handle to the process for various extended info. Only gets
            // current user unless running as administrator
            final HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false,
                    proc.getProcessID());
            if (pHandle != null) {
                // Full path
                try {
                    proc.setPath(Kernel32Util.QueryFullProcessImageName(pHandle, 0));
                } catch (Win32Exception e) {
                    LOG.warn("Failed to set path on PID {}. It may have terminated.", proc.getProcessID());
                }

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
                    if (error != WinError.ERROR_ACCESS_DENIED) {
                        LOG.error("Failed to get process token for process {}: {}", proc.getProcessID(),
                                Kernel32.INSTANCE.GetLastError());
                    }
                }
            }
            Kernel32.INSTANCE.CloseHandle(pHandle);

            // There is no easy way to get ExecutuionState for a process.
            // The WMI value is null. It's possible to get thread Execution
            // State and possibly roll up.
            proc.setState(OSProcess.State.RUNNING);

            proc.setThreadCount(procInfo.NumberOfThreads);
            proc.setVirtualSize(procInfo.PagefileUsage & 0xffff_ffffL);

            proc.setOpenFiles(procInfo.HandleCount);
            processList.add(proc);
        }
        // Clean up memory allocated in C
        if (!Wtsapi32.INSTANCE.WTSFreeMemoryEx(Wtsapi32.WTS_PROCESS_INFO_LEVEL_1, pProcessInfo, pCount.getValue())) {
            LOG.error("Failed to Free Memory for Processes. Error code: {}", Kernel32.INSTANCE.GetLastError());
            return new ArrayList<>(0);
        }

        // Command Line only accessible via WMI.
        // Utilize cache to only update new processes
        Set<Integer> emptyCommandLines = new HashSet<>();
        for (OSProcess cachedProcess : processList) {
            // If the process in the cache has an empty command line.
            if (cachedProcess.getCommandLine().isEmpty()) {
                emptyCommandLines.add(cachedProcess.getProcessID());
            }
        }
        if (!emptyCommandLines.isEmpty()) {
            StringBuilder sb = new StringBuilder(PROCESS_BASE_CLASS);
            boolean first = true;
            for (Integer pid : emptyCommandLines) {
                if (first) {
                    sb.append(" WHERE ProcessID=");
                    first = false;
                } else {
                    sb.append(" OR ProcessID=");
                }
                sb.append(pid);
            }
            PROCESS_QUERY.setWmiClassName(sb.toString());
            WmiResult<ProcessProperty> commandLineProcs = WmiUtil.queryWMI(PROCESS_QUERY);

            for (int p = 0; p < commandLineProcs.getResultCount(); p++) {
                int pid = WmiUtil.getUint32(commandLineProcs, ProcessProperty.PROCESSID, p);
                // This should always be true because emptyCommandLines was
                // built from a subset of the cache, but just in case, protect
                // against dereferencing null
                if (this.processMap.containsKey(pid)) {
                    OSProcess proc = this.processMap.get(pid);
                    proc.setCommandLine(WmiUtil.getString(commandLineProcs, ProcessProperty.COMMANDLINE, p));
                }
            }
        }

        return processList;
    }

    private void updateProcessMapFromRegistry(Collection<Integer> pids) {
        List<Integer> pidsToKeep = new LinkedList<>();

        // Grab the PERF_DATA_BLOCK from the registry.
        // Sequentially increase the buffer until everything fits.
        IntByReference lpcbData = new IntByReference(this.perfDataBufferSize);
        Pointer pPerfData = new Memory(this.perfDataBufferSize);
        while (WinError.ERROR_MORE_DATA == Advapi32.INSTANCE.RegQueryValueEx(WinReg.HKEY_PERFORMANCE_DATA,
                processIndexStr, 0, null, pPerfData, lpcbData)) {
            this.perfDataBufferSize += 4096;
            lpcbData.setValue(this.perfDataBufferSize);
            pPerfData = new Memory(this.perfDataBufferSize);
        }

        PERF_DATA_BLOCK perfData = new PERF_DATA_BLOCK(pPerfData.share(0));
        long perfTime100nSec = perfData.PerfTime100nSec.getValue(); // 1601
        long now = System.currentTimeMillis(); // 1970 epoch

        // See format at
        // https://msdn.microsoft.com/en-us/library/windows/desktop/aa373105(v=vs.85).aspx
        // [ ] Object Type
        // [ ][ ][ ] Multiple counter definitions
        // Then multiple:
        // [ ] Instance Definition
        // [ ] Instance name
        // [ ] Counter Block
        // [ ][ ][ ] Counter data for each definition above

        long perfObjectOffset = perfData.HeaderLength;

        // Iterate object types. For Process should only be one here
        for (int obj = 0; obj < perfData.NumObjectTypes; obj++) {
            PERF_OBJECT_TYPE perfObject = new PERF_OBJECT_TYPE(pPerfData.share(perfObjectOffset));
            // If this isn't the Process object, ignore
            if (perfObject.ObjectNameTitleIndex == processIndex) {
                // Skip over counter definitions
                // There will be many of these, this points to the first one
                long perfInstanceOffset = perfObjectOffset + perfObject.DefinitionLength;

                // We need this for every process, initialize outside loop to
                // save overhead
                PERF_COUNTER_BLOCK perfCounterBlock = null;
                // Iterate instances.
                // The last instance is _Total so subtract 1 from max
                for (int inst = 0; inst < perfObject.NumInstances - 1; inst++) {
                    PERF_INSTANCE_DEFINITION perfInstance = new PERF_INSTANCE_DEFINITION(
                            pPerfData.share(perfInstanceOffset));
                    long perfCounterBlockOffset = perfInstanceOffset + perfInstance.ByteLength;

                    int pid = pPerfData.getInt(perfCounterBlockOffset + this.idProcessOffset);
                    if (pids == null || pids.contains(pid)) {
                        pidsToKeep.add(pid);

                        // If process exists fetch from cache
                        OSProcess proc = null;
                        if (this.processMap.containsKey(pid)) {
                            proc = this.processMap.get(pid);
                        }
                        // If not in cache or if start time differs too much,
                        // create new process to add/replace cache value
                        long upTime = (perfTime100nSec
                                - pPerfData.getLong(perfCounterBlockOffset + this.elapsedTimeOffset)) / 10_000L;
                        long startTime = now - upTime;
                        if (proc == null || Math.abs(startTime - proc.getStartTime()) > 200) {
                            proc = new OSProcess();
                            proc.setProcessID(pid);
                            proc.setStartTime(startTime);
                            proc.setName(pPerfData.getWideString(perfInstanceOffset + perfInstance.NameOffset));
                            // Adds or replaces previous
                            this.processMap.put(pid, proc);
                        }

                        // Update stats
                        proc.setUpTime(upTime < 1L ? 1L : upTime);
                        proc.setBytesRead(pPerfData.getLong(perfCounterBlockOffset + this.ioReadOffset));
                        proc.setBytesWritten(pPerfData.getLong(perfCounterBlockOffset + this.ioWriteOffset));
                        proc.setResidentSetSize(
                                pPerfData.getLong(perfCounterBlockOffset + this.workingSetPrivateOffset));
                        proc.setParentProcessID(
                                pPerfData.getInt(perfCounterBlockOffset + this.creatingProcessIdOffset));
                        proc.setPriority(pPerfData.getInt(perfCounterBlockOffset + this.priorityBaseOffset));
                    }

                    // Increment to next instance
                    perfCounterBlock = new PERF_COUNTER_BLOCK(pPerfData.share(perfCounterBlockOffset));
                    perfInstanceOffset = perfCounterBlockOffset + perfCounterBlock.ByteLength;
                }
                // We've found the process object and are done, no need to look
                // at any other objects (shouldn't be any). Break the loop
                break;
            }
            // Increment for next object (should never need this)
            perfObjectOffset += perfObject.TotalByteLength;
        }
        // If this was a full update, delete any pid we didn't find from the
        // cache.
        if (pids == null) {
            for (Integer pid : new HashSet<>(this.processMap.keySet())) {
                if (!pidsToKeep.contains(pid)) {
                    this.processMap.remove(pid);
                }
            }
        }
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
