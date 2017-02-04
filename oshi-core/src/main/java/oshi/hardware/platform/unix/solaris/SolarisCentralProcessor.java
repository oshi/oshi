/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
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
package oshi.hardware.platform.unix.solaris;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.linux.Libc;
import oshi.jna.platform.unix.solaris.LibKstat.Kstat;
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

    private static final Pattern PSRINFO = Pattern.compile(".*physical processor has (\\d+) virtual processors.*");

    /**
     * Create a Processor
     */
    public SolarisCentralProcessor() {
        super();
        // Initialize class variables
        initVars();
        // Initialize tick arrays
        initTicks();

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
    protected void calculateProcessorCounts() {
        this.logicalProcessorCount = 0;
        this.physicalProcessorCount = 0;
        // Get number of logical processors
        for (String cpu : ExecutingCommand.runNative("psrinfo -pv")) {
            Matcher m = PSRINFO.matcher(cpu.trim());
            if (m.matches()) {
                this.physicalProcessorCount++;
                this.logicalProcessorCount += ParseUtil.parseIntOrDefault(m.group(1), 0);
            }
        }
        if (this.logicalProcessorCount < 1) {
            LOG.error("Couldn't find logical processor count. Assuming 1.");
            this.logicalProcessorCount = 1;
        }
        if (this.physicalProcessorCount < 1) {
            LOG.error("Couldn't find physical processor count. Assuming 1.");
            this.physicalProcessorCount = 1;
        }
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
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public String getSystemSerialNumber() {
        return new SolarisComputerSystem().getSerialNumber();
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
        long processorID = 0L;
        long steppingL = ParseUtil.parseLongOrDefault(stepping, 0L);
        long modelL = ParseUtil.parseLongOrDefault(model, 0L);
        long familyL = ParseUtil.parseLongOrDefault(family, 0L);
        // 3:0 – Stepping
        processorID |= (steppingL & 0xf);
        // 19:16,7:4 – Model
        processorID |= ((modelL & 0x0f) << 4);
        processorID |= ((modelL & 0xf0) << 16);
        // 27:20,11:8 – Family
        processorID |= ((familyL & 0x0f) << 8);
        processorID |= ((familyL & 0xf0) << 20);
        // 13:12 – Processor Type, assume 0
        List<String> isainfo = ExecutingCommand.runNative("isainfo -v");
        String flags = "";
        for (String line : isainfo) {
            if (line.startsWith("64-bit")) {
                continue;
            } else if (line.startsWith("32-bit")) {
                break;
            }
            flags = flags + line;
        }
        for (String flag : flags.toLowerCase().split("\\s+")) {
            switch (flag) {
            case "fpu":
                processorID |= (1L << 32);
                break;
            case "vme":
                processorID |= (1L << 33);
                break;
            case "de":
                processorID |= (1L << 34);
                break;
            case "pse":
                processorID |= (1L << 35);
                break;
            case "tsc":
                processorID |= (1L << 36);
                break;
            case "msr":
                processorID |= (1L << 37);
                break;
            case "pae":
                processorID |= (1L << 38);
                break;
            case "mce":
                processorID |= (1L << 39);
                break;
            case "cx8":
                processorID |= (1L << 40);
                break;
            case "apic":
                processorID |= (1L << 41);
                break;
            case "sep":
                processorID |= (1L << 43);
                break;
            case "mtrr":
                processorID |= (1L << 44);
                break;
            case "pge":
                processorID |= (1L << 45);
                break;
            case "mca":
                processorID |= (1L << 46);
                break;
            case "cmov":
                processorID |= (1L << 47);
                break;
            case "pat":
                processorID |= (1L << 48);
                break;
            case "pse36":
            case "pse-36":
                processorID |= (1L << 49);
                break;
            case "psn":
                processorID |= (1L << 50);
                break;
            case "clfsh":
                processorID |= (1L << 51);
                break;
            case "ds":
                processorID |= (1L << 53);
                break;
            case "acpi":
                processorID |= (1L << 54);
                break;
            case "mmx":
                processorID |= (1L << 55);
                break;
            case "fxsr":
                processorID |= (1L << 56);
                break;
            case "sse":
                processorID |= (1L << 57);
                break;
            case "sse2":
                processorID |= (1L << 58);
                break;
            case "ss":
                processorID |= (1L << 59);
                break;
            case "ht":
            case "htt":
                processorID |= (1L << 60);
                break;
            case "tm":
                processorID |= (1L << 61);
                break;
            case "ia64":
                processorID |= (1L << 62);
                break;
            case "pbe":
                processorID |= (1L << 63);
                break;
            default:
                break;

            }
        }
        return String.format("%016X", processorID);
    }

}
