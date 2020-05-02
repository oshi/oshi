/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native; // NOSONAR
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.mac.SystemB.HostCpuLoadInfo;
import com.sun.jna.platform.mac.SystemB.VMMeter;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtil;

/**
 * A CPU.
 */
@ThreadSafe
final class MacCentralProcessor extends AbstractCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MacCentralProcessor.class);

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        String cpuVendor = SysctlUtil.sysctl("machdep.cpu.vendor", "");
        String cpuName = SysctlUtil.sysctl("machdep.cpu.brand_string", "");
        int i = SysctlUtil.sysctl("machdep.cpu.stepping", -1);
        String cpuStepping = i < 0 ? "" : Integer.toString(i);
        i = SysctlUtil.sysctl("machdep.cpu.model", -1);
        String cpuModel = i < 0 ? "" : Integer.toString(i);
        i = SysctlUtil.sysctl("machdep.cpu.family", -1);
        String cpuFamily = i < 0 ? "" : Integer.toString(i);
        long processorIdBits = 0L;
        processorIdBits |= SysctlUtil.sysctl("machdep.cpu.signature", 0);
        processorIdBits |= (SysctlUtil.sysctl("machdep.cpu.feature_bits", 0L) & 0xffffffff) << 32;
        String processorID = String.format("%016X", processorIdBits);
        boolean cpu64bit = SysctlUtil.sysctl("hw.cpu64bit_capable", 0) != 0;

        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit);
    }

    @Override
    protected List<LogicalProcessor> initProcessorCounts() {
        int logicalProcessorCount = SysctlUtil.sysctl("hw.logicalcpu", 1);
        int physicalProcessorCount = SysctlUtil.sysctl("hw.physicalcpu", 1);
        int physicalPackageCount = SysctlUtil.sysctl("hw.packages", 1);
        List<LogicalProcessor> logProcs = new ArrayList<>(logicalProcessorCount);
        for (int i = 0; i < logicalProcessorCount; i++) {
            logProcs.add(new LogicalProcessor(i, i * physicalProcessorCount / logicalProcessorCount,
                    i * physicalPackageCount / logicalProcessorCount));
        }
        return logProcs;
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        int machPort = SystemB.INSTANCE.mach_host_self();
        HostCpuLoadInfo cpuLoadInfo = new HostCpuLoadInfo();
        if (0 != SystemB.INSTANCE.host_statistics(machPort, SystemB.HOST_CPU_LOAD_INFO, cpuLoadInfo,
                new IntByReference(cpuLoadInfo.size()))) {
            LOG.error("Failed to get System CPU ticks. Error code: {} ", Native.getLastError());
            return ticks;
        }

        ticks[TickType.USER.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_USER];
        ticks[TickType.NICE.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_NICE];
        ticks[TickType.SYSTEM.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_SYSTEM];
        ticks[TickType.IDLE.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_IDLE];
        // Leave IOWait and IRQ values as 0
        return ticks;
    }

    @Override
    public long[] queryCurrentFreq() {
        long[] freqs = new long[getLogicalProcessorCount()];
        Arrays.fill(freqs, SysctlUtil.sysctl("hw.cpufrequency", -1L));
        return freqs;
    }

    @Override
    public long queryMaxFreq() {
        return SysctlUtil.sysctl("hw.cpufrequency_max", -1L);
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = SystemB.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            Arrays.fill(average, -1d);
        }
        return average;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];

        int machPort = SystemB.INSTANCE.mach_host_self();

        IntByReference procCount = new IntByReference();
        PointerByReference procCpuLoadInfo = new PointerByReference();
        IntByReference procInfoCount = new IntByReference();
        if (0 != SystemB.INSTANCE.host_processor_info(machPort, SystemB.PROCESSOR_CPU_LOAD_INFO, procCount,
                procCpuLoadInfo, procInfoCount)) {
            LOG.error("Failed to update CPU Load. Error code: {}", Native.getLastError());
            return ticks;
        }

        int[] cpuTicks = procCpuLoadInfo.getValue().getIntArray(0, procInfoCount.getValue());
        for (int cpu = 0; cpu < procCount.getValue(); cpu++) {
            int offset = cpu * SystemB.CPU_STATE_MAX;
            ticks[cpu][TickType.USER.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_USER]);
            ticks[cpu][TickType.NICE.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_NICE]);
            ticks[cpu][TickType.SYSTEM.getIndex()] = FormatUtil
                    .getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_SYSTEM]);
            ticks[cpu][TickType.IDLE.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_IDLE]);
        }
        return ticks;
    }

    @Override
    public long queryContextSwitches() {
        int machPort = SystemB.INSTANCE.mach_host_self();
        VMMeter vmstats = new VMMeter();
        if (0 != SystemB.INSTANCE.host_statistics(machPort, SystemB.HOST_VM_INFO, vmstats,
                new IntByReference(vmstats.size()))) {
            LOG.error("Failed to update vmstats. Error code: {}", Native.getLastError());
            return -1;
        }
        return ParseUtil.unsignedIntToLong(vmstats.v_swtch);
    }

    @Override
    public long queryInterrupts() {
        int machPort = SystemB.INSTANCE.mach_host_self();
        VMMeter vmstats = new VMMeter();
        if (0 != SystemB.INSTANCE.host_statistics(machPort, SystemB.HOST_VM_INFO, vmstats,
                new IntByReference(vmstats.size()))) {
            LOG.error("Failed to update vmstats. Error code: {}", Native.getLastError());
            return -1;
        }
        return ParseUtil.unsignedIntToLong(vmstats.v_intr);
    }
}
