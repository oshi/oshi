/*
 * MIT License
 *
 * Copyright (c) 2016-2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static oshi.util.Memoizer.memoize;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;
import com.sun.jna.platform.mac.SystemB;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.ByRef.CloseablePointerByReference;
import oshi.jna.Struct.CloseableHostCpuLoadInfo;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.mac.SysctlUtil;
import oshi.util.tuples.Pair;

/**
 * A CPU.
 */
@ThreadSafe
final class MacCentralProcessor extends AbstractCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MacCentralProcessor.class);

    private static final int ARM_CPUTYPE = 0x0100000C;
    private static final int M1_CPUFAMILY = 0x1b588bb3;
    private static final int M2_CPUFAMILY = 0xda33d83d;

    private final Supplier<String> vendor = memoize(MacCentralProcessor::platformExpert);
    private final boolean isArmCpu = isArmCpu();
    private long efficiencyFrequency = 0L;

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        String cpuName = SysctlUtil.sysctl("machdep.cpu.brand_string", "");
        String cpuVendor;
        String cpuStepping;
        String cpuModel;
        String cpuFamily;
        String processorID;
        // Initial M1 chips said "Apple Processor". Later branding includes M1, M1 Pro,
        // M1 Max, M2, etc. So if it starts with Apple it's M-something.
        if (cpuName.startsWith("Apple")) {
            // Processing an M1 chip
            cpuVendor = vendor.get();
            cpuStepping = "0"; // No correlation yet
            cpuModel = "0"; // No correlation yet
            int type;
            int family;
            if (isArmCpu) {
                type = ARM_CPUTYPE;
                family = cpuName.contains("M2") ? M2_CPUFAMILY : M1_CPUFAMILY;
            } else {
                type = SysctlUtil.sysctl("hw.cputype", 0);
                family = SysctlUtil.sysctl("hw.cpufamily", 0);
            }
            // Translate to output
            cpuFamily = String.format("0x%08x", family);
            // Processor ID is an intel concept but CPU type + family conveys same info
            processorID = String.format("%08x%08x", type, family);
        } else {
            // Processing an Intel chip
            cpuVendor = SysctlUtil.sysctl("machdep.cpu.vendor", "");
            int i = SysctlUtil.sysctl("machdep.cpu.stepping", -1);
            cpuStepping = i < 0 ? "" : Integer.toString(i);
            i = SysctlUtil.sysctl("machdep.cpu.model", -1);
            cpuModel = i < 0 ? "" : Integer.toString(i);
            i = SysctlUtil.sysctl("machdep.cpu.family", -1);
            cpuFamily = i < 0 ? "" : Integer.toString(i);
            long processorIdBits = 0L;
            processorIdBits |= SysctlUtil.sysctl("machdep.cpu.signature", 0);
            processorIdBits |= (SysctlUtil.sysctl("machdep.cpu.feature_bits", 0L) & 0xffffffff) << 32;
            processorID = String.format("%016x", processorIdBits);
        }
        long cpuFreq = isArmCpu ? getNominalFrequency() : SysctlUtil.sysctl("hw.cpufrequency", 0L);
        boolean cpu64bit = SysctlUtil.sysctl("hw.cpu64bit_capable", 0) != 0;

        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    @Override
    protected Pair<List<LogicalProcessor>, List<PhysicalProcessor>> initProcessorCounts() {
        int logicalProcessorCount = SysctlUtil.sysctl("hw.logicalcpu", 1);
        int physicalProcessorCount = SysctlUtil.sysctl("hw.physicalcpu", 1);
        int physicalPackageCount = SysctlUtil.sysctl("hw.packages", 1);
        List<LogicalProcessor> logProcs = new ArrayList<>(logicalProcessorCount);
        Set<Integer> pkgCoreKeys = new HashSet<>();
        for (int i = 0; i < logicalProcessorCount; i++) {
            int coreId = i * physicalProcessorCount / logicalProcessorCount;
            int pkgId = i * physicalPackageCount / logicalProcessorCount;
            logProcs.add(new LogicalProcessor(i, coreId, pkgId));
            pkgCoreKeys.add((pkgId << 16) + coreId);
        }
        Map<Integer, String> compatMap = queryCompatibleStrings();
        List<PhysicalProcessor> physProcs = pkgCoreKeys.stream().sorted().map(k -> {
            String compat = compatMap.getOrDefault(k, "").toLowerCase();
            int efficiency = 0; // default, for E-core icestorm or blizzard
            if (compat.contains("firestorm") || compat.contains("avalanche")) {
                // This is brittle. A better long term solution is to use sysctls
                // hw.perflevel1.physicalcpu: 2
                // hw.perflevel0.physicalcpu: 8
                // Note the 1 and 0 values are reversed from OSHI API definition
                efficiency = 1; // P-core, more performance
            }
            return new PhysicalProcessor(k >> 16, k & 0xffff, efficiency, compat);
        }).collect(Collectors.toList());
        return new Pair<>(logProcs, physProcs);
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        int machPort = SystemB.INSTANCE.mach_host_self();
        try (CloseableHostCpuLoadInfo cpuLoadInfo = new CloseableHostCpuLoadInfo();
                CloseableIntByReference size = new CloseableIntByReference(cpuLoadInfo.size())) {
            if (0 != SystemB.INSTANCE.host_statistics(machPort, SystemB.HOST_CPU_LOAD_INFO, cpuLoadInfo, size)) {
                LOG.error("Failed to get System CPU ticks. Error code: {} ", Native.getLastError());
                return ticks;
            }

            ticks[TickType.USER.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_USER];
            ticks[TickType.NICE.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_NICE];
            ticks[TickType.SYSTEM.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_SYSTEM];
            ticks[TickType.IDLE.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_IDLE];
        }
        // Leave IOWait and IRQ values as 0
        return ticks;
    }

    @Override
    public long[] queryCurrentFreq() {
        long nominalFrequency = getProcessorIdentifier().getVendorFreq();
        if (isArmCpu) {
            Map<Integer, Long> physFreqMap = new HashMap<>();
            getPhysicalProcessors().stream().forEach(p -> physFreqMap.put(p.getPhysicalProcessorNumber(),
                    p.getEfficiency() > 0 ? nominalFrequency : efficiencyFrequency));
            return getLogicalProcessors().stream().map(LogicalProcessor::getPhysicalProcessorNumber)
                    .map(p -> physFreqMap.getOrDefault(p, nominalFrequency)).mapToLong(f -> f).toArray();
        }
        return new long[] { nominalFrequency };
    }

    @Override
    public long queryMaxFreq() {
        if (isArmCpu) {
            return getProcessorIdentifier().getVendorFreq();
        }
        return SysctlUtil.sysctl("hw.cpufrequency_max", getProcessorIdentifier().getVendorFreq());
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
        try (CloseableIntByReference procCount = new CloseableIntByReference();
                CloseablePointerByReference procCpuLoadInfo = new CloseablePointerByReference();
                CloseableIntByReference procInfoCount = new CloseableIntByReference()) {
            if (0 != SystemB.INSTANCE.host_processor_info(machPort, SystemB.PROCESSOR_CPU_LOAD_INFO, procCount,
                    procCpuLoadInfo, procInfoCount)) {
                LOG.error("Failed to update CPU Load. Error code: {}", Native.getLastError());
                return ticks;
            }

            int[] cpuTicks = procCpuLoadInfo.getValue().getIntArray(0, procInfoCount.getValue());
            for (int cpu = 0; cpu < procCount.getValue(); cpu++) {
                int offset = cpu * SystemB.CPU_STATE_MAX;
                ticks[cpu][TickType.USER.getIndex()] = FormatUtil
                        .getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_USER]);
                ticks[cpu][TickType.NICE.getIndex()] = FormatUtil
                        .getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_NICE]);
                ticks[cpu][TickType.SYSTEM.getIndex()] = FormatUtil
                        .getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_SYSTEM]);
                ticks[cpu][TickType.IDLE.getIndex()] = FormatUtil
                        .getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_IDLE]);
            }
        }
        return ticks;
    }

    @Override
    public long queryContextSwitches() {
        // Not available on macOS since at least 10.3.9. Early versions may have
        // provided access to the vmmeter structure using sysctl [CTL_VM, VM_METER] but
        // it now fails (ENOENT) and there is no other reference to it in source code
        return 0L;
    }

    @Override
    public long queryInterrupts() {
        // Not available on macOS since at least 10.3.9. Early versions may have
        // provided access to the vmmeter structure using sysctl [CTL_VM, VM_METER] but
        // it now fails (ENOENT) and there is no other reference to it in source code
        return 0L;
    }

    private static String platformExpert() {
        String manufacturer = null;
        IORegistryEntry platformExpert = IOKitUtil.getMatchingService("IOPlatformExpertDevice");
        if (platformExpert != null) {
            // Get manufacturer from IOPlatformExpertDevice
            byte[] data = platformExpert.getByteArrayProperty("manufacturer");
            if (data != null) {
                manufacturer = Native.toString(data, StandardCharsets.UTF_8);
            }
            platformExpert.release();
        }
        return Util.isBlank(manufacturer) ? "Apple Inc." : manufacturer;
    }

    // Called by initProcessorCount in the constructor
    // These populate the physical processor id strings
    private static Map<Integer, String> queryCompatibleStrings() {
        Map<Integer, String> compatibleStrMap = new HashMap<>();
        // All CPUs are an IOPlatformDevice
        // Iterate each CPU and save frequency and "compatible" strings
        IOIterator iter = IOKitUtil.getMatchingServices("IOPlatformDevice");
        if (iter != null) {
            IORegistryEntry cpu = iter.next();
            while (cpu != null) {
                if (cpu.getName().toLowerCase().startsWith("cpu")) {
                    int procId = ParseUtil.getFirstIntValue(cpu.getName());
                    // Compatible key is null-delimited C string array in byte array
                    byte[] data = cpu.getByteArrayProperty("compatible");
                    if (data != null) {
                        // Byte array is null delimited
                        compatibleStrMap.put(procId, new String(data, StandardCharsets.UTF_8).replace('\0', ' '));
                    }
                }
                cpu.release();
                cpu = iter.next();
            }
            iter.release();
        }
        return compatibleStrMap;
    }

    // Called when initiating instance variables which occurs after constructor has
    // populated physical processors
    private boolean isArmCpu() {
        // M1 / M2 chips will have an efficiency > 0
        return getPhysicalProcessors().stream().map(PhysicalProcessor::getEfficiency).anyMatch(e -> e > 0);
    }

    private long getNominalFrequency() {
        long maxFreq = 0L;
        IOIterator iter = IOKitUtil.getMatchingServices("AppleARMIODevice");
        if (iter != null) {
            IORegistryEntry device = iter.next();
            while (device != null) {
                if (device.getName().toLowerCase().equals("pmgr")) {
                    byte[] data = device.getByteArrayProperty("voltage-states5-sram");
                    if (data != null) {
                        maxFreq = getMaxFreqFromByteArray(data);
                    }
                    data = device.getByteArrayProperty("voltage-states1-sram");
                    if (data != null) {
                        long otherFreq = getMaxFreqFromByteArray(data);
                        if (otherFreq > maxFreq) {
                            efficiencyFrequency = maxFreq;
                            maxFreq = otherFreq;
                        } else {
                            efficiencyFrequency = otherFreq;
                        }
                    }
                }
                device.release();
                device = iter.next();
            }
            iter.release();
        }
        if (maxFreq > 0L) {
            if (efficiencyFrequency == 0) {
                efficiencyFrequency = maxFreq;
            }
            return maxFreq;
        }
        // Default as per Rosetta
        efficiencyFrequency = 2_400_000_000L;
        return efficiencyFrequency;
    }

    private static long getMaxFreqFromByteArray(byte[] data) {
        long max = 0L;
        for (int offset = 0; offset < data.length - 3; offset += 4) {
            byte[] freqData = Arrays.copyOfRange(data, offset, offset + 4);
            // Parse little-endian
            long freq = ParseUtil.byteArrayToLong(freqData, 4, false);
            if (freq > max) {
                max = freq;
            }
        }
        return max;
    }
}
