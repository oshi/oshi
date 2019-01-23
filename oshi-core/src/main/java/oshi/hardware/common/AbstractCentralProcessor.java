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
package oshi.hardware.common;

import java.lang.management.ManagementFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.CentralProcessor;
import oshi.util.ParseUtil;

/**
 * A CPU.
 */
@SuppressWarnings("restriction")
public abstract class AbstractCentralProcessor implements CentralProcessor {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCentralProcessor.class);

    /**
     * Instantiate an OperatingSystemMXBean for future convenience
     */
    private static final java.lang.management.OperatingSystemMXBean OS_MXBEAN = ManagementFactory
            .getOperatingSystemMXBean();

    /**
     * Keep track whether MXBean supports Oracle JVM methods
     */
    private static boolean sunMXBean = false;
    static {
        try {
            Class.forName("com.sun.management.OperatingSystemMXBean");
            LOG.debug("Oracle MXBean detected.");
            sunMXBean = true;
        } catch (ClassNotFoundException | ClassCastException e) {
            LOG.debug("Oracle MXBean not detected.");
        }
    }

    // Logical and Physical Processor Counts
    protected int physicalPackageCount = 0;
    protected int physicalProcessorCount = 0;
    protected int logicalProcessorCount = 0;

    // System ticks
    protected long[] systemCpuLoadTicks;
    // Per-processor ticks [cpu][type]
    private long[][] processorCpuLoadTicks;

    // Processor info
    private String cpuVendor;
    private String cpuName;
    private String processorID;
    private String cpuIdentifier;
    private String cpuStepping;
    private String cpuModel;
    private String cpuFamily;
    private long cpuVendorFreq;
    private long cpuMaxFreq;
    private long[] cpuCurrentFreq;
    private Boolean cpu64;
    private LogicalProcessor[] logicalProcessors;

    /**
     * Create a Processor
     */
    public AbstractCentralProcessor() {
        // Initialize processor counts and populate logical processor array
        this.logicalProcessors = initProcessorCounts();
    }

    /**
     * Updates logical and physical processor counts and arrays
     */
    protected abstract LogicalProcessor[] initProcessorCounts();

    /**
     * {@inheritDoc}
     */
    @Override
    public LogicalProcessor[] getLogicalProcessors() {
        return this.logicalProcessors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getSystemCpuLoadTicks() {
        if (this.systemCpuLoadTicks == null) {
            this.systemCpuLoadTicks = querySystemCpuLoadTicks();
        }
        return this.systemCpuLoadTicks;
    }

    /**
     * Get System-wide CPU Load tick counters.
     * 
     * @return The tick counters.
     */
    protected abstract long[] querySystemCpuLoadTicks();

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getCurrentFreq() {
        if (this.cpuCurrentFreq == null) {
            this.cpuCurrentFreq = queryCurrentFreq();
        }
        return this.cpuCurrentFreq;
    }

    /**
     * Get per processor current frequencies.
     * 
     * @return The current frequencies.
     */
    protected abstract long[] queryCurrentFreq();

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMaxFreq() {
        if (this.cpuMaxFreq == 0) {
            this.cpuMaxFreq = queryMaxFreq();
        }
        return this.cpuMaxFreq;
    }

    /**
     * Get processor max frequency.
     * 
     * @return The max frequency.
     */
    protected abstract long queryMaxFreq();

    /**
     * {@inheritDoc}
     */
    @Override
    public long[][] getProcessorCpuLoadTicks() {
        if (processorCpuLoadTicks == null) {
            this.processorCpuLoadTicks = queryProcessorCpuLoadTicks();
        }
        return this.processorCpuLoadTicks;
    }

    /**
     * Get Per-Processor CPU Load tick counters.
     * 
     * @return The tick counters.
     */
    protected abstract long[][] queryProcessorCpuLoadTicks();

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
    public long getVendorFreq() {
        if (this.cpuVendorFreq == 0) {
            Pattern pattern = Pattern.compile("@ (.*)$");
            Matcher matcher = pattern.matcher(getName());

            if (matcher.find()) {
                String unit = matcher.group(1);
                this.cpuVendorFreq = ParseUtil.parseHertz(unit);
            } else {
                this.cpuVendorFreq = -1L;
            }
        }
        return this.cpuVendorFreq;
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
     * @param cpuVendor
     *            the cpuVendor to set
     */
    protected void setVendor(String cpuVendor) {
        this.cpuVendor = cpuVendor;
    }

    /**
     * @param cpuName
     *            the cpuName to set
     */
    protected void setName(String cpuName) {
        this.cpuName = cpuName;
    }

    /**
     * @param cpuIdentifier
     *            the cpuIdentifier to set
     */
    protected void setIdentifier(String cpuIdentifier) {
        this.cpuIdentifier = cpuIdentifier;
    }

    /**
     * @param cpuStepping
     *            the cpuStepping to set
     */
    protected void setStepping(String cpuStepping) {
        this.cpuStepping = cpuStepping;
    }

    /**
     * @param cpuModel
     *            the cpuModel to set
     */
    protected void setModel(String cpuModel) {
        this.cpuModel = cpuModel;
    }

    /**
     * @param cpuFamily
     *            the cpuFamily to set
     */
    protected void setFamily(String cpuFamily) {
        this.cpuFamily = cpuFamily;
    }

    /**
     * @param cpuVendorFreq
     *            the cpuVendorFreq to set
     */
    protected void setVendorFreq(Long cpuVendorFreq) {
        this.cpuVendorFreq = cpuVendorFreq;
    }

    /**
     * @param cpu64
     *            the cpu64 to set
     */
    protected void setCpu64(Boolean cpu64) {
        this.cpu64 = cpu64;
    }

    /**
     * @param processorID
     *            the processorID to set
     */
    protected void setProcessorID(String processorID) {
        this.processorID = processorID;
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
    public synchronized double getSystemCpuLoadBetweenTicks(long[] oldTicks) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public double getSystemCpuLoad() {
        if (sunMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) OS_MXBEAN).getSystemCpuLoad();
        }
        return -1.0;
    }

    /**
     * {@inheritDoc}
     */
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
     * Creates a Processor ID by encoding the stepping, model, family, and
     * feature flags.
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateAttributes() {
        this.systemCpuLoadTicks = null;
        this.processorCpuLoadTicks = null;
        this.cpuCurrentFreq = null;
    }

}
