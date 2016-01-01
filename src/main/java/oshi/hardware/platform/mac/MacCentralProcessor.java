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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.mac;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
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

import oshi.hardware.CentralProcessor;
import oshi.util.ExecutingCommand;
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
public class MacCentralProcessor implements CentralProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(MacCentralProcessor.class);

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

    private String cpuIdentifier = null;

    private String cpuStepping;

    private String cpuModel;

    private String cpuFamily;

    private Long cpuVendorFreq = null;

    private Boolean cpu64;

    /**
     * Create a Processor
     */
    public MacCentralProcessor() {
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
     * Updates logical and physical processor counts from sysctl calls
     */
    private void calculateProcessorCounts() {
        IntByReference size = new IntByReference(SystemB.INT_SIZE);
        Pointer p = new Memory(size.getValue());

        // Get number of logical processors
        if (0 != SystemB.INSTANCE.sysctlbyname("hw.logicalcpu", p, size, null, 0)) {
            LOG.error("Failed to get number of logical CPUs. Error code: " + Native.getLastError());
            this.logicalProcessorCount = 1;
        } else
            this.logicalProcessorCount = p.getInt(0);

        // Get number of physical processors
        if (0 != SystemB.INSTANCE.sysctlbyname("hw.physicalcpu", p, size, null, 0)) {
            LOG.error("Failed to get number of physical CPUs. Error code: " + Native.getLastError());
            this.physicalProcessorCount = 1;
        } else
            this.physicalProcessorCount = p.getInt(0);
    }

    /**
     * {@inheritDoc}
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
        int machPort = SystemB.INSTANCE.mach_host_self();
        HostCpuLoadInfo cpuLoadInfo = new HostCpuLoadInfo();
        if (0 != SystemB.INSTANCE.host_statistics(machPort, SystemB.HOST_CPU_LOAD_INFO, cpuLoadInfo,
                new IntByReference(cpuLoadInfo.size()))) {
            LOG.error("Failed to get System CPU ticks. Error code: " + Native.getLastError());
            return ticks;
        }
        // Switch order to match linux
        ticks[0] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_USER];
        ticks[1] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_NICE];
        ticks[2] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_SYSTEM];
        ticks[3] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_IDLE];
        return ticks;
    }

    /**
     * Updates system tick information from native host_statistics query. Stores
     * in array with four elements representing clock ticks or milliseconds
     * (platform dependent) spent in User (0), Nice (1), System (2), and Idle
     * (3) states. By measuring the difference between ticks across a time
     * interval, CPU load over that interval may be calculated.
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
        return OS_MXBEAN.getSystemLoadAverage();
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

        int machPort = SystemB.INSTANCE.mach_host_self();

        IntByReference procCount = new IntByReference();
        PointerByReference procCpuLoadInfo = new PointerByReference();
        IntByReference procInfoCount = new IntByReference();
        if (0 != SystemB.INSTANCE.host_processor_info(machPort, SystemB.PROCESSOR_CPU_LOAD_INFO, procCount,
                procCpuLoadInfo, procInfoCount)) {
            LOG.error("Failed to update CPU Load. Error code: " + Native.getLastError());
            return ticks;
        }

        int[] cpuTicks = procCpuLoadInfo.getValue().getIntArray(0, procInfoCount.getValue());
        for (int cpu = 0; cpu < procCount.getValue(); cpu++) {
            for (int j = 0; j < 4; j++) {
                int offset = cpu * SystemB.CPU_STATE_MAX;
                ticks[cpu][0] = FormatUtil.getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_USER]);
                ticks[cpu][1] = FormatUtil.getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_NICE]);
                ticks[cpu][2] = FormatUtil.getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_SYSTEM]);
                ticks[cpu][3] = FormatUtil.getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_IDLE]);
            }
        }
        return ticks;
    }

    /**
     * Updates the tick array for all processors by calling
     * host_processor_info(). Stores in 2D array; an array for each logical
     * processor with four elements representing clock ticks or milliseconds
     * (platform dependent) spent in User (0), Nice (1), System (2), and Idle
     * (3) states. By measuring the difference between ticks across a time
     * interval, CPU load over that interval may be calculated.
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSystemSerialNumber() {
        String sn = null;
        ArrayList<String> hwInfo = ExecutingCommand.runNative("system_profiler SPHardwareDataType");
        // Mavericks and later
        for (String checkLine : hwInfo) {
            if (checkLine.contains("Serial Number (system)")) {
                String[] snSplit = checkLine.split("\\s+");
                sn = snSplit[snSplit.length - 1];
                break;
            }
        }
        // Panther and later
        if (sn == null) {
            for (String checkLine : hwInfo) {
                if (checkLine.contains("r (system)")) {
                    String[] snSplit = checkLine.split("\\s+");
                    sn = snSplit[snSplit.length - 1];
                    break;
                }
            }
        }
        return (sn == null) ? "unknown" : sn;
    }

    @Override
    public int getLogicalProcessorCount() {
        return logicalProcessorCount;
    }

    @Override
    public int getPhysicalProcessorCount() {
        return physicalProcessorCount;
    }

    @Override
    public String toString() {
        return getName();
    }
}
