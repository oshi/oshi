/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.linux.Libc;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * A CPU
 *
 * @author widdis[at]gmail[dot]com
 */
public class SolarisCentralProcessor extends AbstractCentralProcessor {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(SolarisCentralProcessor.class);

    private static final Pattern PSRINFO = Pattern.compile(".*physical processor has (\\d+) virtual processors.*");
    private static final Pattern CPU_TICKS = Pattern.compile("cpu:(\\d+):sys:cpu_ticks_(\\S*)\\s+(\\d+)");

    /**
     * Create a Processor
     */
    public SolarisCentralProcessor() {
        // Initialize class variables
        initVars();
        // Initialize tick arrays
        initTicks();

        LOG.debug("Initialized Processor");
    }

    private void initVars() {
        List<String> cpuInfo = null;
        // TODO: Replace kstat command line with native kstat()
        cpuInfo = ExecutingCommand.runNative("kstat -m cpu_info");
        for (String line : cpuInfo) {
            String[] splitLine = line.trim().split("\\s+");
            if (splitLine.length < 2) {
                break;
            }
            switch (splitLine[0]) {
            case "vendor_id":
                this.setVendor(line.replace("vendor_id", "").trim());
                break;
            case "brand":
                this.setName(line.replace("brand", "").trim());
                break;
            case "stepping":
                this.setStepping(splitLine[1]);
                break;
            case "model":
                this.setModel(splitLine[1]);
                break;
            case "family":
                this.setFamily(splitLine[1]);
                break;
            default:
                // Do nothing
            }
        }
        this.setCpu64(ExecutingCommand.getFirstAnswer("isainfo -b").trim().equals("64"));
    }

    /**
     * Updates logical and physical processor counts from psrinfo
     */
    protected void calculateProcessorCounts() {
        List<String> procInfo = ExecutingCommand.runNative("psrinfo -pv");
        this.logicalProcessorCount = 0;
        this.physicalProcessorCount = 0;
        // Get number of logical processors
        for (String cpu : procInfo) {
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
            for (int cpu = 0; cpu < procTicks.length; cpu++) {
                ticks[i] += procTicks[cpu][i];
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
        if (nelem < 1) {
            throw new IllegalArgumentException("Must include at least one element.");
        }
        if (nelem > 3) {
            LOG.warn("Max elements of SystemLoadAverage is 3. " + nelem + " specified. Ignoring extra.");
            nelem = 3;
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
        long[][] ticks = new long[logicalProcessorCount][TickType.values().length];
        // TODO: Replace kstat command line with native kstat()
        ArrayList<String> tickList = ExecutingCommand.runNative("kstat -p cpu::sys:/^cpu_ticks_/");
        // Sample format (Solaris 11)
        // cpu:0:sys:cpu_ticks_idle 8507532
        // cpu:0:sys:cpu_ticks_kernel 141883
        // cpu:0:sys:cpu_ticks_stolen 0
        // cpu:0:sys:cpu_ticks_user 142482
        // cpu:0:sys:cpu_ticks_wait 0
        String instance = "";
        int cpu = -1;
        for (String s : tickList) {
            Matcher m = CPU_TICKS.matcher(s);
            if (m.matches()) {
                if (!m.group(1).equals(instance)) {
                    // This is a new CPU
                    if (++cpu >= ticks.length) {
                        // Shouldn't happen
                        break;
                    }
                    instance = m.group(1);
                }
                if (m.group(2).equals("idle")) {
                    ticks[cpu][TickType.IDLE.getIndex()] = ParseUtil.parseLongOrDefault(m.group(3), 0L);
                } else if (m.group(2).equals("kernel")) {
                    ticks[cpu][TickType.SYSTEM.getIndex()] = ParseUtil.parseLongOrDefault(m.group(3), 0L);
                } else if (m.group(2).equals("user")) {
                    ticks[cpu][TickType.USER.getIndex()] = ParseUtil.parseLongOrDefault(m.group(3), 0L);
                }
            }
        }
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemUptime() {
        return Math.round(getSystemUptimeAsDouble());
    }

    /**
     * Gets system uptime in fractional seconds
     * 
     * @return a double representing system uptime in fractional seconds
     */
    private double getSystemUptimeAsDouble() {
        // Returns a floating point decimal
        // TODO: Replace kstat command line with native kstat()
        String uptimeSecs = ExecutingCommand.getFirstAnswer("kstat -p unix:0:system_misc:snaptime");
        if (uptimeSecs == null) {
            return 0d;
        }
        String[] split = uptimeSecs.split("\\s+");
        return ParseUtil.parseDoubleOrDefault(split[1], 0d);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSystemSerialNumber() {
        if (this.cpuSerialNumber == null) {
            ArrayList<String> hwInfo = ExecutingCommand.runNative("smbios -t SMB_TYPE_SYSTEM");
            String marker = "Serial Number:";
            if (hwInfo != null) {
                for (String checkLine : hwInfo) {
                    if (checkLine.contains(marker)) {
                        this.cpuSerialNumber = checkLine.split(marker)[1].trim();
                        break;
                    }
                }
            }
            // if that didn't work, try...
            if (this.cpuSerialNumber == null) {
                // If they've installed STB (Sun Explorer) this should work
                this.cpuSerialNumber = ExecutingCommand.getFirstAnswer("sneep");
            }
            // if that didn't work, try...
            if (this.cpuSerialNumber == null) {
                marker = "chassis-sn:";
                hwInfo = ExecutingCommand.runNative("prtconf -pv");
                if (hwInfo != null) {
                    for (String checkLine : hwInfo) {
                        if (checkLine.contains(marker)) {
                            String[] temp = checkLine.split(marker)[1].split("'");
                            // Format: '12345' (string)
                            this.cpuSerialNumber = temp.length > 0 ? temp[1] : null;
                            break;
                        }
                    }
                }
            }
            if (this.cpuSerialNumber == null) {
                this.cpuSerialNumber = "unknown";
            }
        }
        return this.cpuSerialNumber;
    }
}
