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
package oshi.driver.windows;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.sun.jna.platform.win32.Kernel32Util; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.GROUP_AFFINITY;
import com.sun.jna.platform.win32.WinNT.LOGICAL_PROCESSOR_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.NUMA_NODE_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.PROCESSOR_RELATIONSHIP;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor.LogicalProcessor;

/**
 * Utility to query Logical Processor Information pre-Win7
 */
@ThreadSafe
public final class LogicalProcessorInformation {

    private LogicalProcessorInformation() {
    }

    /**
     * Get a list of logical processors on this machine. Requires Windows 7 and
     * higher.
     *
     * @return A list of logical processors
     */
    public static List<LogicalProcessor> getLogicalProcessorInformationEx() {
        // Collect a list of logical processors on each physical core and
        // package. These will be 64-bit bitmasks.
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] procInfo = Kernel32Util
                .getLogicalProcessorInformationEx(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationAll);
        List<GROUP_AFFINITY[]> packages = new ArrayList<>();
        List<NUMA_NODE_RELATIONSHIP> numaNodes = new ArrayList<>();
        List<GROUP_AFFINITY> cores = new ArrayList<>();

        for (int i = 0; i < procInfo.length; i++) {
            switch (procInfo[i].relationship) {
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage:
                packages.add(((PROCESSOR_RELATIONSHIP) procInfo[i]).groupMask);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode:
                numaNodes.add((NUMA_NODE_RELATIONSHIP) procInfo[i]);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore:
                cores.add(((PROCESSOR_RELATIONSHIP) procInfo[i]).groupMask[0]);
                break;
            default:
                // Ignore Group and Cache info
                break;
            }
        }
        // Windows doesn't define core and package numbers, so we sort the lists
        // so core and package numbers increment consistently with processor
        // numbers/bitmasks, ordered in groups
        cores.sort(Comparator.comparing(c -> c.group * 64L + c.mask.longValue()));
        packages.sort(Comparator.comparing(p -> p[0].group * 64L + p[0].mask.longValue()));

        // Iterate Logical Processors and use bitmasks to match packages, cores,
        // and NUMA nodes
        List<LogicalProcessor> logProcs = new ArrayList<>();
        for (GROUP_AFFINITY coreMask : cores) {
            int group = coreMask.group;
            long mask = coreMask.mask.longValue();
            // Iterate mask for logical processor numbers
            int lowBit = Long.numberOfTrailingZeros(mask);
            int hiBit = 63 - Long.numberOfLeadingZeros(mask);
            for (int lp = lowBit; lp <= hiBit; lp++) {
                if ((mask & (1L << lp)) != 0) {
                    LogicalProcessor logProc = new LogicalProcessor(lp, getMatchingCore(cores, group, lp),
                            getMatchingPackage(packages, group, lp), getMatchingNumaNode(numaNodes, group, lp), group);
                    logProcs.add(logProc);
                }
            }
        }
        // Sort by numaNode and then logical processor number to match
        // PerfCounter/WMI ordering
        logProcs.sort(Comparator.comparing(LogicalProcessor::getNumaNode)
                .thenComparing(LogicalProcessor::getProcessorNumber));
        return logProcs;
    }

    private static int getMatchingPackage(List<GROUP_AFFINITY[]> packages, int g, int lp) {
        for (int i = 0; i < packages.size(); i++) {
            for (int j = 0; j < packages.get(i).length; j++) {
                if ((packages.get(i)[j].mask.longValue() & (1L << lp)) > 0 && packages.get(i)[j].group == g) {
                    return i;
                }
            }
        }
        return 0;
    }

    private static int getMatchingNumaNode(List<NUMA_NODE_RELATIONSHIP> numaNodes, int g, int lp) {
        for (int j = 0; j < numaNodes.size(); j++) {
            if ((numaNodes.get(j).groupMask.mask.longValue() & (1L << lp)) > 0
                    && numaNodes.get(j).groupMask.group == g) {
                return numaNodes.get(j).nodeNumber;
            }
        }
        return 0;
    }

    private static int getMatchingCore(List<GROUP_AFFINITY> cores, int g, int lp) {
        for (int j = 0; j < cores.size(); j++) {
            if ((cores.get(j).mask.longValue() & (1L << lp)) > 0 && cores.get(j).group == g) {
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
    public static List<LogicalProcessor> getLogicalProcessorInformation() {
        // Collect a list of logical processors on each physical core and
        // package.
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
                if ((coreMask & (1L << i)) > 0) {
                    LogicalProcessor logProc = new LogicalProcessor(i, core,
                            LogicalProcessorInformation.getBitMatchingPackageNumber(packageMaskList, i));
                    logProcs.add(logProc);
                }
            }
        }
        return logProcs;
    }

    /**
     * Iterate over the package mask list and find a matching mask index
     *
     * @param packageMaskList
     *            The list of bitmasks to iterate
     * @param logProc
     *            The bit to find matching mask
     * @return The index of the list which matched the bit
     */
    private static int getBitMatchingPackageNumber(List<Long> packageMaskList, int logProc) {
        for (int i = 0; i < packageMaskList.size(); i++) {
            if ((packageMaskList.get(i).longValue() & (1L << logProc)) > 0) {
                return i;
            }
        }
        return 0;
    }
}
