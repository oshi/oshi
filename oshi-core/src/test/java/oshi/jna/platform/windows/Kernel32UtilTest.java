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
import java.util.List;

import com.sun.jna.platform.win32.WinNT.LOGICAL_PROCESSOR_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.PROCESSOR_CACHE_TYPE;

import junit.framework.TestCase;
import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.jna.platform.windows.WinNT.CACHE_RELATIONSHIP;
import oshi.jna.platform.windows.WinNT.GROUP_RELATIONSHIP;
import oshi.jna.platform.windows.WinNT.NUMA_NODE_RELATIONSHIP;
import oshi.jna.platform.windows.WinNT.PROCESSOR_RELATIONSHIP;
import oshi.jna.platform.windows.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX;

public class Kernel32UtilTest extends TestCase {

    public void testGetLogicalProcessorInformationEx() {
        if (SystemInfo.getCurrentPlatformEnum() != PlatformEnum.WINDOWS) {
            return;
        }
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] procInfo = Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationAll);
        List<GROUP_RELATIONSHIP> groups = new ArrayList<>();
        List<PROCESSOR_RELATIONSHIP> packages = new ArrayList<>();
        List<NUMA_NODE_RELATIONSHIP> numaNodes = new ArrayList<>();
        List<CACHE_RELATIONSHIP> caches = new ArrayList<>();
        List<PROCESSOR_RELATIONSHIP> cores = new ArrayList<>();

        for (int i = 0; i < procInfo.length; i++) {
            // Build list from relationship
            switch (procInfo[i].relationship) {
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup:
                groups.add((GROUP_RELATIONSHIP) procInfo[i]);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage:
                packages.add((PROCESSOR_RELATIONSHIP) procInfo[i]);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode:
                numaNodes.add((NUMA_NODE_RELATIONSHIP) procInfo[i]);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache:
                caches.add((CACHE_RELATIONSHIP) procInfo[i]);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore:
                cores.add((PROCESSOR_RELATIONSHIP) procInfo[i]);
                break;
            default:
                throw new IllegalStateException("Unmapped relationship.");
            }
            // Test that native provided size matches JNA structure size
            assertEquals(procInfo[i].size, procInfo[i].size());
        }

        // Test that getting all relations matches the same totals as
        // individuals.
        assertEquals(groups.size(), Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup).length);
        assertEquals(packages.size(), Kernel32Util.getLogicalProcessorInformationEx(
                WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage).length);
        assertEquals(numaNodes.size(), Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode).length);
        assertEquals(caches.size(), Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache).length);
        assertEquals(cores.size(), Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore).length);
        
        // Test GROUP_RELATIONSHIP
        assertEquals(1, groups.size()); // Should only be one group structure
        for (GROUP_RELATIONSHIP group : groups) {
            assertEquals(LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup, group.relationship);
            assertTrue(group.activeGroupCount <= group.maximumGroupCount);
            assertEquals(group.activeGroupCount, group.groupInfo.length);
            for (int j = 0; j < group.activeGroupCount; j++) {
                assertTrue(group.groupInfo[j].activeProcessorCount <= group.groupInfo[j].maximumProcessorCount);
                assertEquals(group.groupInfo[j].activeProcessorCount,
                        Long.bitCount(group.groupInfo[j].activeProcessorMask.longValue()));
                assertTrue(group.groupInfo[j].maximumProcessorCount <= 64);
            }
        }
        
        // Test PROCESSOR_RELATIONSHIP packages
        assertTrue(cores.size() >= packages.size());
        for (PROCESSOR_RELATIONSHIP pkg : packages) {
            assertEquals(LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage, pkg.relationship);
            assertEquals(0, pkg.flags); // packages have 0 flags
            assertEquals(0, pkg.efficiencyClass); // packages have 0 efficiency
            assertEquals(pkg.groupCount, pkg.groupMask.length);
        }

        // Test PROCESSOR_RELATIONSHIP cores
        for (PROCESSOR_RELATIONSHIP core : cores) {
            assertEquals(LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore, core.relationship);
            // Hyperthreading flag set if at least 2 logical processors
            assertTrue(Long.bitCount(core.groupMask[0].mask.longValue()) > 0);
            if (Long.bitCount(core.groupMask[0].mask.longValue()) > 1) {
                assertEquals(WinNT.LTP_PC_SMT, core.flags);
            } else {
                assertEquals(0, core.flags);
            }
            // Cores are always in one group
            assertEquals(1, core.groupCount);
            assertEquals(1, core.groupMask.length);
        }

        // Test NUMA_NODE_RELATIONSHIP
        for (NUMA_NODE_RELATIONSHIP numaNode : numaNodes) {
            assertEquals(LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode, numaNode.relationship);
            assertTrue(numaNode.nodeNumber >= 0);
        }

        // Test CACHE_RELATIONSHIP
        for (CACHE_RELATIONSHIP cache : caches) {
            assertEquals(LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache, cache.relationship);
            assertTrue(cache.level >= 1);
            assertTrue(cache.level <= 4);
            assertTrue(cache.cacheSize > 0);
            assertTrue(cache.lineSize > 0);
            assertTrue(
                    cache.type == PROCESSOR_CACHE_TYPE.CacheUnified
                            || cache.type == PROCESSOR_CACHE_TYPE.CacheInstruction
                            || cache.type == PROCESSOR_CACHE_TYPE.CacheData
                            || cache.type == PROCESSOR_CACHE_TYPE.CacheTrace);
            assertTrue(cache.associativity == WinNT.CACHE_FULLY_ASSOCIATIVE || cache.associativity > 0);
        }
    }
}

