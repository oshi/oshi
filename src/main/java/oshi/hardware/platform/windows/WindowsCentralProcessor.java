/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION;
import com.sun.jna.platform.win32.WinReg;

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.windows.Kernel32;
import oshi.jna.platform.windows.Psapi;
import oshi.jna.platform.windows.Psapi.PERFORMANCE_INFORMATION;
import oshi.software.os.OSProcess;
import oshi.software.os.windows.WindowsProcess;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;

/**
 * A CPU as defined in Windows registry.
 * 
 * @author dblock[at]dblock[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 * @author widdis[at]gmail[dot]com
 */
public class WindowsCentralProcessor extends AbstractCentralProcessor {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsCentralProcessor.class);

    // For WMI Process queries
    private static String processProperties = "Name,CommandLine,ExecutionState,ProcessID,ParentProcessId"
            + ",ThreadCount,Priority,VirtualSize,WorkingSetSize,KernelModeTime,UserModeTime,CreationDate";
    private static ValueType[] processPropertyTypes = { ValueType.STRING, ValueType.STRING, ValueType.LONG,
            ValueType.LONG, ValueType.LONG, ValueType.LONG, ValueType.LONG, ValueType.STRING, ValueType.STRING,
            ValueType.STRING, ValueType.STRING, ValueType.DATETIME };

    // Compare WMI ticks to GetSystemTimes to determine conversion
    private static final long TICKS_PER_MILLISECOND;
    static {
        // Get total time = Kernel (includes idle) + User
        WinBase.FILETIME lpIdleTime = new WinBase.FILETIME();
        WinBase.FILETIME lpKernelTime = new WinBase.FILETIME();
        WinBase.FILETIME lpUserTime = new WinBase.FILETIME();
        if (!Kernel32.INSTANCE.GetSystemTimes(lpIdleTime, lpKernelTime, lpUserTime)) {
            LOG.error("Failed to init system idle/kernel/user times. Error code: " + Native.getLastError());
        }
        // Units are in 100-ns, divide by 10000 for ms
        long mSec = (WinBase.FILETIME.dateToFileTime(lpKernelTime.toDate())
                + WinBase.FILETIME.dateToFileTime(lpUserTime.toDate())) / 10000L;
        // Get same info from WMI
        Map<String, List<String>> wmiTicks = WmiUtil.selectStringsFrom(null,
                "Win32_PerfRawData_Counters_ProcessorInformation",
                "PercentIdleTime,PercentPrivilegedTime,PercentUserTime", "WHERE Name=\"_Total\"");
        long ticks = 0L;
        if (wmiTicks.get("PercentIdleTime").size() > 0) {
            ticks = ParseUtil.parseLongOrDefault(wmiTicks.get("PercentIdleTime").get(0), 0L)
                    + ParseUtil.parseLongOrDefault(wmiTicks.get("PercentPrivilegedTime").get(0), 0L)
                    + ParseUtil.parseLongOrDefault(wmiTicks.get("PercentUserTime").get(0), 0L);
        }
        // Divide
        TICKS_PER_MILLISECOND = ticks / mSec;
        LOG.debug("Ticks per millisecond: {}", TICKS_PER_MILLISECOND);
    }

    /**
     * Create a Processor
     */
    public WindowsCentralProcessor() {
        // Initialize class variables
        initVars();
        // Initialize tick arrays
        initTicks();

        LOG.debug("Initialized Processor");
    }

    /**
     * Initializes Class variables
     */
    private void initVars() {
        final String cpuRegistryRoot = "HARDWARE\\DESCRIPTION\\System\\CentralProcessor";
        String[] processorIds = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryRoot);
        if (processorIds.length > 0) {
            String cpuRegistryPath = cpuRegistryRoot + "\\" + processorIds[0];
            this.setVendor(Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
                    "VendorIdentifier"));
            this.setName(Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
                    "ProcessorNameString"));
            this.setIdentifier(
                    Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath, "Identifier"));
        }
        SYSTEM_INFO sysinfo = new SYSTEM_INFO();
        Kernel32.INSTANCE.GetNativeSystemInfo(sysinfo);
        if (sysinfo.processorArchitecture.pi.wProcessorArchitecture.intValue() == 9 // PROCESSOR_ARCHITECTURE_AMD64
                || sysinfo.processorArchitecture.pi.wProcessorArchitecture.intValue() == 6) { // PROCESSOR_ARCHITECTURE_IA64
            this.setCpu64(true);
        } else if (sysinfo.processorArchitecture.pi.wProcessorArchitecture.intValue() == 0) { // PROCESSOR_ARCHITECTURE_INTEL
            this.setCpu64(false);
        }
    }

    /**
     * Updates logical and physical processor counts from /proc/cpuinfo
     */
    protected void calculateProcessorCounts() {
        // Get number of logical processors
        SYSTEM_INFO sysinfo = new SYSTEM_INFO();
        Kernel32.INSTANCE.GetSystemInfo(sysinfo);
        this.logicalProcessorCount = sysinfo.dwNumberOfProcessors.intValue();

        // Get number of physical processors
        WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION[] processors = Kernel32Util.getLogicalProcessorInformation();
        for (SYSTEM_LOGICAL_PROCESSOR_INFORMATION proc : processors) {
            if (proc.relationship == WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore) {
                this.physicalProcessorCount++;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getSystemCpuLoadTicks() {
        long[] ticks = new long[curTicks.length];
        WinBase.FILETIME lpIdleTime = new WinBase.FILETIME();
        WinBase.FILETIME lpKernelTime = new WinBase.FILETIME();
        WinBase.FILETIME lpUserTime = new WinBase.FILETIME();
        if (!Kernel32.INSTANCE.GetSystemTimes(lpIdleTime, lpKernelTime, lpUserTime)) {
            LOG.error("Failed to update system idle/kernel/user times. Error code: " + Native.getLastError());
            return ticks;
        }
        // Array order is user,nice,kernel,idle
        // Units are in 100-ns, divide by 10000 for ms
        // TODO: Change to lp*Time.toDWordLong.longValue() with JNA 4.3
        ticks[3] = WinBase.FILETIME.dateToFileTime(lpIdleTime.toDate()) / 10000L;
        ticks[2] = WinBase.FILETIME.dateToFileTime(lpKernelTime.toDate()) / 10000L - ticks[3];
        ticks[1] = 0L; // Windows is not 'nice'
        ticks[0] = WinBase.FILETIME.dateToFileTime(lpUserTime.toDate()) / 10000L;
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemIOWaitTicks() {
        // Avg. Disk sec/Transfer raw value is cumulative ticks spent
        // transferring. Divide result by ticks per ms to get ms
        return FormatUtil.getUnsignedInt(WmiUtil.selectUint32From(null, "Win32_PerfRawData_PerfDisk_LogicalDisk",
                "AvgDisksecPerTransfer", "WHERE Name=\"_Total\"").intValue()) / TICKS_PER_MILLISECOND;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getSystemIrqTicks() {
        long[] ticks = new long[2];
        // Percent time raw value is cumulative 100NS-ticks
        // Divide by 10000 to get milliseconds
        Map<String, List<String>> irq = WmiUtil.selectStringsFrom(null,
                "Win32_PerfRawData_Counters_ProcessorInformation", "PercentInterruptTime,PercentDPCTime",
                "WHERE Name=\"_Total\"");
        if (irq.get("PercentInterruptTime").size() > 0) {
            ticks[0] = ParseUtil.parseLongOrDefault(irq.get("PercentInterruptTime").get(0), 0L) / 10000L;
            ticks[1] = ParseUtil.parseLongOrDefault(irq.get("PercentDPCTime").get(0), 0L) / 10000L;
        }
        return ticks;
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1) {
            throw new IllegalArgumentException("Must include at least one element.");
        }
        if (nelem > 3) {
            LOG.warn("Max elements of SystemLoadAverage is 3. " + nelem + " specified. Ignoring extra.");
            nelem = 3;
        }
        double[] average = new double[nelem];
        // TODO: If Windows ever actually implements a laod average for 1/5/15,
        // return it
        for (int i = 0; i < average.length; i++) {
            average[i] = -1;
        }
        return average;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[][] getProcessorCpuLoadTicks() {
        long[][] ticks = new long[this.logicalProcessorCount][4];
        // Percent time raw value is cumulative 100NS-ticks
        // Divide by 10000 to get milliseconds
        Map<String, List<String>> wmiTicks = WmiUtil.selectStringsFrom(null,
                "Win32_PerfRawData_Counters_ProcessorInformation",
                "Name,PercentIdleTime,PercentPrivilegedTime,PercentUserTime", "WHERE NOT Name LIKE \"%_Total\"");
        for (int index = 0; index < wmiTicks.get("Name").size(); index++) {
            // It would be too easy if the WMI order matched logical processors
            // but alas, it goes "0,3"; "0,2"; "0,1"; "0,0". So let's do it
            // right and actually string match the name. The first 0 will be
            // there unless we're dealing with NUMA nodes
            for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
                String name = "0," + cpu;
                if (wmiTicks.get("Name").get(index).equals(name)) {
                    // Array order is user,nice,kernel,idle
                    ticks[cpu][0] = ParseUtil.parseLongOrDefault(wmiTicks.get("PercentUserTime").get(index), 0L)
                            / TICKS_PER_MILLISECOND;
                    ticks[cpu][1] = 0L;
                    ticks[cpu][2] = ParseUtil.parseLongOrDefault(wmiTicks.get("PercentPrivilegedTime").get(index), 0L)
                            / TICKS_PER_MILLISECOND;
                    ticks[cpu][3] = ParseUtil.parseLongOrDefault(wmiTicks.get("PercentIdleTime").get(index), 0L)
                            / TICKS_PER_MILLISECOND;
                    break;
                }
            }
        }
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemUptime() {
        return Kernel32.INSTANCE.GetTickCount64() / 1000L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSystemSerialNumber() {
        if (this.cpuSerialNumber == null) {
            // This should always work
            this.cpuSerialNumber = WmiUtil.selectStringFrom(null, "Win32_BIOS", "SerialNumber", null);
            // If the above doesn't work, this might
            if (this.cpuSerialNumber.equals("")) {
                this.cpuSerialNumber = WmiUtil.selectStringFrom(null, "Win32_Csproduct", "IdentifyingNumber", null);
            }
            if (this.cpuSerialNumber.equals("")) {
                this.cpuSerialNumber = "unknown";
            }
        }
        return this.cpuSerialNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess[] getProcesses() {
        Map<String, List<Object>> procs = WmiUtil.selectObjectsFrom(null, "Win32_Process", processProperties, null,
                processPropertyTypes);
        List<OSProcess> procList = processMapToList(procs);
        return procList.toArray(new OSProcess[procList.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess getProcess(int pid) {
        Map<String, List<Object>> procs = WmiUtil.selectObjectsFrom(null, "Win32_Process", processProperties,
                String.format("WHERE ProcessId=%d", pid), processPropertyTypes);
        List<OSProcess> procList = processMapToList(procs);
        return procList.size() > 0 ? procList.get(0) : null;
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
                    ((Long) procs.get("CreationDate").get(p)), now));
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