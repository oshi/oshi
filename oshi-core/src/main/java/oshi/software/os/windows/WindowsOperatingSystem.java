/**
 * Oshi (https://github.com/dblock/oshi)
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.windows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.jna.platform.windows.Kernel32;
import oshi.jna.platform.windows.Psapi;
import oshi.jna.platform.windows.Psapi.PERFORMANCE_INFORMATION;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;

public class WindowsOperatingSystem extends AbstractOperatingSystem {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOperatingSystem.class);

    // For WMI Process queries
    private static String processProperties = "Name,ExecutablePath,CommandLine,ExecutionState,ProcessID,ParentProcessId"
            + ",ThreadCount,Priority,VirtualSize,WorkingSetSize,KernelModeTime,UserModeTime,CreationDate"
            + ",ReadTransferCount,WriteTransferCount,__PATH,__PATH";
    private static ValueType[] processPropertyTypes = { ValueType.STRING, ValueType.STRING, ValueType.STRING,
            ValueType.UINT32, ValueType.UINT32, ValueType.UINT32, ValueType.UINT32, ValueType.UINT32, ValueType.STRING,
            ValueType.STRING, ValueType.STRING, ValueType.STRING, ValueType.DATETIME, ValueType.UINT64,
            ValueType.UINT64, ValueType.PROCESS_GETOWNER, ValueType.PROCESS_GETOWNERSID };

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
        // All map lists should be the same length. Pick one size and iterate
        for (int p = 0; p < procs.get("Name").size(); p++) {
            OSProcess proc = new OSProcess();
            proc.setName((String) procs.get("Name").get(p));
            proc.setPath((String) procs.get("ExecutablePath").get(p));
            proc.setCommandLine((String) procs.get("CommandLine").get(p));
            proc.setUser((String) procs.get("PROCESS_GETOWNER").get(p));
            proc.setUserID((String) procs.get("PROCESS_GETOWNERSID").get(p));
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
            proc.setProcessID(((Long) procs.get("ProcessID").get(p)).intValue());
            proc.setParentProcessID(((Long) procs.get("ParentProcessId").get(p)).intValue());
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
}
