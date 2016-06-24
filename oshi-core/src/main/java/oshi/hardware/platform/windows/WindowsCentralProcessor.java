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
package oshi.hardware.platform.windows;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION;
import com.sun.jna.platform.win32.WinReg;

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.windows.Kernel32;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;

/**
 * A CPU as defined in Windows registry.
 * 
 * @author dblock[at]dblock[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 * @author widdis[at]gmail[dot]com
 */
public class WindowsCentralProcessor extends AbstractCentralProcessor {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsCentralProcessor.class);

    // Compare WMI ticks to GetSystemTimes to determine conversion
    private static final long TICKS_PER_MILLISECOND;
    static {
        // Get total time = Kernel (includes idle) + User
        WinBase.FILETIME lpIdleTime = new WinBase.FILETIME();
        WinBase.FILETIME lpKernelTime = new WinBase.FILETIME();
        WinBase.FILETIME lpUserTime = new WinBase.FILETIME();
        if (!Kernel32.INSTANCE.GetSystemTimes(lpIdleTime, lpKernelTime, lpUserTime)) {
            LOG.error("Failed to init system idle/kernel/user times. Error code: " + Native.getLastError());
        }
        // Units are in 100-ns, divide by 10000 for ms
        long mSec = (WinBase.FILETIME.dateToFileTime(lpKernelTime.toDate())
                + WinBase.FILETIME.dateToFileTime(lpUserTime.toDate())) / 10000L;
        // Get same info from WMI
        Map<String, List<String>> wmiTicks = WmiUtil.selectStringsFrom(null,
                "Win32_PerfRawData_Counters_ProcessorInformation",
                "PercentIdleTime,PercentPrivilegedTime,PercentUserTime", "WHERE Name=\"_Total\"");
        long ticks = 0L;
        if (wmiTicks.get("PercentIdleTime").size() > 0) {
            ticks = ParseUtil.parseLongOrDefault(wmiTicks.get("PercentIdleTime").get(0), 0L)
                    + ParseUtil.parseLongOrDefault(wmiTicks.get("PercentPrivilegedTime").get(0), 0L)
                    + ParseUtil.parseLongOrDefault(wmiTicks.get("PercentUserTime").get(0), 0L);
        }
        // Divide
        TICKS_PER_MILLISECOND = ticks / mSec;
        LOG.debug("Ticks per millisecond: {}", TICKS_PER_MILLISECOND);
    }

    /**
     * Create a Processor
     */
    public WindowsCentralProcessor() {
        // Initialize class variables
        initVars();
        // Initialize tick arrays
        initTicks();

        LOG.debug("Initialized Processor");
    }

    /**
     * Initializes Class variables
     */
    private void initVars() {
        final String cpuRegistryRoot = "HARDWARE\\DESCRIPTION\\System\\CentralProcessor";
        String[] processorIds = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryRoot);
        if (processorIds.length > 0) {
            String cpuRegistryPath = cpuRegistryRoot + "\\" + processorIds[0];
            this.setVendor(Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
                    "VendorIdentifier"));
            this.setName(Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
                    "ProcessorNameString"));
            this.setIdentifier(
                    Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath, "Identifier"));
        }
        SYSTEM_INFO sysinfo = new SYSTEM_INFO();
        Kernel32.INSTANCE.GetNativeSystemInfo(sysinfo);
        if (sysinfo.processorArchitecture.pi.wProcessorArchitecture.intValue() == 9 // PROCESSOR_ARCHITECTURE_AMD64
                || sysinfo.processorArchitecture.pi.wProcessorArchitecture.intValue() == 6) { // PROCESSOR_ARCHITECTURE_IA64
            this.setCpu64(true);
        } else if (sysinfo.processorArchitecture.pi.wProcessorArchitecture.intValue() == 0) { // PROCESSOR_ARCHITECTURE_INTEL
            this.setCpu64(false);
        }
    }

    /**
     * Updates logical and physical processor counts from /proc/cpuinfo
     */
    protected void calculateProcessorCounts() {
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
        // IOwait:
        // Avg. Disk sec/Transfer raw value is cumulative ticks spent
        // transferring. Divide result by ticks per ms to get ms
        ticks[TickType.IOWAIT.getIndex()] = FormatUtil.getUnsignedInt(WmiUtil.selectUint32From(null,
                "Win32_PerfRawData_PerfDisk_LogicalDisk", "AvgDisksecPerTransfer", "WHERE Name=\"_Total\"").intValue())
                / TICKS_PER_MILLISECOND;

        // IRQ:
        // Percent time raw value is cumulative 100NS-ticks
        // Divide by 10000 to get milliseconds
        Map<String, List<String>> irq = WmiUtil.selectStringsFrom(null,
                "Win32_PerfRawData_Counters_ProcessorInformation", "PercentInterruptTime,PercentDPCTime",
                "WHERE Name=\"_Total\"");
        if (irq.get("PercentInterruptTime").size() > 0) {
            ticks[TickType.IRQ.getIndex()] = ParseUtil.parseLongOrDefault(irq.get("PercentInterruptTime").get(0), 0L)
                    / 10000L;
            ticks[TickType.SOFTIRQ.getIndex()] = ParseUtil.parseLongOrDefault(irq.get("PercentDPCTime").get(0), 0L)
                    / 10000L;
        }

        // Units are in 100-ns, divide by 10000 for ms
        // TODO: Change to lp*Time.toDWordLong.longValue() with JNA 4.3
        ticks[TickType.IDLE.getIndex()] = WinBase.FILETIME.dateToFileTime(lpIdleTime.toDate()) / 10000L;
        ticks[TickType.SYSTEM.getIndex()] = WinBase.FILETIME.dateToFileTime(lpKernelTime.toDate()) / 10000L
                - ticks[TickType.IDLE.getIndex()];
        ticks[TickType.USER.getIndex()] = WinBase.FILETIME.dateToFileTime(lpUserTime.toDate()) / 10000L;
        // Additional decrement to avoid double counting in the total array
        ticks[TickType.IDLE.getIndex()] -= ticks[TickType.IOWAIT.getIndex()];
        ticks[TickType.SYSTEM.getIndex()] -= (ticks[TickType.IRQ.getIndex()] + ticks[TickType.SOFTIRQ.getIndex()]);
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1) {
            throw new IllegalArgumentException("Must include at least one element.");
        }
        if (nelem > 3) {
            LOG.warn("Max elements of SystemLoadAverage is 3. " + nelem + " specified. Ignoring extra.");
            nelem = 3;
        }
        double[] average = new double[nelem];
        // TODO: If Windows ever actually implements a laod average for 1/5/15,
        // return it
        for (int i = 0; i < average.length; i++) {
            average[i] = -1;
        }
        return average;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[][] getProcessorCpuLoadTicks() {
        long[][] ticks = new long[this.logicalProcessorCount][TickType.values().length];
        // Percent time raw value is cumulative 100NS-ticks
        // Divide by 10000 to get milliseconds
        Map<String, List<String>> wmiTicks = WmiUtil.selectStringsFrom(null,
                "Win32_PerfRawData_Counters_ProcessorInformation",
                "Name,PercentIdleTime,PercentPrivilegedTime,PercentUserTime,PercentInterruptTime,PercentDPCTime",
                "WHERE NOT Name LIKE \"%_Total\"");
        for (int index = 0; index < wmiTicks.get("Name").size(); index++) {
            // It would be too easy if the WMI order matched logical processors
            // but alas, it goes "0,3"; "0,2"; "0,1"; "0,0". So let's do it
            // right and actually string match the name. The first 0 will be
            // there unless we're dealing with NUMA nodes
            for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
                String name = "0," + cpu;
                if (wmiTicks.get("Name").get(index).equals(name)) {
                    // Skipping nice and IOWait, they'll stay 0
                    ticks[cpu][TickType.USER.getIndex()] = ParseUtil
                            .parseLongOrDefault(wmiTicks.get("PercentUserTime").get(index), 0L) / TICKS_PER_MILLISECOND;
                    ticks[cpu][TickType.SYSTEM.getIndex()] = ParseUtil.parseLongOrDefault(
                            wmiTicks.get("PercentPrivilegedTime").get(index), 0L) / TICKS_PER_MILLISECOND;
                    ticks[cpu][TickType.IDLE.getIndex()] = ParseUtil
                            .parseLongOrDefault(wmiTicks.get("PercentIdleTime").get(index), 0L) / TICKS_PER_MILLISECOND;
                    ticks[cpu][TickType.IRQ.getIndex()] = ParseUtil.parseLongOrDefault(
                            wmiTicks.get("PercentInterruptTime").get(index), 0L) / TICKS_PER_MILLISECOND;
                    ticks[cpu][TickType.SOFTIRQ.getIndex()] = ParseUtil
                            .parseLongOrDefault(wmiTicks.get("PercentDPCTime").get(index), 0L) / TICKS_PER_MILLISECOND;
                    // Additional decrement to avoid double counting in the
                    // total array
                    ticks[cpu][TickType.SYSTEM.getIndex()] -= (ticks[cpu][TickType.IRQ.getIndex()]
                            + ticks[cpu][TickType.SOFTIRQ.getIndex()]);
                    break;
                }
            }
        }
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemUptime() {
        return Kernel32.INSTANCE.GetTickCount64() / 1000L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSystemSerialNumber() {
        if (this.cpuSerialNumber == null) {
            // This should always work
            this.cpuSerialNumber = WmiUtil.selectStringFrom(null, "Win32_BIOS", "SerialNumber", null);
            // If the above doesn't work, this might
            if (this.cpuSerialNumber.equals("")) {
                this.cpuSerialNumber = WmiUtil.selectStringFrom(null, "Win32_Csproduct", "IdentifyingNumber", null);
            }
            if (this.cpuSerialNumber.equals("")) {
                this.cpuSerialNumber = "unknown";
            }
        }
        return this.cpuSerialNumber;
    }
}