/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows;

import static oshi.hardware.common.AbstractCentralProcessor.orderedProcCaches;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Processor.ProcessorIdProperty;
import oshi.driver.windows.wmi.Win32ProcessorFFM;
import oshi.ffm.windows.Kernel32FFM;
import oshi.ffm.windows.VersionHelpersFFM;
import oshi.hardware.CentralProcessor.LogicalProcessor;
import oshi.hardware.CentralProcessor.PhysicalProcessor;
import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.util.platform.windows.WmiUtilFFM;
import oshi.util.tuples.Triplet;

/**
 * FFM-based utility to query Logical Processor Information using GetLogicalProcessorInformationEx.
 */
@ThreadSafe
public final class LogicalProcessorInformationFFM {

    private static final Logger LOG = LoggerFactory.getLogger(LogicalProcessorInformationFFM.class);

    // RelationAll = 0xffff
    private static final int RELATION_ALL = 0xffff;
    // Relationship types
    private static final int RELATION_PROCESSOR_CORE = 0;
    private static final int RELATION_NUMA_NODE = 1;
    private static final int RELATION_CACHE = 2;
    private static final int RELATION_PROCESSOR_PACKAGE = 3;

    private static final boolean IS_WIN10_OR_GREATER = VersionHelpersFFM.IsWindows10OrGreater();

    private LogicalProcessorInformationFFM() {
    }

    /**
     * Get a list of logical processors on this machine using GetLogicalProcessorInformationEx.
     *
     * @return A triplet of logical processors, physical processors, and processor caches
     */
    public static Triplet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>> getLogicalProcessorInformationEx() {
        List<List<long[]>> packages = new ArrayList<>(); // each entry: list of [group, mask]
        Set<ProcessorCache> caches = new HashSet<>();
        List<long[]> cores = new ArrayList<>(); // each entry: [group, mask, efficiency]
        List<long[]> numaNodes = new ArrayList<>(); // each entry: [nodeNumber, group, mask]

        try (Arena arena = Arena.ofConfined()) {
            // First call to get required buffer size
            MemorySegment returnedLength = arena.allocate(ValueLayout.JAVA_INT);
            Kernel32FFM.GetLogicalProcessorInformationEx(RELATION_ALL, MemorySegment.NULL, returnedLength);
            int bufferSize = returnedLength.get(ValueLayout.JAVA_INT, 0);
            if (bufferSize == 0) {
                LOG.error("Failed to get buffer size for GetLogicalProcessorInformationEx");
                return new Triplet<>(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            }

            // Second call with properly sized buffer
            MemorySegment buffer = arena.allocate(bufferSize);
            returnedLength.set(ValueLayout.JAVA_INT, 0, bufferSize);
            if (!Kernel32FFM.GetLogicalProcessorInformationEx(RELATION_ALL, buffer, returnedLength)) {
                LOG.error("Failed to get logical processor information");
                return new Triplet<>(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            }

            // Parse the buffer
            int offset = 0;
            while (offset < bufferSize) {
                // SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX structure:
                // Relationship (DWORD at offset 0)
                // Size (DWORD at offset 4)
                // Union data starts at offset 8
                int relationship = buffer.get(ValueLayout.JAVA_INT, offset);
                int size = buffer.get(ValueLayout.JAVA_INT, offset + 4);

                switch (relationship) {
                    case RELATION_PROCESSOR_CORE:
                        parseProcessorCore(buffer, offset, cores);
                        break;
                    case RELATION_NUMA_NODE:
                        parseNumaNode(buffer, offset, numaNodes);
                        break;
                    case RELATION_CACHE:
                        parseCache(buffer, offset, caches);
                        break;
                    case RELATION_PROCESSOR_PACKAGE:
                        parsePackage(buffer, offset, packages);
                        break;
                    default:
                        break;
                }
                offset += size;
            }
        }

        // Sort cores by group*64 + trailing zeros of mask
        cores.sort(Comparator.comparing(c -> c[0] * 64L + Long.numberOfTrailingZeros(c[1])));
        // Sort packages by first group affinity
        packages.sort(Comparator.comparing(p -> p.get(0)[0] * 64L + Long.numberOfTrailingZeros(p.get(0)[1])));
        // Sort numa nodes by node number
        numaNodes.sort(Comparator.comparing(n -> n[0]));

        // Fetch processorIDs from WMI
        Map<Integer, String> processorIdMap = new HashMap<>();
        var processorId = Win32ProcessorFFM.queryProcessorId();
        for (int pkg = 0; pkg < processorId.getResultCount(); pkg++) {
            processorIdMap.put(pkg, WmiUtilFFM.getString(processorId, ProcessorIdProperty.PROCESSORID, pkg));
        }

        // Build logical processor list
        List<LogicalProcessor> logProcs = new ArrayList<>();
        Map<Integer, Integer> corePkgMap = new HashMap<>();
        Map<Integer, String> pkgCpuidMap = new HashMap<>();
        for (long[] node : numaNodes) {
            int nodeNum = (int) node[0];
            int group = (int) node[1];
            long mask = node[2];
            int lowBit = Long.numberOfTrailingZeros(mask);
            int hiBit = 63 - Long.numberOfLeadingZeros(mask);
            for (int lp = lowBit; lp <= hiBit; lp++) {
                if ((mask & (1L << lp)) != 0) {
                    int coreId = getMatchingCore(cores, group, lp);
                    int pkgId = getMatchingPackage(packages, group, lp);
                    corePkgMap.put(coreId, pkgId);
                    pkgCpuidMap.put(coreId, processorIdMap.getOrDefault(pkgId, ""));
                    logProcs.add(new LogicalProcessor(lp, coreId, pkgId, nodeNum, group));
                }
            }
        }

        List<PhysicalProcessor> physProcs = new ArrayList<>();
        for (int coreId = 0; coreId < cores.size(); coreId++) {
            int efficiency = (int) cores.get(coreId)[2];
            String cpuid = pkgCpuidMap.getOrDefault(coreId, "");
            int pkgId = corePkgMap.getOrDefault(coreId, 0);
            physProcs.add(new PhysicalProcessor(pkgId, coreId, efficiency, cpuid));
        }

        return new Triplet<>(logProcs, physProcs, orderedProcCaches(caches));
    }

    private static void parseProcessorCore(MemorySegment buffer, int baseOffset, List<long[]> cores) {
        // PROCESSOR_RELATIONSHIP at offset 8:
        // Flags (BYTE at 0), EfficiencyClass (BYTE at 1), Reserved[20] (at 2), GroupCount (WORD at 22)
        // GroupMask[1] starts at 24: GROUP_AFFINITY is { Mask (KAFFINITY=8 bytes), Group (WORD), Reserved[3] (3 WORD) }
        int dataOffset = baseOffset + 8;
        int efficiencyClass = IS_WIN10_OR_GREATER
                ? Byte.toUnsignedInt(buffer.get(ValueLayout.JAVA_BYTE, dataOffset + 1))
                : 0;
        // For core, groupCount is always 1
        long mask = buffer.get(ValueLayout.JAVA_LONG, dataOffset + 24);
        int group = Short.toUnsignedInt(buffer.get(ValueLayout.JAVA_SHORT, dataOffset + 24 + 8));
        cores.add(new long[] { group, mask, efficiencyClass });
    }

    private static void parseNumaNode(MemorySegment buffer, int baseOffset, List<long[]> numaNodes) {
        // NUMA_NODE_RELATIONSHIP at offset 8:
        // NodeNumber (DWORD at 0), Reserved[18] (at 4), GroupCount (WORD at 22),
        // GroupMasks[] (GROUP_AFFINITY[GroupCount] at 24, each 16 bytes: KAFFINITY mask + WORD group + WORD[3]
        // reserved)
        int dataOffset = baseOffset + 8;
        int nodeNumber = buffer.get(ValueLayout.JAVA_INT, dataOffset);
        int groupCount = Short.toUnsignedInt(buffer.get(ValueLayout.JAVA_SHORT, dataOffset + 22));
        if (groupCount == 0) {
            // Pre-20H2: single GROUP_AFFINITY at offset 24
            groupCount = 1;
        }
        for (int i = 0; i < groupCount; i++) {
            int affinityOffset = dataOffset + 24 + i * 16;
            long mask = buffer.get(ValueLayout.JAVA_LONG, affinityOffset);
            int group = Short.toUnsignedInt(buffer.get(ValueLayout.JAVA_SHORT, affinityOffset + 8));
            numaNodes.add(new long[] { nodeNumber, group, mask });
        }
    }

    private static void parseCache(MemorySegment buffer, int baseOffset, Set<ProcessorCache> caches) {
        // CACHE_RELATIONSHIP at offset 8:
        // Level (BYTE at 0), Associativity (BYTE at 1), LineSize (WORD at 2), CacheSize (DWORD at 4),
        // Type (DWORD at 8), Reserved[18] (at 12), padding (2 at 30), GroupMask (16 at 32)
        int dataOffset = baseOffset + 8;
        int level = Byte.toUnsignedInt(buffer.get(ValueLayout.JAVA_BYTE, dataOffset));
        int associativity = Byte.toUnsignedInt(buffer.get(ValueLayout.JAVA_BYTE, dataOffset + 1));
        int lineSize = Short.toUnsignedInt(buffer.get(ValueLayout.JAVA_SHORT, dataOffset + 2));
        int cacheSize = buffer.get(ValueLayout.JAVA_INT, dataOffset + 4);
        int type = buffer.get(ValueLayout.JAVA_INT, dataOffset + 8);
        Type[] types = Type.values();
        Type cacheType = (type >= 0 && type < types.length) ? types[type] : Type.UNIFIED;
        caches.add(new ProcessorCache(level, associativity, lineSize, cacheSize, cacheType));
    }

    private static void parsePackage(MemorySegment buffer, int baseOffset, List<List<long[]>> packages) {
        // PROCESSOR_RELATIONSHIP at offset 8 (same as core):
        // Flags(1), EfficiencyClass(1), Reserved[20](20), GroupCount(2), GroupMask[](16 each)
        int dataOffset = baseOffset + 8;
        int groupCount = Short.toUnsignedInt(buffer.get(ValueLayout.JAVA_SHORT, dataOffset + 22));
        List<long[]> groupMasks = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            int gmOffset = dataOffset + 24 + i * 16;
            long mask = buffer.get(ValueLayout.JAVA_LONG, gmOffset);
            int group = Short.toUnsignedInt(buffer.get(ValueLayout.JAVA_SHORT, gmOffset + 8));
            groupMasks.add(new long[] { group, mask });
        }
        if (groupMasks.isEmpty()) {
            // Fallback: read at least one
            long mask = buffer.get(ValueLayout.JAVA_LONG, dataOffset + 24);
            int group = Short.toUnsignedInt(buffer.get(ValueLayout.JAVA_SHORT, dataOffset + 24 + 8));
            groupMasks.add(new long[] { group, mask });
        }
        packages.add(groupMasks);
    }

    private static int getMatchingPackage(List<List<long[]>> packages, int g, int lp) {
        for (int i = 0; i < packages.size(); i++) {
            for (long[] gm : packages.get(i)) {
                if ((gm[1] & (1L << lp)) != 0 && gm[0] == g) {
                    return i;
                }
            }
        }
        return 0;
    }

    private static int getMatchingCore(List<long[]> cores, int g, int lp) {
        for (int j = 0; j < cores.size(); j++) {
            if ((cores.get(j)[1] & (1L << lp)) != 0 && cores.get(j)[0] == g) {
                return j;
            }
        }
        return 0;
    }
}
