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
package oshi.hardware.common;

import java.lang.management.ManagementFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.CentralProcessor;
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.util.ParseUtil;

/**
 * A CPU as defined in Linux /proc.
 *
 * @author alessandro[at]perucchi[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 * @author widdis[at]gmail[dot]com
 */
@SuppressWarnings("restriction")
public abstract class AbstractCentralProcessor implements CentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCentralProcessor.class);

    // MXBean getSystemCpuLoad() returns NaN if not enough time has elapsed
    private static double lastCpuLoad = 0d;
    private static long lastCpuLoadTime = 0;

    // Determine whether MXBean supports Oracle JVM methods
    private static final java.lang.management.OperatingSystemMXBean OS_MXBEAN = ManagementFactory
            .getOperatingSystemMXBean();

    private static boolean sunMXBean;

    static {
        try {
            Class.forName("com.sun.management.OperatingSystemMXBean");
            // Initialize CPU usage
            lastCpuLoad = ((com.sun.management.OperatingSystemMXBean) OS_MXBEAN).getSystemCpuLoad();
            lastCpuLoadTime = System.currentTimeMillis();
            sunMXBean = true;
            LOG.debug("Oracle MXBean detected.");
        } catch (ClassNotFoundException e) {
            sunMXBean = false;
            LOG.debug("Oracle MXBean not detected.");
            LOG.trace("", e);
        }
    }

    // Logical and Physical Processor Counts
    protected int logicalProcessorCount = 0;

    protected int physicalProcessorCount = 0;

    // Maintain previous ticks to be used for calculating usage between them.
    // System ticks
    protected long tickTime;

    protected long[] prevTicks;

    protected long[] curTicks;

    // Per-processor ticks [cpu][type]
    protected long procTickTime;

    protected long[][] prevProcTicks;

    protected long[][] curProcTicks;

    // Processor info
    protected String cpuVendor;

    protected String cpuName;

    protected String cpuSerialNumber = null;

    protected String cpuIdentifier;

    protected String cpuStepping;

    protected String cpuModel;

    protected String cpuFamily;

    protected Long cpuVendorFreq;

    protected Boolean cpu64;

    protected JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    /**
     * Create a Processor
     */
    public AbstractCentralProcessor() {
        // Initialize processor counts
        calculateProcessorCounts();
    }

    /**
     * Initializes tick arrays
     */
    protected void initTicks() {
        // System ticks
        this.prevTicks = new long[4];
        this.curTicks = new long[4];
        updateSystemTicks();

        // Per-processor ticks
        this.prevProcTicks = new long[logicalProcessorCount][4];
        this.curProcTicks = new long[logicalProcessorCount][4];
        updateProcessorTicks();
    }

    /**
     * Updates logical and physical processor counts
     */
    protected abstract void calculateProcessorCounts();

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVendor() {
        if (this.cpuVendor == null) {
            setVendor("");
        }
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
        if (this.cpuName == null) {
            setName("");
        }
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
            if (getVendor().contentEquals("GenuineIntel")) {
                sb.append(isCpu64bit() ? "Intel64" : "x86");
            } else {
                sb.append(getVendor());
            }
            sb.append(" Family ").append(getFamily());
            sb.append(" Model ").append(getModel());
            sb.append(" Stepping ").append(getStepping());
            setIdentifier(sb.toString());
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
        if (this.cpu64 == null) {
            setCpu64(false);
        }
        return this.cpu64;
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
        if (this.cpuStepping == null) {
            setStepping(parseIdentifier("Stepping"));
        }
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
        if (this.cpuModel == null) {
            setModel(parseIdentifier("Model"));
        }
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
        if (this.cpuFamily == null) {
            setFamily(parseIdentifier("Family"));
        }
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
     * Parses identifier string
     * 
     * @param id
     *            the id to retrieve
     * @return the string following id
     */
    private String parseIdentifier(String id) {
        String[] idSplit = getIdentifier().split("\\s+");
        boolean found = false;
        for (String s : idSplit) {
            // If id string found, return next value
            if (found) {
                return s;
            }
            found = s.equals(id);
        }
        // If id string not found, return empty string
        return "";
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
    public abstract long[] getSystemCpuLoadTicks();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract long getSystemIOWaitTicks();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract long[] getSystemIrqTicks();

    /**
     * Updates system tick information. Stores in array with four elements
     * representing clock ticks or milliseconds (platform dependent) spent in
     * User (0), Nice (1), System (2), and Idle (3) states. By measuring the
     * difference between ticks across a time interval, CPU load over that
     * interval may be calculated.
     */
    protected void updateSystemTicks() {
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
            long now = System.currentTimeMillis();
            // If called too recently, return latest value
            if (now - lastCpuLoadTime < 200) {
                return lastCpuLoad;
            }
            lastCpuLoad = ((com.sun.management.OperatingSystemMXBean) OS_MXBEAN).getSystemCpuLoad();
            lastCpuLoadTime = now;
            return lastCpuLoad;
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
    public abstract double[] getSystemLoadAverage(int nelem);

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
    public abstract long[][] getProcessorCpuLoadTicks();

    /**
     * Updates per-processor tick information. Stores in 2D array; an array for
     * each logical processor with four elements representing clock ticks or
     * milliseconds (platform dependent) spent in User (0), Nice (1), System
     * (2), and Idle (3) states. By measuring the difference between ticks
     * across a time interval, CPU load over that interval may be calculated.
     */
    protected void updateProcessorTicks() {
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
    public abstract long getSystemUptime();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String getSystemSerialNumber();

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
    public abstract int getProcessCount();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract int getThreadCount();

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
