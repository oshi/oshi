/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors:
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
package oshi.hardware.platform.linux;

import java.util.List;

import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * Memory obtained by /proc/meminfo and sysinfo.totalram
 *
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class LinuxGlobalMemory extends AbstractGlobalMemory {

    private static final long serialVersionUID = 1L;

    // Values read from /proc/meminfo used for other calculations
    private long memFree = 0;
    private long activeFile = 0;
    private long inactiveFile = 0;
    private long sReclaimable = 0;
    private long swapFree = 0;

    private long lastUpdate = 0;

    public LinuxGlobalMemory() {
        this.pageSize = ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("getconf PAGE_SIZE"), 4096L);
    }

    /**
     * Updates instance variables from reading /proc/meminfo no more frequently than
     * every 100ms. While most of the information is available in the sysinfo
     * structure, the most accurate calculation of MemAvailable is only available
     * from reading this pseudo-file. The maintainers of the Linux Kernel have
     * indicated this location will be kept up to date if the calculation changes:
     * see https://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?
     * id=34e431b0ae398fc54ea69ff85ec700722c9da773
     *
     * Internally, reading /proc/meminfo is faster than sysinfo because it only
     * spends time populating the memory components of the sysinfo structure.
     */
    @Override
    protected void updateMeminfo() {
        long now = System.currentTimeMillis();
        if (now - this.lastUpdate > 100) {
            List<String> memInfo = FileUtil.readFile("/proc/meminfo");
            boolean found = false;
            for (String checkLine : memInfo) {
                String[] memorySplit = ParseUtil.whitespaces.split(checkLine);
                if (memorySplit.length > 1) {
                    if (memorySplit[0].equals("MemTotal:")) {
                        this.memTotal = parseMeminfo(memorySplit);
                    } else if (memorySplit[0].equals("MemFree:")) {
                        this.memFree = parseMeminfo(memorySplit);
                    } else if (memorySplit[0].equals("MemAvailable:")) {
                        this.memAvailable = parseMeminfo(memorySplit);
                        found = true;
                    } else if (memorySplit[0].equals("Active(file):")) {
                        this.activeFile = parseMeminfo(memorySplit);
                    } else if (memorySplit[0].equals("Inactive(file):")) {
                        this.inactiveFile = parseMeminfo(memorySplit);
                    } else if (memorySplit[0].equals("SReclaimable:")) {
                        this.sReclaimable = parseMeminfo(memorySplit);
                    } else if (memorySplit[0].equals("SwapTotal:")) {
                        this.swapTotal = parseMeminfo(memorySplit);
                    } else if (memorySplit[0].equals("SwapFree:")) {
                        this.swapFree = parseMeminfo(memorySplit);
                    }
                    // do nothing with other lines
                }
            }
            this.swapUsed = this.swapTotal - this.swapFree;
            // If no MemAvailable, calculate from other fields
            if (!found) {
                this.memAvailable = this.memFree + this.activeFile + this.inactiveFile + this.sReclaimable;
            }

            memInfo = FileUtil.readFile("/proc/vmstat");
            for (String checkLine : memInfo) {
                String[] memorySplit = ParseUtil.whitespaces.split(checkLine);
                if (memorySplit.length > 1) {
                    if (memorySplit[0].equals("pgpgin")) {
                        this.swapPagesIn = ParseUtil.parseLongOrDefault(memorySplit[1], 0L);
                    } else if (memorySplit[0].equals("pgpgout")) {
                        this.swapPagesOut = ParseUtil.parseLongOrDefault(memorySplit[1], 0L);
                    }
                    // do nothing with other lines
                }
            }

            this.lastUpdate = now;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateSwap() {
        updateMeminfo();
    }

    /**
     * Parses lines from the display of /proc/meminfo
     *
     * @param memorySplit
     *            Array of Strings representing the 3 columns of /proc/meminfo
     * @return value, multiplied by 1024 if kB is specified
     */
    private long parseMeminfo(String[] memorySplit) {
        if (memorySplit.length < 2) {
            return 0l;
        }
        long memory = ParseUtil.parseLongOrDefault(memorySplit[1], 0L);
        if (memorySplit.length > 2 && "kB".equals(memorySplit[2])) {
            memory *= 1024;
        }
        return memory;
    }
}
