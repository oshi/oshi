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
import oshi.software.os.OSProcess;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;

public class WindowsOperatingSystem extends AbstractOperatingSystem {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOperatingSystem.class);

    // For WMI Process queries
    private static String processProperties = "Name,CommandLine,ExecutionState,ProcessID,ParentProcessId"
            + ",ThreadCount,Priority,VirtualSize,WorkingSetSize,KernelModeTime,UserModeTime,CreationDate"
            + ",ReadTransferCount,WriteTransferCount";
    private static ValueType[] processPropertyTypes = { ValueType.STRING, ValueType.STRING, ValueType.UINT32,
            ValueType.UINT32, ValueType.UINT32, ValueType.UINT32, ValueType.UINT32, ValueType.STRING, ValueType.STRING,
            ValueType.STRING, ValueType.STRING, ValueType.DATETIME, ValueType.UINT64, ValueType.UINT64 };

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
            procList.add(new WindowsProcess((String) procs.get("Name").get(p), (String) procs.get("CommandLine").get(p),
                    ((Long) procs.get("ExecutionState").get(p)).intValue(),
                    ((Long) procs.get("ProcessID").get(p)).intValue(),
                    ((Long) procs.get("ParentProcessId").get(p)).intValue(),
                    ((Long) procs.get("ThreadCount").get(p)).intValue(),
                    ((Long) procs.get("Priority").get(p)).intValue(),
                    ParseUtil.parseLongOrDefault((String) procs.get("VirtualSize").get(p), 0L),
                    ParseUtil.parseLongOrDefault((String) procs.get("WorkingSetSize").get(p), 0L),
                    // Kernel and User time units are 100ns
                    ParseUtil.parseLongOrDefault((String) procs.get("KernelModeTime").get(p), 0L) / 10000L,
                    ParseUtil.parseLongOrDefault((String) procs.get("UserModeTime").get(p), 0L) / 10000L,
                    (Long) procs.get("CreationDate").get(p), (Long) procs.get("ReadTransferCount").get(p),
                    (Long) procs.get("WriteTransferCount").get(p), now));
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
}
