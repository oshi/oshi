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
package oshi.hardware.platform.windows;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.hardware.CentralProcessor;
import oshi.jna.platform.windows.Kernel32;
import oshi.jna.platform.windows.Pdh;
import oshi.jna.platform.windows.Pdh.PdhFmtCounterValue;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * A CPU as defined in Windows registry.
 * 
 * @author dblock[at]dblock[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 * @author widdis[at]gmail[dot]com
 */
@SuppressWarnings("restriction")
public class WindowsCentralProcessor implements CentralProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(WindowsCentralProcessor.class);

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

    // PDH counters only give increments between calls so we maintain our own
    // "ticks" here
    private long allProcTickTime;

    private long[][] allProcTicks;

    // Initialize numCPU and open a Performance Data Helper Thread for
    // monitoring each processor ticks
    private PointerByReference phQuery = new PointerByReference();

    private final IntByReference pZero = new IntByReference(0);

    // Set up user and idle tick counters for each processor
    private PointerByReference[] phUserCounters;

    private PointerByReference[] phIdleCounters;

    // Set up Performance Data Helper thread for uptime
    private PointerByReference uptimeQuery = new PointerByReference();

    private final IntByReference pOne = new IntByReference(1);

    // Set up counter for uptime
    private PointerByReference pUptime;

    private String cpuVendor;

    private String cpuName;

    private String cpuIdentifier;

    private Long cpuVendorFreq;

    /**
     * Create a Processor
     */
    public WindowsCentralProcessor() {
        // Processor counts
        calculateProcessorCounts();

        // PDH counter setup
        initPdhCounters();

        // System ticks
        this.prevTicks = new long[4];
        this.curTicks = new long[4];
        updateSystemTicks();

        // Per-processor ticks
        this.allProcTicks = new long[logicalProcessorCount][4];
        this.prevProcTicks = new long[logicalProcessorCount][4];
        this.curProcTicks = new long[logicalProcessorCount][4];
        updateProcessorTicks();

        LOG.debug("Initialized Processor");
    }

    /**
     * Updates logical and physical processor counts from /proc/cpuinfo
     */
    private void calculateProcessorCounts() {
        // Get number of logical processors
        SYSTEM_INFO sysinfo = new SYSTEM_INFO();
        Kernel32.INSTANCE.GetSystemInfo(sysinfo);
        this.logicalProcessorCount = sysinfo.dwNumberOfProcessors.intValue();

        // Get number of physical processors
        WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION[] processors = Kernel32Util.getLogicalProcessorInformation();
        for (SYSTEM_LOGICAL_PROCESSOR_INFORMATION proc : processors) {
            if (proc.relationship == WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore) {
                this.physicalProcessorCount++;
            }
        }
    }

    /**
     * Initializes PDH Tick and Uptime Counters
     */
    private void initPdhCounters() {
        // Open tick query
        int pdhOpenTickQueryError = Pdh.INSTANCE.PdhOpenQuery(null, pZero, phQuery);
        if (pdhOpenTickQueryError != 0) {
            LOG.error("Failed to open PDH Tick Query. Error code: {}", String.format("0x%08X", pdhOpenTickQueryError));
        }

        // Set up counters
        phUserCounters = new PointerByReference[logicalProcessorCount];
        phIdleCounters = new PointerByReference[logicalProcessorCount];

        if (pdhOpenTickQueryError == 0) {
            for (int p = 0; p < logicalProcessorCount; p++) {
                // Options are (only need 2 to calculate all)
                // "\Processor(0)\% processor time"
                // "\Processor(0)\% idle time" (1 - processor)
                // "\Processor(0)\% privileged time" (subset of processor)
                // "\Processor(0)\% user time" (other subset of processor)
                // Note need to make \ = \\ for Java Strings and %% for format
                String counterPath = String.format("\\Processor(%d)\\%% user time", p);
                phUserCounters[p] = new PointerByReference();
                // Open tick query for this processor
                int pdhAddTickCounterError = Pdh.INSTANCE.PdhAddEnglishCounterA(phQuery.getValue(), counterPath, pZero,
                        phUserCounters[p]);
                if (pdhAddTickCounterError != 0) {
                    LOG.error("Failed to add PDH User Tick Counter for processor {}. Error code: {}", p,
                            String.format("0x%08X", pdhAddTickCounterError));
                    break;
                }
                counterPath = String.format("\\Processor(%d)\\%% idle time", p);
                phIdleCounters[p] = new PointerByReference();
                pdhAddTickCounterError = Pdh.INSTANCE.PdhAddEnglishCounterA(phQuery.getValue(), counterPath, pZero,
                        phIdleCounters[p]);
                if (pdhAddTickCounterError != 0) {
                    LOG.error("Failed to add PDH Idle Tick Counter for processor {}. Error code: {}", p,
                            String.format("0x%08X", pdhAddTickCounterError));
                    break;
                }
            }

            LOG.debug("Tick counter queries added.  Initializing with first query.");
            // Initialize by collecting data the first time
            int ret = Pdh.INSTANCE.PdhCollectQueryData(phQuery.getValue());
            if (ret != 0) {
                LOG.warn("Failed to update Tick Counters. Error code: {}", String.format("0x%08X", ret));
            }
        }

        // Open uptime query
        int pdhOpenUptimeQueryError = Pdh.INSTANCE.PdhOpenQuery(null, pOne, uptimeQuery);
        if (pdhOpenTickQueryError != 0) {
            LOG.error("Failed to open PDH Uptime Query. Error code: {}",
                    String.format("0x%08X", pdhOpenUptimeQueryError));
        }
        if (pdhOpenUptimeQueryError == 0) {
            // \System\System Up Time
            String uptimePath = "\\System\\System Up Time";
            pUptime = new PointerByReference();
            int pdhAddUptimeCounterError = Pdh.INSTANCE.PdhAddEnglishCounterA(uptimeQuery.getValue(), uptimePath, pOne,
                    pUptime);
            if (pdhAddUptimeCounterError != 0) {
                LOG.error("Failed to add PDH Uptime Counter. Error code: {}",
                        String.format("0x%08X", pdhAddUptimeCounterError));
            }
        }

        // Set up hook to close the query on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Pdh.INSTANCE.PdhCloseQuery(phQuery.getValue());
                Pdh.INSTANCE.PdhCloseQuery(uptimeQuery.getValue());
            }
        });
    }

    /**
     * Vendor identifier, eg. GenuineIntel.
     * 
     * @return Processor vendor.
     */
    @Override
    public String getVendor() {
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
     * {@inheritDoc}
     */
    @Override
    public boolean isCpu64bit() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCpu64(boolean cpu64) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStepping() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStepping(String stepping) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModel() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setModel(String model) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFamily() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFamily(String family) {
        throw new UnsupportedOperationException();
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
        WinBase.FILETIME lpIdleTime = new WinBase.FILETIME();
        WinBase.FILETIME lpKernelTime = new WinBase.FILETIME();
        WinBase.FILETIME lpUserTime = new WinBase.FILETIME();
        if (!Kernel32.INSTANCE.GetSystemTimes(lpIdleTime, lpKernelTime, lpUserTime)) {
            LOG.error("Failed to update system idle/kernel/user times. Error code: " + Native.getLastError());
            return ticks;
        }
        // Array order is user,nice,kernel,idle
        // TODO: Change to lp*Time.toDWordLong.longValue() with JNA 4.2.2
        ticks[3] = WinBase.FILETIME.dateToFileTime(lpIdleTime.toDate());
        ticks[2] = WinBase.FILETIME.dateToFileTime(lpKernelTime.toDate()) - ticks[3];
        ticks[1] = 0L; // Windows is not 'nice'
        ticks[0] = WinBase.FILETIME.dateToFileTime(lpUserTime.toDate());
        return ticks;
    }

    /**
     * Updates system tick information from native call to GetSystemTimes().
     * Array with four elements representing clock ticks or milliseconds
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
        // Expected to be -1 for Windows
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

        // This call updates all process counters to % used since last call
        int ret = Pdh.INSTANCE.PdhCollectQueryData(phQuery.getValue());
        if (ret != 0) {
            LOG.warn("Failed to update Tick Counters. Error code: {}", String.format("0x%08X", ret));
            return ticks;
        }
        long now = System.currentTimeMillis();

        // We'll manufacture our own ticks by multiplying the % used (from the
        // counter) by time elapsed since the last call to get a tick increment
        long elapsed = now - allProcTickTime;
        for (int cpu = 0; cpu < logicalProcessorCount; cpu++) {
            PdhFmtCounterValue phUserCounterValue = new PdhFmtCounterValue();
            ret = Pdh.INSTANCE.PdhGetFormattedCounterValue(phUserCounters[cpu].getValue(),
                    Pdh.PDH_FMT_LARGE | Pdh.PDH_FMT_1000, null, phUserCounterValue);
            if (ret != 0) {
                LOG.warn("Failed to get Uer Tick Counters. Error code: {}", String.format("0x%08X", ret));
                return ticks;
            }

            PdhFmtCounterValue phIdleCounterValue = new PdhFmtCounterValue();
            ret = Pdh.INSTANCE.PdhGetFormattedCounterValue(phIdleCounters[cpu].getValue(),
                    Pdh.PDH_FMT_LARGE | Pdh.PDH_FMT_1000, null, phIdleCounterValue);
            if (ret != 0) {
                LOG.warn("Failed to get idle Tick Counters. Error code: {}", String.format("0x%08X", ret));
                return ticks;
            }

            // Returns results in 1000's of percent, e.g. 5% is 5000
            // Multiply by elapsed to get total ms and Divide by 100 * 1000
            // Putting division at end avoids need to cast division to double
            long user = elapsed * phUserCounterValue.value.largeValue / 100000;
            long idle = elapsed * phIdleCounterValue.value.largeValue / 100000;
            // Elasped is only since last read, so increment previous value
            allProcTicks[cpu][0] += user;
            // allProcTicks[cpu][1] is ignored, Windows is not nice
            allProcTicks[cpu][2] += Math.max(0, elapsed - user - idle); // u+i+sys=100%
            allProcTicks[cpu][3] += idle;
        }
        allProcTickTime = now;

        // Make a copy of the array to return
        for (int cpu = 0; cpu < logicalProcessorCount; cpu++) {
            System.arraycopy(allProcTicks[cpu], 0, ticks[cpu], 0, allProcTicks[cpu].length);
        }
        return ticks;
    }

    /**
     * Updates the tick array for all processors by querying PDH counter. Stores
     * in 2D array; an array for each logical processor with four elements
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
        int ret = Pdh.INSTANCE.PdhCollectQueryData(uptimeQuery.getValue());
        if (ret != 0) {
            LOG.error("Failed to update Uptime Counters. Error code: {}", String.format("0x%08X", ret));
            return 0L;
        }

        PdhFmtCounterValue uptimeCounterValue = new PdhFmtCounterValue();
        ret = Pdh.INSTANCE.PdhGetFormattedCounterValue(pUptime.getValue(), Pdh.PDH_FMT_LARGE, null, uptimeCounterValue);
        if (ret != 0) {
            LOG.error("Failed to get Uptime Counters. Error code: {}", String.format("0x%08X", ret));
            return 0L;
        }

        return uptimeCounterValue.value.largeValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSystemSerialNumber() {
        String sn = null;
        // This should always work
        ArrayList<String> hwInfo = ExecutingCommand.runNative("wmic bios get serialnumber");
        for (String checkLine : hwInfo) {
            if (checkLine.length() == 0 || checkLine.toLowerCase().contains("serialnumber")) {
                continue;
            } else {
                sn = checkLine.trim();
                break;
            }
        }
        // Just in case the above doesn't
        if (sn == null || sn.length() == 0) {
            hwInfo = ExecutingCommand.runNative("wmic csproduct get identifyingnumber");
            for (String checkLine : hwInfo) {
                if (checkLine.length() == 0 || checkLine.toLowerCase().contains("identifyingnumber")) {
                    continue;
                } else {
                    sn = checkLine.trim();
                    break;
                }
            }
        }
        return (sn == null || sn.length() == 0) ? "unknown" : sn;
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
