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
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * enrico[dot]bianchi[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.linux;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;

import oshi.hardware.CentralProcessor;
import oshi.jna.platform.linux.Libc;
import oshi.jna.platform.linux.Libc.Sysinfo;
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * A CPU as defined in Linux /proc.
 *
 * @author alessandro[at]perucchi[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 * @author widdis[at]gmail[dot]com
 */
@SuppressWarnings("restriction")
public class LinuxCentralProcessor implements CentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxCentralProcessor.class);

    // Determine whether MXBean supports Oracle JVM methods
    private static final java.lang.management.OperatingSystemMXBean OS_MXBEAN = ManagementFactory
            .getOperatingSystemMXBean();

    private static boolean sunMXBean;

    static {
        try {
            Class.forName("com.sun.management.OperatingSystemMXBean");
            // Initialize CPU usage
            ((com.sun.management.OperatingSystemMXBean) OS_MXBEAN).getSystemCpuLoad();
            sunMXBean = true;
            LOG.debug("Oracle MXBean detected.");
        } catch (ClassNotFoundException e) {
            sunMXBean = false;
            LOG.debug("Oracle MXBean not detected.");
            LOG.trace("", e);
        }
    }

    // Logical and Physical Processor Counts
    private int logicalProcessorCount = 0;

    private int physicalProcessorCount = 0;

    // Maintain previous ticks to be used for calculating usage between them.
    // System ticks
    private long tickTime;

    private long[] prevTicks;

    private long[] curTicks;

    // Per-processor ticks [cpu][type]
    private long procTickTime;

    private long[][] prevProcTicks;

    private long[][] curProcTicks;

    // Processor info
    private String cpuVendor;

    private String cpuName;

    private String cpuSerialNumber = null;

    private String cpuIdentifier;

    private String cpuStepping;

    private String cpuModel;

    private String cpuFamily;

    private Long cpuVendorFreq;

    private Boolean cpu64;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    /**
     * Create a Processor
     */
    public LinuxCentralProcessor() {
        // Processor counts
        calculateProcessorCounts();

        // System ticks
        this.prevTicks = new long[4];
        this.curTicks = new long[4];
        updateSystemTicks();

        // Per-processor ticks
        this.prevProcTicks = new long[logicalProcessorCount][4];
        this.curProcTicks = new long[logicalProcessorCount][4];
        updateProcessorTicks();

        LOG.debug("Initialized Processor");
    }

    /**
     * Updates logical and physical processor counts from /proc/cpuinfo
     */
    private void calculateProcessorCounts() {
        try {
            List<String> procCpu = FileUtil.readFile("/proc/cpuinfo");
            // Get number of logical processors
            for (String cpu : procCpu) {
                if (cpu.startsWith("processor")) {
                    logicalProcessorCount++;
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
                    siblings = ParseUtil.parseString(cpu, 1);
                    if (siblings == 1) {
                        physicalProcessorCount = logicalProcessorCount;
                        break;
                    }
                }
                if (cpu.startsWith("cpu cores")) {
                    // if siblings > 1, ratio with cores
                    cpucores = ParseUtil.parseString(cpu, 1);
                    if (siblings > 1) {
                        physicalProcessorCount = logicalProcessorCount * cpucores / siblings;
                        break;
                    }
                }
                // If siblings and cpu cores don't define it, count unique
                // combinations of core id and physical id.
                if (cpu.startsWith("core id") || cpu.startsWith("cpu number")) {
                    uniqueID[0] = ParseUtil.parseString(cpu, 0);
                } else if (cpu.startsWith("physical id")) {
                    uniqueID[1] = ParseUtil.parseString(cpu, 0);
                }
                if (uniqueID[0] >= 0 && uniqueID[1] >= 0) {
                    ids.add(uniqueID[0] + " " + uniqueID[1]);
                    uniqueID[0] = -1;
                    uniqueID[1] = -1;
                }
            }
            if (physicalProcessorCount == 0) {
                physicalProcessorCount = ids.size();
            }
        } catch (IOException e) {
            LOG.error("Problem with /proc/cpuinfo: {}", e.getMessage());
        }
        // Force at least one processor
        if (logicalProcessorCount < 1) {
            LOG.error("Couldn't find logical processor count. Assuming 1.");
            logicalProcessorCount = 1;
        }
        if (physicalProcessorCount < 1) {
            LOG.error("Couldn't find physical processor count. Assuming 1.");
            physicalProcessorCount = 1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVendor() {
        return this.cpuVendor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVendor(String vendor) {
        this.cpuVendor = vendor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.cpuName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setName(String name) {
        this.cpuName = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getVendorFreq() {
        if (this.cpuVendorFreq == null) {
            Pattern pattern = Pattern.compile("@ (.*)$");
            Matcher matcher = pattern.matcher(getName());

            if (matcher.find()) {
                String unit = matcher.group(1);
                this.cpuVendorFreq = Long.valueOf(ParseUtil.parseHertz(unit));
            } else {
                this.cpuVendorFreq = Long.valueOf(-1L);
            }
        }

        return this.cpuVendorFreq.longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVendorFreq(long freq) {
        this.cpuVendorFreq = Long.valueOf(freq);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentifier() {
        if (this.cpuIdentifier == null) {
            StringBuilder sb = new StringBuilder();
            if (getVendor() != null && getVendor().contentEquals("GenuineIntel")) {
                sb.append(isCpu64bit() ? "Intel64" : "x86");
            } else {
                sb.append(getVendor());
            }
            sb.append(" Family ");
            sb.append(getFamily());
            sb.append(" Model ");
            sb.append(getModel());
            sb.append(" Stepping ");
            sb.append(getStepping());
            this.cpuIdentifier = sb.toString();
        }
        return this.cpuIdentifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIdentifier(String identifier) {
        this.cpuIdentifier = identifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCpu64bit() {
        return this.cpu64.booleanValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCpu64(boolean value) {
        this.cpu64 = Boolean.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStepping() {
        return this.cpuStepping;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStepping(String stepping) {
        this.cpuStepping = stepping;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModel() {
        return this.cpuModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setModel(String model) {
        this.cpuModel = model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFamily() {
        return this.cpuFamily;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFamily(String family) {
        this.cpuFamily = family;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized double getSystemCpuLoadBetweenTicks() {
        // Check if > ~ 0.95 seconds since last tick count.
        long now = System.currentTimeMillis();
        LOG.trace("Current time: {}  Last tick time: {}", now, tickTime);
        if (now - tickTime > 950) {
            // Enough time has elapsed.
            updateSystemTicks();
        }
        // Calculate total
        long total = 0;
        for (int i = 0; i < curTicks.length; i++) {
            total += (curTicks[i] - prevTicks[i]);
        }
        // Calculate idle from last field [3]
        long idle = curTicks[3] - prevTicks[3];
        LOG.trace("Total ticks: {}  Idle ticks: {}", total, idle);

        return (total > 0 && idle >= 0) ? (double) (total - idle) / total : 0d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getSystemCpuLoadTicks() {
        long[] ticks = new long[curTicks.length];
        // /proc/stat expected format
        // first line is overall user,nice,system,idle,iowait,irq, etc.
        // cpu 3357 0 4313 1362393 ...
        String tickStr = "";
        try {
            List<String> procStat = FileUtil.readFile("/proc/stat");
            if (!procStat.isEmpty()) {
                tickStr = procStat.get(0);
            }
        } catch (IOException e) {
            LOG.error("Problem with /proc/stat: {}", e.getMessage());
            return ticks;
        }
        String[] tickArr = tickStr.split("\\s+");
        if (tickArr.length < 5) {
            return ticks;
        }
        // Note tickArr is offset by 1
        for (int i = 0; i < 4; i++) {
            ticks[i] = Long.parseLong(tickArr[i + 1]);
        }
        if (tickArr.length > 5) {
            // Add iowait to idle
            ticks[3] += Long.parseLong(tickArr[5]);
            // Add other fields to system
            for (int i = 6; i < tickArr.length; i++) {
                ticks[2] += Long.parseLong(tickArr[i]);
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
        try {
            List<String> procStat = FileUtil.readFile("/proc/stat");
            if (!procStat.isEmpty())
                tickStr = procStat.get(0);
        } catch (IOException e) {
            LOG.error("Problem with /proc/stat: {}", e.getMessage());
            return 0;
        }
        String[] tickArr = tickStr.split("\\s+");
        if (tickArr.length < 6) {
            return 0;
        }
        return Long.parseLong(tickArr[5]);
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
        try {
            List<String> procStat = FileUtil.readFile("/proc/stat");
            if (!procStat.isEmpty())
                tickStr = procStat.get(0);
        } catch (IOException e) {
            LOG.error("Problem with /proc/stat: {}", e.getMessage());
            return ticks;
        }
        String[] tickArr = tickStr.split("\\s+");
        if (tickArr.length < 8) {
            return ticks;
        }
        ticks[0] = Long.parseLong(tickArr[6]);
        ticks[1] = Long.parseLong(tickArr[7]);
        return ticks;
    };

    /**
     * Updates system tick information from parsing /proc/stat. Stores in array
     * with four elements representing clock ticks or milliseconds (platform
     * dependent) spent in User (0), Nice (1), System (2), and Idle (3) states.
     * By measuring the difference between ticks across a time interval, CPU
     * load over that interval may be calculated.
     */
    private void updateSystemTicks() {
        LOG.trace("Updating System Ticks");
        // Copy to previous
        System.arraycopy(curTicks, 0, prevTicks, 0, curTicks.length);
        this.tickTime = System.currentTimeMillis();
        long[] ticks = getSystemCpuLoadTicks();
        System.arraycopy(ticks, 0, curTicks, 0, ticks.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getSystemCpuLoad() {
        if (sunMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) OS_MXBEAN).getSystemCpuLoad();
        }
        return getSystemCpuLoadBetweenTicks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getSystemLoadAverage() {
        return getSystemLoadAverage(1)[0];
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
    public double[] getProcessorCpuLoadBetweenTicks() {
        // Check if > ~ 0.95 seconds since last tick count.
        long now = System.currentTimeMillis();
        LOG.trace("Current time: {}  Last tick time: {}", now, procTickTime);
        if (now - procTickTime > 950) {
            // Enough time has elapsed.
            // Update latest
            updateProcessorTicks();
        }
        double[] load = new double[logicalProcessorCount];
        for (int cpu = 0; cpu < logicalProcessorCount; cpu++) {
            long total = 0;
            for (int i = 0; i < this.curProcTicks[cpu].length; i++) {
                total += (this.curProcTicks[cpu][i] - this.prevProcTicks[cpu][i]);
            }
            // Calculate idle from last field [3]
            long idle = this.curProcTicks[cpu][3] - this.prevProcTicks[cpu][3];
            LOG.trace("CPU: {}  Total ticks: {}  Idle ticks: {}", cpu, total, idle);
            // update
            load[cpu] = (total > 0 && idle >= 0) ? (double) (total - idle) / total : 0d;
        }
        return load;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[][] getProcessorCpuLoadTicks() {
        long[][] ticks = new long[logicalProcessorCount][4];
        // /proc/stat expected format
        // first line is overall user,nice,system,idle, etc.
        // cpu 3357 0 4313 1362393 ...
        // per-processor subsequent lines for cpu0, cpu1, etc.
        try {
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
                        ticks[cpu][i] = Long.parseLong(tickArr[i + 1]);
                    }
                    if (tickArr.length > 5) {
                        // Add iowait to idle
                        ticks[cpu][3] += Long.parseLong(tickArr[5]);
                        // Add other fields to system
                        for (int i = 6; i < tickArr.length; i++) {
                            ticks[cpu][2] += Long.parseLong(tickArr[i]);
                        }
                    }
                    if (++cpu >= logicalProcessorCount) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Problem with /proc/stat: {}", e.getMessage());
        }
        return ticks;
    }

    /**
     * Updates per-processor tick information from parsing /proc/stat. Stores in
     * 2D array; an array for each logical processor with four elements
     * representing clock ticks or milliseconds (platform dependent) spent in
     * User (0), Nice (1), System (2), and Idle (3) states. By measuring the
     * difference between ticks across a time interval, CPU load over that
     * interval may be calculated.
     */
    private void updateProcessorTicks() {
        LOG.trace("Updating Processor Ticks");
        // Copy to previous
        for (int cpu = 0; cpu < logicalProcessorCount; cpu++) {
            System.arraycopy(curProcTicks[cpu], 0, prevProcTicks[cpu], 0, curProcTicks[cpu].length);
        }
        this.procTickTime = System.currentTimeMillis();
        long[][] ticks = getProcessorCpuLoadTicks();
        for (int cpu = 0; cpu < logicalProcessorCount; cpu++) {
            System.arraycopy(ticks[cpu], 0, curProcTicks[cpu], 0, ticks[cpu].length);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemUptime() {
        try {
            Sysinfo info = new Sysinfo();
            if (0 != Libc.INSTANCE.sysinfo(info)) {
                LOG.error("Failed to get system uptime. Error code: " + Native.getLastError());
                return 0L;
            }
            return info.uptime.longValue();
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            LOG.error("Failed to get uptime from sysinfo. {}", e.getMessage());
        }
        return 0L;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLogicalProcessorCount() {
        return this.logicalProcessorCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPhysicalProcessorCount() {
        return this.physicalProcessorCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessCount() {
        // Get all filenames in /proc directory with only digits (pids)
        File procdir = new File("/proc");
        final Pattern p = Pattern.compile("\\d+");
        File[] pids = procdir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return p.matcher(file.getName()).matches();
            }
        });
        return pids.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThreadCount() {
        try {
            Sysinfo info = new Sysinfo();
            if (0 != Libc.INSTANCE.sysinfo(info)) {
                LOG.error("Failed to get process thread count. Error code: " + Native.getLastError());
                return 0;
            }
            return info.procs;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            LOG.error("Failed to get procs from sysinfo. {}", e.getMessage());
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON() {
        JsonArrayBuilder systemLoadAverageArrayBuilder = jsonFactory.createArrayBuilder();
        for (double avg : getSystemLoadAverage(3)) {
            systemLoadAverageArrayBuilder.add(avg);
        }
        JsonArrayBuilder systemCpuLoadTicksArrayBuilder = jsonFactory.createArrayBuilder();
        for (long ticks : getSystemCpuLoadTicks()) {
            systemCpuLoadTicksArrayBuilder.add(ticks);
        }
        JsonArrayBuilder processorCpuLoadBetweenTicksArrayBuilder = jsonFactory.createArrayBuilder();
        for (double load : getProcessorCpuLoadBetweenTicks()) {
            processorCpuLoadBetweenTicksArrayBuilder.add(load);
        }
        JsonArrayBuilder processorCpuLoadTicksArrayBuilder = jsonFactory.createArrayBuilder();
        for (long[] procTicks : getProcessorCpuLoadTicks()) {
            JsonArrayBuilder processorTicksArrayBuilder = jsonFactory.createArrayBuilder();
            for (long ticks : procTicks) {
                processorTicksArrayBuilder.add(ticks);
            }
            processorCpuLoadTicksArrayBuilder.add(processorTicksArrayBuilder.build());
        }
        JsonArrayBuilder systemIrqTicksArrayBuilder = jsonFactory.createArrayBuilder();
        for (long ticks : getSystemIrqTicks()) {
            systemIrqTicksArrayBuilder.add(ticks);
        }
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("name", getName())
                .add("physicalProcessorCount", getPhysicalProcessorCount())
                .add("logicalProcessorCount", getLogicalProcessorCount())
                .add("systemSerialNumber", getSystemSerialNumber()).add("vendor", getVendor())
                .add("vendorFreq", getVendorFreq()).add("cpu64bit", isCpu64bit()).add("family", getFamily())
                .add("model", getModel()).add("stepping", getStepping())
                .add("systemCpuLoadBetweenTicks", getSystemCpuLoadBetweenTicks())
                .add("systemCpuLoadTicks", systemCpuLoadTicksArrayBuilder.build())
                .add("systemCpuLoad", getSystemCpuLoad()).add("systemLoadAverage", getSystemLoadAverage())
                .add("systemLoadAverages", systemLoadAverageArrayBuilder.build())
                .add("systemIOWaitTicks", getSystemIOWaitTicks())
                .add("systemIrqTicks", systemIrqTicksArrayBuilder.build())
                .add("processorCpuLoadBetweenTicks", processorCpuLoadBetweenTicksArrayBuilder.build())
                .add("processorCpuLoadTicks", processorCpuLoadTicksArrayBuilder.build())
                .add("systemUptime", getSystemUptime()).add("processes", getProcessCount())
                .add("threads", getThreadCount()).build();
    }

    @Override
    public String toString() {
        return getName();
    }
}
