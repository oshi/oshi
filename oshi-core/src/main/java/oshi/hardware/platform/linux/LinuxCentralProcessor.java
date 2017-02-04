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
package oshi.hardware.platform.linux;

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
        super();
        // Initialize class variables
        initVars();
        // Initialize tick arrays
        initTicks();

        LOG.debug("Initialized Processor");
    }

    private void initVars() {
        String[] flags = null;
        List<String> cpuInfo = FileUtil.readFile("/proc/cpuinfo");
        for (String line : cpuInfo) {
            String[] splitLine = line.split("\\s+:\\s");
            if (splitLine.length < 2) {
                break;
            }
            switch (splitLine[0]) {
            case "vendor_id":
                setVendor(splitLine[1]);
                break;
            case "model name":
                setName(splitLine[1]);
                break;
            case "flags":
                flags = splitLine[1].toLowerCase().split(" ");
                boolean found = false;
                for (String flag : flags) {
                    if ("lm".equals(flag)) {
                        found = true;
                        break;
                    }
                }
                setCpu64(found);
                break;
            case "stepping":
                setStepping(splitLine[1]);
                break;
            case "model":
                setModel(splitLine[1]);
                break;
            case "cpu family":
                setFamily(splitLine[1]);
                break;
            default:
                // Do nothing
            }
        }
        setProcessorID(getProcessorID(getStepping(), getModel(), getFamily(), flags));
    }

    /**
     * Updates logical and physical processor counts from /proc/cpuinfo
     */
    @Override
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
        int cpucores;
        int[] uniqueID = new int[2];
        uniqueID[0] = -1;
        uniqueID[1] = -1;

        Set<String> ids = new HashSet<>();

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
        long[] ticks = new long[TickType.values().length];
        // /proc/stat expected format
        // first line is overall user,nice,system,idle,iowait,irq, etc.
        // cpu 3357 0 4313 1362393 ...
        String tickStr;
        List<String> procStat = FileUtil.readFile("/proc/stat");
        if (!procStat.isEmpty()) {
            tickStr = procStat.get(0);
        } else {
            return ticks;
        }
        // Split the line. Note the first (0) element is "cpu" so remaining
        // elements are offset by 1 from the enum index
        String[] tickArr = tickStr.split("\\s+");
        if (tickArr.length <= TickType.IDLE.getIndex()) {
            // If ticks don't at least go user/nice/system/idle, abort
            return ticks;
        }
        // Note tickArr is offset by 1
        for (int i = 0; i < TickType.values().length; i++) {
            ticks[i] = ParseUtil.parseLongOrDefault(tickArr[i + 1], 0L);
        }
        // If next value is steal, add it
        if (tickArr.length > TickType.values().length + 1) {
            // Add steal to system
            ticks[TickType.SYSTEM.getIndex()] += ParseUtil.parseLongOrDefault(tickArr[TickType.values().length + 1],
                    0L);
            // Ignore guest or guest_nice, they are included in user/nice
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
        // /proc/stat expected format
        // first line is overall user,nice,system,idle, etc.
        // cpu 3357 0 4313 1362393 ...
        // per-processor subsequent lines for cpu0, cpu1, etc.
        int cpu = 0;
        List<String> procStat = FileUtil.readFile("/proc/stat");
        for (String stat : procStat) {
            if (stat.startsWith("cpu") && !stat.startsWith("cpu ")) {
                // Split the line. Note the first (0) element is "cpu" so
                // remaining
                // elements are offset by 1 from the enum index
                String[] tickArr = stat.split("\\s+");
                if (tickArr.length <= TickType.IDLE.getIndex()) {
                    // If ticks don't at least go user/nice/system/idle, abort
                    return ticks;
                }
                // Note tickArr is offset by 1
                for (int i = 0; i < TickType.values().length; i++) {
                    ticks[cpu][i] = ParseUtil.parseLongOrDefault(tickArr[i + 1], 0L);
                }
                // If next value is steal, add it
                if (tickArr.length > TickType.values().length + 1) {
                    // Add steal to system
                    ticks[cpu][TickType.SYSTEM.getIndex()] += ParseUtil
                            .parseLongOrDefault(tickArr[TickType.values().length + 1], 0L);
                    // Ignore guest or guest_nice, they are included in
                    // user/nice
                }
                if (++cpu >= this.logicalProcessorCount) {
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
    @Deprecated
    public String getSystemSerialNumber() {
        return new LinuxComputerSystem().getSerialNumber();
    }

    /**
     * Fetches the ProcessorID from dmidecode (if possible with root
     * permissions), the cpuid command (if installed) or by encoding the
     * stepping, model, family, and feature flags.
     * 
     * @param stepping
     * @param model
     * @param family
     * @param flags
     * @return The Processor ID string
     */
    private String getProcessorID(String stepping, String model, String family, String[] flags) {
        boolean procInfo = false;
        String marker = "Processor Information";
        for (String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
            if (!procInfo && checkLine.contains(marker)) {
                marker = "ID:";
                procInfo = true;
            } else if (procInfo && checkLine.contains(marker)) {
                return checkLine.split(marker)[1].trim();
            }
        }
        // If we've gotten this far, dmidecode failed. Try cpuid.
        marker = "eax=";
        for (String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
            if (checkLine.contains(marker) && checkLine.trim().startsWith("0x00000001")) {
                String eax = "";
                String edx = "";
                for (String register : checkLine.split("\\s+")) {
                    if (register.startsWith("eax=")) {
                        eax = register.replace("eax=0x", "");
                    } else if (register.startsWith("edx=")) {
                        edx = register.replace("edx=0x", "");
                    }
                }
                return edx + eax;
            }
        }
        // If we've gotten this far, dmidecode failed. Encode arguments
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
        for (String flag : flags) {
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
