/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystem.HAS_UDEV;
import static oshi.util.platform.linux.ProcPath.CPUINFO;
import static oshi.util.platform.linux.ProcPath.MODEL;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.linux.Udev;
import com.sun.jna.platform.linux.Udev.UdevContext;
import com.sun.jna.platform.linux.Udev.UdevDevice;
import com.sun.jna.platform.linux.Udev.UdevEnumerate;
import com.sun.jna.platform.linux.Udev.UdevListEntry;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.Lshw;
import oshi.driver.linux.proc.CpuStat;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.linux.LinuxLibc;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Quartet;
import oshi.util.tuples.Triplet;

/**
 * A CPU as defined in Linux /proc.
 */
@ThreadSafe
final class LinuxCentralProcessor extends AbstractCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxCentralProcessor.class);

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        String cpuVendor = "";
        String cpuName = "";
        String cpuFamily = "";
        String cpuModel = "";
        String cpuStepping = "";
        String processorID;
        long cpuFreq = 0L;
        boolean cpu64bit = false;

        StringBuilder armStepping = new StringBuilder(); // For ARM equivalent
        String[] flags = new String[0];
        List<String> cpuInfo = FileUtil.readFile(CPUINFO);
        for (String line : cpuInfo) {
            String[] splitLine = ParseUtil.whitespacesColonWhitespace.split(line);
            if (splitLine.length < 2) {
                // special case
                if (line.startsWith("CPU architecture: ")) {
                    cpuFamily = line.replace("CPU architecture: ", "").trim();
                }
                continue;
            }
            switch (splitLine[0]) {
            case "vendor_id":
            case "CPU implementer":
                cpuVendor = splitLine[1];
                break;
            case "model name":
            case "Processor": // some ARM chips
                // May be a processor number. Check for a space.
                if (splitLine[1].indexOf(' ') > 0) {
                    cpuName = splitLine[1];
                }
                break;
            case "flags":
                flags = splitLine[1].toLowerCase().split(" ");
                for (String flag : flags) {
                    if ("lm".equals(flag)) {
                        cpu64bit = true;
                        break;
                    }
                }
                break;
            case "stepping":
                cpuStepping = splitLine[1];
                break;
            case "CPU variant":
                if (!armStepping.toString().startsWith("r")) {
                    // CPU variant format always starts with 0x
                    int rev = ParseUtil.parseLastInt(splitLine[1], 0);
                    armStepping.insert(0, "r" + rev);
                }
                break;
            case "CPU revision":
                if (!armStepping.toString().contains("p")) {
                    armStepping.append('p').append(splitLine[1]);
                }
                break;
            case "model":
            case "CPU part":
                cpuModel = splitLine[1];
                break;
            case "cpu family":
                cpuFamily = splitLine[1];
                break;
            case "cpu MHz":
                cpuFreq = ParseUtil.parseHertz(splitLine[1]);
                break;
            default:
                // Do nothing
            }
        }
        if (cpuName.isEmpty()) {
            cpuName = FileUtil.getStringFromFile(MODEL);
        }
        if (cpuName.contains("Hz")) {
            // if Name contains CPU vendor frequency, ignore cpuinfo and use it
            cpuFreq = -1L;
        } else {
            // Try lshw and use it in preference to cpuinfo
            long cpuCapacity = Lshw.queryCpuCapacity();
            if (cpuCapacity > cpuFreq) {
                cpuFreq = cpuCapacity;
            }
        }
        if (cpuStepping.isEmpty()) {
            cpuStepping = armStepping.toString();
        }
        processorID = getProcessorID(cpuVendor, cpuStepping, cpuModel, cpuFamily, flags);
        if (cpuVendor.startsWith("0x")) {
            List<String> lscpu = ExecutingCommand.runNative("lscpu");
            for (String line : lscpu) {
                if (line.startsWith("Architecture:")) {
                    cpuVendor = line.replace("Architecture:", "").trim();
                }
            }
        }
        return new ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit,
                cpuFreq);
    }

    @Override
    protected Triplet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>> initProcessorCounts() {
        Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> topology = HAS_UDEV
                ? readTopologyFromUdev()
                : readTopologyFromSysfs();
        List<LogicalProcessor> logProcs = topology.getA();
        List<ProcessorCache> caches = topology.getB();
        Map<Integer, Integer> coreEfficiencyMap = topology.getC();
        Map<Integer, String> modAliasMap = topology.getD();
        // Failsafe
        if (logProcs.isEmpty()) {
            logProcs.add(new LogicalProcessor(0, 0, 0));
            coreEfficiencyMap.put(0, 0);
        }
        // Sort
        logProcs.sort(Comparator.comparingInt(LogicalProcessor::getProcessorNumber));

        List<PhysicalProcessor> physProcs = coreEfficiencyMap.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    int pkgId = e.getKey() >> 16;
                    int coreId = e.getKey() & 0xffff;
                    return new PhysicalProcessor(pkgId, coreId, e.getValue(), modAliasMap.getOrDefault(e.getKey(), ""));
                }).collect(Collectors.toList());

        return new Triplet<>(logProcs, physProcs, caches);
    }

    private static Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> readTopologyFromUdev() {
        List<LogicalProcessor> logProcs = new ArrayList<>();
        Set<ProcessorCache> caches = new HashSet<>();
        Map<Integer, Integer> coreEfficiencyMap = new HashMap<>();
        Map<Integer, String> modAliasMap = new HashMap<>();
        // Enumerate CPU topology from sysfs via udev
        UdevContext udev = Udev.INSTANCE.udev_new();
        try {
            UdevEnumerate enumerate = udev.enumerateNew();
            try {
                enumerate.addMatchSubsystem("cpu");
                enumerate.scanDevices();
                for (UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                    String syspath = entry.getName(); // /sys/devices/system/cpu/cpuX
                    UdevDevice device = udev.deviceNewFromSyspath(syspath);
                    String modAlias = null;
                    if (device != null) {
                        try {
                            modAlias = device.getPropertyValue("MODALIAS");
                        } finally {
                            device.unref();
                        }
                    }
                    logProcs.add(
                            getLogicalProcessorFromSyspath(syspath, caches, modAlias, coreEfficiencyMap, modAliasMap));
                }
            } finally {
                enumerate.unref();
            }
        } finally {
            udev.unref();
        }
        return new Quartet<>(logProcs, orderedProcCaches(caches), coreEfficiencyMap, modAliasMap);
    }

    private static Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> readTopologyFromSysfs() {
        List<LogicalProcessor> logProcs = new ArrayList<>();
        Set<ProcessorCache> caches = new HashSet<>();
        Map<Integer, Integer> coreEfficiencyMap = new HashMap<>();
        Map<Integer, String> modAliasMap = new HashMap<>();
        String cpuPath = "/sys/devices/system/cpu/";
        try {
            try (Stream<Path> cpuFiles = Files.find(Paths.get(cpuPath), Integer.MAX_VALUE,
                    (path, basicFileAttributes) -> path.toFile().getName().matches("cpu\\d+"))) {
                cpuFiles.forEach(cpu -> {
                    String syspath = cpu.toString(); // /sys/devices/system/cpu/cpuX
                    Map<String, String> uevent = FileUtil.getKeyValueMapFromFile(syspath + "/uevent", "=");
                    String modAlias = uevent.get("MODALIAS");
                    // updates caches as a side-effect
                    logProcs.add(
                            getLogicalProcessorFromSyspath(syspath, caches, modAlias, coreEfficiencyMap, modAliasMap));
                });
            }
        } catch (IOException e) {
            // No udev and no cpu info in sysfs? Bad.
            LOG.warn("Unable to find CPU information in sysfs at path {}", cpuPath);
        }
        return new Quartet<>(logProcs, orderedProcCaches(caches), coreEfficiencyMap, modAliasMap);
    }

    private static LogicalProcessor getLogicalProcessorFromSyspath(String syspath, Set<ProcessorCache> caches,
            String modAlias, Map<Integer, Integer> coreEfficiencyMap, Map<Integer, String> modAliasMap) {
        int processor = ParseUtil.getFirstIntValue(syspath);
        int coreId = FileUtil.getIntFromFile(syspath + "/topology/core_id");
        int pkgId = FileUtil.getIntFromFile(syspath + "/topology/physical_package_id");
        int pkgCoreKey = (pkgId << 16) + coreId;
        // The cpu_capacity value may not exist, this will just store 0
        coreEfficiencyMap.put(pkgCoreKey, FileUtil.getIntFromFile(syspath + "/cpu_capacity"));
        if (!Util.isBlank(modAlias)) {
            modAliasMap.put(pkgCoreKey, modAlias);
        }
        int nodeId = 0;
        final String nodePrefix = syspath + "/node";
        try (Stream<Path> path = Files.list(Paths.get(syspath))) {
            Optional<Path> first = path.filter(p -> p.toString().startsWith(nodePrefix)).findFirst();
            if (first.isPresent()) {
                nodeId = ParseUtil.getFirstIntValue(first.get().getFileName().toString());
            }
        } catch (IOException e) {
            // ignore
        }
        final String cachePath = syspath + "/cache";
        final String indexPrefix = cachePath + "/index";
        try (Stream<Path> path = Files.list(Paths.get(cachePath))) {
            path.filter(p -> p.toString().startsWith(indexPrefix)).forEach(c -> {
                int level = FileUtil.getIntFromFile(c + "/level"); // 1
                Type type = parseCacheType(FileUtil.getStringFromFile(c + "/type")); // Data
                int associativity = FileUtil.getIntFromFile(c + "/ways_of_associativity"); // 8
                int lineSize = FileUtil.getIntFromFile(c + "/coherency_line_size"); // 64
                long size = ParseUtil.parseDecimalMemorySizeToBinary(FileUtil.getStringFromFile(c + "/size")); // 32K
                caches.add(new ProcessorCache(level, associativity, lineSize, size, type));
            });
        } catch (IOException e) {
            // ignore
        }
        return new LogicalProcessor(processor, coreId, pkgId, nodeId);
    }

    private static ProcessorCache.Type parseCacheType(String type) {
        try {
            return ProcessorCache.Type.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ProcessorCache.Type.UNIFIED;
        }
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        // convert the Linux Jiffies to Milliseconds.
        long[] ticks = CpuStat.getSystemCpuLoadTicks();
        // In rare cases, /proc/stat reading fails. If so, try again.
        if (LongStream.of(ticks).sum() == 0) {
            ticks = CpuStat.getSystemCpuLoadTicks();
        }
        long hz = LinuxOperatingSystem.getHz();
        for (int i = 0; i < ticks.length; i++) {
            ticks[i] = ticks[i] * 1000L / hz;
        }
        return ticks;
    }

    @Override
    public long[] queryCurrentFreq() {
        long[] freqs = new long[getLogicalProcessorCount()];
        // Attempt to fill array from cpu-freq source
        long max = 0L;
        UdevContext udev = Udev.INSTANCE.udev_new();
        try {
            UdevEnumerate enumerate = udev.enumerateNew();
            try {
                enumerate.addMatchSubsystem("cpu");
                enumerate.scanDevices();
                for (UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                    String syspath = entry.getName(); // /sys/devices/system/cpu/cpuX
                    int cpu = ParseUtil.getFirstIntValue(syspath);
                    if (cpu >= 0 && cpu < freqs.length) {
                        freqs[cpu] = FileUtil.getLongFromFile(syspath + "/cpufreq/scaling_cur_freq");
                        if (freqs[cpu] == 0) {
                            freqs[cpu] = FileUtil.getLongFromFile(syspath + "/cpufreq/cpuinfo_cur_freq");
                        }
                    }
                    if (max < freqs[cpu]) {
                        max = freqs[cpu];
                    }
                }
                if (max > 0L) {
                    // If successful, array is filled with values in KHz.
                    for (int i = 0; i < freqs.length; i++) {
                        freqs[i] *= 1000L;
                    }
                    return freqs;
                }
            } finally {
                enumerate.unref();
            }
        } finally {
            udev.unref();
        }
        // If unsuccessful, try from /proc/cpuinfo
        Arrays.fill(freqs, -1);
        List<String> cpuInfo = FileUtil.readFile(CPUINFO);
        int proc = 0;
        for (String s : cpuInfo) {
            if (s.toLowerCase().contains("cpu mhz")) {
                freqs[proc] = Math.round(ParseUtil.parseLastDouble(s, 0d) * 1_000_000d);
                if (++proc >= freqs.length) {
                    break;
                }
            }
        }
        return freqs;
    }

    @Override
    public long queryMaxFreq() {
        long max = Arrays.stream(this.getCurrentFreq()).max().orElse(-1L);
        // Max of current freq, if populated, is in units of Hz, convert to kHz
        if (max > 0) {
            max /= 1000L;
        }
        // Iterating CPUs only gets the existing policy, so we need to iterate the
        // policy directories to find the system-wide policy max
        UdevContext udev = Udev.INSTANCE.udev_new();
        try {
            UdevEnumerate enumerate = udev.enumerateNew();
            try {
                enumerate.addMatchSubsystem("cpu");
                enumerate.scanDevices();
                // Find the parent directory of cpuX paths
                // We only need the first one of the iteration
                UdevListEntry entry = enumerate.getListEntry();
                if (entry != null) {
                    String syspath = entry.getName(); // /sys/devices/system/cpu/cpu0
                    String cpuFreqPath = syspath.substring(0, syspath.lastIndexOf(File.separatorChar)) + "/cpuFreq";
                    String policyPrefix = cpuFreqPath + "/policy";
                    try (Stream<Path> path = Files.list(Paths.get(cpuFreqPath))) {
                        Optional<Long> maxPolicy = path.filter(p -> p.toString().startsWith(policyPrefix)).map(p -> {
                            long freq = FileUtil.getLongFromFile(p.toString() + "/scaling_max_freq");
                            if (freq == 0) {
                                freq = FileUtil.getLongFromFile(p.toString() + "/cpuinfo_max_freq");
                            }
                            return freq;
                        }).max(Long::compare);
                        if (maxPolicy.isPresent() && max < maxPolicy.get()) {
                            max = maxPolicy.get();
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            } finally {
                enumerate.unref();
            }
        } finally {
            udev.unref();
        }
        if (max == 0L) {
            return -1L;
        }
        // If successful, value is in KHz.
        max *= 1000L;
        // Cpufreq result assumes intel pstates and is unreliable for AMD processors.
        // Check lshw as a backup
        long lshwMax = Lshw.queryCpuCapacity();
        return lshwMax > max ? lshwMax : max;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = LinuxLibc.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            for (int i = Math.max(retval, 0); i < average.length; i++) {
                average[i] = -1d;
            }
        }
        return average;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = CpuStat.getProcessorCpuLoadTicks(getLogicalProcessorCount());
        // In rare cases, /proc/stat reading fails. If so, try again.
        // In theory we should check all of them, but on failure we can expect all 0's
        // so we only need to check for processor 0
        if (LongStream.of(ticks[0]).sum() == 0) {
            ticks = CpuStat.getProcessorCpuLoadTicks(getLogicalProcessorCount());
        }
        // convert the Linux Jiffies to Milliseconds.
        long hz = LinuxOperatingSystem.getHz();
        for (int i = 0; i < ticks.length; i++) {
            for (int j = 0; j < ticks[i].length; j++) {
                ticks[i][j] = ticks[i][j] * 1000L / hz;
            }
        }
        return ticks;
    }

    /**
     * Fetches the ProcessorID from dmidecode (if possible with root permissions), the cpuid command (if installed) or
     * by encoding the stepping, model, family, and feature flags.
     *
     * @param vendor   The vendor
     * @param stepping The stepping
     * @param model    The model
     * @param family   The family
     * @param flags    The flags
     * @return The Processor ID string
     */
    private static String getProcessorID(String vendor, String stepping, String model, String family, String[] flags) {
        boolean procInfo = false;
        String marker = "Processor Information";
        for (String checkLine : ExecutingCommand.runNative("dmidecode -t 4")) {
            if (!procInfo && checkLine.contains(marker)) {
                marker = "ID:";
                procInfo = true;
            } else if (procInfo && checkLine.contains(marker)) {
                return checkLine.split(marker)[1].trim();
            }
        }
        // If we've gotten this far, dmidecode failed. Try cpuid.
        marker = "eax=";
        for (String checkLine : ExecutingCommand.runNative("cpuid -1r")) {
            if (checkLine.contains(marker) && checkLine.trim().startsWith("0x00000001")) {
                String eax = "";
                String edx = "";
                for (String register : ParseUtil.whitespaces.split(checkLine)) {
                    if (register.startsWith("eax=")) {
                        eax = ParseUtil.removeMatchingString(register, "eax=0x");
                    } else if (register.startsWith("edx=")) {
                        edx = ParseUtil.removeMatchingString(register, "edx=0x");
                    }
                }
                return edx + eax;
            }
        }
        // If we've gotten this far, dmidecode failed. Encode arguments
        if (vendor.startsWith("0x")) {
            return createMIDR(vendor, stepping, model, family) + "00000000";
        }
        return createProcessorID(stepping, model, family, flags);
    }

    /**
     * Creates the MIDR, the ARM equivalent of CPUID ProcessorID
     *
     * @param vendor   the CPU implementer
     * @param stepping the "rnpn" variant and revision
     * @param model    the partnum
     * @param family   the architecture
     * @return A 32-bit hex string for the MIDR
     */
    private static String createMIDR(String vendor, String stepping, String model, String family) {
        int midrBytes = 0;
        // Build 32-bit MIDR
        if (stepping.startsWith("r") && stepping.contains("p")) {
            String[] rev = stepping.substring(1).split("p");
            // 3:0 – Revision: last n in rnpn
            midrBytes |= ParseUtil.parseLastInt(rev[1], 0);
            // 23:20 - Variant: first n in rnpn
            midrBytes |= ParseUtil.parseLastInt(rev[0], 0) << 20;
        }
        // 15:4 - PartNum = model
        midrBytes |= ParseUtil.parseLastInt(model, 0) << 4;
        // 19:16 - Architecture = family
        midrBytes |= ParseUtil.parseLastInt(family, 0) << 16;
        // 31:24 - Implementer = vendor
        midrBytes |= ParseUtil.parseLastInt(vendor, 0) << 24;

        return String.format("%08X", midrBytes);
    }

    @Override
    public long queryContextSwitches() {
        return CpuStat.getContextSwitches();
    }

    @Override
    public long queryInterrupts() {
        return CpuStat.getInterrupts();
    }
}
