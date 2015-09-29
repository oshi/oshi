/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2015 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.mac.local;

import java.lang.management.ManagementFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.mac.SystemB.HostCpuLoadInfo;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.hardware.Processor;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;

/**
 * A CPU.
 * 
 * @author alessandro[at]perucchi[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 * @author widdis[at]gmail[dot]com
 */
@SuppressWarnings("restriction")
public class CentralProcessor implements Processor {
    private static final Logger LOG = LoggerFactory.getLogger(CentralProcessor.class);

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

    // Maintain two sets of previous ticks to be used for calculating usage
    // between them.
    // System ticks (static)
    private static long tickTime = System.currentTimeMillis();
    private static long[] prevTicks = new long[4];
    private static long[] curTicks = new long[4];

    static {
        updateSystemTicks();
        System.arraycopy(curTicks, 0, prevTicks, 0, curTicks.length);
    }

    // Maintain similar arrays for per-processor ticks (class variables)
    private long procTickTime = System.currentTimeMillis();
    private long[] prevProcTicks = new long[4];
    private long[] curProcTicks = new long[4];

    // Initialize numCPU
    private static int numCPU = 0;

    static {
        IntByReference size = new IntByReference(SystemB.INT_SIZE);
        Pointer p = new Memory(size.getValue());
        if (0 != SystemB.INSTANCE.sysctlbyname("hw.logicalcpu", p, size, null, 0)) {
            LOG.error("Failed to get number of CPUs. Error code: " + Native.getLastError());
            numCPU = 1;
        } else
            numCPU = p.getInt(0);
    }

    // Set up array to maintain current ticks for rapid reference. This array
    // will be updated in place and used as a cache to avoid rereading file
    // while iterating processors
    private static long[][] allProcessorTicks = new long[numCPU][4];
    private static long allProcTickTime = 0;

    private int processorNumber;
    private String cpuVendor;
    private String cpuName;
    private String cpuIdentifier = null;
    private String cpuStepping;
    private String cpuModel;
    private String cpuFamily;
    private Long cpuVendorFreq = null;
    private Boolean cpu64;

    /**
     * Create a Processor with the given number
     * 
     * @param procNo
     *            The processor number
     */
    public CentralProcessor(int procNo) {
        if (procNo >= numCPU)
            throw new IllegalArgumentException("Processor number (" + procNo
                    + ") must be less than the number of CPUs: " + numCPU);
        this.processorNumber = procNo;
        updateProcessorTicks();
        System.arraycopy(allProcessorTicks[this.processorNumber], 0, this.curProcTicks, 0, this.curProcTicks.length);
        LOG.debug("Initialized Processor {}", procNo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessorNumber() {
        return this.processorNumber;
    }

    /**
     * Vendor identifier, eg. GenuineIntel.
     * 
     * @return Processor vendor.
     */
    @Override
    public String getVendor() {
        if (this.cpuVendor == null) {
            IntByReference size = new IntByReference();
            if (0 != SystemB.INSTANCE.sysctlbyname("machdep.cpu.vendor", null, size, null, 0)) {
                LOG.error("Failed to get Vendor. Error code: " + Native.getLastError());
                return "";
            }
            Pointer p = new Memory(size.getValue() + 1);
            if (0 != SystemB.INSTANCE.sysctlbyname("machdep.cpu.vendor", p, size, null, 0)) {
                LOG.error("Failed to get Vendor. Error code: " + Native.getLastError());
                return "";
            }
            this.cpuVendor = p.getString(0);
        }
        return this.cpuVendor;
    }

    /**
     * Set processor vendor.
     * 
     * @param vendor
     *            Vendor.
     */
    @Override
    public void setVendor(String vendor) {
        this.cpuVendor = vendor;
    }

    /**
     * Name, eg. Intel(R) Core(TM)2 Duo CPU T7300 @ 2.00GHz
     * 
     * @return Processor name.
     */
    @Override
    public String getName() {
        if (this.cpuName == null) {
            IntByReference size = new IntByReference();
            if (0 != SystemB.INSTANCE.sysctlbyname("machdep.cpu.brand_string", null, size, null, 0)) {
                LOG.error("Failed to get Name. Error code: " + Native.getLastError());
                return "";
            }
            Pointer p = new Memory(size.getValue() + 1);
            if (0 != SystemB.INSTANCE.sysctlbyname("machdep.cpu.brand_string", p, size, null, 0)) {
                LOG.error("Failed to get Name. Error code: " + Native.getLastError());
                return "";
            }
            this.cpuName = p.getString(0);
        }
        return this.cpuName;
    }

    /**
     * Set processor name.
     * 
     * @param name
     *            Name.
     */
    @Override
    public void setName(String name) {
        this.cpuName = name;
    }

    /**
     * Vendor frequency (in Hz), eg. for processor named Intel(R) Core(TM)2 Duo
     * CPU T7300 @ 2.00GHz the vendor frequency is 2000000000.
     * 
     * @return Processor frequency or -1 if unknown.
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
     * Set vendor frequency.
     * 
     * @param freq
     *            Frequency.
     */
    @Override
    public void setVendorFreq(long freq) {
        this.cpuVendorFreq = Long.valueOf(freq);
    }

    /**
     * Identifier, eg. x86 Family 6 Model 15 Stepping 10.
     * 
     * @return Processor identifier.
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
            this.cpuIdentifier = sb.toString();
        }
        return this.cpuIdentifier;
    }

    /**
     * Set processor identifier.
     * 
     * @param identifier
     *            Identifier.
     */
    @Override
    public void setIdentifier(String identifier) {
        this.cpuIdentifier = identifier;
    }

    /**
     * Is CPU 64bit?
     * 
     * @return True if cpu is 64bit.
     */
    @Override
    public boolean isCpu64bit() {
        if (this.cpu64 == null) {
            IntByReference size = new IntByReference(SystemB.INT_SIZE);
            Pointer p = new Memory(size.getValue());
            if (0 != SystemB.INSTANCE.sysctlbyname("hw.cpu64bit_capable", p, size, null, 0)) {
                LOG.error("Failed to get 64Bit_capable. Error code: " + Native.getLastError());
                return false;
            }
            this.cpu64 = p.getInt(0) != 0;
        }
        return this.cpu64.booleanValue();
    }

    /**
     * Set flag is cpu is 64bit.
     * 
     * @param value
     *            True if cpu is 64.
     */
    @Override
    public void setCpu64(boolean value) {
        this.cpu64 = Boolean.valueOf(value);
    }

    /**
     * @return the stepping
     */
    @Override
    public String getStepping() {
        if (this.cpuStepping == null) {
            IntByReference size = new IntByReference(SystemB.INT_SIZE);
            Pointer p = new Memory(size.getValue());
            if (0 != SystemB.INSTANCE.sysctlbyname("machdep.cpu.stepping", p, size, null, 0)) {
                LOG.error("Failed to get Stepping. Error code: " + Native.getLastError());
                return "";
            }
            this.cpuStepping = Integer.toString(p.getInt(0));
        }
        return this.cpuStepping;
    }

    /**
     * @param stepping
     *            the stepping to set
     */
    @Override
    public void setStepping(String stepping) {
        this.cpuStepping = stepping;
    }

    /**
     * @return the model
     */
    @Override
    public String getModel() {
        if (this.cpuModel == null) {
            IntByReference size = new IntByReference(SystemB.INT_SIZE);
            Pointer p = new Memory(size.getValue());
            if (0 != SystemB.INSTANCE.sysctlbyname("machdep.cpu.model", p, size, null, 0)) {
                LOG.error("Failed to get Model. Error code: " + Native.getLastError());
                return "";
            }
            this.cpuModel = Integer.toString(p.getInt(0));
        }
        return this.cpuModel;
    }

    /**
     * @param model
     *            the model to set
     */
    @Override
    public void setModel(String model) {
        this.cpuModel = model;
    }

    /**
     * @return the family
     */
    @Override
    public String getFamily() {
        if (this.cpuFamily == null) {
            IntByReference size = new IntByReference(SystemB.INT_SIZE);
            Pointer p = new Memory(size.getValue());
            if (0 != SystemB.INSTANCE.sysctlbyname("machdep.cpu.family", p, size, null, 0)) {
                LOG.error("Failed to get Family. Error code: " + Native.getLastError());
                return "";
            }
            this.cpuFamily = Integer.toString(p.getInt(0));
        }
        return this.cpuFamily;
    }

    /**
     * @param family
     *            the family to set
     */
    @Override
    public void setFamily(String family) {
        this.cpuFamily = family;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public float getLoad() {
        // TODO Remove in 2.0
        return (float) getSystemCpuLoadBetweenTicks() * 100;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized double getSystemCpuLoadBetweenTicks() {
        // Check if > ~ 0.95 seconds since last tick count.
        long now = System.currentTimeMillis();
        LOG.trace("Current time: {}  Last tick time: {}", now, tickTime);
        boolean update = (now - tickTime > 950);
        if (update) {
            // Enough time has elapsed.
            // Update latest
            updateSystemTicks();
            tickTime = now;
        }
        // Calculate total
        long total = 0;
        for (int i = 0; i < curTicks.length; i++) {
            total += (curTicks[i] - prevTicks[i]);
        }
        // Calculate idle from last field [3]
        long idle = curTicks[3] - prevTicks[3];
        LOG.trace("Total ticks: {}  Idle ticks: {}", total, idle);

        // Copy latest ticks to earlier position for next call
        if (update) {
            System.arraycopy(curTicks, 0, prevTicks, 0, curTicks.length);
        }

        // return
        if (total > 0 && idle >= 0) {
            return (double) (total - idle) / total;
        }
        return 0d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getSystemCpuLoadTicks() {
        updateSystemTicks();
        // Make a copy
        long[] ticks = new long[curTicks.length];
        System.arraycopy(curTicks, 0, ticks, 0, curTicks.length);
        return ticks;
    }

    /**
     * Updates system tick information from native host_statistics query. Array
     * with four elements representing clock ticks or milliseconds (platform
     * dependent) spent in User (0), Nice (1), System (2), and Idle (3) states.
     * By measuring the difference between ticks across a time interval, CPU
     * load over that interval may be calculated.
     * 
     * @return An array of 4 long values representing time spent in User,
     *         Nice(if applicable), System, and Idle states.
     */
    private static void updateSystemTicks() {
        LOG.trace("Updating System Ticks");
        int machPort = SystemB.INSTANCE.mach_host_self();
        HostCpuLoadInfo cpuLoadInfo = new HostCpuLoadInfo();
        if (0 != SystemB.INSTANCE.host_statistics(machPort, SystemB.HOST_CPU_LOAD_INFO, cpuLoadInfo,
                new IntByReference(cpuLoadInfo.size()))) {
            LOG.error("Failed to get System CPU ticks. Error code: " + Native.getLastError());
            return;
        }
        // Switch order to match linux
        curTicks[0] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_USER];
        curTicks[1] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_NICE];
        curTicks[2] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_SYSTEM];
        curTicks[3] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_IDLE];
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
        return OS_MXBEAN.getSystemLoadAverage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getProcessorCpuLoadBetweenTicks() {
        // Check if > ~ 0.95 seconds since last tick count.
        long now = System.currentTimeMillis();
        LOG.trace("Current time: {}  Last processor tick time: {}", now, this.procTickTime);
        if (now - this.procTickTime > 950) {
            // Enough time has elapsed. Update array in place
            updateProcessorTicks();
            // Copy arrays in place
            System.arraycopy(this.curProcTicks, 0, this.prevProcTicks, 0, this.curProcTicks.length);
            System.arraycopy(allProcessorTicks[this.processorNumber], 0, this.curProcTicks, 0, this.curProcTicks.length);
            this.procTickTime = now;
        }
        long total = 0;
        for (int i = 0; i < this.curProcTicks.length; i++) {
            total += (this.curProcTicks[i] - this.prevProcTicks[i]);
        }
        // Calculate idle from last field [3]
        long idle = this.curProcTicks[3] - this.prevProcTicks[3];
        LOG.trace("Total ticks: {}  Idle ticks: {}", total, idle);
        // update
        return (total > 0 && idle >= 0) ? (double) (total - idle) / total : 0d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getProcessorCpuLoadTicks() {
        updateProcessorTicks();
        return allProcessorTicks[this.processorNumber];
    }

    /**
     * Updates the tick array for all processors if more than 100ms has elapsed
     * since the last update. This permits using the allProcessorTicks as a
     * cache when iterating over processors so that the host_processor_info
     * query is only done once
     */
    private static void updateProcessorTicks() {
        // Update no more frequently than 100ms so this is only triggered once
        // during iteration over Processors
        long now = System.currentTimeMillis();
        LOG.trace("Current time: {}  Last all processor tick time: {}", now, allProcTickTime);
        if (now - allProcTickTime < 100)
            return;

        int machPort = SystemB.INSTANCE.mach_host_self();

        IntByReference procCount = new IntByReference();
        PointerByReference procCpuLoadInfo = new PointerByReference();
        IntByReference procInfoCount = new IntByReference();
        if (0 != SystemBLib.INSTANCE.host_processor_info(machPort, SystemBLib.PROCESSOR_CPU_LOAD_INFO, procCount,
                procCpuLoadInfo, procInfoCount)) {
            LOG.error("Failed to update CPU Load. Error code: " + Native.getLastError());
            return;
        }

        int[] cpuTicks = procCpuLoadInfo.getValue().getIntArray(0, procInfoCount.getValue());
        for (int cpu = 0; cpu < procCount.getValue(); cpu++) {
            for (int j = 0; j < 4; j++) {
                int offset = cpu * SystemB.CPU_STATE_MAX;
                allProcessorTicks[cpu][0] = FormatUtil.getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_USER]);
                allProcessorTicks[cpu][1] = FormatUtil.getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_NICE]);
                allProcessorTicks[cpu][2] = FormatUtil.getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_SYSTEM]);
                allProcessorTicks[cpu][3] = FormatUtil.getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_IDLE]);
            }
        }
        allProcTickTime = now;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemUptime() {
        IntByReference size = new IntByReference();
        if (0 != SystemB.INSTANCE.sysctlbyname("kern.boottime", null, size, null, 0)) {
            LOG.error("Failed to get Boot Time. Error code: " + Native.getLastError());
            return 0L;
        }
        // This should point to a 16-byte structure. If not, this code is valid
        if (size.getValue() != 16)
            throw new UnsupportedOperationException("sysctl kern.boottime should be 16 bytes but isn't.");
        Pointer p = new Memory(size.getValue() + 1);
        if (0 != SystemB.INSTANCE.sysctlbyname("kern.boottime", p, size, null, 0)) {
            LOG.error("Failed to get Boot Time. Error code: " + Native.getLastError());
            return 0L;
        }
        // p now points to a 16-bit timeval structure for boot time.
        // First 8 bytes are seconds, second 8 bytes are microseconds (ignore)
        return System.currentTimeMillis() / 1000 - p.getLong(0);
    }

    @Override
    public String toString() {
        return getName();
    }

}
