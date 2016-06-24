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
package oshi.hardware.platform.linux;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.linux.Libc;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcUtil;

/**
 * A CPU as defined in Linux /proc.
 *
 * @author alessandro[at]perucchi[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 * @author widdis[at]gmail[dot]com
 */
public class LinuxCentralProcessor extends AbstractCentralProcessor {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LinuxCentralProcessor.class);

    /**
     * Create a Processor
     */
    public LinuxCentralProcessor() {
        // Initialize class variables
        initVars();
        // Initialize tick arrays
        initTicks();

        LOG.debug("Initialized Processor");
    }

    private void initVars() {
        List<String> cpuInfo = null;
        cpuInfo = FileUtil.readFile("/proc/cpuinfo");
        for (String line : cpuInfo) {
            String[] splitLine = line.split("\\s+:\\s");
            if (splitLine.length < 2) {
                break;
            }
            switch (splitLine[0]) {
            case "vendor_id":
                this.setVendor(splitLine[1]);
                break;
            case "model name":
                this.setName(splitLine[1]);
                break;
            case "flags":
                String[] flags = splitLine[1].toUpperCase().split(" ");
                boolean found = false;
                for (String flag : flags) {
                    if (flag.equals("LM")) {
                        found = true;
                        break;
                    }
                }
                this.setCpu64(found);
                break;
            case "stepping":
                this.setStepping(splitLine[1]);
                break;
            case "model":
                this.setModel(splitLine[1]);
                break;
            case "cpu family":
                this.setFamily(splitLine[1]);
                break;
            default:
                // Do nothing
            }
        }
    }

    /**
     * Updates logical and physical processor counts from /proc/cpuinfo
     */
    protected void calculateProcessorCounts() {
        List<String> procCpu = FileUtil.readFile("/proc/cpuinfo");
        // Get number of logical processors
        for (String cpu : procCpu) {
            if (cpu.startsWith("processor")) {
                this.logicalProcessorCount++;
            }
        }
        // Get number of physical processors
        int siblings = 0;
        int cpucores = 0;
        int[] uniqueID = new int[2];
        uniqueID[0] = -1;
        uniqueID[1] = -1;

        Set<String> ids = new HashSet<String>();

        for (String cpu : procCpu) {
            if (cpu.startsWith("siblings")) {
                // if siblings = 1, no hyperthreading
                siblings = ParseUtil.parseLastInt(cpu, 1);
                if (siblings == 1) {
                    this.physicalProcessorCount = this.logicalProcessorCount;
                    break;
                }
            }
            if (cpu.startsWith("cpu cores")) {
                // if siblings > 1, ratio with cores
                cpucores = ParseUtil.parseLastInt(cpu, 1);
                if (siblings > 1) {
                    this.physicalProcessorCount = this.logicalProcessorCount * cpucores / siblings;
                    break;
                }
            }
            // If siblings and cpu cores don't define it, count unique
            // combinations of core id and physical id.
            if (cpu.startsWith("core id") || cpu.startsWith("cpu number")) {
                uniqueID[0] = ParseUtil.parseLastInt(cpu, 0);
            } else if (cpu.startsWith("physical id")) {
                uniqueID[1] = ParseUtil.parseLastInt(cpu, 0);
            }
            if (uniqueID[0] >= 0 && uniqueID[1] >= 0) {
                ids.add(uniqueID[0] + " " + uniqueID[1]);
                uniqueID[0] = -1;
                uniqueID[1] = -1;
            }
        }
        if (this.physicalProcessorCount == 0) {
            this.physicalProcessorCount = ids.size();
        }
        // Force at least one processor
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
        long[] ticks = new long[curTicks.length];
        // /proc/stat expected format
        // first line is overall user,nice,system,idle,iowait,irq, etc.
        // cpu 3357 0 4313 1362393 ...
        String tickStr = "";
        List<String> procStat = FileUtil.readFile("/proc/stat");
        if (!procStat.isEmpty()) {
            tickStr = procStat.get(0);
        } else {
            return ticks;
        }
        String[] tickArr = tickStr.split("\\s+");
        if (tickArr.length < 5) {
            return ticks;
        }
        // Note tickArr is offset by 1
        for (int i = 0; i < 4; i++) {
            ticks[i] = ParseUtil.parseLongOrDefault(tickArr[i + 1], 0L);
        }
        if (tickArr.length > 5) {
            // Add iowait to idle
            ticks[3] += ParseUtil.parseLongOrDefault(tickArr[5], 0L);
            // Add other fields to system
            for (int i = 6; i < tickArr.length; i++) {
                ticks[2] += ParseUtil.parseLongOrDefault(tickArr[i], 0L);
            }
        }
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemIOWaitTicks() {
        // /proc/stat expected format
        // first line is overall user,nice,system,idle,iowait,irq, etc.
        // cpu 3357 0 4313 1362393 ...
        String tickStr = "";
        List<String> procStat = FileUtil.readFile("/proc/stat");
        if (!procStat.isEmpty()) {
            tickStr = procStat.get(0);
        } else {
            return 0;
        }
        String[] tickArr = tickStr.split("\\s+");
        if (tickArr.length < 6) {
            return 0;
        }
        return ParseUtil.parseLongOrDefault(tickArr[5], 0L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getSystemIrqTicks() {
        // /proc/stat expected format
        // first line is overall user,nice,system,idle,iowait,irq, etc.
        // cpu 3357 0 4313 1362393 ...
        String tickStr = "";
        long[] ticks = new long[2];
        List<String> procStat = FileUtil.readFile("/proc/stat");
        if (!procStat.isEmpty()) {
            tickStr = procStat.get(0);
        } else {
            return ticks;
        }
        String[] tickArr = tickStr.split("\\s+");
        if (tickArr.length < 8) {
            return ticks;
        }
        ticks[0] = ParseUtil.parseLongOrDefault(tickArr[6], 0L);
        ticks[1] = ParseUtil.parseLongOrDefault(tickArr[7], 0L);
        return ticks;
    };

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
        long[][] ticks = new long[logicalProcessorCount][4];
        // /proc/stat expected format
        // first line is overall user,nice,system,idle, etc.
        // cpu 3357 0 4313 1362393 ... + for (String stat : procStat) {
        // per-processor subsequent lines for cpu0, cpu1, etc.
        int cpu = 0;
        List<String> procStat = FileUtil.readFile("/proc/stat");
        for (String stat : procStat) {
            if (stat.startsWith("cpu") && !stat.startsWith("cpu ")) {
                String[] tickArr = stat.split("\\s+");
                if (tickArr.length < 5) {
                    break;
                }
                // Note tickArr is offset by 1
                for (int i = 0; i < 4; i++) {
                    ticks[cpu][i] = ParseUtil.parseLongOrDefault(tickArr[i + 1], 0L);
                }
                if (tickArr.length > 5) {
                    // Add iowait to idle
                    ticks[cpu][3] += ParseUtil.parseLongOrDefault(tickArr[5], 0L);
                    // Add other fields to system
                    for (int i = 6; i < tickArr.length; i++) {
                        ticks[cpu][2] += ParseUtil.parseLongOrDefault(tickArr[i], 0L);
                    }
                }
                if (++cpu >= logicalProcessorCount) {
                    break;
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
        return (long) ProcUtil.getSystemUptimeFromProc();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSystemSerialNumber() {
        if (this.cpuSerialNumber == null) {
            // If root privileges this will work
            ArrayList<String> hwInfo = ExecutingCommand.runNative("dmidecode -t system");
            String marker = "Serial Number:";
            if (hwInfo != null) {
                for (String checkLine : hwInfo) {
                    if (checkLine.contains(marker)) {
                        this.cpuSerialNumber = checkLine.split(marker)[1].trim();
                        break;
                    }
                }
            }
            // if lshal command available (HAL deprecated in newer linuxes)
            if (this.cpuSerialNumber == null) {
                marker = "system.hardware.serial =";
                hwInfo = ExecutingCommand.runNative("lshal");
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
