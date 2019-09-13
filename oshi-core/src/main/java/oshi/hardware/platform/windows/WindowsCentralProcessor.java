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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; // NOSONAR squid:S1191
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.GROUP_AFFINITY;
import com.sun.jna.platform.win32.WinNT.LOGICAL_PROCESSOR_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.NUMA_NODE_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.PROCESSOR_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.windows.PowrProf;
import oshi.jna.platform.windows.PowrProf.ProcessorPowerInformation;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.PerfCounterQuery;
import oshi.util.platform.windows.PerfCounterQuery.PdhCounterProperty;
import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery.PdhCounterWildcardProperty;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * A CPU, representing all of a system's processors. It may contain multiple
 * individual Physical and Logical processors.
 */
public class WindowsCentralProcessor extends AbstractCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsCentralProcessor.class);

    private static final String PROCESSOR = "Processor";

    private Map<String, Integer> numaNodeProcToLogicalProcMap;

    enum ProcessorProperty {
        PROCESSORID;
    }

    /*
     * For tick counts
     */
    enum ProcessorTickCountProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME(PerfCounterQuery.NOT_TOTAL_INSTANCES),
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

        @Override
        public String getCounter() {
            return counter;
        }
    }

    private final PerfCounterWildcardQuery<ProcessorTickCountProperty> processorTickPerfCounters = VersionHelpers
            .IsWindows7OrGreater()
                    ? new PerfCounterWildcardQuery<>(ProcessorTickCountProperty.class, "Processor Information",
                            // NAME field includes NUMA nodes
                            "Win32_PerfRawData_Counters_ProcessorInformation WHERE NOT Name LIKE\"%_Total\"",
                            "Processor Tick Count")
                    : new PerfCounterWildcardQuery<>(ProcessorTickCountProperty.class, PROCESSOR,
                            // Older systems just have processor # in name
                            "Win32_PerfRawData_PerfOS_Processor WHERE NOT Name=\"_Total\"", "Processor Tick Count");

    enum SystemTickCountProperty implements PdhCounterProperty {
        PERCENTDPCTIME(PerfCounterQuery.TOTAL_INSTANCE, "% DPC Time"), //
        PERCENTINTERRUPTTIME(PerfCounterQuery.TOTAL_INSTANCE, "% Interrupt Time");

        private final String instance;
        private final String counter;

        SystemTickCountProperty(String instance, String counter) {
            this.instance = instance;
            this.counter = counter;
        }

        @Override
        public String getInstance() {
            return instance;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    private final PerfCounterQuery<SystemTickCountProperty> systemTickPerfCounters = new PerfCounterQuery<>(
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

        @Override
        public String getInstance() {
            return instance;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    private final PerfCounterQuery<InterruptsProperty> interruptsPerfCounters = new PerfCounterQuery<>(
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

        @Override
        public String getInstance() {
            return instance;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    private final PerfCounterQuery<ContextSwitchProperty> contextSwitchPerfCounters = new PerfCounterQuery<>(
            ContextSwitchProperty.class, "System", "Win32_PerfRawData_PerfOS_System");

    // Requires Win7 or greater
    enum ProcessorFrequencyProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        Name(PerfCounterQuery.NOT_TOTAL_INSTANCES),
        // Remaining elements define counters
        PercentofMaximumFrequency("% of Maximum Frequency");

        private final String counter;

        ProcessorFrequencyProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    // Requires Win7 or greater
    private final PerfCounterWildcardQuery<ProcessorFrequencyProperty> processorFrequencyCounters = new PerfCounterWildcardQuery<>(
            ProcessorFrequencyProperty.class, "Processor Information",
            // NAME field includes NUMA nodes
            "Win32_PerfRawData_Counters_ProcessorInformation WHERE NOT Name LIKE\"%_Total\"", "Processor Frequency");

    /**
     * Initializes Class variables
     */
    @Override
    protected ProcessorIdentifier queryProcessorId() {
        String cpuVendor = "";
        String cpuName = "";
        String cpuIdentifier = "";
        String cpuFamily = "";
        String cpuModel = "";
        String cpuStepping = "";
        String processorID;
        boolean cpu64bit = false;

        final String cpuRegistryRoot = "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\";
        String[] processorIds = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryRoot);
        if (processorIds.length > 0) {
            String cpuRegistryPath = cpuRegistryRoot + processorIds[0];
            cpuVendor = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
                    "VendorIdentifier");
            cpuName = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
                    "ProcessorNameString");
            cpuIdentifier = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
                    "Identifier");
        }
        if (!cpuIdentifier.isEmpty()) {
            cpuFamily = parseIdentifier(cpuIdentifier, "Family");
            cpuModel = parseIdentifier(cpuIdentifier, "Model");
            cpuStepping = parseIdentifier(cpuIdentifier, "Stepping");
        }
        SYSTEM_INFO sysinfo = new SYSTEM_INFO();
        Kernel32.INSTANCE.GetNativeSystemInfo(sysinfo);
        int processorArchitecture = sysinfo.processorArchitecture.pi.wProcessorArchitecture.intValue();
        // JNA reads wrong union member. Temporarily override.
        // See https://github.com/java-native-access/jna/pull/1134
        if (sysinfo.processorArchitecture.dwOemID.intValue() > 0) {
            processorArchitecture = sysinfo.processorArchitecture.dwOemID.intValue() & 0xffff;
        }
        if (processorArchitecture == 9 // PROCESSOR_ARCHITECTURE_AMD64
                || processorArchitecture == 12 // PROCESSOR_ARCHITECTURE_ARM64
                || processorArchitecture == 6) { // PROCESSOR_ARCHITECTURE_IA64
            cpu64bit = true;
        }

        WmiQuery<ProcessorProperty> processorIdQuery = new WmiQuery<>("Win32_Processor", ProcessorProperty.class);
        WmiResult<ProcessorProperty> processorId = WmiQueryHandler.createInstance().queryWMI(processorIdQuery);
        if (processorId.getResultCount() > 0) {
            processorID = WmiUtil.getString(processorId, ProcessorProperty.PROCESSORID, 0);
        } else {
            processorID = createProcessorID(cpuStepping, cpuModel, cpuFamily,
                    cpu64bit ? new String[] { "ia64" } : new String[0]);
        }

        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit);
    }

    /**
     * Parses identifier string
     *
     * @param identifier
     *            the full identifier string
     * @param key
     *            the key to retrieve
     * @return the string following id
     */
    private String parseIdentifier(String identifier, String key) {
        String[] idSplit = ParseUtil.whitespaces.split(identifier);
        boolean found = false;
        for (String s : idSplit) {
            // If key string found, return next value
            if (found) {
                return s;
            }
            found = s.equals(key);
        }
        // If key string not found, return empty string
        return "";
    }

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
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] procInfo = Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationAll);
        List<GROUP_AFFINITY[]> packages = new ArrayList<>();
        List<NUMA_NODE_RELATIONSHIP> numaNodes = new ArrayList<>();
        List<GROUP_AFFINITY> cores = new ArrayList<>();

        for (int i = 0; i < procInfo.length; i++) {
            switch (procInfo[i].relationship) {
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage:
                packages.add(((PROCESSOR_RELATIONSHIP) procInfo[i]).groupMask);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode:
                numaNodes.add((NUMA_NODE_RELATIONSHIP) procInfo[i]);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore:
                cores.add(((PROCESSOR_RELATIONSHIP) procInfo[i]).groupMask[0]);
                break;
            default:
                // Ignore Group and Cache info
                break;
            }
        }
        // Windows doesn't define core and package numbers, so we sort the lists
        // so core and package numbers increment consistently with processor
        // numbers/bitmasks, ordered in groups
        cores.sort(Comparator.comparing(c -> c.group * 64L + c.mask.longValue()));
        packages.sort(Comparator.comparing(p -> p[0].group * 64L + p[0].mask.longValue()));

        // Iterate Logical Processors and use bitmasks to match packages, cores,
        // and NUMA nodes
        List<LogicalProcessor> logProcs = new ArrayList<>();
        for (GROUP_AFFINITY coreMask : cores) {
            int group = coreMask.group;
            long mask = coreMask.mask.longValue();
            // Iterate mask for logical processor numbers
            int lowBit = Long.numberOfTrailingZeros(mask);
            int hiBit = 63 - Long.numberOfLeadingZeros(mask);
            for (int lp = lowBit; lp <= hiBit; lp++) {
                if ((mask & (1L << lp)) > 0) {
                    LogicalProcessor logProc = new LogicalProcessor(lp, getMatchingCore(cores, group, lp),
                            getMatchingPackage(packages, group, lp), getMatchingNumaNode(numaNodes, group, lp), group);
                    logProcs.add(logProc);
                }
            }
        }
        // Sort by numaNode and then logical processor number to match
        // PerfCounter/WMI ordering
        logProcs.sort(Comparator.comparing(LogicalProcessor::getNumaNode)
                .thenComparing(LogicalProcessor::getProcessorNumber));
        // Save numaNode,Processor lookup for future PerfCounter instance lookup
        int lp = 0;
        this.numaNodeProcToLogicalProcMap = new HashMap<>();
        for (LogicalProcessor logProc : logProcs) {
            numaNodeProcToLogicalProcMap
                    .put(String.format("%d,%d", logProc.getNumaNode(), logProc.getProcessorNumber()), lp++);
        }
        return logProcs.toArray(new LogicalProcessor[0]);
    }

    private static int getMatchingPackage(List<GROUP_AFFINITY[]> packages, int g, int lp) {
        for (int i = 0; i < packages.size(); i++) {
            for (int j = 0; j < packages.get(i).length; j++) {
                if ((packages.get(i)[j].mask.longValue() & (1L << lp)) > 0 && packages.get(i)[j].group == g) {
                    return i;
                }
            }
        }
        return 0;
    }

    private static int getMatchingNumaNode(List<NUMA_NODE_RELATIONSHIP> numaNodes, int g, int lp) {
        for (int j = 0; j < numaNodes.size(); j++) {
            if ((numaNodes.get(j).groupMask.mask.longValue() & (1L << lp)) > 0
                    && numaNodes.get(j).groupMask.group == g) {
                return numaNodes.get(j).nodeNumber;
            }
        }
        return 0;
    }

    private static int getMatchingCore(List<GROUP_AFFINITY> cores, int g, int lp) {
        for (int j = 0; j < cores.size(); j++) {
            if ((cores.get(j).mask.longValue() & (1L << lp)) > 0 && cores.get(j).group == g) {
                return j;
            }
        }
        return 0;
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
        // Sort the list (natural ordering) so core and package numbers
        // increment as expected.
        coreMaskList.sort(null);
        packageMaskList.sort(null);

        // Assign logical processors to cores and packages
        List<LogicalProcessor> logProcs = new ArrayList<>();
        for (int core = 0; core < coreMaskList.size(); core++) {
            long coreMask = coreMaskList.get(core);
            // Lowest and Highest set bits, indexing from 0
            int lowBit = Long.numberOfTrailingZeros(coreMask);
            int hiBit = 63 - Long.numberOfLeadingZeros(coreMask);
            // Create logical processors for this core
            for (int i = lowBit; i <= hiBit; i++) {
                if ((coreMask & (1L << i)) > 0) {
                    LogicalProcessor logProc = new LogicalProcessor(i, core,
                            getBitMatchingPackageNumber(packageMaskList, i));
                    logProcs.add(logProc);
                }
            }
        }
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

    @Override
    public long[] queryCurrentFreq() {
        if (VersionHelpers.IsWindows7OrGreater()) {
            Map<ProcessorFrequencyProperty, List<Long>> valueMap = this.processorFrequencyCounters
                    .queryValuesWildcard();
            List<String> instances = this.processorTickPerfCounters.getInstancesFromLastQuery();
            List<Long> percentMaxList = valueMap.get(ProcessorFrequencyProperty.PercentofMaximumFrequency);
            if (!instances.isEmpty()) {
                long maxFreq = this.getMaxFreq();
                long[] freqs = new long[getLogicalProcessorCount()];
                for (int p = 0; p < instances.size(); p++) {
                    int cpu = instances.get(p).contains(",")
                            ? numaNodeProcToLogicalProcMap.getOrDefault(instances.get(p), 0)
                            : ParseUtil.parseIntOrDefault(instances.get(p), 0);
                    if (cpu >= getLogicalProcessorCount()) {
                        continue;
                    }
                    freqs[cpu] = percentMaxList.get(cpu) * maxFreq / 100L;
                }
                return freqs;
            }
        }
        // If <Win7 or anything failed in PDH/WMI, use the native call
        return queryNTPower(2); // Current is field index 2
    }

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
        ProcessorPowerInformation ppi = new ProcessorPowerInformation();
        long[] freqs = new long[getLogicalProcessorCount()];
        int bufferSize = ppi.size() * freqs.length;
        Memory mem = new Memory(bufferSize);
        if (0 != PowrProf.INSTANCE.CallNtPowerInformation(PowrProf.POWER_INFORMATION_LEVEL.ProcessorInformation, null,
                0, mem, bufferSize)) {
            LOG.error("Unable to get Processor Information");
            Arrays.fill(freqs, -1L);
            return freqs;
        }
        for (int i = 0; i < freqs.length; i++) {
            ppi = new ProcessorPowerInformation(mem.share(i * (long) ppi.size()));
            if (fieldIndex == 1) { // Max
                freqs[i] = ppi.maxMhz * 1_000_000L;
            } else if (fieldIndex == 2) { // Current
                freqs[i] = ppi.currentMhz * 1_000_000L;
            } else {
                freqs[i] = -1L;
            }
        }
        return freqs;
    }

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

        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        if (instances.isEmpty() || systemList == null || userList == null || irqList == null || softIrqList == null
                || idleList == null) {
            return ticks;
        }
        for (int p = 0; p < instances.size(); p++) {
            int cpu = instances.get(p).contains(",") ? numaNodeProcToLogicalProcMap.getOrDefault(instances.get(p), 0)
                    : ParseUtil.parseIntOrDefault(instances.get(p), 0);
            if (cpu >= getLogicalProcessorCount()) {
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

    @Override
    public long queryContextSwitches() {
        Map<ContextSwitchProperty, Long> valueMap = this.contextSwitchPerfCounters.queryValues();
        return valueMap.getOrDefault(ContextSwitchProperty.CONTEXTSWITCHESPERSEC, 0L);
    }

    @Override
    public long queryInterrupts() {
        Map<InterruptsProperty, Long> valueMap = this.interruptsPerfCounters.queryValues();
        return valueMap.getOrDefault(InterruptsProperty.INTERRUPTSPERSEC, 0L);
    }
}
