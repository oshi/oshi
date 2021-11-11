/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors:
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
package oshi.hardware.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.CentralProcessor;
import oshi.util.ParseUtil;

/**
 * A CPU as defined in Linux /proc.
 *
 * @author alessandro[at]perucchi[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 * @author widdis[at]gmail[dot]com
 */
public abstract class AbstractCentralProcessor implements CentralProcessor {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCentralProcessor.class);

    // Logical and Physical Processor Counts
    protected int logicalProcessorCount = 0;

    protected int physicalProcessorCount = 0;

    protected int physicalPackageCount = 0;

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

    private String processorID;

    private String cpuIdentifier;

    private String cpuStepping;

    private String cpuModel;

    private String cpuFamily;

    private Long cpuVendorFreq;

    private Boolean cpu64;

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
    protected synchronized void initTicks() {
        this.prevProcTicks = new long[this.logicalProcessorCount][TickType.values().length];
        this.curProcTicks = new long[this.logicalProcessorCount][TickType.values().length];
        this.prevTicks = new long[TickType.values().length];
        this.curTicks = new long[TickType.values().length];
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
    public String getProcessorID() {
        if (this.processorID == null) {
            setProcessorID("");
        }
        return this.processorID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProcessorID(String processorID) {
        this.processorID = processorID;
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
            if (this.cpuIdentifier == null) {
                return "?";
            }
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
            if (this.cpuIdentifier == null) {
                return "?";
            }
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
            if (this.cpuIdentifier == null) {
                return "?";
            }
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
        String[] idSplit = ParseUtil.whitespaces.split(getIdentifier());
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
        LOG.trace("Current time: {}  Last tick time: {}", now, this.tickTime);
        if (now - this.tickTime > 950) {
            // Enough time has elapsed.
            updateSystemTicks();
        }
        // Calculate total
        long total = 0;
        for (int i = 0; i < this.curTicks.length; i++) {
            total += this.curTicks[i] - this.prevTicks[i];
        }
        // Calculate idle from difference in idle and IOwait
        long idle = this.curTicks[TickType.IDLE.getIndex()] + this.curTicks[TickType.IOWAIT.getIndex()]
                - this.prevTicks[TickType.IDLE.getIndex()] - this.prevTicks[TickType.IOWAIT.getIndex()];
        LOG.trace("Total ticks: {}  Idle ticks: {}", total, idle);

        return total > 0 && idle >= 0 ? (double) (total - idle) / total : 0d;
    }

    /**
     * Updates system tick information. Stores in array with seven elements
     * representing clock ticks or milliseconds (platform dependent) spent in User
     * (0), Nice (1), System (2), Idle (3), IOwait (4), IRQ (5), and SoftIRQ (6)
     * states. By measuring the difference between ticks across a time interval, CPU
     * load over that interval may be calculated.
     */
    protected void updateSystemTicks() {
        LOG.trace("Updating System Ticks");
        long[] ticks = getSystemCpuLoadTicks();
        // Skip update if ticks is all zero.
        // Iterate to find a nonzero tick value and return; this should quickly
        // find a nonzero value if one exists and be fast in checking 0's
        // through branch prediction if it doesn't
        for (long tick : ticks) {
            if (tick != 0) {
                // We have a nonzero tick array, update and return!
                this.tickTime = System.currentTimeMillis();
                // Copy to previous
                System.arraycopy(this.curTicks, 0, this.prevTicks, 0, this.curTicks.length);
                System.arraycopy(ticks, 0, this.curTicks, 0, ticks.length);
                return;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getSystemCpuLoad() {
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
    public double[] getProcessorCpuLoadBetweenTicks() {
        // Check if > ~ 0.95 seconds since last tick count.
        long now = System.currentTimeMillis();
        LOG.trace("Current time: {}  Last tick time: {}", now, this.procTickTime);
        if (now - this.procTickTime > 950) {
            // Enough time has elapsed.
            // Update latest
            updateProcessorTicks();
        }
        double[] load = new double[this.logicalProcessorCount];
        for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
            long total = 0;
            for (int i = 0; i < this.curProcTicks[cpu].length; i++) {
                total += this.curProcTicks[cpu][i] - this.prevProcTicks[cpu][i];
            }
            // Calculate idle from difference in idle and IOwait
            long idle = this.curProcTicks[cpu][TickType.IDLE.getIndex()]
                    + this.curProcTicks[cpu][TickType.IOWAIT.getIndex()]
                    - this.prevProcTicks[cpu][TickType.IDLE.getIndex()]
                    - this.prevProcTicks[cpu][TickType.IOWAIT.getIndex()];
            LOG.trace("CPU: {}  Total ticks: {}  Idle ticks: {}", cpu, total, idle);
            // update
            load[cpu] = total > 0 && idle >= 0 ? (double) (total - idle) / total : 0d;
        }
        return load;
    }

    /**
     * Updates per-processor tick information. Stores in 2D array; an array for each
     * logical processor with with seven elements representing clock ticks or
     * milliseconds (platform dependent) spent in User (0), Nice (1), System (2),
     * Idle (3), IOwait (4), IRQ (5), and SoftIRQ (6) states. By measuring the
     * difference between ticks across a time interval, CPU load over that interval
     * may be calculated.
     */
    protected void updateProcessorTicks() {
        LOG.trace("Updating Processor Ticks");
        long[][] ticks = getProcessorCpuLoadTicks();
        // Skip update if ticks is all zero.
        // Iterate to find a nonzero tick value and return; this should quickly
        // find a nonzero value if one exists and be fast in checking 0's
        // through branch prediction if it doesn't
        for (long[] tick : ticks) {
            for (long element : tick) {
                if (element != 0L) {
                    // We have a nonzero tick array, update and return!
                    this.procTickTime = System.currentTimeMillis();
                    // Copy to previous
                    for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
                        System.arraycopy(this.curProcTicks[cpu], 0, this.prevProcTicks[cpu], 0,
                                this.curProcTicks[cpu].length);
                    }
                    for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
                        System.arraycopy(ticks[cpu], 0, this.curProcTicks[cpu], 0, ticks[cpu].length);
                    }
                    return;
                }
            }
        }
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
    public int getPhysicalPackageCount() {
        return this.physicalPackageCount;
    }

    @Override
    public String toString() {
        return getName();
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
    protected String createProcessorID(String stepping, String model, String family, String[] flags) {
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
            if (flag.equals("fpu")) {
                processorIdBytes |= 1L << 32;
            } else if (flag.equals("vme")) {
                processorIdBytes |= 1L << 33;
            } else if (flag.equals("de")) {
                processorIdBytes |= 1L << 34;
            } else if (flag.equals("pse")) {
                processorIdBytes |= 1L << 35;
            } else if (flag.equals("tsc")) {
                processorIdBytes |= 1L << 36;
            } else if (flag.equals("msr")) {
                processorIdBytes |= 1L << 37;
            } else if (flag.equals("pae")) {
                processorIdBytes |= 1L << 38;
            } else if (flag.equals("mce")) {
                processorIdBytes |= 1L << 39;
            } else if (flag.equals("cx8")) {
                processorIdBytes |= 1L << 40;
            } else if (flag.equals("apic")) {
                processorIdBytes |= 1L << 41;
            } else if (flag.equals("sep")) {
                processorIdBytes |= 1L << 43;
            } else if (flag.equals("mtrr")) {
                processorIdBytes |= 1L << 44;
            } else if (flag.equals("pge")) {
                processorIdBytes |= 1L << 45;
            } else if (flag.equals("mca")) {
                processorIdBytes |= 1L << 46;
            } else if (flag.equals("cmov")) {
                processorIdBytes |= 1L << 47;
            } else if (flag.equals("pat")) {
                processorIdBytes |= 1L << 48;
            } else if (flag.equals("pse-36")) {
                processorIdBytes |= 1L << 49;
            } else if (flag.equals("psn")) {
                processorIdBytes |= 1L << 50;
            } else if (flag.equals("clfsh")) {
                processorIdBytes |= 1L << 51;
            } else if (flag.equals("ds")) {
                processorIdBytes |= 1L << 53;
            } else if (flag.equals("acpi")) {
                processorIdBytes |= 1L << 54;
            } else if (flag.equals("mmx")) {
                processorIdBytes |= 1L << 55;
            } else if (flag.equals("fxsr")) {
                processorIdBytes |= 1L << 56;
            } else if (flag.equals("sse")) {
                processorIdBytes |= 1L << 57;
            } else if (flag.equals("sse2")) {
                processorIdBytes |= 1L << 58;
            } else if (flag.equals("ss")) {
                processorIdBytes |= 1L << 59;
            } else if (flag.equals("htt")) {
                processorIdBytes |= 1L << 60;
            } else if (flag.equals("tm")) {
                processorIdBytes |= 1L << 61;
            } else if (flag.equals("ia64")) {
                processorIdBytes |= 1L << 62;
            } else if (flag.equals("pbe")) {
                processorIdBytes |= 1L << 63;
            }
        }
        return String.format("%016X", processorIdBytes);
    }
}
