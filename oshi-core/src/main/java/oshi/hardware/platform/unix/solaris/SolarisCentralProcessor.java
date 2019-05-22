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

import java.util.HashSet;
import java.util.List;
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
        // Initialize tick arrays
        initTicks();

        LOG.debug("Initialized Processor");
    }

    @Override
    protected CentralProcessorInitializer getInitializer() {
        CentralProcessorInitializer result = new CentralProcessorInitializer();
        // Get first result
        Kstat ksp = KstatUtil.kstatLookup("cpu_info", -1, null);
        // Set values
        if (ksp != null && KstatUtil.kstatRead(ksp)) {
            result.cpuVendor = KstatUtil.kstatDataLookupString(ksp, "vendor_id");
            result.cpuName = KstatUtil.kstatDataLookupString(ksp, "brand");
            result.cpuStepping = KstatUtil.kstatDataLookupString(ksp, "stepping");
            result.cpuModel = KstatUtil.kstatDataLookupString(ksp, "model");
            result.cpuFamily = KstatUtil.kstatDataLookupString(ksp, "family");
        }
        result.cpu64 = "64".equals(ExecutingCommand.getFirstAnswer("isainfo -b").trim());
        result.processorID = getProcessorID(result.cpuStepping, result.cpuModel, result.cpuFamily);

        // Calculate processor counts
        List<Kstat> kstats = KstatUtil.kstatLookupAll("cpu_info", -1, null);
        Set<String> chipIDs = new HashSet<>();
        Set<String> coreIDs = new HashSet<>();
        result.logicalProcessorCount = 0;
        for (Kstat tpmKsp : kstats) {
            if (tpmKsp != null && KstatUtil.kstatRead(tpmKsp)) {
                result.logicalProcessorCount++;
                chipIDs.add(KstatUtil.kstatDataLookupString(tpmKsp, "chip_id"));
                coreIDs.add(KstatUtil.kstatDataLookupString(tpmKsp, "core_id"));
            }
        }

        result.physicalPackageCount = chipIDs.size();
        if (result.physicalPackageCount < 1) {
            LOG.error("Couldn't find physical package count. Assuming 1.");
            result.physicalPackageCount = 1;
        }
        result.physicalProcessorCount = coreIDs.size();
        if (result.physicalProcessorCount < 1) {
            LOG.error("Couldn't find physical processor count. Assuming 1.");
            result.physicalProcessorCount = 1;
        }
        if (result.logicalProcessorCount < 1) {
            LOG.error("Couldn't find logical processor count. Assuming 1.");
            result.logicalProcessorCount = 1;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long[] getSystemCpuLoadTicks() {
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
     * {@inheritDoc}
     */
    @Override
    public long getSystemUptime() {
        Kstat ksp = KstatUtil.kstatLookup("unix", 0, "system_misc");
        if (ksp == null) {
            return 0L;
        }
        // Snap Time is in nanoseconds; divide for seconds
        return ksp.ks_snaptime / 1000000000L;
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
