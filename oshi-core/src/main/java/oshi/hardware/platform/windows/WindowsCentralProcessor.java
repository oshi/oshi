/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.PowrProf.POWER_INFORMATION_LEVEL;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.LogicalProcessorInformation;
import oshi.driver.windows.perfmon.ProcessorInformation;
import oshi.driver.windows.perfmon.ProcessorInformation.InterruptsProperty;
import oshi.driver.windows.perfmon.ProcessorInformation.ProcessorFrequencyProperty;
import oshi.driver.windows.perfmon.ProcessorInformation.ProcessorTickCountProperty;
import oshi.driver.windows.perfmon.SystemInformation;
import oshi.driver.windows.perfmon.SystemInformation.ContextSwitchProperty;
import oshi.driver.windows.wmi.Win32Processor;
import oshi.driver.windows.wmi.Win32Processor.ProcessorIdProperty;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.windows.PowrProf;
import oshi.jna.platform.windows.PowrProf.ProcessorPowerInformation;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;

/**
 * A CPU, representing all of a system's processors. It may contain multiple
 * individual Physical and Logical processors.
 */
@ThreadSafe
final class WindowsCentralProcessor extends AbstractCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsCentralProcessor.class);

    // populated by initProcessorCounts called by the parent constructor
    private Map<String, Integer> numaNodeProcToLogicalProcMap;

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
        long cpuVendorFreq = 0L;
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
            try {
                cpuVendorFreq = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath, "~MHz")
                        * 1_000_000L;
            } catch (Win32Exception e) {
                // Leave as 0, parse the identifier as backup
            }
        }
        if (!cpuIdentifier.isEmpty()) {
            cpuFamily = parseIdentifier(cpuIdentifier, "Family");
            cpuModel = parseIdentifier(cpuIdentifier, "Model");
            cpuStepping = parseIdentifier(cpuIdentifier, "Stepping");
        }
        SYSTEM_INFO sysinfo = new SYSTEM_INFO();
        Kernel32.INSTANCE.GetNativeSystemInfo(sysinfo);
        int processorArchitecture = sysinfo.processorArchitecture.pi.wProcessorArchitecture.intValue();
        if (processorArchitecture == 9 // PROCESSOR_ARCHITECTURE_AMD64
                || processorArchitecture == 12 // PROCESSOR_ARCHITECTURE_ARM64
                || processorArchitecture == 6) { // PROCESSOR_ARCHITECTURE_IA64
            cpu64bit = true;
        }
        WmiResult<ProcessorIdProperty> processorId = Win32Processor.queryProcessorId();
        if (processorId.getResultCount() > 0) {
            processorID = WmiUtil.getString(processorId, ProcessorIdProperty.PROCESSORID, 0);
        } else {
            processorID = createProcessorID(cpuStepping, cpuModel, cpuFamily,
                    cpu64bit ? new String[] { "ia64" } : new String[0]);
        }
        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuVendorFreq);
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
    private static String parseIdentifier(String identifier, String key) {
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
    protected List<LogicalProcessor> initProcessorCounts() {
        if (VersionHelpers.IsWindows7OrGreater()) {
            List<LogicalProcessor> logProcs = LogicalProcessorInformation.getLogicalProcessorInformationEx();
            // Save numaNode,Processor lookup for future PerfCounter instance lookup
            // The processor number is based on the Processor Group, so we keep a separate
            // index by NUMA node.
            int curNode = -1;
            int procNum = 0;
            // 0-indexed list of all lps for array lookup
            int lp = 0;
            this.numaNodeProcToLogicalProcMap = new HashMap<>();
            for (LogicalProcessor logProc : logProcs) {
                int node = logProc.getNumaNode();
                // This list is grouped by NUMA node so a change in node will reset this counter
                if (node != curNode) {
                    curNode = node;
                    procNum = 0;
                }
                numaNodeProcToLogicalProcMap.put(String.format("%d,%d", logProc.getNumaNode(), procNum++), lp++);
            }
            return logProcs;
        } else {
            return LogicalProcessorInformation.getLogicalProcessorInformation();
        }
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        // To get load in processor group scenario, we need perfmon counters, but the
        // _Total instance is an average rather than total (scaled) number of ticks
        // which matches GetSystemTimes() results. We can just query the per-processor
        // ticks and add them up. Calling the get() method gains the benefit of
        // synchronizing this output with the memoized result of per-processor ticks as
        // well.
        long[] ticks = new long[TickType.values().length];
        // Sum processor ticks
        long[][] procTicks = getProcessorCpuLoadTicks();
        for (int i = 0; i < ticks.length; i++) {
            for (long[] procTick : procTicks) {
                ticks[i] += procTick[i];
            }
        }
        return ticks;
    }

    @Override
    public long[] queryCurrentFreq() {
        if (VersionHelpers.IsWindows7OrGreater()) {
            Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> instanceValuePair = ProcessorInformation
                    .queryFrequencyCounters();
            List<String> instances = instanceValuePair.getA();
            Map<ProcessorFrequencyProperty, List<Long>> valueMap = instanceValuePair.getB();
            List<Long> percentMaxList = valueMap.get(ProcessorFrequencyProperty.PERCENTOFMAXIMUMFREQUENCY);
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
        return Arrays.stream(freqs).max().orElse(-1L);
    }

    /**
     * Call CallNTPowerInformation for Processor information and return an array of
     * the specified index
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
        if (0 != PowrProf.INSTANCE.CallNtPowerInformation(POWER_INFORMATION_LEVEL.ProcessorInformation, null, 0, mem,
                bufferSize)) {
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
        Pair<List<String>, Map<ProcessorTickCountProperty, List<Long>>> instanceValuePair = ProcessorInformation
                .queryProcessorCounters();
        List<String> instances = instanceValuePair.getA();
        Map<ProcessorTickCountProperty, List<Long>> valueMap = instanceValuePair.getB();
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
        return SystemInformation.queryContextSwitchCounters().getOrDefault(ContextSwitchProperty.CONTEXTSWITCHESPERSEC,
                0L);
    }

    @Override
    public long queryInterrupts() {
        return ProcessorInformation.queryInterruptCounters().getOrDefault(InterruptsProperty.INTERRUPTSPERSEC, 0L);
    }
}
