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
package oshi.hardware.platform.unix.freebsd;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.unix.CLibrary.Timeval;
import oshi.jna.platform.unix.freebsd.Libc;
import oshi.jna.platform.unix.freebsd.Libc.CpTime;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * A CPU
 *
 * @author widdis[at]gmail[dot]com
 */
public class FreeBsdCentralProcessor extends AbstractCentralProcessor {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdCentralProcessor.class);

    private static final Pattern CPUMASK = Pattern.compile(".*<cpu\\s.*mask=\"(?:0x)?(\\p{XDigit}+)\".*>.*</cpu>.*");

    private static final Pattern CPUINFO = Pattern
            .compile("Origin=\"([^\"]*)\".*Id=(\\S+).*Family=(\\S+).*Model=(\\S+).*Stepping=(\\S+).*");
    private static final Pattern CPUINFO2 = Pattern.compile("Features=(\\S+)<.*");

    private static final long BOOTTIME;
    static {
        Timeval tv = new Timeval();
        if (!BsdSysctlUtil.sysctl("kern.boottime", tv) || tv.tv_sec == 0) {
            // Usually this works. If it doesn't, fall back to text parsing.
            // Boot time will be the first consecutive string of digits.
            BOOTTIME = ParseUtil.parseLongOrDefault(
                    ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                    System.currentTimeMillis() / 1000);
        } else {
            // tv now points to a 128-bit timeval structure for boot time.
            // First 8 bytes are seconds, second 8 bytes are microseconds
            // (we ignore)
            BOOTTIME = tv.tv_sec;
        }
    }

    /**
     * Create a Processor
     */
    public FreeBsdCentralProcessor() {
        super();
        // Initialize class variables
        initVars();

        LOG.debug("Initialized Processor");
    }

    private void initVars() {
        setName(BsdSysctlUtil.sysctl("hw.model", ""));
        // This is apparently the only reliable source for this stuff on
        // FreeBSD...
        long processorID = 0L;
        List<String> cpuInfo = FileUtil.readFile("/var/run/dmesg.boot");
        for (String line : cpuInfo) {
            line = line.trim();
            // Prefer hw.model to this one
            if (line.startsWith("CPU:") && getName().isEmpty()) {
                setName(line.replace("CPU:", "").trim());
            } else if (line.startsWith("Origin=")) {
                Matcher m = CPUINFO.matcher(line);
                if (m.matches()) {
                    setVendor(m.group(1));
                    processorID |= Long.decode(m.group(2));
                    setFamily(Integer.decode(m.group(3)).toString());
                    setModel(Integer.decode(m.group(4)).toString());
                    setStepping(Integer.decode(m.group(5)).toString());
                }
            } else if (line.startsWith("Features=")) {
                Matcher m = CPUINFO2.matcher(line);
                if (m.matches()) {
                    processorID |= Long.decode(m.group(1)) << 32;
                }
                // No further interest in this file
                break;
            }
        }
        setCpu64(ExecutingCommand.getFirstAnswer("uname -m").trim().contains("64"));
        setProcessorID(getProcessorID(processorID));
    }

    /**
     * Updates logical and physical processor/package counts
     */
    @Override
    protected LogicalProcessor[] initProcessorCounts() {
        // Get number of CPUs
        this.logicalProcessorCount = BsdSysctlUtil.sysctl("hw.ncpu", 1);
        // Force at least one processor
        if (this.logicalProcessorCount < 1) {
            LOG.error("Couldn't find logical processor count. Assuming 1.");
            this.logicalProcessorCount = 1;
        }
        LogicalProcessor[] logProcs = new LogicalProcessor[this.logicalProcessorCount];
        for (int i = 0; i < logProcs.length; i++) {
            logProcs[i] = new LogicalProcessor();
            logProcs[i].setProcessorNumber(i);
        }

        parseTopology(logProcs);

        // Force at least one processor
        if (this.physicalProcessorCount < 1) {
            // We never found a group level 3; all logical processors are
            // physical
            this.physicalProcessorCount = this.logicalProcessorCount;
            for (int i = 0; i < logProcs.length; i++) {
                logProcs[i].setPhysicalProcessorNumber(i);
            }
        }
        if (this.physicalPackageCount < 1) {
            // We never found a group level 2; assume one package
            this.physicalPackageCount = 1;
        }
        return logProcs;
    }

    private void parseTopology(LogicalProcessor[] logProcs) {
        String[] topology = BsdSysctlUtil.sysctl("kern.sched.topology_spec", "").split("\\n|\\r");
        /*-
         * Sample output:
        
        <groups>
        <group level="1" cache-level="0">
         <cpu count="24" mask="ffffff">0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23</cpu>
         <children>
          <group level="2" cache-level="2">
           <cpu count="12" mask="fff">0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11</cpu>
           <children>
            <group level="3" cache-level="1">
             <cpu count="2" mask="3">0, 1</cpu>
             <flags><flag name="THREAD">THREAD group</flag><flag name="SMT">SMT group</flag></flags>
            </group>
        
        * Opens with <groups>
        * <group> level 1 identifies all the processors via bitmask, should only be one
        * <group> level 2 separates by physical package
        * <group> level 3 puts hyperthreads together: if THREAD or SMT or HTT all the CPUs are one physical
        * If there is no level 3, then all logical processors are physical
        */
        int groupLevel = 0;
        for (String topo : topology) {
            if (topo.contains("<group level=")) {
                groupLevel++;
            } else if (topo.contains("</group>")) {
                groupLevel--;
            } else if (topo.contains("<cpu") && (groupLevel == 2 || groupLevel == 3)) {
                // Find <cpu> tag and extract bits
                Matcher m = CPUMASK.matcher(topo);
                if (m.matches()) {
                    // Regex guarantees parsing digits so we won't get a
                    // NumberFormatException
                    assignIds(groupLevel, Long.parseLong(m.group(1), 16), logProcs);
                }
            }
        }
    }

    private void assignIds(int groupLevel, long bitMask, LogicalProcessor[] logProcs) {
        for (int i = 0; i < logProcs.length; i++) {
            if ((bitMask & (1L << i)) > 0) {
                if (groupLevel == 2) {
                    // This group is a physical package
                    logProcs[i].setPhysicalPackageNumber(getPhysicalPackageCount());
                    this.physicalPackageCount++;
                } else { // groupLevel == 3
                    // This group is a physical processor
                    logProcs[i].setPhysicalProcessorNumber(getPhysicalProcessorCount());
                    this.physicalProcessorCount++;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        CpTime cpTime = new CpTime();
        BsdSysctlUtil.sysctl("kern.cp_time", cpTime);
        ticks[TickType.USER.getIndex()] = cpTime.cpu_ticks[Libc.CP_USER];
        ticks[TickType.NICE.getIndex()] = cpTime.cpu_ticks[Libc.CP_NICE];
        ticks[TickType.SYSTEM.getIndex()] = cpTime.cpu_ticks[Libc.CP_SYS];
        ticks[TickType.IRQ.getIndex()] = cpTime.cpu_ticks[Libc.CP_INTR];
        ticks[TickType.IDLE.getIndex()] = cpTime.cpu_ticks[Libc.CP_IDLE];
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] queryCurrentFreq() {
        long freq = BsdSysctlUtil.sysctl("dev.cpu.0.freq", -1L);
        if (freq > 0) {
            // If success, value is in MHz
            freq *= 1_000_000L;
        } else {
            freq = BsdSysctlUtil.sysctl("machdep.tsc_freq", -1L);
        }
        long[] freqs = new long[getLogicalProcessorCount()];
        Arrays.fill(freqs, freq);
        return freqs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long queryMaxFreq() {
        long max = -1L;
        String freqLevels = BsdSysctlUtil.sysctl("dev.cpu.0.freq_levels", "");
        // MHz/Watts pairs like: 2501/32000 2187/27125 2000/24000
        for (String s : ParseUtil.whitespaces.split(freqLevels)) {
            long freq = ParseUtil.parseLongOrDefault(s.split("/")[0], -1L);
            if (max < freq) {
                max = freq;
            }
        }
        if (max > 0) {
            // If success, value is in MHz
            max *= 1_000_000;
        } else {
            max = BsdSysctlUtil.sysctl("machdep.tsc_freq", -1L);
        }
        return max;
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
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[this.logicalProcessorCount][TickType.values().length];

        // Allocate memory for array of CPTime
        long size = new CpTime().size();
        long arraySize = size * this.logicalProcessorCount;
        Pointer p = new Memory(arraySize);
        String name = "kern.cp_times";
        // Fetch
        if (0 != Libc.INSTANCE.sysctlbyname(name, p, new IntByReference((int) arraySize), null, 0)) {
            LOG.error("Failed syctl call: {}, Error code: {}", name, Native.getLastError());
            return ticks;
        }
        // p now points to the data; need to copy each element
        for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
            ticks[cpu][TickType.USER.getIndex()] = p.getLong(size * cpu + Libc.CP_USER * Libc.UINT64_SIZE); // lgtm
            ticks[cpu][TickType.NICE.getIndex()] = p.getLong(size * cpu + Libc.CP_NICE * Libc.UINT64_SIZE); // lgtm
            ticks[cpu][TickType.SYSTEM.getIndex()] = p.getLong(size * cpu + Libc.CP_SYS * Libc.UINT64_SIZE); // lgtm
            ticks[cpu][TickType.IRQ.getIndex()] = p.getLong(size * cpu + Libc.CP_INTR * Libc.UINT64_SIZE); // lgtm
            ticks[cpu][TickType.IDLE.getIndex()] = p.getLong(size * cpu + Libc.CP_IDLE * Libc.UINT64_SIZE); // lgtm
        }
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemUptime() {
        return System.currentTimeMillis() / 1000 - BOOTTIME;
    }

    /**
     * Fetches the ProcessorID from dmidecode (if possible with root
     * permissions), otherwise uses the values from /var/run/dmesg.boot
     *
     * @param processorID
     * @return The ProcessorID string
     */
    private String getProcessorID(long processorID) {
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
        // If we've gotten this far, dmidecode failed. Used the passed-in values
        return String.format("%016X", processorID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getContextSwitches() {
        String name = "vm.stats.sys.v_swtch";
        IntByReference size = new IntByReference(Libc.INT_SIZE);
        Pointer p = new Memory(size.getValue());
        if (0 != Libc.INSTANCE.sysctlbyname(name, p, size, null, 0)) {
            return -1;
        }
        return ParseUtil.unsignedIntToLong(p.getInt(0));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getInterrupts() {
        String name = "vm.stats.sys.v_intr";
        IntByReference size = new IntByReference(Libc.INT_SIZE);
        Pointer p = new Memory(size.getValue());
        if (0 != Libc.INSTANCE.sysctlbyname(name, p, size, null, 0)) {
            return -1;
        }
        return ParseUtil.unsignedIntToLong(p.getInt(0));
    }
}
