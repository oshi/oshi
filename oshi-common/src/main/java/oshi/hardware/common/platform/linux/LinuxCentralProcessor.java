/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static oshi.util.linux.ProcPath.CPUINFO;
import static oshi.util.linux.ProcPath.LOADAVG;
import static oshi.util.linux.ProcPath.MODEL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.ExceptionUtil;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.driver.linux.Lshw;
import oshi.util.driver.linux.proc.CpuInfo;
import oshi.util.driver.linux.proc.CpuStat;
import oshi.util.linux.SysPath;
import oshi.util.tuples.Quartet;
import oshi.util.tuples.Triplet;

/**
 * A CPU as defined in Linux /proc.
 */
@ThreadSafe
public abstract class LinuxCentralProcessor extends AbstractCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxCentralProcessor.class);

    private final long hz;

    /**
     * LinuxCentralProcessor.
     *
     * @param hz the hz
     */
    protected LinuxCentralProcessor(long hz) {
        this.hz = hz;
    }

    @Override
    protected ProcessorIdentifier queryProcessorId() {
        CpuInfoIdentity id = parseProcessorIdFromCpuinfo(FileUtil.readFile(CPUINFO));
        String cpuName = id.name();
        if (cpuName.isEmpty()) {
            cpuName = FileUtil.getStringFromFile(MODEL).trim();
        }
        long cpuFreq = id.freq();
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
        String processorID = getProcessorID(id.vendor(), id.stepping(), id.model(), id.family(), id.flags(),
                queryHwcap());
        String cpuVendor = id.vendor();
        String cpuModel = id.model();
        if (cpuVendor.startsWith("0x") || cpuModel.isEmpty() || cpuName.isEmpty()) {
            Triplet<String, String, String> lscpu = parseLscpuIdentity(ExecutingCommand.runNative("lscpu"), cpuVendor,
                    cpuModel, cpuName);
            cpuVendor = lscpu.getA();
            cpuModel = lscpu.getB();
            cpuName = lscpu.getC();
        }
        return new ProcessorIdentifier(cpuVendor, cpuName, id.family(), cpuModel, id.stepping(), processorID,
                id.cpu64bit(), cpuFreq);
    }

    /** Immutable holder for the identifier fields parsed from {@code /proc/cpuinfo}. Package-private for testing. */
    static final class CpuInfoIdentity {
        private final String vendor;
        private final String name;
        private final String family;
        private final String model;
        private final String stepping;
        private final String[] flags;
        private final long freq;
        private final boolean cpu64bit;

        CpuInfoIdentity(String vendor, String name, String family, String model, String stepping, String[] flags,
                long freq, boolean cpu64bit) {
            this.vendor = vendor;
            this.name = name;
            this.family = family;
            this.model = model;
            this.stepping = stepping;
            this.flags = flags;
            this.freq = freq;
            this.cpu64bit = cpu64bit;
        }

        String vendor() {
            return vendor;
        }

        String name() {
            return name;
        }

        String family() {
            return family;
        }

        String model() {
            return model;
        }

        String stepping() {
            return stepping;
        }

        String[] flags() {
            return flags;
        }

        long freq() {
            return freq;
        }

        boolean cpu64bit() {
            return cpu64bit;
        }
    }

    /**
     * Parses the processor identifier fields from {@code /proc/cpuinfo} content. Package-private for testing.
     *
     * @param cpuInfo the lines of {@code /proc/cpuinfo}
     * @return the parsed identifier fields
     */
    static CpuInfoIdentity parseProcessorIdFromCpuinfo(List<String> cpuInfo) {
        String vendor = "";
        String name = "";
        String family = "";
        String model = "";
        String stepping = "";
        String[] flags = new String[0];
        long freq = 0L;
        boolean cpu64bit = false;
        StringBuilder armStepping = new StringBuilder(); // For ARM equivalent
        for (String line : cpuInfo) {
            String[] splitLine = ParseUtil.whitespacesColonWhitespace.split(line);
            if (splitLine.length < 2) {
                // special case
                if (line.startsWith("CPU architecture: ")) {
                    family = line.replace("CPU architecture: ", "").trim();
                }
                continue;
            }
            switch (splitLine[0].toLowerCase(Locale.ROOT)) {
                case "vendor_id":
                case "cpu implementer":
                    vendor = splitLine[1];
                    break;
                case "model name":
                case "processor": // some ARM chips
                    // Ignore processor number
                    if (!splitLine[1].matches("\\d+")) {
                        name = splitLine[1];
                    }
                    break;
                case "flags":
                    flags = splitLine[1].toLowerCase(Locale.ROOT).split(" ");
                    for (String flag : flags) {
                        if ("lm".equals(flag)) {
                            cpu64bit = true;
                            break;
                        }
                    }
                    break;
                case "stepping":
                    stepping = splitLine[1];
                    break;
                case "cpu variant":
                    if (!armStepping.toString().startsWith("r")) {
                        // CPU variant format always starts with 0x
                        int rev = ParseUtil.parseLastInt(splitLine[1], 0);
                        armStepping.insert(0, "r" + rev);
                    }
                    break;
                case "cpu revision":
                    if (!armStepping.toString().contains("p")) {
                        armStepping.append('p').append(splitLine[1]);
                    }
                    break;
                case "model":
                case "cpu part":
                    model = splitLine[1];
                    break;
                case "cpu family":
                    family = splitLine[1];
                    break;
                case "cpu mhz":
                    freq = ParseUtil.parseHertz(splitLine[1]);
                    break;
                default:
                    // Do nothing
            }
        }
        if (stepping.isEmpty()) {
            stepping = armStepping.toString();
        }
        return new CpuInfoIdentity(vendor, name, family, model, stepping, flags, freq, cpu64bit);
    }

    /**
     * Refines the vendor, model, and name from {@code lscpu} output when {@code /proc/cpuinfo} was insufficient.
     * Package-private for testing.
     *
     * @param lscpu  the lines of {@code lscpu} output
     * @param vendor the current CPU vendor
     * @param model  the current CPU model
     * @param name   the current CPU name
     * @return a triplet of the refined (vendor, model, name)
     */
    static Triplet<String, String, String> parseLscpuIdentity(List<String> lscpu, String vendor, String model,
            String name) {
        for (String line : lscpu) {
            if (line.startsWith("Architecture:") && vendor.startsWith("0x")) {
                vendor = line.replace("Architecture:", "").trim();
            } else if (line.startsWith("Vendor ID:")) {
                vendor = line.replace("Vendor ID:", "").trim();
            } else if (line.startsWith("Model name:")) {
                String modelName = line.replace("Model name:", "").trim();
                model = model.isEmpty() ? modelName : model;
                name = name.isEmpty() ? modelName : name;
            }
        }
        return new Triplet<>(vendor, model, name);
    }

    @Override
    protected Quartet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>, List<String>> initProcessorCounts() {
        // Attempt to read from sysfs
        Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> topology = readTopologyWithUdev();
        // This sometimes fails so fall back to CPUID
        if (topology.getA().isEmpty()) {
            topology = readTopologyFromCpuinfo();
        }
        List<LogicalProcessor> logProcs = topology.getA();
        List<ProcessorCache> caches = topology.getB();
        Map<Integer, Integer> coreEfficiencyMap = topology.getC();
        Map<Integer, String> modAliasMap = topology.getD();
        // Failsafe
        if (logProcs.isEmpty()) {
            logProcs.add(new LogicalProcessor(0, 0, 0));
        }
        if (coreEfficiencyMap.isEmpty()) {
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
        List<String> featureFlags = CpuInfo.queryFeatureFlags();
        return new Quartet<>(logProcs, physProcs, caches, featureFlags);
    }

    /**
     * Reads processor topology, using udev to enumerate CPUs when available and falling back to a sysfs directory scan.
     *
     * @return a quartet of logical processors, caches, efficiency map, and modalias map
     */
    protected Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> readTopologyWithUdev() {
        return buildTopology(cpuSyspaths());
    }

    /**
     * Enumerates the sysfs paths of all CPUs (e.g. {@code /sys/devices/system/cpu/cpuN}) via udev, or returns
     * {@code null} if udev is unavailable so the caller can fall back to a sysfs directory scan.
     *
     * @return the CPU syspaths from udev, or {@code null} if udev is unavailable
     */
    protected abstract List<String> enumerateCpuSyspathsViaUdev();

    /**
     * Enumerates the sysfs paths of all CPUs, preferring udev and falling back to a sysfs directory scan.
     *
     * @return the CPU syspaths
     */
    private List<String> cpuSyspaths() {
        List<String> viaUdev = enumerateCpuSyspathsViaUdev();
        return viaUdev != null ? viaUdev : cpuSyspathsFromSysfs(SysPath.CPU);
    }

    /**
     * Enumerates the sysfs paths of all CPUs by scanning a sysfs cpu directory for {@code cpuN} entries.
     *
     * @param cpuPath the sysfs cpu directory to scan
     * @return the CPU syspaths, or an empty list if the path cannot be read
     */
    protected static List<String> cpuSyspathsFromSysfs(String cpuPath) {
        List<String> syspaths = new ArrayList<>();
        // Only immediate children (the cpuN entries); avoid walking each CPU's topology/cache/cpufreq subtree
        try (Stream<Path> cpuFiles = Files.find(Paths.get(cpuPath), 1,
                (path, basicFileAttributes) -> path.toFile().getName().matches("cpu\\d+"))) {
            cpuFiles.forEach(cpu -> syspaths.add(cpu.toString()));
        } catch (IOException e) {
            // No udev and no cpu info in sysfs? Bad.
            LOG.warn("Unable to find CPU information in sysfs at path {}", cpuPath);
        }
        return syspaths;
    }

    static Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> readTopologyFromSysfs(
            String cpuPath) {
        return buildTopology(cpuSyspathsFromSysfs(cpuPath));
    }

    /**
     * Builds processor topology from a list of CPU syspaths, reading each CPU's MODALIAS from its {@code uevent} file.
     *
     * @param syspaths the CPU syspaths (e.g. {@code /sys/devices/system/cpu/cpuN})
     * @return a quartet of logical processors, caches, efficiency map, and modalias map
     */
    static Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> buildTopology(
            List<String> syspaths) {
        List<LogicalProcessor> logProcs = new ArrayList<>();
        Set<ProcessorCache> caches = new HashSet<>();
        Map<Integer, Integer> coreEfficiencyMap = new HashMap<>();
        Map<Integer, String> modAliasMap = new HashMap<>();
        for (String syspath : syspaths) {
            Map<String, String> uevent = FileUtil.getKeyValueMapFromFile(syspath + "/uevent", "=");
            String modAlias = uevent.get("MODALIAS");
            // updates caches as a side-effect
            logProcs.add(getLogicalProcessorFromSyspath(syspath, caches, modAlias, coreEfficiencyMap, modAliasMap));
        }
        return new Quartet<>(logProcs, orderedProcCaches(caches), coreEfficiencyMap, modAliasMap);
    }

    /**
     * getLogicalProcessorFromSyspath.
     *
     * @param syspath           the syspath
     * @param caches            the caches
     * @param modAlias          the modAlias
     * @param coreEfficiencyMap the coreEfficiencyMap
     * @param modAliasMap       the modAliasMap
     * @return a LogicalProcessor for the given syspath
     */
    protected static LogicalProcessor getLogicalProcessorFromSyspath(String syspath, Set<ProcessorCache> caches,
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
        final String nodePrefix = Paths.get(syspath, "node").toString();
        try (Stream<Path> path = Files.list(Paths.get(syspath))) {
            Optional<Path> first = path.filter(p -> p.toString().startsWith(nodePrefix)).findFirst();
            if (first.isPresent()) {
                nodeId = ParseUtil.getFirstIntValue(FileUtil.getFileName(first.get().toString()));
            }
        } catch (IOException e) {
            // ignore
        }
        final String cachePath = Paths.get(syspath, "cache").toString();
        final String indexPrefix = Paths.get(cachePath, "index").toString();
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
        return ExceptionUtil.getOrDefault(() -> ProcessorCache.Type.valueOf(type.toUpperCase(Locale.ROOT)),
                ProcessorCache.Type.UNIFIED);
    }

    private static Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> readTopologyFromCpuinfo() {
        return readTopologyFromCpuinfo(FileUtil.readFile(CPUINFO));
    }

    static Quartet<List<LogicalProcessor>, List<ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> readTopologyFromCpuinfo(
            List<String> procCpu) {
        List<LogicalProcessor> logProcs = new ArrayList<>();
        Set<ProcessorCache> caches = mapCachesFromLscpu();
        Map<Integer, Integer> numaNodeMap = mapNumaNodesFromLscpu();
        Map<Integer, Integer> coreEfficiencyMap = new HashMap<>();

        int currentProcessor = 0;
        int currentCore = 0;
        int currentPackage = 0;

        boolean first = true;
        for (String cpu : procCpu) {
            // Count logical processors
            if (cpu.startsWith("processor")) {
                if (first) {
                    first = false;
                } else {
                    // add from the previous iteration
                    logProcs.add(new LogicalProcessor(currentProcessor, currentCore, currentPackage,
                            numaNodeMap.getOrDefault(currentProcessor, 0)));
                    // Count unique combinations of core id and physical id.
                    coreEfficiencyMap.put((currentPackage << 16) + currentCore, 0);
                }
                // start creating for this iteration
                currentProcessor = ParseUtil.parseLastInt(cpu, 0);
            } else if (cpu.startsWith("core id") || cpu.startsWith("cpu number")) {
                currentCore = ParseUtil.parseLastInt(cpu, 0);
            } else if (cpu.startsWith("physical id")) {
                currentPackage = ParseUtil.parseLastInt(cpu, 0);
            }
        }
        logProcs.add(new LogicalProcessor(currentProcessor, currentCore, currentPackage,
                numaNodeMap.getOrDefault(currentProcessor, 0)));
        coreEfficiencyMap.put((currentPackage << 16) + currentCore, 0);
        return new Quartet<>(logProcs, orderedProcCaches(caches), coreEfficiencyMap, Collections.emptyMap());
    }

    private static Map<Integer, Integer> mapNumaNodesFromLscpu() {
        return mapNumaNodesFromLscpu(ExecutingCommand.runNative("lscpu -p=cpu,node"));
    }

    /**
     * Parse NUMA node mapping from lscpu output.
     *
     * @param lscpu output of {@code lscpu -p=cpu,node}
     * @return a map of logical processor number to NUMA node
     */
    static Map<Integer, Integer> mapNumaNodesFromLscpu(List<String> lscpu) {
        Map<Integer, Integer> numaNodeMap = new HashMap<>();
        // Format:
        // # comment lines starting with #
        // # then comma-delimited cpu,node
        // 0,0
        // 1,0
        for (String line : lscpu) {
            if (!line.startsWith("#")) {
                int pos = line.indexOf(',');
                if (pos > 0 && pos < line.length()) {
                    numaNodeMap.put(ParseUtil.parseIntOrDefault(line.substring(0, pos), 0),
                            ParseUtil.parseIntOrDefault(line.substring(pos + 1), 0));
                }
            }
        }
        return numaNodeMap;
    }

    private static Set<ProcessorCache> mapCachesFromLscpu() {
        return mapCachesFromLscpu(ExecutingCommand.runNative("lscpu -B -C --json"));
    }

    /**
     * Parse processor cache information from lscpu JSON output.
     *
     * @param lscpu output of {@code lscpu -B -C --json}
     * @return a set of processor caches
     */
    static Set<ProcessorCache> mapCachesFromLscpu(List<String> lscpu) {
        Set<ProcessorCache> caches = new HashSet<>();
        int level = 0;
        Type type = null;
        int associativity = 0;
        int lineSize = 0;
        long size = 0L;
        for (String line : lscpu) {
            String s = line.trim();
            if (s.startsWith("}")) {
                // done with this entry, save it
                if (level > 0 && type != null) {
                    caches.add(new ProcessorCache(level, associativity, lineSize, size, type));
                }
                level = 0;
                type = null;
                associativity = 0;
                lineSize = 0;
                size = 0L;
            } else if (s.contains("one-size")) {
                // "one-size": "65536",
                String[] split = ParseUtil.notDigits.split(s);
                if (split.length > 1) {
                    size = ParseUtil.parseLongOrDefault(split[1], 0L);
                }
            } else if (s.contains("ways")) {
                // "ways": null,
                // "ways": 4,
                String[] split = ParseUtil.notDigits.split(s);
                if (split.length > 1) {
                    associativity = ParseUtil.parseIntOrDefault(split[1], 0);
                }
            } else if (s.contains("type")) {
                // "type": "Unified",
                String[] split = s.split("\"");
                if (split.length > 2) {
                    type = parseCacheType(split[split.length - 2]);
                }
            } else if (s.contains("level")) {
                // "level": 3,
                String[] split = ParseUtil.notDigits.split(s);
                if (split.length > 1) {
                    level = ParseUtil.parseIntOrDefault(split[1], 0);
                }
            } else if (s.contains("coherency-size")) {
                // "coherency-size": 64
                String[] split = ParseUtil.notDigits.split(s);
                if (split.length > 1) {
                    lineSize = ParseUtil.parseIntOrDefault(split[1], 0);
                }
            }
        }
        return caches;
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = CpuStat.getSystemCpuLoadTicks();
        // In rare cases, /proc/stat reading fails. If so, try again.
        if (LongStream.of(ticks).sum() == 0) {
            ticks = CpuStat.getSystemCpuLoadTicks();
        }
        for (int i = 0; i < ticks.length; i++) {
            ticks[i] = ticks[i] * 1000L / hz;
        }
        return ticks;
    }

    @Override
    public long[] queryCurrentFreq() {
        long[] freqs = new long[getLogicalProcessorCount()];
        if (queryCurrentFreqFromUdev(freqs)) {
            return freqs;
        }
        // Fall back to /proc/cpuinfo
        Arrays.fill(freqs, -1);
        List<String> cpuInfo = FileUtil.readFile(CPUINFO);
        int proc = 0;
        for (String s : cpuInfo) {
            if (s.toLowerCase(Locale.ROOT).contains("cpu mhz")) {
                freqs[proc] = Math.round(ParseUtil.parseLastDouble(s, 0d) * 1_000_000d);
                if (++proc >= freqs.length) {
                    break;
                }
            }
        }
        return freqs;
    }

    /**
     * Fills the freqs array with per-logical-processor current frequencies in Hz, reading each CPU's cpufreq sysfs
     * files. CPUs are enumerated via udev when available, otherwise via a sysfs directory scan.
     *
     * @param freqs array to fill with per-logical-processor frequencies in Hz
     * @return true if frequencies were successfully read (max &gt; 0)
     */
    protected boolean queryCurrentFreqFromUdev(long[] freqs) {
        long max = 0L;
        for (String syspath : cpuSyspaths()) {
            int cpuIdx = ParseUtil.getFirstIntValue(syspath);
            if (cpuIdx >= 0 && cpuIdx < freqs.length) {
                freqs[cpuIdx] = FileUtil.getLongFromFile(syspath + "/cpufreq/scaling_cur_freq");
                if (freqs[cpuIdx] == 0) {
                    freqs[cpuIdx] = FileUtil.getLongFromFile(syspath + "/cpufreq/cpuinfo_cur_freq");
                }
                if (max < freqs[cpuIdx]) {
                    max = freqs[cpuIdx];
                }
            }
        }
        if (max > 0L) {
            for (int i = 0; i < freqs.length; i++) {
                freqs[i] *= 1000L;
            }
            return true;
        }
        return false;
    }

    @Override
    public long queryMaxFreq() {
        long policyMax = queryMaxFreqFromUdev();
        long lshwMax = Lshw.queryCpuCapacity();
        return LongStream.concat(LongStream.of(policyMax, lshwMax), Arrays.stream(this.getCurrentFreq())).max()
                .orElse(-1L);
    }

    /**
     * Queries the maximum (policy) frequency from the cpufreq sysfs tree, enumerating CPUs via udev when available and
     * otherwise via a sysfs directory scan.
     *
     * @return the maximum frequency in Hz
     */
    protected long queryMaxFreqFromUdev() {
        List<String> syspaths = cpuSyspaths();
        if (!syspaths.isEmpty()) {
            String syspath = syspaths.get(0);
            return queryMaxFreqFromCpuFreqPath(syspath.substring(0, syspath.lastIndexOf('/')) + "/cpufreq");
        }
        return -1L;
    }

    /**
     * Queries the maximum frequency from a cpufreq path.
     *
     * @param cpuFreqPath the cpufreq directory path
     * @return the maximum frequency in Hz
     */
    protected static long queryMaxFreqFromCpuFreqPath(String cpuFreqPath) {
        String policyPrefix = Paths.get(cpuFreqPath, "policy").toString();
        try (Stream<Path> path = Files.list(Paths.get(cpuFreqPath))) {
            Optional<Long> maxPolicy = path.filter(p -> p.toString().startsWith(policyPrefix)).map(p -> {
                long freq = FileUtil.getLongFromFile(p.toString() + "/scaling_max_freq");
                if (freq == 0) {
                    freq = FileUtil.getLongFromFile(p.toString() + "/cpuinfo_max_freq");
                }
                return freq;
            }).max(Long::compare);
            if (maxPolicy.isPresent()) {
                return maxPolicy.get() * 1000L;
            }
        } catch (IOException e) {
            // ignore
        }
        return -1L;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = getloadavgNative(average, nelem);
        if (retval < 0) {
            // Native call unavailable or failed; fall back to /proc/loadavg
            return getSystemLoadAverage(nelem, FileUtil.getStringFromFile(LOADAVG));
        }
        if (retval < nelem) {
            // All-or-nothing: a partial result is more likely a misread than a real load average
            Arrays.fill(average, -1d);
        }
        return average;
    }

    static double[] getSystemLoadAverage(int nelem, String loadavgContent) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        String[] parts = ParseUtil.whitespaces.split(loadavgContent.trim());
        for (int i = 0; i < nelem; i++) {
            average[i] = i < parts.length ? ParseUtil.parseDoubleOrDefault(parts[i], -1d) : -1d;
        }
        return average;
    }

    /**
     * Native {@code getloadavg(3)} call, filling {@code loadavg} with up to {@code nelem} samples.
     *
     * @param loadavg the array to populate
     * @param nelem   the number of elements requested
     * @return the number of samples retrieved, or a negative value if the native call is unavailable or failed
     */
    protected abstract int getloadavgNative(double[] loadavg, int nelem);

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = CpuStat.getProcessorCpuLoadTicks(getLogicalProcessorCount());
        // In rare cases, /proc/stat reading fails. If so, try again.
        // In theory we should check all of them, but on failure we can expect all 0's
        // so we only need to check for processor 0
        if (LongStream.of(ticks[0]).sum() == 0) {
            ticks = CpuStat.getProcessorCpuLoadTicks(getLogicalProcessorCount());
        }
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
     * @param hwcap    Hardware capabilities from the auxiliary vector, or 0 if unavailable
     * @return The Processor ID string
     */
    private static String getProcessorID(String vendor, String stepping, String model, String family, String[] flags,
            long hwcap) {
        boolean procInfo = false;
        String marker = "Processor Information";
        for (String checkLine : ExecutingCommand.runPrivilegedNative("dmidecode -t 4")) {
            if (!procInfo && checkLine.contains(marker)) {
                marker = "ID:";
                procInfo = true;
            } else if (procInfo && checkLine.contains(marker)) {
                return checkLine.split(marker)[1].trim();
            }
        }
        // If we've gotten this far, dmidecode failed. Try cpuid.
        String cpuidResult = parseCpuidOutput(ExecutingCommand.runNative("cpuid -1r"));
        if (cpuidResult != null) {
            return cpuidResult;
        }
        // If we've gotten this far, dmidecode failed. Encode arguments
        if (vendor.startsWith("0x")) {
            return createMIDR(vendor, stepping, model, family) + "00000000";
        }
        return createProcessorID(stepping, model, family, flags, hwcap);
    }

    /**
     * Parses the output of {@code cpuid -1r} to extract the processor ID.
     *
     * @param cpuidLines the output lines from {@code cpuid -1r}
     * @return the processor ID string (edx + eax), or {@code null} if not found
     */
    static String parseCpuidOutput(List<String> cpuidLines) {
        for (String checkLine : cpuidLines) {
            if (checkLine.contains("eax=") && checkLine.trim().startsWith("0x00000001")) {
                String eax = "";
                String edx = "";
                for (String register : ParseUtil.whitespaces.split(checkLine)) {
                    if (register.startsWith("eax=")) {
                        eax = ParseUtil.removeMatchingString(register, "eax=0x");
                    } else if (register.startsWith("edx=")) {
                        edx = ParseUtil.removeMatchingString(register, "edx=0x");
                    }
                }
                if (!eax.isEmpty() && !edx.isEmpty()) {
                    return edx + eax;
                }
            }
        }
        return null;
    }

    /**
     * Queries the hardware capabilities from the auxiliary vector. Subclasses provide platform-specific implementations
     * using JNA or FFM native access.
     *
     * @return The hardware capabilities value, or 0 if unavailable
     */
    protected long queryHwcap() {
        return 0L;
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
    static String createMIDR(String vendor, String stepping, String model, String family) {
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

        return String.format(Locale.ROOT, "%08X", midrBytes);
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
