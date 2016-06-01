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
import com.sun.jna.ptr.PointerByReference;

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.windows.Kernel32;
import oshi.jna.platform.windows.Pdh;
import oshi.jna.platform.windows.Psapi;
import oshi.jna.platform.windows.Psapi.PERFORMANCE_INFORMATION;
import oshi.software.os.OSProcess;
import oshi.software.os.windows.WindowsProcess;
import oshi.util.platform.windows.PdhUtil;
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

    // PDH counters only give increments between calls so we maintain our own
    // "ticks" here
    private long allProcTickTime;

    private long[][] allProcTicks;

    // Open a Performance Data Helper Thread for monitoring each processor ticks
    private PointerByReference phQuery = new PointerByReference();
    private PointerByReference[] phUserCounters;
    private PointerByReference[] phIdleCounters;

    // Set up Performance Data Helper thread for IOWait
    private long iowaitTime;
    private PointerByReference iowaitQuery = new PointerByReference();
    private PointerByReference pLatency = new PointerByReference();
    private PointerByReference pTransfers = new PointerByReference();
    private long iowaitTicks;

    // Set up Performance Data Helper thread for IRQticks
    private long irqTime;
    private PointerByReference irqQuery = new PointerByReference();
    private PointerByReference pIrq = new PointerByReference();
    private PointerByReference pDpc = new PointerByReference();
    private long[] irqTicks = new long[2];

    // For WMI Process queries
    private static String processProperties = "Name,CommandLine,ExecutionState,ProcessID,ParentProcessId"
            + ",ThreadCount,Priority,VirtualSize,WorkingSetSize,KernelModeTime,UserModeTime,CreationDate";
    private static ValueType[] processPropertyTypes = { ValueType.STRING, ValueType.STRING, ValueType.LONG,
            ValueType.LONG, ValueType.LONG, ValueType.LONG, ValueType.LONG, ValueType.STRING, ValueType.STRING,
            ValueType.STRING, ValueType.STRING, ValueType.DATETIME };

    /**
     * Create a Processor
     */
    public WindowsCentralProcessor() {
        // Initialize class variables
        initVars();
        // Initialize PDH
        initPdhCounters();
        // Initialize tick arrays
        this.allProcTicks = new long[this.logicalProcessorCount][4];
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
     * Initializes PDH Tick Counters
     */
    private void initPdhCounters() {
        // Set up counters
        this.phUserCounters = new PointerByReference[this.logicalProcessorCount];
        this.phIdleCounters = new PointerByReference[this.logicalProcessorCount];

        // Open tick query
        if (PdhUtil.openQuery(phQuery)) {
            for (int p = 0; p < this.logicalProcessorCount; p++) {
                // Options are (only need 2 to calculate all)
                // "\Processor(0)\% processor time"
                // "\Processor(0)\% idle time" (1 - processor)
                // "\Processor(0)\% privileged time" (subset of processor)
                // "\Processor(0)\% user time" (other subset of processor)
                // Note need to make \ = \\ for Java Strings and %% for format
                String counterPath = String.format("\\Processor(%d)\\%% user time", p);
                this.phUserCounters[p] = new PointerByReference();
                // Add tick query for this processor
                PdhUtil.addCounter(phQuery, counterPath, this.phUserCounters[p]);

                counterPath = String.format("\\Processor(%d)\\%% idle time", p);
                this.phIdleCounters[p] = new PointerByReference();
                PdhUtil.addCounter(phQuery, counterPath, this.phIdleCounters[p]);
            }
            LOG.debug("Tick counter queries added.  Initializing with first query.");

            // Initialize by collecting data the first time
            Pdh.INSTANCE.PdhCollectQueryData(phQuery.getValue());
        }

        // Open iowait query
        if (PdhUtil.openQuery(iowaitQuery)) {
            // \LogicalDisk(_Total)\Avg. Disk sec/Transfer
            PdhUtil.addCounter(iowaitQuery, "\\LogicalDisk(_Total)\\Avg. Disk sec/Transfer", pLatency);
            // \LogicalDisk(_Total)\Disk Transfers/sec
            PdhUtil.addCounter(iowaitQuery, "\\LogicalDisk(_Total)\\Disk Transfers/sec", pTransfers);
            // Initialize by collecting data the first time
            Pdh.INSTANCE.PdhCollectQueryData(iowaitQuery.getValue());
        }

        // Open irq query
        if (PdhUtil.openQuery(irqQuery)) {
            // \Processor(_Total)\% Interrupt Time
            PdhUtil.addCounter(irqQuery, "\\Processor(_Total)\\% Interrupt Time", pIrq);
            // \Processor(_Total)\% DPC Time
            PdhUtil.addCounter(irqQuery, "\\Processor(_Total)\\% DPC Time", pDpc);
            // Initialize by collecting data the first time
            Pdh.INSTANCE.PdhCollectQueryData(irqQuery.getValue());
        }

        // Set up hook to close the queries on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Pdh.INSTANCE.PdhCloseQuery(phQuery.getValue());
                Pdh.INSTANCE.PdhCloseQuery(iowaitQuery.getValue());
                Pdh.INSTANCE.PdhCloseQuery(irqQuery.getValue());
            }
        });
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
        // TODO: Change to lp*Time.toDWordLong.longValue() with JNA 4.3
        ticks[3] = WinBase.FILETIME.dateToFileTime(lpIdleTime.toDate());
        ticks[2] = WinBase.FILETIME.dateToFileTime(lpKernelTime.toDate()) - ticks[3];
        ticks[1] = 0L; // Windows is not 'nice'
        ticks[0] = WinBase.FILETIME.dateToFileTime(lpUserTime.toDate());
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemIOWaitTicks() {
        // Avg. Disk sec/Transfer x Avg. Disk Transfers/sec * seconds between
        // reads
        if (!PdhUtil.updateCounters(iowaitQuery)) {
            return 0;
        }

        // We'll manufacture our own ticks by multiplying the latency (sec per
        // read) by transfers (reads per sec) by time elapsed since the last
        // call to get a tick increment
        long now = System.currentTimeMillis();
        long elapsed = now - iowaitTime;

        long latency = PdhUtil.queryCounter(pLatency);
        long transfers = PdhUtil.queryCounter(pTransfers);

        // Since PDH_FMT_1000 is used, results must be divided by 1000 to get
        // actual. Units are sec (*1000) per read * reads (*1000) per sec time *
        // ms time. Reads cancel so result is in sec (*1000*1000) per sec, which
        // is a unitless percentage (times a million) multiplied by elapsed time
        // Multiply by elapsed to get total ms and Divide by 1000 * 1000.
        // Putting division at end avoids need to cast division to double
        // Elasped is only since last read, so increment previous value
        iowaitTicks += elapsed * latency * transfers / 1000000;

        iowaitTime = now;

        return iowaitTicks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getSystemIrqTicks() {
        long[] ticks = new long[2];
        // Time * seconds between reads
        if (!PdhUtil.updateCounters(irqQuery)) {
            return ticks;
        }

        // We'll manufacture our own ticks by multiplying the % used (from the
        // counter) by time elapsed since the last call to get a tick increment
        long now = System.currentTimeMillis();
        long elapsed = now - irqTime;

        long irq = PdhUtil.queryCounter(pIrq);
        long dpc = PdhUtil.queryCounter(pDpc);

        // Returns results in 1000's of percent, e.g. 5% is 5000
        // Multiply by elapsed to get total ms and Divide by 100 * 1000
        // Putting division at end avoids need to cast division to double
        // Elasped is only since last read, so increment previous value
        irqTicks[0] += elapsed * irq / 100000;
        irqTicks[1] += elapsed * dpc / 100000;

        irqTime = now;

        // Make a copy of the array to return
        System.arraycopy(irqTicks, 0, ticks, 0, irqTicks.length);
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

        if (!PdhUtil.updateCounters(phQuery)) {
            return ticks;
        }

        // We'll manufacture our own ticks by multiplying the % used (from the
        // counter) by time elapsed since the last call to get a tick increment
        long now = System.currentTimeMillis();
        long elapsed = now - allProcTickTime;

        for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
            long userPct = PdhUtil.queryCounter(this.phUserCounters[cpu]);
            long idlePct = PdhUtil.queryCounter(this.phIdleCounters[cpu]);

            // Returns results in 1000's of percent, e.g. 5% is 5000
            // Multiply by elapsed to get total ms and Divide by 100 * 1000
            // Putting division at end avoids need to cast division to double
            long user = elapsed * userPct / 100000;
            long idle = elapsed * idlePct / 100000;
            // Elasped is only since last read, so increment previous value
            allProcTicks[cpu][0] += user;
            // allProcTicks[cpu][1] is ignored, Windows is not nice
            allProcTicks[cpu][2] += Math.max(0, elapsed - user - idle); // u+i+sys=100%
            allProcTicks[cpu][3] += idle;
        }
        allProcTickTime = now;

        // Make a copy of the array to return
        for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
            System.arraycopy(allProcTicks[cpu], 0, ticks[cpu], 0, allProcTicks[cpu].length);
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
            try {
                procList.add(
                        new WindowsProcess((String) procs.get("Name").get(p), (String) procs.get("CommandLine").get(p),
                                ((Long) procs.get("ExecutionState").get(p)).intValue(),
                                ((Long) procs.get("ProcessID").get(p)).intValue(),
                                ((Long) procs.get("ParentProcessId").get(p)).intValue(),
                                ((Long) procs.get("ThreadCount").get(p)).intValue(),
                                ((Long) procs.get("Priority").get(p)).intValue(),
                                Long.parseLong((String) procs.get("VirtualSize").get(p)),
                                Long.parseLong((String) procs.get("WorkingSetSize").get(p)),
                                // Kernel and User time units are 100ns
                                Long.parseLong((String) procs.get("KernelModeTime").get(p)) / 10000L,
                                Long.parseLong((String) procs.get("UserModeTime").get(p)) / 10000L,
                                ((Long) procs.get("CreationDate").get(p)), now));
            } catch (NumberFormatException nfe) {
                // Ignore errors, just don't add
                LOG.debug("Parse Exception");
            }
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