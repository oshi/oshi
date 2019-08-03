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
package oshi.hardware.platform.unix.solaris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; // NOSONAR

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.linux.Libc;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.solaris.KstatUtil;

/**
 * A CPU
 *
 * @author widdis[at]gmail[dot]com
 */
public class SolarisCentralProcessor extends AbstractCentralProcessor {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(SolarisCentralProcessor.class);

    /**
     * Create a Processor
     */
    public SolarisCentralProcessor() {
        super();
        // Initialize class variables
        initVars();

        LOG.debug("Initialized Processor");
    }

    private void initVars() {
        // Get first result
        Kstat ksp = KstatUtil.kstatLookup("cpu_info", -1, null);
        // Set values
        if (ksp != null && KstatUtil.kstatRead(ksp)) {
            setVendor(KstatUtil.kstatDataLookupString(ksp, "vendor_id"));
            setName(KstatUtil.kstatDataLookupString(ksp, "brand"));
            setStepping(KstatUtil.kstatDataLookupString(ksp, "stepping"));
            setModel(KstatUtil.kstatDataLookupString(ksp, "model"));
            setFamily(KstatUtil.kstatDataLookupString(ksp, "family"));
        }
        setCpu64("64".equals(ExecutingCommand.getFirstAnswer("isainfo -b").trim()));
        setProcessorID(getProcessorID(getStepping(), getModel(), getFamily()));

    }

    /**
     * Updates logical and physical processor counts from psrinfo
     */
    @Override
    protected LogicalProcessor[] initProcessorCounts() {
        Map<Integer, Integer> numaNodeMap = mapNumaNodes();
        List<Kstat> kstats = KstatUtil.kstatLookupAll("cpu_info", -1, null);
        Set<String> chipIDs = new HashSet<>();
        Set<String> coreChipIDs = new HashSet<>();
        this.logicalProcessorCount = 0;

        List<LogicalProcessor> logProcs = new ArrayList<>();
        for (Kstat ksp : kstats) {
            if (ksp != null && KstatUtil.kstatRead(ksp)) {
                int procId = logProcs.size(); // 0-indexed
                String chipId = KstatUtil.kstatDataLookupString(ksp, "chip_id");
                String coreId = KstatUtil.kstatDataLookupString(ksp, "core_id");
                LogicalProcessor logProc = new LogicalProcessor(procId, ParseUtil.parseIntOrDefault(coreId, 0),
                        ParseUtil.parseIntOrDefault(chipId, 0), numaNodeMap.getOrDefault(procId, 0));
                logProcs.add(logProc);
                coreChipIDs.add(coreId + ":" + chipId);
                chipIDs.add(chipId);
            }
        }

        this.logicalProcessorCount = logProcs.size();
        if (this.logicalProcessorCount < 1) {
            LOG.error("Couldn't find logical processor count. Assuming 1.");
            this.logicalProcessorCount = 1;
            logProcs.add(new LogicalProcessor(0, 0, 0));
        }
        this.physicalPackageCount = chipIDs.size();
        if (this.physicalPackageCount < 1) {
            LOG.error("Couldn't find physical package count. Assuming 1.");
            this.physicalPackageCount = 1;
        }
        this.physicalProcessorCount = coreChipIDs.size();
        if (this.physicalProcessorCount < 1) {
            LOG.error("Couldn't find physical processor count. Assuming 1.");
            this.physicalProcessorCount = 1;
        }
        return logProcs.toArray(new LogicalProcessor[0]);
    }

    private Map<Integer, Integer> mapNumaNodes() {
        Map<Integer, Integer> numaNodeMap = new HashMap<>();
        // Get numa node info from lgrpinfo
        List<String> lgrpinfo = ExecutingCommand.runNative("lgrpinfo -c leaves");
        // Format:
        // lgroup 0 (root):
        // CPUs 0 1
        // CPUs 0-7
        // CPUs 0-3 6 7 12 13
        int lgroup = 0;
        for (String line : lgrpinfo) {
            if (line.startsWith("lgroup")) {
                lgroup = ParseUtil.getFirstIntValue(line);
            } else if (line.contains("CPUs:")) {
                String[] cpuList = ParseUtil.whitespaces.split(line.split(":")[1]);
                for (String cpu : cpuList) {
                    // Will either be individual CPU or hyphen-delimited range
                    if (cpu.contains("-")) {
                        int first = ParseUtil.getFirstIntValue(cpu);
                        int last = ParseUtil.getNthIntValue(line, 2);
                        for (int i = first; i <= last; i++) {
                            numaNodeMap.put(i, lgroup);
                        }
                    } else {
                        numaNodeMap.put(ParseUtil.parseIntOrDefault(cpu, 0), lgroup);
                    }
                }
            }
        }
        return numaNodeMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getSystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        // Average processor ticks
        long[][] procTicks = getProcessorCpuLoadTicks();
        for (int i = 0; i < ticks.length; i++) {
            for (long[] procTick : procTicks) {
                ticks[i] += procTick[i];
            }
            ticks[i] /= procTicks.length;
        }
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getCurrentFreq() {
        long[] freqs = new long[getLogicalProcessorCount()];
        Arrays.fill(freqs, -1);
        for (int i = 0; i < freqs.length; i++) {
            for (Kstat ksp : KstatUtil.kstatLookupAll("cpu_info", i, null)) {
                if (KstatUtil.kstatRead(ksp)) {
                    freqs[i] = KstatUtil.kstatDataLookupLong(ksp, "current_clock_Hz");
                }
            }
        }
        return freqs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long queryMaxFreq() {
        long max = -1L;
        for (Kstat ksp : KstatUtil.kstatLookupAll("cpu_info", 0, null)) {
            if (KstatUtil.kstatRead(ksp)) {
                String suppFreq = KstatUtil.kstatDataLookupString(ksp, "supported_frequencies_Hz");
                if (!suppFreq.isEmpty()) {
                    for (String s : suppFreq.split(":")) {
                        long freq = ParseUtil.parseLongOrDefault(s, -1L);
                        if (max < freq) {
                            max = freq;
                        }
                    }
                }
            }
        }
        return max;
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
        int retval = Libc.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            for (int i = Math.max(retval, 0); i < average.length; i++) {
                average[i] = -1d;
            }
        }
        return average;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[][] getProcessorCpuLoadTicks() {
        long[][] ticks = new long[this.logicalProcessorCount][TickType.values().length];
        int cpu = -1;
        for (Kstat ksp : KstatUtil.kstatLookupAll("cpu", -1, "sys")) {
            // This is a new CPU
            if (++cpu >= ticks.length) {
                // Shouldn't happen
                break;
            }
            if (KstatUtil.kstatRead(ksp)) {
                ticks[cpu][TickType.IDLE.getIndex()] = KstatUtil.kstatDataLookupLong(ksp, "cpu_ticks_idle");
                ticks[cpu][TickType.SYSTEM.getIndex()] = KstatUtil.kstatDataLookupLong(ksp, "cpu_ticks_kernel");
                ticks[cpu][TickType.USER.getIndex()] = KstatUtil.kstatDataLookupLong(ksp, "cpu_ticks_user");
            }
        }
        return ticks;
    }

    /**
     * Fetches the ProcessorID by encoding the stepping, model, family, and
     * feature flags.
     *
     * @param stepping
     * @param model
     * @param family
     * @return The Processor ID string
     */
    private String getProcessorID(String stepping, String model, String family) {
        List<String> isainfo = ExecutingCommand.runNative("isainfo -v");
        StringBuilder flags = new StringBuilder();
        for (String line : isainfo) {
            if (line.startsWith("32-bit")) {
                break;
            } else if (!line.startsWith("64-bit")) {
                flags.append(' ').append(line.trim());
            }
        }
        return createProcessorID(stepping, model, family, ParseUtil.whitespaces.split(flags.toString().toLowerCase()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getContextSwitches() {
        long swtch = 0;
        List<String> kstat = ExecutingCommand.runNative("kstat -p cpu_stat:::/pswitch\\\\|inv_swtch/");
        for (String s : kstat) {
            swtch += ParseUtil.parseLastLong(s, 0L);
        }
        return swtch > 0 ? swtch : -1L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getInterrupts() {
        long intr = 0;
        List<String> kstat = ExecutingCommand.runNative("kstat -p cpu_stat:::/intr/");
        for (String s : kstat) {
            intr += ParseUtil.parseLastLong(s, 0L);
        }
        return intr > 0 ? intr : -1L;
    }
}
