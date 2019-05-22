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

import static oshi.util.StringUtil.isBlank;
import java.lang.management.ManagementFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.CentralProcessor;
import oshi.util.ParseUtil;

/**
 * A CPU as defined in Linux /proc. This implementation is thread-safe.
 *
 * @author alessandro[at]perucchi[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 * @author widdis[at]gmail[dot]com
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
    private final boolean sunMXBean;

    /**
     * The lock for all mutable (non-final) fields
     */
    protected final Object instanceLock = new Object();

    /**
     * Calling OperatingSystemMxBean too rapidly results in NaN. Store the
     * latest value to return if polling is too rapid
     */
    private double lastCpuLoad = 0d;

    /**
     * Keep track of last CPU Load poll to OperatingSystemMXBean to ensure
     * enough time has elapsed
     */
    private long lastCpuLoadTime = 0;

    // Maintain previous ticks to be used for calculating usage between them.
    // System ticks
    private long tickTime;

    private long[] prevTicks;

    private long[] curTicks;

    // Per-processor ticks [cpu][type]
    private long procTickTime;

    private long[][] prevProcTicks;

    private long[][] curProcTicks;

    // Logical and Physical Processor Counts
    protected final int logicalProcessorCount;

    protected final int physicalProcessorCount;

    protected final int physicalPackageCount;

    // Processor info
    protected final String cpuVendor;

    protected final String cpuName;

    protected final String processorID;

    protected final String cpuIdentifier;

    protected final String cpuStepping;

    protected final String cpuModel;

    protected final String cpuFamily;

    protected final long cpuVendorFreq;

    protected final boolean cpu64;

    /**
     * Create a Processor
     */
    public AbstractCentralProcessor() {
        sunMXBean = initMXBean();

        CentralProcessorInitializer initializer = getInitializer();

        if (initializer != null) {
            logicalProcessorCount = initializer.logicalProcessorCount;
            physicalProcessorCount = initializer.physicalProcessorCount;
            physicalPackageCount = initializer.physicalPackageCount;
            cpuVendor = isBlank(initializer.cpuVendor) ? "" : initializer.cpuVendor;
            cpuName = isBlank(initializer.cpuName) ? "" : initializer.cpuName;
            processorID = isBlank(initializer.processorID) ? "" : initializer.processorID;
            cpu64 = initializer.cpu64;
            if (initializer.cpuFamily == null) {
                cpuFamily = initializer.cpuIdentifier == null ? "?" : parseIdentifier("Family", initializer.cpuIdentifier);
            } else {
                cpuFamily = initializer.cpuFamily;
            }
            if (initializer.cpuModel == null) {
                cpuModel = initializer.cpuIdentifier == null ? "?" : parseIdentifier("Model", initializer.cpuIdentifier);
            } else {
                cpuModel = initializer.cpuModel;
            }
            if (initializer.cpuStepping == null) {
                cpuStepping = initializer.cpuIdentifier == null ? "?" : parseIdentifier("Stepping", initializer.cpuIdentifier);
            } else {
                cpuStepping = initializer.cpuStepping;
            }
            if (initializer.cpuIdentifier == null) {
                StringBuilder sb = new StringBuilder();
                if (cpuVendor.contentEquals("GenuineIntel")) {
                    sb.append(cpu64 ? "Intel64" : "x86");
                } else {
                    sb.append(cpuVendor);
                }
                sb.append(" Family ").append(cpuFamily);
                sb.append(" Model ").append(cpuModel);
                sb.append(" Stepping ").append(cpuStepping);
                cpuIdentifier = sb.toString();
            } else {
                cpuIdentifier = initializer.cpuIdentifier;
            }

            Pattern pattern = Pattern.compile("@ (.*)$");
            Matcher matcher = pattern.matcher(cpuName);

            if (matcher.find()) {
                String unit = matcher.group(1);
                this.cpuVendorFreq = ParseUtil.parseHertz(unit);
            } else {
                this.cpuVendorFreq = -1L;
            }
        } else {
            logicalProcessorCount = 0;
            physicalProcessorCount = 0;
            physicalPackageCount = 0;
            cpuVendor = "";
            cpuName = "";
            processorID = "";
            cpuIdentifier = "?";
            cpuStepping = "?";
            cpuModel = "?";
            cpuFamily = "?";
            cpuVendorFreq = -1L;
            cpu64 = false;
        }
    }

    /**
     * Initializes mxBean boolean
     */
    private boolean initMXBean() {
        try {
            Class.forName("com.sun.management.OperatingSystemMXBean");
            // Initialize CPU usage
            synchronized (instanceLock) {
                this.lastCpuLoad = ((com.sun.management.OperatingSystemMXBean) OS_MXBEAN).getSystemCpuLoad();
                this.lastCpuLoadTime = System.currentTimeMillis();
            }
            LOG.debug("Oracle MXBean detected.");
            return true;
        } catch (ClassNotFoundException | ClassCastException e) {
            LOG.debug("Oracle MXBean not detected.");
            LOG.trace("{}", e);
            return false;
        }
    }

    /**
     * Initializes tick arrays
     */
    protected void initTicks() {
        synchronized (instanceLock) {
            this.prevProcTicks = new long[this.logicalProcessorCount][TickType.values().length];
            this.curProcTicks = new long[this.logicalProcessorCount][TickType.values().length];
            this.prevTicks = new long[TickType.values().length];
            this.curTicks = new long[TickType.values().length];
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
    public String getName() {
        return this.cpuName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProcessorID() {
        return this.processorID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getVendorFreq() {
        return this.cpuVendorFreq;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentifier() {
        return this.cpuIdentifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCpu64bit() {
        return this.cpu64;
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
    public String getModel() {
        return this.cpuModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFamily() {
        return this.cpuFamily;
    }

    /**
     * Parses identifier string
     *
     * @param id
     *            the id to retrieve
     * @return the string following id
     */
    private static String parseIdentifier(String id, String identifier) {
        String[] idSplit = ParseUtil.whitespaces.split(identifier);
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
    public double getSystemCpuLoadBetweenTicks() {
        // Check if > ~ 0.95 seconds since last tick count.
        long now = System.currentTimeMillis();
        long total, idle;
        synchronized (instanceLock) {
            LOG.trace("Current time: {}  Last tick time: {}", now, this.tickTime);
            if (now - this.tickTime > 950) {
                // Enough time has elapsed.
                updateSystemTicks();
            }
            // Calculate total
            total = 0;
            for (int i = 0; i < this.curTicks.length; i++) {
                total += this.curTicks[i] - this.prevTicks[i];
            }
            // Calculate idle from difference in idle and IOwait
            idle = this.curTicks[TickType.IDLE.getIndex()] + this.curTicks[TickType.IOWAIT.getIndex()]
                    - this.prevTicks[TickType.IDLE.getIndex()] - this.prevTicks[TickType.IOWAIT.getIndex()];
        }
        LOG.trace("Total ticks: {}  Idle ticks: {}", total, idle);

        return total > 0 && idle >= 0 ? (double) (total - idle) / total : 0d;
    }

    /**
     * Updates system tick information. Stores in array with seven elements
     * representing clock ticks or milliseconds (platform dependent) spent in
     * User (0), Nice (1), System (2), Idle (3), IOwait (4), IRQ (5), and
     * SoftIRQ (6) states. By measuring the difference between ticks across a
     * time interval, CPU load over that interval may be calculated.
     */
    protected void updateSystemTicks() {
        LOG.trace("Updating System Ticks");
        long[] ticks = getSystemCpuLoadTicks();
        // Skip update if ticks is all zero.
        // Iterate to find a nonzero tick value and return; this should quickly
        // find a nonzero value if one exists and be fast in checking 0's
        // through branch prediction if it doesn't
        synchronized (instanceLock) {
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getSystemCpuLoad() {
        if (this.sunMXBean) {
            synchronized (instanceLock) {
                long now = System.currentTimeMillis();
                // If called too recently, return latest value
                if (now - this.lastCpuLoadTime < 200) {
                    return this.lastCpuLoad;
                }
                this.lastCpuLoad = ((com.sun.management.OperatingSystemMXBean) OS_MXBEAN).getSystemCpuLoad();
                this.lastCpuLoadTime = now;
                return this.lastCpuLoad;
            }
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
    public double[] getProcessorCpuLoadBetweenTicks() {
        // Check if > ~ 0.95 seconds since last tick count.
        long now = System.currentTimeMillis();
        synchronized (instanceLock) {
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
    }

    /**
     * Updates per-processor tick information. Stores in 2D array; an array for
     * each logical processor with with seven elements representing clock ticks
     * or milliseconds (platform dependent) spent in User (0), Nice (1), System
     * (2), Idle (3), IOwait (4), IRQ (5), and SoftIRQ (6) states. By measuring
     * the difference between ticks across a time interval, CPU load over that
     * interval may be calculated.
     */
    protected void updateProcessorTicks() {
        LOG.trace("Updating Processor Ticks");
        long[][] ticks = getProcessorCpuLoadTicks();
        // Skip update if ticks is all zero.
        // Iterate to find a nonzero tick value and return; this should quickly
        // find a nonzero value if one exists and be fast in checking 0's
        // through branch prediction if it doesn't
        synchronized (instanceLock) {
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
            switch (flag) {
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

    protected abstract CentralProcessorInitializer getInitializer();

    public static class CentralProcessorInitializer {
        public int logicalProcessorCount;
        public int physicalProcessorCount;
        public int physicalPackageCount;
        public String cpuVendor;
        public String cpuName;
        public String processorID;
        public String cpuIdentifier;
        public String cpuStepping;
        public String cpuModel;
        public String cpuFamily;
        public long cpuVendorFreq;
        public boolean cpu64;
    }
}
