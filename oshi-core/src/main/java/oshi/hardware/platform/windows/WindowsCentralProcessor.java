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
package oshi.hardware.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native; //NOSONAR
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION;
import com.sun.jna.platform.win32.WinReg;

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.platform.windows.PerfDataUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;
import oshi.util.platform.windows.WmiUtil.WmiProperty;
import oshi.util.platform.windows.WmiUtil.WmiQuery;
import oshi.util.platform.windows.WmiUtil.WmiResult;

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

    enum ProcessorProperty implements WmiProperty {
        PROCESSORID(ValueType.STRING);

        private ValueType type;

        ProcessorProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }
    }

    // Save Windows version info for 32 bit/64 bit branch later
    private static final byte MAJOR_VERSION = Kernel32.INSTANCE.GetVersion().getLow().byteValue();

    private String[][] pdhCounters = null;
    private String pdhIrqCounter = null;
    private String pdhSoftIrqCounter = null;
    private String pdhContextSwitchesPerSecCounter = null;
    private String pdhInterruptsPerSecCounter = null;

    /**
     * Create a Processor
     */
    public WindowsCentralProcessor() {
        super();
        // Initialize class variables
        initVars();
        // Initialize pdh counters
        initPdhCounters();
        // Initialize tick arrays
        initTicks();

        LOG.debug("Initialized Processor");
    }

    /**
     * Initializes Class variables
     */
    private void initVars() {
        final String cpuRegistryRoot = "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\";
        String[] processorIds = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryRoot);
        if (processorIds.length > 0) {
            String cpuRegistryPath = cpuRegistryRoot + processorIds[0];
            setVendor(Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
                    "VendorIdentifier"));
            setName(Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
                    "ProcessorNameString"));
            setIdentifier(
                    Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath, "Identifier"));
        }
        SYSTEM_INFO sysinfo = new SYSTEM_INFO();
        Kernel32.INSTANCE.GetNativeSystemInfo(sysinfo);
        if (sysinfo.processorArchitecture.pi.wProcessorArchitecture.intValue() == 9 // PROCESSOR_ARCHITECTURE_AMD64
                || sysinfo.processorArchitecture.pi.wProcessorArchitecture.intValue() == 6) { // PROCESSOR_ARCHITECTURE_IA64
            setCpu64(true);
        } else if (sysinfo.processorArchitecture.pi.wProcessorArchitecture.intValue() == 0) { // PROCESSOR_ARCHITECTURE_INTEL
            setCpu64(false);
        }

        WmiQuery<ProcessorProperty> processorIdQuery = WmiUtil.createQuery("Win32_Processor", ProcessorProperty.class);
        WmiResult<ProcessorProperty> processorId = WmiUtil.queryWMI(processorIdQuery);
        if (processorId.getResultCount() > 0) {
            setProcessorID((String) processorId.get(ProcessorProperty.PROCESSORID).get(0));
        }
    }

    /**
     * Initializes PDH Tick Counters
     */
    private void initPdhCounters() {
        // Set up counters.
        this.pdhCounters = new String[this.logicalProcessorCount][TickType.values().length];
        String[] queryName = new String[TickType.values().length];
        // The Idle time counter is inconsistent across Windows versions and vs.
        // WMI, but the Processor Time matches. Subtract User+Privileged from
        // Processor to get Idle.
        queryName[TickType.IDLE.getIndex()] = "\\Processor(%d)\\%% Processor Time";
        queryName[TickType.SYSTEM.getIndex()] = "\\Processor(%d)\\%% Privileged Time";
        queryName[TickType.USER.getIndex()] = "\\Processor(%d)\\%% User Time";
        queryName[TickType.IRQ.getIndex()] = "\\Processor(%d)\\%% Interrupt Time";
        queryName[TickType.SOFTIRQ.getIndex()] = "\\Processor(%d)\\%% DPC Time";

        for (int p = 0; p < this.logicalProcessorCount; p++) {
            this.pdhCounters[p][TickType.IDLE.getIndex()] = String.format(queryName[TickType.IDLE.getIndex()], p);
            this.pdhCounters[p][TickType.SYSTEM.getIndex()] = String.format(queryName[TickType.SYSTEM.getIndex()], p);
            this.pdhCounters[p][TickType.USER.getIndex()] = String.format(queryName[TickType.USER.getIndex()], p);
            this.pdhCounters[p][TickType.IRQ.getIndex()] = String.format(queryName[TickType.IRQ.getIndex()], p);
            this.pdhCounters[p][TickType.SOFTIRQ.getIndex()] = String.format(queryName[TickType.SOFTIRQ.getIndex()], p);
        }
        PerfDataUtil.addCounter2DArray("pdhCounters", this.pdhCounters);

        pdhIrqCounter = "\\Processor(_Total)\\% Interrupt Time";
        pdhSoftIrqCounter = "\\Processor(_Total)\\% DPC Time";
        pdhContextSwitchesPerSecCounter = "\\System\\Context Switches/sec";
        pdhInterruptsPerSecCounter = "\\Processor(_Total)\\Interrupts/sec";

        PerfDataUtil.addCounter(pdhIrqCounter);
        PerfDataUtil.addCounter(pdhSoftIrqCounter);
        PerfDataUtil.addCounter(pdhContextSwitchesPerSecCounter);
        PerfDataUtil.addCounter(pdhInterruptsPerSecCounter);
    }

    /**
     * Updates logical and physical processor counts
     */
    @Override
    protected void calculateProcessorCounts() {
        // Get number of logical processors
        SYSTEM_INFO sysinfo = new SYSTEM_INFO();
        Kernel32.INSTANCE.GetSystemInfo(sysinfo);
        this.logicalProcessorCount = sysinfo.dwNumberOfProcessors.intValue();

        // Get number of physical processors
        WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION[] processors = Kernel32Util.getLogicalProcessorInformation();
        for (SYSTEM_LOGICAL_PROCESSOR_INFORMATION proc : processors) {
            if (proc.relationship == WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage) {
                this.physicalPackageCount++;
            }
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
        long[] ticks = new long[TickType.values().length];
        WinBase.FILETIME lpIdleTime = new WinBase.FILETIME();
        WinBase.FILETIME lpKernelTime = new WinBase.FILETIME();
        WinBase.FILETIME lpUserTime = new WinBase.FILETIME();
        if (!Kernel32.INSTANCE.GetSystemTimes(lpIdleTime, lpKernelTime, lpUserTime)) {
            LOG.error("Failed to update system idle/kernel/user times. Error code: {}", Native.getLastError());
            return ticks;
        }
        // IOwait:
        // Windows does not measure IOWait.

        // IRQ:
        // Percent time raw value is cumulative 100NS-ticks
        // Divide by 10000 to get milliseconds
        ticks[TickType.IRQ.getIndex()] = PerfDataUtil.queryCounter(this.pdhIrqCounter) / 10000L;
        ticks[TickType.SOFTIRQ.getIndex()] = PerfDataUtil.queryCounter(this.pdhSoftIrqCounter) / 10000L;

        ticks[TickType.IDLE.getIndex()] = lpIdleTime.toDWordLong().longValue() / 10000L;
        ticks[TickType.SYSTEM.getIndex()] = lpKernelTime.toDWordLong().longValue() / 10000L
                - ticks[TickType.IDLE.getIndex()];
        ticks[TickType.USER.getIndex()] = lpUserTime.toDWordLong().longValue() / 10000L;
        // Additional decrement to avoid double counting in the total array
        ticks[TickType.SYSTEM.getIndex()] -= ticks[TickType.IRQ.getIndex()] + ticks[TickType.SOFTIRQ.getIndex()];
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        // Windows doesn't have load average
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
        long[][] ticks = PerfDataUtil.queryCounter2DArray("pdhCounters", this.pdhCounters);
        for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
            // Decrement idle as it's really total
            ticks[cpu][TickType.IDLE.getIndex()] -= ticks[cpu][TickType.SYSTEM.getIndex()]
                    + ticks[cpu][TickType.USER.getIndex()];
            // Decrement system to avoid double counting in the total array
            ticks[cpu][TickType.SYSTEM.getIndex()] -= ticks[cpu][TickType.IRQ.getIndex()]
                    + ticks[cpu][TickType.SOFTIRQ.getIndex()];

            // Raw value is cumulative 100NS-ticks
            // Divide by 10000 to get milliseconds
            ticks[cpu][TickType.IDLE.getIndex()] /= 10000L;
            ticks[cpu][TickType.SYSTEM.getIndex()] /= 10000L;
            ticks[cpu][TickType.USER.getIndex()] /= 10000L;
            ticks[cpu][TickType.IRQ.getIndex()] /= 10000L;
            ticks[cpu][TickType.SOFTIRQ.getIndex()] /= 10000L;
            // Skipping nice and IOWait, they'll stay 0
        }
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemUptime() {
        // GetTickCount64 requires Vista (6.0) or later
        if (MAJOR_VERSION >= 6) {
            return Kernel32.INSTANCE.GetTickCount64() / 1000L;
        } else {
            // 32 bit rolls over at ~ 49 days
            return Kernel32.INSTANCE.GetTickCount() / 1000L;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public String getSystemSerialNumber() {
        return new WindowsComputerSystem().getSerialNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getContextSwitches() {
        return PerfDataUtil.queryCounter(pdhContextSwitchesPerSecCounter) / 10000L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getInterrupts() {
        return PerfDataUtil.queryCounter(pdhInterruptsPerSecCounter) / 10000L;
    }
}