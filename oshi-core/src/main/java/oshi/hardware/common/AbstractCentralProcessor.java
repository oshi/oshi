/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.hardware.common;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.util.ParseUtil;

/**
 * A CPU.
 */
@ThreadSafe
public abstract class AbstractCentralProcessor implements CentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCentralProcessor.class);

    private final Supplier<ProcessorIdentifier> cpuid = memoize(this::queryProcessorId);
    private final Supplier<Long> maxFreq = memoize(this::queryMaxFreq, defaultExpiration());
    private final Supplier<long[]> currentFreq = memoize(this::queryCurrentFreq, defaultExpiration());
    private final Supplier<Long> contextSwitches = memoize(this::queryContextSwitches, defaultExpiration());
    private final Supplier<Long> interrupts = memoize(this::queryInterrupts, defaultExpiration());

    private final Supplier<long[]> systemCpuLoadTicks = memoize(this::querySystemCpuLoadTicks, defaultExpiration());
    private final Supplier<long[][]> processorCpuLoadTicks = memoize(this::queryProcessorCpuLoadTicks,
            defaultExpiration());

    // Logical and Physical Processor Counts
    private final int physicalPackageCount;
    private final int physicalProcessorCount;
    private final int logicalProcessorCount;

    // Processor info, initialized in constructor
    private final List<LogicalProcessor> logicalProcessors;

    /**
     * Create a Processor
     */
    protected AbstractCentralProcessor() {
        // Populate logical processor array.
        this.logicalProcessors = Collections.unmodifiableList(initProcessorCounts());
        // Init processor counts
        Set<String> physProcPkgs = new HashSet<>();
        Set<Integer> physPkgs = new HashSet<>();
        for (LogicalProcessor logProc : this.logicalProcessors) {
            int pkg = logProc.getPhysicalPackageNumber();
            physProcPkgs.add(logProc.getPhysicalProcessorNumber() + ":" + pkg);
            physPkgs.add(pkg);
        }
        this.logicalProcessorCount = this.logicalProcessors.size();
        this.physicalProcessorCount = physProcPkgs.size();
        this.physicalPackageCount = physPkgs.size();
    }

    /**
     * Updates logical and physical processor counts and arrays
     *
     * @return An array of initialized Logical Processors
     */
    protected abstract List<LogicalProcessor> initProcessorCounts();

    /**
     * Updates logical and physical processor counts and arrays
     *
     * @return An array of initialized Logical Processors
     */
    protected abstract ProcessorIdentifier queryProcessorId();

    @Override
    public ProcessorIdentifier getProcessorIdentifier() {
        return cpuid.get();
    }

    @Override
    public long getMaxFreq() {
        return maxFreq.get();
    }

    /**
     * Get processor max frequency.
     *
     * @return The max frequency.
     */
    protected abstract long queryMaxFreq();

    @Override
    public long[] getCurrentFreq() {
        long[] freq = currentFreq.get();
        if (freq.length == getLogicalProcessorCount()) {
            return freq;
        }
        long[] freqs = new long[getLogicalProcessorCount()];
        Arrays.fill(freqs, freq[0]);
        return freqs;
    }

    /**
     * Get processor current frequency.
     *
     * @return The current frequency.
     */
    protected abstract long[] queryCurrentFreq();

    @Override
    public long getContextSwitches() {
        return contextSwitches.get();
    }

    /**
     * Get number of context switches
     *
     * @return The context switches
     */
    protected abstract long queryContextSwitches();

    @Override
    public long getInterrupts() {
        return interrupts.get();
    }

    /**
     * Get number of interrupts
     *
     * @return The interrupts
     */
    protected abstract long queryInterrupts();

    @Override
    public List<LogicalProcessor> getLogicalProcessors() {
        return this.logicalProcessors;
    }

    @Override
    public long[] getSystemCpuLoadTicks() {
        return systemCpuLoadTicks.get();
    }

    /**
     * Get the system CPU load ticks
     *
     * @return The system CPU load ticks
     */
    protected abstract long[] querySystemCpuLoadTicks();

    @Override
    public long[][] getProcessorCpuLoadTicks() {
        return processorCpuLoadTicks.get();
    }

    /**
     * Get the processor CPU load ticks
     *
     * @return The processor CPU load ticks
     */
    protected abstract long[][] queryProcessorCpuLoadTicks();

    @Override
    public double getSystemCpuLoadBetweenTicks(long[] oldTicks) {
        if (oldTicks.length != TickType.values().length) {
            throw new IllegalArgumentException(
                    "Tick array " + oldTicks.length + " should have " + TickType.values().length + " elements");
        }
        long[] ticks = getSystemCpuLoadTicks();
        // Calculate total
        long total = 0;
        for (int i = 0; i < ticks.length; i++) {
            total += ticks[i] - oldTicks[i];
        }
        // Calculate idle from difference in idle and IOwait
        long idle = ticks[TickType.IDLE.getIndex()] + ticks[TickType.IOWAIT.getIndex()]
                - oldTicks[TickType.IDLE.getIndex()] - oldTicks[TickType.IOWAIT.getIndex()];
        LOG.trace("Total ticks: {}  Idle ticks: {}", total, idle);

        return total > 0 && idle >= 0 ? (double) (total - idle) / total : 0d;
    }

    @Override
    public double[] getProcessorCpuLoadBetweenTicks(long[][] oldTicks) {
        if (oldTicks.length != this.logicalProcessorCount || oldTicks[0].length != TickType.values().length) {
            throw new IllegalArgumentException(
                    "Tick array " + oldTicks.length + " should have " + this.logicalProcessorCount
                            + " arrays, each of which has " + TickType.values().length + " elements");
        }
        long[][] ticks = getProcessorCpuLoadTicks();
        double[] load = new double[this.logicalProcessorCount];
        for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
            long total = 0;
            for (int i = 0; i < ticks[cpu].length; i++) {
                total += ticks[cpu][i] - oldTicks[cpu][i];
            }
            // Calculate idle from difference in idle and IOwait
            long idle = ticks[cpu][TickType.IDLE.getIndex()] + ticks[cpu][TickType.IOWAIT.getIndex()]
                    - oldTicks[cpu][TickType.IDLE.getIndex()] - oldTicks[cpu][TickType.IOWAIT.getIndex()];
            LOG.trace("CPU: {}  Total ticks: {}  Idle ticks: {}", cpu, total, idle);
            // update
            load[cpu] = total > 0 && idle >= 0 ? (double) (total - idle) / total : 0d;
        }
        return load;
    }

    @Override
    public int getLogicalProcessorCount() {
        return this.logicalProcessorCount;
    }

    @Override
    public int getPhysicalProcessorCount() {
        return this.physicalProcessorCount;
    }

    @Override
    public int getPhysicalPackageCount() {
        return this.physicalPackageCount;
    }

    /**
     * Creates a Processor ID by encoding the stepping, model, family, and feature
     * flags.
     *
     * @param stepping
     *            The CPU stepping
     * @param model
     *            The CPU model
     * @param family
     *            The CPU family
     * @param flags
     *            A space-delimited list of CPU feature flags
     * @return The Processor ID string
     */
    protected static String createProcessorID(String stepping, String model, String family, String[] flags) {
        long processorIdBytes = 0L;
        long steppingL = ParseUtil.parseLongOrDefault(stepping, 0L);
        long modelL = ParseUtil.parseLongOrDefault(model, 0L);
        long familyL = ParseUtil.parseLongOrDefault(family, 0L);
        // 3:0 – Stepping
        processorIdBytes |= steppingL & 0xf;
        // 19:16,7:4 – Model
        processorIdBytes |= (modelL & 0x0f) << 4;
        processorIdBytes |= (modelL & 0xf0) << 16;
        // 27:20,11:8 – Family
        processorIdBytes |= (familyL & 0x0f) << 8;
        processorIdBytes |= (familyL & 0xf0) << 20;
        // 13:12 – Processor Type, assume 0
        for (String flag : flags) {
            switch (flag) { // NOSONAR squid:S1479
            case "fpu":
                processorIdBytes |= 1L << 32;
                break;
            case "vme":
                processorIdBytes |= 1L << 33;
                break;
            case "de":
                processorIdBytes |= 1L << 34;
                break;
            case "pse":
                processorIdBytes |= 1L << 35;
                break;
            case "tsc":
                processorIdBytes |= 1L << 36;
                break;
            case "msr":
                processorIdBytes |= 1L << 37;
                break;
            case "pae":
                processorIdBytes |= 1L << 38;
                break;
            case "mce":
                processorIdBytes |= 1L << 39;
                break;
            case "cx8":
                processorIdBytes |= 1L << 40;
                break;
            case "apic":
                processorIdBytes |= 1L << 41;
                break;
            case "sep":
                processorIdBytes |= 1L << 43;
                break;
            case "mtrr":
                processorIdBytes |= 1L << 44;
                break;
            case "pge":
                processorIdBytes |= 1L << 45;
                break;
            case "mca":
                processorIdBytes |= 1L << 46;
                break;
            case "cmov":
                processorIdBytes |= 1L << 47;
                break;
            case "pat":
                processorIdBytes |= 1L << 48;
                break;
            case "pse-36":
                processorIdBytes |= 1L << 49;
                break;
            case "psn":
                processorIdBytes |= 1L << 50;
                break;
            case "clfsh":
                processorIdBytes |= 1L << 51;
                break;
            case "ds":
                processorIdBytes |= 1L << 53;
                break;
            case "acpi":
                processorIdBytes |= 1L << 54;
                break;
            case "mmx":
                processorIdBytes |= 1L << 55;
                break;
            case "fxsr":
                processorIdBytes |= 1L << 56;
                break;
            case "sse":
                processorIdBytes |= 1L << 57;
                break;
            case "sse2":
                processorIdBytes |= 1L << 58;
                break;
            case "ss":
                processorIdBytes |= 1L << 59;
                break;
            case "htt":
                processorIdBytes |= 1L << 60;
                break;
            case "tm":
                processorIdBytes |= 1L << 61;
                break;
            case "ia64":
                processorIdBytes |= 1L << 62;
                break;
            case "pbe":
                processorIdBytes |= 1L << 63;
                break;
            default:
                break;
            }
        }
        return String.format("%016X", processorIdBytes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getProcessorIdentifier().getName());
        sb.append("\n ").append(getPhysicalPackageCount()).append(" physical CPU package(s)");
        sb.append("\n ").append(getPhysicalProcessorCount()).append(" physical CPU core(s)");
        sb.append("\n ").append(getLogicalProcessorCount()).append(" logical CPU(s)");
        sb.append('\n').append("Identifier: ").append(getProcessorIdentifier().getIdentifier());
        sb.append('\n').append("ProcessorID: ").append(getProcessorIdentifier().getProcessorID());
        sb.append('\n').append("Microarchitecture: ").append(getProcessorIdentifier().getMicroarchitecture());
        return sb.toString();
    }
}