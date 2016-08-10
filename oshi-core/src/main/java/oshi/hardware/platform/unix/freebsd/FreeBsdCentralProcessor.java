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
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
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
import oshi.jna.platform.unix.LibC;
import oshi.jna.platform.unix.LibC.CpTime;
import oshi.jna.platform.unix.LibC.Timeval;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
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
            .compile("Origin=\"([^\"]*)\".*Family=(\\S+).*Model=(\\S+).*Stepping=(\\S+).*");

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
                    setFamily(Integer.decode(m.group(2)).toString());
                    setModel(Integer.decode(m.group(3)).toString());
                    setStepping(Integer.decode(m.group(4)).toString());
                }
                // No further interest in this file
                break;
            }
        }
        setCpu64(ExecutingCommand.getFirstAnswer("uname -m").trim().contains("64"));
    }

    /**
     * Updates logical and physical processor counts from psrinfo
     */
    @Override
    protected void calculateProcessorCounts() {
        String[] topology = BsdSysctlUtil.sysctl("kern.sched.topology_spec", "").split("\\n|\\r");
        long physMask = 0;
        long virtMask = 0;
        long lastMask = 0;
        for (String topo : topology) {
            if (topo.contains("<cpu")) {
                // Find <cpu> tag and extract bits
                Matcher m = CPUMASK.matcher(topo);
                if (m.matches()) {
                    // Add this processor mask to cpus. Regex guarantees parsing
                    lastMask = Long.parseLong(m.group(1), 16);
                    physMask |= lastMask;
                    virtMask |= lastMask;
                }
            } else if (topo.contains("<flags>")
                    && (topo.contains("HTT") || topo.contains("SMT") || topo.contains("THREAD"))) {
                // These are virtual cpus, remove processor mask from physical
                physMask &= ~lastMask;
            }
        }

        this.logicalProcessorCount = Long.bitCount(virtMask);
        this.physicalProcessorCount = Long.bitCount(physMask);

        if (this.logicalProcessorCount < 1) {
            LOG.error("Couldn't find logical processor count. Assuming 1.");
            this.logicalProcessorCount = 1;
        }
        if (this.physicalProcessorCount < 1) {
            LOG.error("Couldn't find physical processor count. Assuming 1.");
            this.physicalProcessorCount = 1;
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
        ticks[TickType.USER.getIndex()] = cpTime.cpu_ticks[LibC.CP_USER];
        ticks[TickType.NICE.getIndex()] = cpTime.cpu_ticks[LibC.CP_NICE];
        ticks[TickType.SYSTEM.getIndex()] = cpTime.cpu_ticks[LibC.CP_SYS];
        ticks[TickType.IRQ.getIndex()] = cpTime.cpu_ticks[LibC.CP_INTR];
        ticks[TickType.IDLE.getIndex()] = cpTime.cpu_ticks[LibC.CP_IDLE];
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
        int retval = LibC.INSTANCE.getloadavg(average, nelem);
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
        if (0 != LibC.INSTANCE.sysctlbyname(name, p, new IntByReference(size), null, 0)) {
            LOG.error("Failed syctl call: {}, Error code: {}", name, Native.getLastError());
            return ticks;
        }
        // p now points to the data; need to copy each element
        for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
            ticks[cpu][TickType.USER.getIndex()] = p.getLong((long) offset * cpu + LibC.CP_USER * LibC.UINT64_SIZE);
            ticks[cpu][TickType.NICE.getIndex()] = p.getLong((long) offset * cpu + LibC.CP_NICE * LibC.UINT64_SIZE);
            ticks[cpu][TickType.SYSTEM.getIndex()] = p.getLong((long) offset * cpu + LibC.CP_SYS * LibC.UINT64_SIZE);
            ticks[cpu][TickType.IRQ.getIndex()] = p.getLong((long) offset * cpu + LibC.CP_INTR * LibC.UINT64_SIZE);
            ticks[cpu][TickType.IDLE.getIndex()] = p.getLong((long) offset * cpu + LibC.CP_IDLE * LibC.UINT64_SIZE);
        }
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemUptime() {
        Timeval tv = new Timeval();
        if (!BsdSysctlUtil.sysctl("kern.boottime", tv)) {
            return 0L;
        }
        // tv now points to a 16-bit timeval structure for boot time.
        // First 8 bytes are seconds, second 8 bytes are microseconds (ignore)
        return System.currentTimeMillis() / 1000 - tv.tv_sec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSystemSerialNumber() {
        if (this.cpuSerialNumber == null) {
            // If root privileges this will work
            String marker = "Serial Number:";
            for (String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
                if (checkLine.contains(marker)) {
                    this.cpuSerialNumber = checkLine.split(marker)[1].trim();
                    break;
                }
            }
            // if lshal command available (hald must be manually installed)
            if (this.cpuSerialNumber == null) {
                marker = "system.hardware.serial =";
                for (String checkLine : ExecutingCommand.runNative("lshal")) {
                    if (checkLine.contains(marker)) {
                        String[] temp = checkLine.split(marker)[1].split("'");
                        // Format: '12345' (string)
                        this.cpuSerialNumber = temp.length > 0 ? temp[1] : null;
                        break;
                    }
                }
            }
            if (this.cpuSerialNumber == null) {
                this.cpuSerialNumber = "unknown";
            }
        }
        return this.cpuSerialNumber;
    }
}
