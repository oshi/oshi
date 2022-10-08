/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows;

import static oshi.hardware.common.AbstractCentralProcessor.orderedProcCaches;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.CACHE_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.GROUP_AFFINITY;
import com.sun.jna.platform.win32.WinNT.LOGICAL_PROCESSOR_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.NUMA_NODE_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.PROCESSOR_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.wmi.Win32Processor;
import oshi.driver.windows.wmi.Win32Processor.ProcessorIdProperty;
import oshi.hardware.CentralProcessor.LogicalProcessor;
import oshi.hardware.CentralProcessor.PhysicalProcessor;
import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.hardware.CentralProcessor.ProcessorCache.Type;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Triplet;

/**
 * Utility to query Logical Processor Information
 */
@ThreadSafe
public final class LogicalProcessorInformation {

    private static final boolean IS_WIN10_OR_GREATER = VersionHelpers.IsWindows10OrGreater();

    private LogicalProcessorInformation() {
    }

    /**
     * Get a list of logical processors on this machine. Requires Windows 7 and higher.
     *
     * @return A list of logical processors
     */
    public static Triplet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>> getLogicalProcessorInformationEx() {
        // Collect a list of logical processors on each physical core and
        // package. These will be 64-bit bitmasks.
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] procInfo = Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationAll);
        // Used to cross-reference a processor to package, cache, pr core
        List<GROUP_AFFINITY[]> packages = new ArrayList<>();
        Set<ProcessorCache> caches = new HashSet<>();
        List<GROUP_AFFINITY> cores = new ArrayList<>();
        // Used to iterate
        List<NUMA_NODE_RELATIONSHIP> numaNodes = new ArrayList<>();
        // Map to store efficiency class of a processor core
        Map<GROUP_AFFINITY, Integer> coreEfficiencyMap = new HashMap<>();

        for (SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX info : procInfo) {
            switch (info.relationship) {
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage:
                // could assign a package to more than one processor group
                packages.add(((PROCESSOR_RELATIONSHIP) info).groupMask);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache:
                CACHE_RELATIONSHIP cache = (CACHE_RELATIONSHIP) info;
                caches.add(new ProcessorCache(cache.level, cache.associativity, cache.lineSize, cache.size,
                        Type.values()[cache.type]));
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore:
                PROCESSOR_RELATIONSHIP core = ((PROCESSOR_RELATIONSHIP) info);
                // for Core, groupCount is always 1
                cores.add(core.groupMask[0]);
                if (IS_WIN10_OR_GREATER) {
                    coreEfficiencyMap.put(core.groupMask[0], (int) core.efficiencyClass);
                }
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode:
                numaNodes.add((NUMA_NODE_RELATIONSHIP) info);
                break;
            default:
                // Ignore Group info
                break;
            }
        }
        // Windows doesn't define core, cache, and package numbers, so we define our own
        // for consistent use across the API. Here we sort so core, cache, and package
        // numbers increment consistently with processor numbers/bitmasks, ordered in
        // processor groups.
        cores.sort(Comparator.comparing(c -> c.group * 64L + Long.numberOfTrailingZeros(c.mask.longValue())));

        // if package in multiple groups will still use first group for sorting
        packages.sort(Comparator.comparing(p -> p[0].group * 64L + Long.numberOfTrailingZeros(p[0].mask.longValue())));

        // Iterate Logical Processors and use bitmasks to match packages, cores,
        // and NUMA nodes. Perfmon instances are numa node + processor number, so we
        // iterate by numa node so the returned list will properly index perfcounter
        // numa/proc-per-numa indices with all numa nodes grouped together
        numaNodes.sort(Comparator.comparing(n -> n.nodeNumber));

        // Fetch the processorIDs from WMI
        Map<Integer, String> processorIdMap = new HashMap<>();
        WmiResult<ProcessorIdProperty> processorId = Win32Processor.queryProcessorId();
        // One entry for each package/socket
        for (int pkg = 0; pkg < processorId.getResultCount(); pkg++) {
            processorIdMap.put(pkg, WmiUtil.getString(processorId, ProcessorIdProperty.PROCESSORID, pkg));
        }

        List<LogicalProcessor> logProcs = new ArrayList<>();
        Map<Integer, Integer> corePkgMap = new HashMap<>();
        Map<Integer, String> pkgCpuidMap = new HashMap<>();
        for (NUMA_NODE_RELATIONSHIP node : numaNodes) {
            int nodeNum = node.nodeNumber;
            int group = node.groupMask.group;
            long mask = node.groupMask.mask.longValue();
            // Processor numbers are uniquely identified by processor group and processor
            // number on that group, which matches the bitmask.
            int lowBit = Long.numberOfTrailingZeros(mask);
            int hiBit = 63 - Long.numberOfLeadingZeros(mask);
            for (int lp = lowBit; lp <= hiBit; lp++) {
                if ((mask & (1L << lp)) != 0) {
                    int coreId = getMatchingCore(cores, group, lp);
                    int pkgId = getMatchingPackage(packages, group, lp);
                    corePkgMap.put(coreId, pkgId);
                    pkgCpuidMap.put(coreId, processorIdMap.getOrDefault(pkgId, ""));
                    LogicalProcessor logProc = new LogicalProcessor(lp, coreId, pkgId, nodeNum, group);
                    logProcs.add(logProc);
                }
            }
        }
        List<PhysicalProcessor> physProcs = getPhysProcs(cores, coreEfficiencyMap, corePkgMap, pkgCpuidMap);
        return new Triplet<>(logProcs, physProcs, orderedProcCaches(caches));
    }

    private static List<PhysicalProcessor> getPhysProcs(List<GROUP_AFFINITY> cores,
            Map<GROUP_AFFINITY, Integer> coreEfficiencyMap, Map<Integer, Integer> corePkgMap,
            Map<Integer, String> coreCpuidMap) {
        List<PhysicalProcessor> physProcs = new ArrayList<>();
        for (int coreId = 0; coreId < cores.size(); coreId++) {
            int efficiency = coreEfficiencyMap.getOrDefault(cores.get(coreId), 0);
            String cpuid = coreCpuidMap.getOrDefault(coreId, "");
            int pkgId = corePkgMap.getOrDefault(coreId, 0);
            physProcs.add(new PhysicalProcessor(pkgId, coreId, efficiency, cpuid));
        }
        return physProcs;
    }

    private static int getMatchingPackage(List<GROUP_AFFINITY[]> packages, int g, int lp) {
        for (int i = 0; i < packages.size(); i++) {
            for (int j = 0; j < packages.get(i).length; j++) {
                if ((packages.get(i)[j].mask.longValue() & (1L << lp)) != 0 && packages.get(i)[j].group == g) {
                    return i;
                }
            }
        }
        return 0;
    }

    private static int getMatchingCore(List<GROUP_AFFINITY> cores, int g, int lp) {
        for (int j = 0; j < cores.size(); j++) {
            if ((cores.get(j).mask.longValue() & (1L << lp)) != 0 && cores.get(j).group == g) {
                return j;
            }
        }
        return 0;
    }

    /*
     * Non-EX version for pre-Win7
     */

    /**
     * Get a list of logical processors on this machine
     *
     * @return A list of logical processors
     */
    public static Triplet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>> getLogicalProcessorInformation() {
        // Collect a list of logical processors on each physical core and package.
        List<Long> packageMaskList = new ArrayList<>();
        List<Long> coreMaskList = new ArrayList<>();
        WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION[] processors = Kernel32Util.getLogicalProcessorInformation();
        for (SYSTEM_LOGICAL_PROCESSOR_INFORMATION proc : processors) {
            if (proc.relationship == WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage) {
                packageMaskList.add(proc.processorMask.longValue());
            } else if (proc.relationship == WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore) {
                coreMaskList.add(proc.processorMask.longValue());
            }
        }
        // Sort the list (natural ordering) so core and package numbers
        // increment as expected.
        coreMaskList.sort(null);
        packageMaskList.sort(null);

        // Assign logical processors to cores and packages
        List<LogicalProcessor> logProcs = new ArrayList<>();
        for (int core = 0; core < coreMaskList.size(); core++) {
            long coreMask = coreMaskList.get(core);
            // Lowest and Highest set bits, indexing from 0
            int lowBit = Long.numberOfTrailingZeros(coreMask);
            int hiBit = 63 - Long.numberOfLeadingZeros(coreMask);
            // Create logical processors for this core
            for (int i = lowBit; i <= hiBit; i++) {
                if ((coreMask & (1L << i)) != 0) {
                    LogicalProcessor logProc = new LogicalProcessor(i, core,
                            LogicalProcessorInformation.getBitMatchingPackageNumber(packageMaskList, i));
                    logProcs.add(logProc);
                }
            }
        }
        return new Triplet<>(logProcs, null, null);
    }

    /**
     * Iterate over the package mask list and find a matching mask index
     *
     * @param packageMaskList The list of bitmasks to iterate
     * @param logProc         The bit to find matching mask
     * @return The index of the list which matched the bit
     */
    private static int getBitMatchingPackageNumber(List<Long> packageMaskList, int logProc) {
        for (int i = 0; i < packageMaskList.size(); i++) {
            if ((packageMaskList.get(i).longValue() & (1L << logProc)) != 0) {
                return i;
            }
        }
        return 0;
    }
}
