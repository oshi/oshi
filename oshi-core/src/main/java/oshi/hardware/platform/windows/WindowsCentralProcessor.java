/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.windows;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.PdhUtil;
import com.sun.jna.platform.win32.PdhUtil.PdhEnumObjectItems;
import com.sun.jna.platform.win32.PdhUtil.PdhException;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.windows.VersionHelpers;
import oshi.util.platform.windows.PdhUtilXP;
import oshi.util.platform.windows.PerfDataUtil;
import oshi.util.platform.windows.PerfDataUtil.PerfCounter;
import oshi.util.platform.windows.WmiQueryHandler;
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

    private static boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

    private static final String PROCESSOR = "Processor";
    private static String processorLocalized = null;
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
        WmiResult<ProcessorProperty> processorId = WmiQueryHandler.getInstance().queryWMI(processorIdQuery);
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
            initWmiContextSwitchQuery();
        }

        boolean enumeration = true;
        try {
            PdhEnumObjectItems objectItems = PdhUtil.PdhEnumObjectItems(null, null, getProcessorLocalized(), 100);

            if (!objectItems.getInstances().isEmpty()) {
                // % Processor Time is actually Idle time
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
            LOG.warn("Unable to enumerate performance counter instances for {}.", getProcessorLocalized());
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

            this.processorTickCountQuery = new WmiQuery<>(
                    "Win32_PerfRawData_PerfOS_Processor WHERE NOT Name=\"_Total\"", ProcessorTickCountProperty.class);
            this.systemTickCountQuery = new WmiQuery<>("Win32_PerfRawData_PerfOS_Processor WHERE Name=\"_Total\"",
                    SystemTickCountProperty.class);
            this.interruptsQuery = new WmiQuery<>("Win32_PerfRawData_PerfOS_Processor WHERE Name=\"_Total\"",
                    InterruptsProperty.class);
        }
    }

    /**
     * Nulls PDH counters and sets up WMI query for context switch counters.
     */
    private void initWmiContextSwitchQuery() {
        PerfDataUtil.removeCounterFromQuery(this.contextSwitchesPerSecCounter);
        this.contextSwitchesPerSecCounter = null;
        this.contextSwitchQuery = new WmiQuery<>("Win32_PerfRawData_PerfOS_System", ContextSwitchProperty.class);
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
        // Divide by 10_000 to get milliseconds
        if (this.systemTickCountQuery == null) {
            refreshTickCounters();
            ticks[TickType.IRQ.getIndex()] = PerfDataUtil.queryCounter(this.irqTickCounter) / 10_000L;
            ticks[TickType.SOFTIRQ.getIndex()] = PerfDataUtil.queryCounter(this.softIrqTickCounter) / 10_000L;
        } else {
            WmiResult<SystemTickCountProperty> result = WmiQueryHandler.getInstance()
                    .queryWMI(this.systemTickCountQuery);
            if (result.getResultCount() > 0) {
                ticks[TickType.IRQ.getIndex()] = WmiUtil.getUint64(result, SystemTickCountProperty.PERCENTINTERRUPTTIME,
                        0) / 10_000L;
                ticks[TickType.SOFTIRQ.getIndex()] = WmiUtil.getUint64(result, SystemTickCountProperty.PERCENTDPCTIME,
                        0) / 10_000L;
            }
        }

        ticks[TickType.IDLE.getIndex()] = lpIdleTime.toDWordLong().longValue() / 10_000L;
        ticks[TickType.SYSTEM.getIndex()] = lpKernelTime.toDWordLong().longValue() / 10_000L
                - ticks[TickType.IDLE.getIndex()];
        ticks[TickType.USER.getIndex()] = lpUserTime.toDWordLong().longValue() / 10_000L;
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
                // % Processor Time is actually Idle time
                ticks[cpu][TickType.IDLE.getIndex()] = PerfDataUtil.queryCounter(this.processorTickCounter[cpu]);
            }
        } else {
            ticks = new long[this.logicalProcessorCount][TickType.values().length];
            WmiResult<ProcessorTickCountProperty> result = WmiQueryHandler.getInstance()
                    .queryWMI(this.processorTickCountQuery);
            for (int cpu = 0; cpu < result.getResultCount() && cpu < this.logicalProcessorCount; cpu++) {
                ticks[cpu][TickType.SYSTEM.getIndex()] = WmiUtil.getUint64(result,
                        ProcessorTickCountProperty.PERCENTPRIVILEGEDTIME, cpu);
                ticks[cpu][TickType.USER.getIndex()] = WmiUtil.getUint64(result,
                        ProcessorTickCountProperty.PERCENTUSERTIME, cpu);
                ticks[cpu][TickType.IRQ.getIndex()] = WmiUtil.getUint64(result,
                        ProcessorTickCountProperty.PERCENTINTERRUPTTIME, cpu);
                ticks[cpu][TickType.SOFTIRQ.getIndex()] = WmiUtil.getUint64(result,
                        ProcessorTickCountProperty.PERCENTDPCTIME, cpu);
                // % Processor Time is actually Idle time
                ticks[cpu][TickType.IDLE.getIndex()] = WmiUtil.getUint64(result,
                        ProcessorTickCountProperty.PERCENTPROCESSORTIME, cpu);
            }
        }
        for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
            // Additional decrement to avoid double counting in the
            // total array
            ticks[cpu][TickType.SYSTEM.getIndex()] -= ticks[cpu][TickType.IRQ.getIndex()]
                    + ticks[cpu][TickType.SOFTIRQ.getIndex()];

            // Raw value is cumulative 100NS-ticks
            // Divide by 10_000 to get milliseconds
            ticks[cpu][TickType.SYSTEM.getIndex()] /= 10_000L;
            ticks[cpu][TickType.USER.getIndex()] /= 10_000L;
            ticks[cpu][TickType.IRQ.getIndex()] /= 10_000L;
            ticks[cpu][TickType.SOFTIRQ.getIndex()] /= 10_000L;
            ticks[cpu][TickType.IDLE.getIndex()] /= 10_000L;
        }
        // Skipping nice and IOWait, they'll stay 0
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemUptime() {
        // Uptime is in seconds so divide milliseconds
        // GetTickCount64 requires Vista (6.0) or later
        if (IS_VISTA_OR_GREATER) {
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
    public long getContextSwitches() {
        if (this.contextSwitchQuery == null) {
            PerfDataUtil.updateQuery(this.contextSwitchesPerSecCounter);
            return PerfDataUtil.queryCounter(this.contextSwitchesPerSecCounter);
        }
        WmiResult<ContextSwitchProperty> result = WmiQueryHandler.getInstance().queryWMI(this.contextSwitchQuery);
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
        WmiResult<InterruptsProperty> result = WmiQueryHandler.getInstance().queryWMI(this.interruptsQuery);
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

    /**
     * Localize the "Processor" counter string. English counter names should
     * normally be in HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows
     * NT\CurrentVersion\Perflib\009\Counter, but language manipulations may
     * delete the 009 index. In this case we can assume English must be the
     * language and continue. We may still fail to match the name if the
     * assumption is wrong but it's better than nothing.
     */
    private static String getProcessorLocalized() {
        if (processorLocalized == null) {
            try {
                processorLocalized = PdhUtilXP.PdhLookupPerfNameByIndex(null,
                        PdhUtil.PdhLookupPerfIndexByEnglishName(PROCESSOR));
            } catch (Win32Exception e) {
                LOG.error("Unable to locate English counter names in registry Perflib 009. Assuming English counters.");
            }
            if (processorLocalized == null || processorLocalized.length() == 0) {
                processorLocalized = PROCESSOR;
            }
        }
        return processorLocalized;
    }

}