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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.data.windows.PerfCounterQuery;
import oshi.data.windows.PerfCounterQuery.PdhCounterProperty;
import oshi.data.windows.PerfCounterWildcardQuery;
import oshi.data.windows.PerfCounterWildcardQuery.PdhCounterWildcardProperty;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.windows.Kernel32;
import oshi.jna.platform.windows.Kernel32Util;
import oshi.jna.platform.windows.PowrProf;
import oshi.jna.platform.windows.PowrProf.ProcessorPowerInformation;
import oshi.jna.platform.windows.VersionHelpers;
import oshi.jna.platform.windows.WinNT;
import oshi.jna.platform.windows.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX;
import oshi.jna.platform.windows.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.GROUP_AFFINITY;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * A CPU, representing all of a system's processors. It may contain multiple
 * individual Physical and Logical processors.
 */
public class WindowsCentralProcessor extends AbstractCentralProcessor {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsCentralProcessor.class);

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

    private static final String PROCESSOR = "Processor";

    enum ProcessorProperty {
        PROCESSORID;
    }

    /*
     * For tick counts
     */
    enum ProcessorTickCountProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME(PerfCounterQuery.NOT_TOTAL_INSTANCE),
        // Remaining elements define counters
        PERCENTDPCTIME("% DPC Time"), //
        PERCENTINTERRUPTTIME("% Interrupt Time"), //
        PERCENTPRIVILEGEDTIME("% Privileged Time"), //
        PERCENTPROCESSORTIME("% Processor Time"), //
        PERCENTUSERTIME("% User Time");

        private final String counter;

        ProcessorTickCountProperty(String counter) {
            this.counter = counter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getCounter() {
            return counter;
        }
    }

    private final transient PerfCounterWildcardQuery<ProcessorTickCountProperty> processorTickPerfCounters = new PerfCounterWildcardQuery<>(
            ProcessorTickCountProperty.class, PROCESSOR, "Win32_PerfRawData_PerfOS_Processor WHERE NOT Name=\"_Total\"",
            "Processor Tick Count");

    enum SystemTickCountProperty implements PdhCounterProperty {
        PERCENTDPCTIME(PerfCounterQuery.TOTAL_INSTANCE, "% DPC Time"), //
        PERCENTINTERRUPTTIME(PerfCounterQuery.TOTAL_INSTANCE, "% Interrupt Time");

        private final String instance;
        private final String counter;

        SystemTickCountProperty(String instance, String counter) {
            this.instance = instance;
            this.counter = counter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getInstance() {
            return instance;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getCounter() {
            return counter;
        }
    }

    private final transient PerfCounterQuery<SystemTickCountProperty> systemTickPerfCounters = new PerfCounterQuery<>(
            SystemTickCountProperty.class, PROCESSOR, "Win32_PerfRawData_PerfOS_Processor WHERE Name=\"_Total\"",
            "System Tick Count");

    enum InterruptsProperty implements PdhCounterProperty {
        INTERRUPTSPERSEC(PerfCounterQuery.TOTAL_INSTANCE, "Interrupts/sec");

        private final String instance;
        private final String counter;

        InterruptsProperty(String instance, String counter) {
            this.instance = instance;
            this.counter = counter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getInstance() {
            return instance;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getCounter() {
            return counter;
        }
    }

    private final transient PerfCounterQuery<InterruptsProperty> interruptsPerfCounters = new PerfCounterQuery<>(
            InterruptsProperty.class, PROCESSOR, "Win32_PerfRawData_PerfOS_Processor WHERE Name=\"_Total\"",
            "Interrupt Count");

    /*
     * For tick counts
     */
    enum ContextSwitchProperty implements PdhCounterProperty {
        CONTEXTSWITCHESPERSEC(null, "Context Switches/sec");

        private final String instance;
        private final String counter;

        ContextSwitchProperty(String instance, String counter) {
            this.instance = instance;
            this.counter = counter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getInstance() {
            return instance;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getCounter() {
            return counter;
        }
    }

    private final transient PerfCounterQuery<ContextSwitchProperty> contextSwitchPerfCounters = new PerfCounterQuery<>(
            ContextSwitchProperty.class, "System", "Win32_PerfRawData_PerfOS_System");

    /**
     * Create a Processor
     */
    public WindowsCentralProcessor() {
        super();
        // Initialize class variables
        initVars();

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
        WmiResult<ProcessorProperty> processorId = WmiQueryHandler.createInstance().queryWMI(processorIdQuery);
        if (processorId.getResultCount() > 0) {
            setProcessorID(WmiUtil.getString(processorId, ProcessorProperty.PROCESSORID, 0));
        }
    }

    /**
     * Updates logical and physical processor counts
     */
    @Override
    protected LogicalProcessor[] initProcessorCounts() {
        if (VersionHelpers.IsWindows7OrGreater()) {
            return getLogicalProcessorInformationEx();
        } else {
            return getLogicalProcessorInformation();
        }
    }

    private LogicalProcessor[] getLogicalProcessorInformationEx() {
        // Collect a list of logical processors on each physical core and
        // package. These will be 64-bit bitmasks.
        List<GROUP_AFFINITY[]> packageMaskList = new ArrayList<>();
        List<GROUP_AFFINITY> coreMaskList = new ArrayList<>();
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] processors = Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationAll);
        for (SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX proc : processors) {
            if (proc.relationship == WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage) {
                // Package may be on multiple processor groups
                packageMaskList.add(proc.payload.Processor.getGroupMask());
            } else if (proc.relationship == WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore) {
                // Exactly one element for the ProcessorCore relationship
                coreMaskList.add(proc.payload.Processor.getGroupMask()[0]);
            }
        }
        this.physicalProcessorCount = coreMaskList.size();
        this.physicalPackageCount = packageMaskList.size();
        // Sort the list so core and package numbers
        // increment as expected
        coreMaskList.sort(Comparator.comparing(c -> c.group * 64L + c.mask.longValue()));
        packageMaskList.sort(Comparator.comparing(p -> p[0].group * 64L + p[0].mask.longValue()));

        // Assign logical processors to cores and packages
        List<LogicalProcessor> logProcs = new ArrayList<>();
        for (int core = 0; core < this.physicalProcessorCount; core++) {
            GROUP_AFFINITY coreMask = coreMaskList.get(core);
            int group = coreMask.group;
            long mask = coreMask.mask.longValue();
            // Lowest and Highest set bits, indexing from 0
            int lowBit = Long.numberOfTrailingZeros(mask);
            int hiBit = 63 - Long.numberOfLeadingZeros(mask);
            // Create logical processors for this core
            for (int i = lowBit; i <= hiBit; i++) {
                if ((mask & (1L << i)) > 0) {
                    LogicalProcessor logProc = new LogicalProcessor();
                    logProc.setProcessorGroup(group);
                    logProc.setProcessorNumber(i);
                    logProc.setPhysicalProcessorNumber(core);
                    logProc.setPhysicalPackageNumber(
                            getBitMatchingPackageNumber(packageMaskList, i, group));
                    logProcs.add(logProc);
                }
            }
        }
        this.logicalProcessorCount = logProcs.size();
        return logProcs.toArray(new LogicalProcessor[0]);
    }

    private LogicalProcessor[] getLogicalProcessorInformation() {
        // Collect a list of logical processors on each physical core and
        // package. These will be 64-bit bitmasks.
        List<Long> packageMaskList = new ArrayList<>();
        List<Long> coreMaskList = new ArrayList<>();
        WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION[] processors = Kernel32Util.getLogicalProcessorInformation();
        for (SYSTEM_LOGICAL_PROCESSOR_INFORMATION proc : processors) {
            if (proc.relationship == WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage) {
                packageMaskList.add(proc.processorMask.longValue());
            } else if (proc.relationship == WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore) {
                coreMaskList.add(proc.processorMask.longValue());
            }
        }
        this.physicalProcessorCount = coreMaskList.size();
        this.physicalPackageCount = packageMaskList.size();
        // Sort the list (natural ordering) so core and package numbers
        // increment as expected.
        coreMaskList.sort(null);
        packageMaskList.sort(null);

        // Assign logical processors to cores and packages
        List<LogicalProcessor> logProcs = new ArrayList<>();
        for (int core = 0; core < this.physicalProcessorCount; core++) {
            long coreMask = coreMaskList.get(core);
            // Lowest and Highest set bits, indexing from 0
            int lowBit = Long.numberOfTrailingZeros(coreMask);
            int hiBit = 63 - Long.numberOfLeadingZeros(coreMask);
            // Create logical processors for this core
            for (int i = lowBit; i <= hiBit; i++) {
                if ((coreMask & (1L << i)) > 0) {
                    LogicalProcessor logProc = new LogicalProcessor();
                    logProc.setProcessorNumber(i);
                    logProc.setPhysicalProcessorNumber(core);
                    logProc.setPhysicalPackageNumber(getBitMatchingPackageNumber(packageMaskList, i));
                    logProcs.add(logProc);
                }
            }
        }
        this.logicalProcessorCount = logProcs.size();
        return logProcs.toArray(new LogicalProcessor[0]);
    }

    /**
     * Iterate over the package mask list and find a matching mask index
     * 
     * @param packageMaskList
     *            The list of bitmasks to iterate
     * @param logProc
     *            The bit to find matching mask
     * @return The index of the list which matched the bit
     */
    private int getBitMatchingPackageNumber(List<Long> packageMaskList, int logProc) {
        for (int i = 0; i < packageMaskList.size(); i++) {
            if ((packageMaskList.get(i).longValue() & (1L << logProc)) > 0) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Iterate over the package mask list and find a matching mask index
     * 
     * @param packageMaskList
     *            The list of bitmasks to iterate
     * @param logProc
     *            The bit to find matching mask
     * @param group
     *            The processor group to match
     * @return The index of the list which matched the bit
     */
    private int getBitMatchingPackageNumber(List<GROUP_AFFINITY[]> packageMaskList, int logProc, int group) {
        for (int i = 0; i < packageMaskList.size(); i++) {
            for (int j = 0; j < packageMaskList.get(i).length; j++) {
                if ((packageMaskList.get(i)[j].mask.longValue() & (1L << logProc)) > 0
                        && packageMaskList.get(i)[j].group == group) {
                    return i;
                }
            }
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] querySystemCpuLoadTicks() {
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

        // IRQ and ticks:
        // Percent time raw value is cumulative 100NS-ticks
        // Divide by 10_000 to get milliseconds

        Map<SystemTickCountProperty, Long> valueMap = this.systemTickPerfCounters.queryValues();
        ticks[TickType.IRQ.getIndex()] = valueMap.getOrDefault(SystemTickCountProperty.PERCENTINTERRUPTTIME, 0L)
                / 10_000L;
        ticks[TickType.SOFTIRQ.getIndex()] = valueMap.getOrDefault(SystemTickCountProperty.PERCENTDPCTIME, 0L)
                / 10_000L;

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
    public long[] queryCurrentFreq() {
        return queryNTPower(2); // Current is field index 2
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long queryMaxFreq() {
        long[] freqs = queryNTPower(1); // Max is field index 1
        return Arrays.stream(freqs).max().getAsLong();
    }

    /**
     * Call CallNTPowerInformation for Processor information and return an array
     * of the specified index
     * 
     * @param fieldIndex
     *            The field, in order as defined in the
     *            {@link PowrProf#PROCESSOR_INFORMATION} structure.
     * @return The array of values.
     */
    private long[] queryNTPower(int fieldIndex) {
        long[] freqs = new long[getLogicalProcessorCount()];
        ProcessorPowerInformation[] ppiArray = (ProcessorPowerInformation[]) new ProcessorPowerInformation()
                .toArray(freqs.length);
        if (0 != PowrProf.INSTANCE.CallNtPowerInformation(PowrProf.PROCESSOR_INFORMATION, null, 0, ppiArray[0],
                ppiArray[0].size() * ppiArray.length)) {
            LOG.error("Unable to get Processor Information");
            Arrays.fill(freqs, -1L);
            return freqs;
        }
        for (int i = 0; i < freqs.length; i++) {
            if (fieldIndex == 1) { // Max
                freqs[i] = ppiArray[i].MaxMhz * 1_000_000L;
            } else if (fieldIndex == 2) { // Current
                freqs[i] = ppiArray[i].CurrentMhz * 1_000_000L;
            } else {
                freqs[i] = -1L;
            }
        }
        return freqs;
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
    public long[][] queryProcessorCpuLoadTicks() {
        Map<ProcessorTickCountProperty, List<Long>> valueMap = this.processorTickPerfCounters.queryValuesWildcard();
        List<String> instances = this.processorTickPerfCounters.getInstancesFromLastQuery();
        List<Long> systemList = valueMap.get(ProcessorTickCountProperty.PERCENTPRIVILEGEDTIME);
        List<Long> userList = valueMap.get(ProcessorTickCountProperty.PERCENTUSERTIME);
        List<Long> irqList = valueMap.get(ProcessorTickCountProperty.PERCENTINTERRUPTTIME);
        List<Long> softIrqList = valueMap.get(ProcessorTickCountProperty.PERCENTDPCTIME);
        // % Processor Time is actually Idle time
        List<Long> idleList = valueMap.get(ProcessorTickCountProperty.PERCENTPROCESSORTIME);

        long[][] ticks = new long[this.logicalProcessorCount][TickType.values().length];
        if (instances.isEmpty() || systemList == null || userList == null || irqList == null || softIrqList == null
                || idleList == null) {
            return ticks;
        }
        for (int p = 0; p < instances.size(); p++) {
            int cpu = ParseUtil.parseIntOrDefault(instances.get(p), 0);
            if (cpu >= this.logicalProcessorCount) {
                continue;
            }
            ticks[cpu][TickType.SYSTEM.getIndex()] = systemList.get(cpu);
            ticks[cpu][TickType.USER.getIndex()] = userList.get(cpu);
            ticks[cpu][TickType.IRQ.getIndex()] = irqList.get(cpu);
            ticks[cpu][TickType.SOFTIRQ.getIndex()] = softIrqList.get(cpu);
            ticks[cpu][TickType.IDLE.getIndex()] = idleList.get(cpu);

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
        Map<ContextSwitchProperty, Long> valueMap = this.contextSwitchPerfCounters.queryValues();
        return valueMap.getOrDefault(ContextSwitchProperty.CONTEXTSWITCHESPERSEC, 0L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getInterrupts() {
        Map<InterruptsProperty, Long> valueMap = this.interruptsPerfCounters.queryValues();
        return valueMap.getOrDefault(InterruptsProperty.INTERRUPTSPERSEC, 0L);
    }
}