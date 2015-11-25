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
package oshi.software.os.windows.nt;

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

import oshi.hardware.Processor;
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

    // Logical and Physical Processor Counts
    private static final int logicalProcessorCount;
    private static int physicalProcessorCount = 0;
    static {
        // Get number of logical processors
        SYSTEM_INFO sysinfo = new SYSTEM_INFO();
        Kernel32.INSTANCE.GetSystemInfo(sysinfo);
        logicalProcessorCount = sysinfo.dwNumberOfProcessors.intValue();

        // Get number of physical processors
        WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION[] processors = Kernel32Util.getLogicalProcessorInformation();
        for (SYSTEM_LOGICAL_PROCESSOR_INFORMATION proc : processors) {
            if (proc.relationship == WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore) {
                physicalProcessorCount++;
            }
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

    // Initialize numCPU and open a Performance Data Helper Thread for
    // monitoring each processor ticks
    static PointerByReference phQuery = new PointerByReference();
    private static final IntByReference zero = new IntByReference(0);

    // Set up Performance Data Helper thread for uptime
    static PointerByReference uptimeQuery = new PointerByReference();
    private static final IntByReference one = new IntByReference(1);

    // Return values from PDH methods, if nonzero signifies error
    private static int pdhOpenTickQueryError = 0;
    private static int pdhAddTickCounterError = 0;
    private static int pdhOpenUptimeQueryError = 0;
    private static int pdhAddUptimeCounterError = 0;

    static {
        // Open tick query for this processor
        pdhOpenTickQueryError = Pdh.INSTANCE.PdhOpenQuery(null, zero, phQuery);
        if (pdhOpenTickQueryError != 0) {
            LOG.error("Failed to open PDH Tick Query. Error code: {}", String.format("0x%08X", pdhOpenTickQueryError));
        }

        // Open uptime query for this processor
        pdhOpenUptimeQueryError = Pdh.INSTANCE.PdhOpenQuery(null, one, uptimeQuery);
        if (pdhOpenTickQueryError != 0) {
            LOG.error("Failed to open PDH Uptime Query. Error code: {}",
                    String.format("0x%08X", pdhOpenUptimeQueryError));
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

    // Set up a counter for each processor
    private static PointerByReference[] phUserCounters = new PointerByReference[logicalProcessorCount];
    private static PointerByReference[] phIdleCounters = new PointerByReference[logicalProcessorCount];

    static {
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
                pdhAddTickCounterError = Pdh.INSTANCE.PdhAddEnglishCounterA(phQuery.getValue(), counterPath, zero,
                        phUserCounters[p]);
                if (pdhAddTickCounterError != 0) {
                    LOG.error("Failed to add PDH User Tick Counter for processor {}. Error code: {}", p,
                            String.format("0x%08X", pdhAddTickCounterError));
                    break;
                }
                counterPath = String.format("\\Processor(%d)\\%% idle time", p);
                phIdleCounters[p] = new PointerByReference();
                pdhAddTickCounterError = Pdh.INSTANCE.PdhAddEnglishCounterA(phQuery.getValue(), counterPath, zero,
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
    }

    // Set up counter for uptime
    private static PointerByReference pUptime;

    static {
        if (pdhOpenUptimeQueryError == 0) {
            // \System\System Up Time
            String uptimePath = "\\System\\System Up Time";
            pUptime = new PointerByReference();
            pdhAddUptimeCounterError = Pdh.INSTANCE.PdhAddEnglishCounterA(uptimeQuery.getValue(), uptimePath, one,
                    pUptime);
            if (pdhAddUptimeCounterError != 0) {
                LOG.error("Failed to add PDH Uptime Counter. Error code: {}",
                        String.format("0x%08X", pdhAddUptimeCounterError));
            }
        }
    }

    // Set up array to maintain current ticks for rapid reference. This array
    // will be updated in place and used to increment ticks based on processor
    // data helper which only gives % between reads
    private static long[][] allProcessorTicks = new long[logicalProcessorCount][4];
    private static long allProcTickTime = System.currentTimeMillis() - 100;

    private int processorNumber;
    private String cpuVendor;
    private String cpuName;
    private String cpuIdentifier;
    private Long cpuVendorFreq;

    /**
     * Create a Processor with the given number
     * 
     * @param procNo
     *            The processor number
     */
    public CentralProcessor(int procNo) {
        if (procNo >= logicalProcessorCount) {
            throw new IllegalArgumentException("Processor number (" + procNo
                    + ") must be less than the number of CPUs: " + logicalProcessorCount);
        }
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
            LOG.debug("System Ticks Updated");
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
     * Updates system tick information from native call to GetSystemTimes().
     * Array with four elements representing clock ticks or milliseconds
     * (platform dependent) spent in User (0), Nice (1), System (2), and Idle
     * (3) states. By measuring the difference between ticks across a time
     * interval, CPU load over that interval may be calculated.
     */
    private static void updateSystemTicks() {
        LOG.trace("Updating System Ticks");
        WinBase.FILETIME lpIdleTime = new WinBase.FILETIME();
        WinBase.FILETIME lpKernelTime = new WinBase.FILETIME();
        WinBase.FILETIME lpUserTime = new WinBase.FILETIME();
        if (0 == Kernel32.INSTANCE.GetSystemTimes(lpIdleTime, lpKernelTime, lpUserTime)) {
            LOG.error("Failed to update system idle/kernel/user times. Error code: " + Native.getLastError());
            return;
        }
        // Array order is user,nice,kernel,idle
        curTicks[0] = lpUserTime.toLong() + Kernel32.WIN32_TIME_OFFSET;
        curTicks[1] = 0L; // Windows is not 'nice'
        curTicks[2] = lpKernelTime.toLong() - lpIdleTime.toLong();
        curTicks[3] = lpIdleTime.toLong() + Kernel32.WIN32_TIME_OFFSET;
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
     * cache when iterating over processors since pdh query updates all counters
     * at once so we can't separate individual processors
     */
    private void updateProcessorTicks() {
        // Do nothing if we have PDH errors
        if (pdhOpenTickQueryError != 0 || pdhAddTickCounterError != 0) {
            LOG.warn("PDH Tick Counters not initialized. Processor ticks not updated.");
            return;
        }
        // Update no more frequently than 100ms so this is only triggered once
        // during iteration over Processors
        long now = System.currentTimeMillis();
        LOG.trace("Current time: {}  Last all processor tick time: {}", now, allProcTickTime);
        if (now - allProcTickTime < 100)
            return;

        // This call updates all process counters to % used since last call
        int ret = Pdh.INSTANCE.PdhCollectQueryData(phQuery.getValue());
        if (ret != 0) {
            LOG.warn("Failed to update Tick Counters. Error code: {}", String.format("0x%08X", ret));
            return;
        }
        // Multiply % usage times elapsed MS to recreate ticks, then increment
        long elapsed = now - allProcTickTime;
        for (int cpu = 0; cpu < logicalProcessorCount; cpu++) {
            PdhFmtCounterValue phUserCounterValue = new PdhFmtCounterValue();
            ret = Pdh.INSTANCE.PdhGetFormattedCounterValue(phUserCounters[cpu].getValue(), Pdh.PDH_FMT_LARGE
                    | Pdh.PDH_FMT_1000, null, phUserCounterValue);
            if (ret != 0) {
                LOG.warn("Failed to get Uer Tick Counters. Error code: {}", String.format("0x%08X", ret));
                return;
            }

            PdhFmtCounterValue phIdleCounterValue = new PdhFmtCounterValue();
            ret = Pdh.INSTANCE.PdhGetFormattedCounterValue(phIdleCounters[cpu].getValue(), Pdh.PDH_FMT_LARGE
                    | Pdh.PDH_FMT_1000, null, phIdleCounterValue);
            if (ret != 0) {
                LOG.warn("Failed to get idle Tick Counters. Error code: {}", String.format("0x%08X", ret));
                return;
            }

            // Returns results in 1000's of percent, e.g. 5% is 5000
            // Multiply by elapsed to get total ms and Divide by 100 * 1000
            // Putting division at end avoids need to cast division to double
            long user = elapsed * phUserCounterValue.value.largeValue / 100000;
            long idle = elapsed * phIdleCounterValue.value.largeValue / 100000;
            // Elasped is only since last read, so increment previous value
            allProcessorTicks[cpu][0] += user;
            // allProcessorTicks[cpu][1] is ignored, Windows is not nice
            allProcessorTicks[cpu][2] += (elapsed - user - idle); // u+i+sys=100%
            allProcessorTicks[cpu][3] += idle;
        }
        allProcTickTime = now;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemUptime() {
        // Return 0 if we have PDH errors
        if (pdhOpenUptimeQueryError != 0 || pdhAddUptimeCounterError != 0) {
            LOG.warn("Uptime Counters not initialized. Returning 0.");
            return 0L;
        }

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
