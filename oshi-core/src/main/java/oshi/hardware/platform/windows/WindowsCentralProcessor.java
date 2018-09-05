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

import java.util.List;

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
import oshi.jna.platform.windows.PdhUtil;
import oshi.jna.platform.windows.PdhUtil.PdhEnumObjectItems;
import oshi.jna.platform.windows.PdhUtil.PdhException;
import oshi.jna.platform.windows.WbemcliUtil.WmiQuery;
import oshi.jna.platform.windows.WbemcliUtil.WmiResult;
import oshi.util.platform.windows.PerfDataUtil;
import oshi.util.platform.windows.PerfDataUtil.PerfCounter;
import oshi.util.platform.windows.WmiUtil;

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

    enum ProcessorProperty {
        PROCESSORID;
    }

    // Save Windows version info for 32 bit/64 bit branch later
    private static final byte MAJOR_VERSION = Kernel32.INSTANCE.GetVersion().getLow().byteValue();

    private static final String PROCESSOR = "Processor";
    private static final String PROCESSOR_LOCALIZED = PdhUtil.PdhLookupPerfNameByIndex(null,
            PdhUtil.PdhLookupPerfIndexByEnglishName(PROCESSOR));
    private static final String TOTAL_INSTANCE = "_Total";

    /*
     * For tick counts
     */
    enum ProcessorTickCountProperty {
        PERCENTDPCTIME, PERCENTINTERRUPTTIME, PERCENTPRIVILEGEDTIME, PERCENTPROCESSORTIME, PERCENTUSERTIME;
    }

    // Only counters or WMI will be used
    // Per-processor
    private transient PerfCounter[] dpcTickCounter = null;
    private transient PerfCounter[] interruptTickCounter = null;
    private transient PerfCounter[] privilegedTickCounter = null;
    private transient PerfCounter[] processorTickCounter = null;
    private transient PerfCounter[] userTickCounter = null;

    private transient WmiQuery<ProcessorTickCountProperty> processorTickCountQuery = null;

    // _Total
    enum SystemTickCountProperty {
        PERCENTDPCTIME, PERCENTINTERRUPTTIME;
    }

    private transient PerfCounter irqTickCounter = null;
    private transient PerfCounter softIrqTickCounter = null;

    private transient WmiQuery<SystemTickCountProperty> systemTickCountQuery = null;

    enum InterruptsProperty {
        INTERRUPTSPERSEC;
    }

    private transient PerfCounter interruptsPerSecCounter = null;
    private transient WmiQuery<InterruptsProperty> interruptsQuery = null;

    /*
     * For tick counts
     */
    enum ContextSwitchProperty {
        CONTEXTSWITCHESPERSEC;
    }

    private transient PerfCounter contextSwitchesPerSecCounter = null;
    private transient WmiQuery<ContextSwitchProperty> contextSwitchQuery = null;

    private static long lastRefresh = 0L;

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

        WmiQuery<ProcessorProperty> processorIdQuery = new WmiQuery<>("Win32_Processor", ProcessorProperty.class);
        WmiResult<ProcessorProperty> processorId = WmiUtil.queryWMI(processorIdQuery);
        if (processorId.getResultCount() > 0) {
            setProcessorID(WmiUtil.getString(processorId, ProcessorProperty.PROCESSORID, 0));
        }
    }

    /**
     * Initializes PDH Tick Counters
     */
    private void initPdhCounters() {
        this.contextSwitchesPerSecCounter = PerfDataUtil.createCounter("System", null, "Context Switches/sec");
        if (!PerfDataUtil.addCounterToQuery(this.contextSwitchesPerSecCounter)) {
            this.contextSwitchesPerSecCounter = null;
            this.contextSwitchQuery = new WmiQuery<>("Win32_PerfRawData_PerfOS_System", ContextSwitchProperty.class);
        }

        boolean enumeration = true;
        try {
            PdhEnumObjectItems objectItems = PdhUtil.PdhEnumObjectItems(null, null, PROCESSOR_LOCALIZED, 100);

            if (!objectItems.getInstances().isEmpty()) {
                // The Idle time counter is inconsistent across Windows versions
                // and vs. WMI, but the Processor Time matches. Subtract
                // User+Privileged from Processor to get Idle.
                this.dpcTickCounter = new PerfCounter[this.logicalProcessorCount];
                this.interruptTickCounter = new PerfCounter[this.logicalProcessorCount];
                this.privilegedTickCounter = new PerfCounter[this.logicalProcessorCount];
                this.processorTickCounter = new PerfCounter[this.logicalProcessorCount];
                this.userTickCounter = new PerfCounter[this.logicalProcessorCount];

                List<String> instances = objectItems.getInstances();
                PerfCounter counter;
                for (int i = 0; i < instances.size() && i < this.logicalProcessorCount; i++) {
                    String instance = instances.get(i);

                    counter = PerfDataUtil.createCounter(PROCESSOR, instance, "% DPC Time");
                    this.dpcTickCounter[i] = counter;
                    if (!PerfDataUtil.addCounterToQuery(counter)) {
                        throw new PdhException(0);
                    }

                    counter = PerfDataUtil.createCounter(PROCESSOR, instance, "% Interrupt Time");
                    this.interruptTickCounter[i] = counter;
                    if (!PerfDataUtil.addCounterToQuery(counter)) {
                        throw new PdhException(0);
                    }

                    counter = PerfDataUtil.createCounter(PROCESSOR, instance, "% Privileged Time");
                    this.privilegedTickCounter[i] = counter;
                    if (!PerfDataUtil.addCounterToQuery(counter)) {
                        throw new PdhException(0);
                    }

                    counter = PerfDataUtil.createCounter(PROCESSOR, instance, "% Processor Time");
                    this.processorTickCounter[i] = counter;
                    if (!PerfDataUtil.addCounterToQuery(counter)) {
                        throw new PdhException(0);
                    }

                    counter = PerfDataUtil.createCounter(PROCESSOR, instance, "% User Time");
                    this.userTickCounter[i] = counter;
                    if (!PerfDataUtil.addCounterToQuery(counter)) {
                        throw new PdhException(0);
                    }
                }

                counter = PerfDataUtil.createCounter(PROCESSOR, TOTAL_INSTANCE, "% Interrupt Time");
                this.irqTickCounter = counter;
                if (!PerfDataUtil.addCounterToQuery(counter)) {
                    throw new PdhException(0);
                }

                counter = PerfDataUtil.createCounter(PROCESSOR, TOTAL_INSTANCE, "% DPC Time");
                this.softIrqTickCounter = counter;
                if (!PerfDataUtil.addCounterToQuery(counter)) {
                    throw new PdhException(0);
                }

                counter = PerfDataUtil.createCounter(PROCESSOR, TOTAL_INSTANCE, "Interrupts/sec");
                this.interruptsPerSecCounter = counter;
                if (!PerfDataUtil.addCounterToQuery(counter)) {
                    throw new PdhException(0);
                }
            }
        } catch (PdhException e) {
            LOG.warn("Unable to enumerate performance counter instances for {}.", PROCESSOR_LOCALIZED);
            enumeration = false;
        }
        if (!enumeration) {
            PerfDataUtil.removeAllCounters(PROCESSOR);
            this.dpcTickCounter = null;
            this.interruptTickCounter = null;
            this.privilegedTickCounter = null;
            this.processorTickCounter = null;
            this.userTickCounter = null;
            this.irqTickCounter = null;
            this.softIrqTickCounter = null;
            this.interruptsPerSecCounter = null;

            processorTickCountQuery = new WmiQuery<>("Win32_PerfRawData_PerfOS_Processor WHERE NOT Name=\"_Total\"",
                    ProcessorTickCountProperty.class);
            systemTickCountQuery = new WmiQuery<>("Win32_PerfRawData_PerfOS_Processor WHERE Name=\"_Total\"",
                    SystemTickCountProperty.class);
            interruptsQuery = new WmiQuery<>("Win32_PerfRawData_PerfOS_Processor WHERE Name=\"_Total\"",
                    InterruptsProperty.class);
        }
        // REMOVE
        processorTickCountQuery = new WmiQuery<>("Win32_PerfRawData_PerfOS_Processor WHERE NOT Name=\"_Total\"",
                ProcessorTickCountProperty.class);
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
        if (this.systemTickCountQuery == null) {
            refreshTickCounters();
            ticks[TickType.IRQ.getIndex()] = PerfDataUtil.queryCounter(this.irqTickCounter) / 10000L;
            ticks[TickType.SOFTIRQ.getIndex()] = PerfDataUtil.queryCounter(this.softIrqTickCounter) / 10000L;
        } else {
            WmiResult<SystemTickCountProperty> result = WmiUtil.queryWMI(this.systemTickCountQuery);
            if (result.getResultCount() > 0) {
                ticks[TickType.IRQ.getIndex()] = WmiUtil.getUint64(result, SystemTickCountProperty.PERCENTINTERRUPTTIME,
                        0) / 10000L;
                ticks[TickType.SOFTIRQ.getIndex()] = WmiUtil.getUint64(result, SystemTickCountProperty.PERCENTDPCTIME,
                        0) / 10000L;
            }
        }

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
        long[][] ticks = new long[this.logicalProcessorCount][TickType.values().length];
        if (this.processorTickCountQuery == null) {
            refreshTickCounters();
            for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
                ticks[cpu][TickType.SYSTEM.getIndex()] = PerfDataUtil.queryCounter(this.privilegedTickCounter[cpu]);
                ticks[cpu][TickType.USER.getIndex()] = PerfDataUtil.queryCounter(this.userTickCounter[cpu]);
                ticks[cpu][TickType.IRQ.getIndex()] = PerfDataUtil.queryCounter(this.interruptTickCounter[cpu]);
                ticks[cpu][TickType.SOFTIRQ.getIndex()] = PerfDataUtil.queryCounter(this.dpcTickCounter[cpu]);
                // Fetch total processor ticks
                // Later decrement by system + user
                ticks[cpu][TickType.IDLE.getIndex()] = PerfDataUtil.queryCounter(this.processorTickCounter[cpu]);
            }
        } else {
            ticks = new long[this.logicalProcessorCount][TickType.values().length];
            WmiResult<ProcessorTickCountProperty> result = WmiUtil.queryWMI(this.processorTickCountQuery);
            for (int cpu = 0; cpu < result.getResultCount() && cpu < this.logicalProcessorCount; cpu++) {
                ticks[cpu][TickType.SYSTEM.getIndex()] = WmiUtil.getUint64(result,
                        ProcessorTickCountProperty.PERCENTPRIVILEGEDTIME, cpu);
                ticks[cpu][TickType.USER.getIndex()] = WmiUtil.getUint64(result,
                        ProcessorTickCountProperty.PERCENTUSERTIME, cpu);
                ticks[cpu][TickType.IRQ.getIndex()] = WmiUtil.getUint64(result,
                        ProcessorTickCountProperty.PERCENTINTERRUPTTIME, cpu);
                ticks[cpu][TickType.SOFTIRQ.getIndex()] = WmiUtil.getUint64(result,
                        ProcessorTickCountProperty.PERCENTDPCTIME, cpu);
                // Fetch total processor ticks
                // Later decrement by system + user
                ticks[cpu][TickType.IDLE.getIndex()] = WmiUtil.getUint64(result,
                        ProcessorTickCountProperty.PERCENTPROCESSORTIME, cpu);
            }
        }
        for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
            ticks[cpu][TickType.IDLE.getIndex()] -= ticks[cpu][TickType.SYSTEM.getIndex()]
                    + ticks[cpu][TickType.USER.getIndex()];
            // Additional decrement to avoid double counting in the
            // total array
            ticks[cpu][TickType.SYSTEM.getIndex()] -= ticks[cpu][TickType.IRQ.getIndex()]
                    + ticks[cpu][TickType.SOFTIRQ.getIndex()];

            // Raw value is cumulative 100NS-ticks
            // Divide by 10000 to get milliseconds
            ticks[cpu][TickType.SYSTEM.getIndex()] /= 10000L;
            ticks[cpu][TickType.USER.getIndex()] /= 10000L;
            ticks[cpu][TickType.IRQ.getIndex()] /= 10000L;
            ticks[cpu][TickType.SOFTIRQ.getIndex()] /= 10000L;
            ticks[cpu][TickType.IDLE.getIndex()] /= 10000L;
        }
        // Skipping nice and IOWait, they'll stay 0
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
        if (this.contextSwitchQuery == null) {
            PerfDataUtil.updateQuery(this.contextSwitchesPerSecCounter);
            return PerfDataUtil.queryCounter(this.contextSwitchesPerSecCounter);
        }
        WmiResult<ContextSwitchProperty> result = WmiUtil.queryWMI(this.contextSwitchQuery);
        if (result.getResultCount() > 0) {
            return WmiUtil.getUint32(result, ContextSwitchProperty.CONTEXTSWITCHESPERSEC, 0);
        }
        return 0L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getInterrupts() {
        if (this.interruptsQuery == null) {
            refreshTickCounters();
            return PerfDataUtil.queryCounter(this.interruptsPerSecCounter);
        }
        WmiResult<InterruptsProperty> result = WmiUtil.queryWMI(this.interruptsQuery);
        if (result.getResultCount() > 0) {
            return WmiUtil.getUint32(result, InterruptsProperty.INTERRUPTSPERSEC, 0);
        }
        return 0L;
    }

    /**
     * Refresh PDH counters no more often than 100ms
     */
    private static void refreshTickCounters() {
        if (System.currentTimeMillis() - lastRefresh > 100L) {
            lastRefresh = PerfDataUtil.updateQuery(PROCESSOR);
        }
    }
}