/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.platform.unix.freebsd;

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
        // Initialize tick arrays
        initTicks();

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
    protected void calculateProcessorCounts() {
        String[] topology = BsdSysctlUtil.sysctl("kern.sched.topology_spec", "").split("\\n|\\r");
        int physMask = 0;
        int virtMask = 0;
        int lastMask = 0;
        int physPackage = 0;
        for (String topo : topology) {
            if (topo.contains("<cpu")) {
                // Find <cpu> tag and extract bits
                Matcher m = CPUMASK.matcher(topo);
                if (m.matches()) {
                    // Add this processor mask to cpus. Regex guarantees parsing
                    lastMask = Integer.parseInt(m.group(1), 16);
                    physMask |= lastMask;
                    virtMask |= lastMask;
                }
            } else if (topo.contains("<flags>")
                    && (topo.contains("HTT") || topo.contains("SMT") || topo.contains("THREAD"))) {
                // These are virtual cpus, remove processor mask from physical
                physMask &= ~lastMask;
            } else if (topo.contains("<group level=\"2\"")) {
                // This is a physical package
                physPackage++;
            }
        }

        this.logicalProcessorCount = Integer.bitCount(virtMask);
        if (this.logicalProcessorCount < 1) {
            LOG.error("Couldn't find logical processor count. Assuming 1.");
            this.logicalProcessorCount = 1;
        }
        this.physicalProcessorCount = Integer.bitCount(physMask);
        if (this.physicalProcessorCount < 1) {
            LOG.error("Couldn't find physical processor count. Assuming 1.");
            this.physicalProcessorCount = 1;
        }
        this.physicalPackageCount = physPackage;
        if (this.physicalPackageCount < 1) {
            LOG.error("Couldn't find physical package count. Assuming 1.");
            this.physicalPackageCount = 1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long[] getSystemCpuLoadTicks() {
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
    public long[][] getProcessorCpuLoadTicks() {
        long[][] ticks = new long[this.logicalProcessorCount][TickType.values().length];

        // Allocate memory for array of CPTime
        int offset = new CpTime().size();
        int size = offset * this.logicalProcessorCount;
        Pointer p = new Memory(size);
        String name = "kern.cp_times";
        // Fetch
        if (0 != Libc.INSTANCE.sysctlbyname(name, p, new IntByReference(size), null, 0)) {
            LOG.error("Failed syctl call: {}, Error code: {}", name, Native.getLastError());
            return ticks;
        }
        // p now points to the data; need to copy each element
        for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
            ticks[cpu][TickType.USER.getIndex()] = p.getLong(offset * cpu + (long) Libc.CP_USER * Libc.UINT64_SIZE); // lgtm[java/evaluation-to-constant]
            ticks[cpu][TickType.NICE.getIndex()] = p.getLong(offset * cpu + (long) Libc.CP_NICE * Libc.UINT64_SIZE);
            ticks[cpu][TickType.SYSTEM.getIndex()] = p.getLong(offset * cpu + (long) Libc.CP_SYS * Libc.UINT64_SIZE);
            ticks[cpu][TickType.IRQ.getIndex()] = p.getLong(offset * cpu + (long) Libc.CP_INTR * Libc.UINT64_SIZE);
            ticks[cpu][TickType.IDLE.getIndex()] = p.getLong(offset * cpu + (long) Libc.CP_IDLE * Libc.UINT64_SIZE);
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
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public String getSystemSerialNumber() {
        return new FreeBsdComputerSystem().getSerialNumber();
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
