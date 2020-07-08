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
import java.util.List;

import com.sun.jna.platform.win32.Kernel32Util; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION;

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
