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
package oshi.jna.platform.windows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.platform.win32.WinNT.LOGICAL_PROCESSOR_RELATIONSHIP;

import oshi.jna.platform.windows.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX;
import oshi.jna.platform.windows.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.CACHE_RELATIONSHIP;
import oshi.jna.platform.windows.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.GROUP_AFFINITY;
import oshi.jna.platform.windows.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.GROUP_RELATIONSHIP;
import oshi.jna.platform.windows.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.NUMA_NODE_RELATIONSHIP;
import oshi.jna.platform.windows.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.PROCESSOR_GROUP_INFO;
import oshi.jna.platform.windows.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.PROCESSOR_RELATIONSHIP;

/**
 * Temporary test case to verify code behaves as expected. These methods will be
 * translated to junit tests prior to submission to JNA project.
 */
public class ProcInfo {

    public static void main(String[] args) {
        getGroups();
        System.out.println();
        getPackages();
        System.out.println();
        getNumaNodes();
        System.out.println();
        getCaches();
        System.out.println();
        getCores();
        System.out.println();
        getItAll();
    }


    private static void getGroups() {
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] processors = Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup);
        System.out.format("Info for %d group structure (should only be 1):%n", processors.length);
        for (int i = 0; i < processors.length; i++) {
            System.out.format("Relationship should be %d, it is %d%n", LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup,
                    processors[i].relationship);
            GROUP_RELATIONSHIP group = processors[i].payload.Group;
            PROCESSOR_GROUP_INFO[] info = group.getGroupInfo();
            System.out.format("Details for %d group(s):%n", info.length);
            for (int j = 0; j < info.length; j++) {
                System.out.format("Group %d had %d active of %d max processors with bitmask %s.%n", j,
                        info[j].activeProcessorCount, info[j].maximumProcessorCount,
                        Long.toBinaryString(info[j].activeProcessorMask.longValue()));
            }
        }
    }

    private static void getPackages() {
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] processors = Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage);
        System.out.format("Details of %d package(s):%n", processors.length);
        for (int i = 0; i < processors.length; i++) {
            System.out.format("Relationship should be %d, it is %d%n",
                    LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage, processors[i].relationship);
            PROCESSOR_RELATIONSHIP pkg = processors[i].payload.Processor;
            System.out.format("Flags should be 0 for a package, it is %d. Efficiency class is %d. Group count is %d.%n",
                    pkg.flags, pkg.efficiencyClass, pkg.groupCount);
            GROUP_AFFINITY[] mask = pkg.getGroupMask();
            for (int j = 0; j < mask.length; j++) {
                System.out.format("Mask %d is in group %d with bitmask %s.%n", j, mask[j].group,
                        Long.toBinaryString(mask[j].mask.longValue()));
            }
        }
    }

    private static void getNumaNodes() {
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] processors = Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode);
        System.out.format("Details of %d NUMA node(s):%n", processors.length);
        for (int i = 0; i < processors.length; i++) {
            System.out.format("Relationship should be %d, it is %d%n",
                    LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode, processors[i].relationship);
            NUMA_NODE_RELATIONSHIP node = processors[i].payload.NumaNode;
            System.out.format("Node %d is in group %d with bitmask %s.%n", node.nodeNumber, node.groupMask.group,
                    Long.toBinaryString(node.groupMask.mask.longValue()));
        }
    }

    private static void getCaches() {
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] processors = Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache);
        System.out.format("Details of %d Cache(s):%n", processors.length);
        for (int i = 0; i < processors.length; i++) {
            System.out.format("Relationship should be %d, it is %d%n", LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache,
                    processors[i].relationship);
            CACHE_RELATIONSHIP cache = processors[i].payload.Cache;
            System.out.format(
                    "Cache is level %d with associativity %d and linesize %d. Type is %d and size is %d. It is in group %d with bitmask %s.%n",
                    cache.level, cache.associativity, cache.lineSize, cache.type, cache.cacheSize,
                    cache.groupMask.group, Long.toBinaryString(cache.groupMask.mask.longValue()));
        }
    }

    private static void getCores() {
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] processors = Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore);
        System.out.format("Details of %d core(s):%n", processors.length);
        for (int i = 0; i < processors.length; i++) {
            System.out.format("Relationship should be %d, it is %d%n",
                    LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore, processors[i].relationship);
            PROCESSOR_RELATIONSHIP core = processors[i].payload.Processor;
            System.out.format("Efficiency class is %d. Group count is %d.%n", core.efficiencyClass, core.groupCount);
            GROUP_AFFINITY[] mask = core.getGroupMask();
            System.out.format(
                    "Hyperthreading flag is %d, should be 1 only if bitmask bits %d > 1 or group count %d > 1.%n",
                    core.flags, Long.bitCount(mask[0].mask.longValue()), core.groupCount);
            for (int j = 0; j < mask.length; j++) {
                System.out.format("Mask %d is in group %d with bitmask %s.%n", j, mask[0].group,
                        Long.toBinaryString(mask[0].mask.longValue()));
            }
        }
    }


    private static void getItAll() {
        System.out.println("Grabbing all info using Kernel32Util...");
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] procInfo = Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationAll);
        List<PROCESSOR_GROUP_INFO> groups = new ArrayList<>();
        List<GROUP_AFFINITY[]> packages = new ArrayList<>();
        List<NUMA_NODE_RELATIONSHIP> numaNodes = new ArrayList<>();
        List<CACHE_RELATIONSHIP> caches = new ArrayList<>();
        List<GROUP_AFFINITY> cores = new ArrayList<>();

        for (int i = 0; i < procInfo.length; i++) {
            switch (procInfo[i].relationship) {
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup:
                groups = Arrays.asList(procInfo[i].payload.Group.getGroupInfo());
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage:
                packages.add(procInfo[i].payload.Processor.getGroupMask());
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode:
                numaNodes.add(procInfo[i].payload.NumaNode);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache:
                caches.add(procInfo[i].payload.Cache);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore:
                cores.add(procInfo[i].payload.Processor.getGroupMask()[0]);
                break;
            default:
                System.out.println("You should never see this message.");
                break;
            }
        }
        System.out.format("Retrieved %d structures: %d groups, %d packages, %d numa nodes, %d caches, %d cores%n",
                procInfo.length, groups.size(), packages.size(), numaNodes.size(), caches.size(), cores.size());
        System.out.println("Topology:");
        System.out.format("%6s %7s %11s %5s %9s%n", "Group", "package", "NUMA node", "core", "logical processor");
        for (int g = 0; g < groups.size(); g++) {
            for (int lp = 0; lp < 64; lp++) {
                int pkg = getMatchingPackage(packages, g, lp);
                if (pkg < 0) {
                    continue;
                }
                System.out.format("%4d %8d %8d %8d %8d%n", g, pkg,
                        getMatchingNumaNode(numaNodes, g, lp), getMatchingCore(cores, g, lp), lp);
            }
        }
    }


    private static int getMatchingPackage(List<GROUP_AFFINITY[]> packages, int g, int lp) {
        for (int i = 0; i < packages.size(); i++) {
            for (int j = 0; j < packages.get(i).length; j++) {
                if ((packages.get(i)[j].mask.longValue() & (1L << lp)) > 0 && packages.get(i)[j].group == g) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int getMatchingNumaNode(List<NUMA_NODE_RELATIONSHIP> numaNodes, int g, int lp) {
        for (int j = 0; j < numaNodes.size(); j++) {
            if ((numaNodes.get(j).groupMask.mask.longValue() & (1L << lp)) > 0
                    && numaNodes.get(j).groupMask.group == g) {
                return numaNodes.get(j).nodeNumber;
            }
        }
        return -1;
    }

    private static int getMatchingCore(List<GROUP_AFFINITY> cores, int g, int lp) {
        for (int j = 0; j < cores.size(); j++) {
            if ((cores.get(j).mask.longValue() & (1L << lp)) > 0 && cores.get(j).group == g) {
                return j;
            }
        }
        return -1;
    }

}
