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
package oshi.hardware.platform.unix.solaris;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; // NOSONAR

import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.solaris.KstatUtil;

/**
 * Memory obtained by /proc/meminfo and sysinfo.totalram
 *
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class SolarisGlobalMemory extends AbstractGlobalMemory {

    private static final long serialVersionUID = 1L;

    private static final Pattern SWAPINFO = Pattern.compile(".+\\s(\\d+)K\\s+(\\d+)K$");

    public SolarisGlobalMemory() {
        this.pageSize = ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("pagesize"), 4096L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateMeminfo() {
        // Get first result
        Kstat ksp = KstatUtil.kstatLookup(null, -1, "system_pages");
        // Set values
        if (ksp != null && KstatUtil.kstatRead(ksp)) {
            this.memAvailable = KstatUtil.kstatDataLookupLong(ksp, "availrmem") * this.pageSize;
            this.memTotal = KstatUtil.kstatDataLookupLong(ksp, "physmem") * this.pageSize;
        }

        this.swapPagesIn = 0L;
        List<String> kstat = ExecutingCommand.runNative("kstat -p cpu_stat:::pgpgin");
        for (String s : kstat) {
            this.swapPagesIn += ParseUtil.parseLastLong(s, 0L);
        }

        this.swapPagesOut = 0L;
        kstat = ExecutingCommand.runNative("kstat -p cpu_stat:::pgpgout");
        for (String s : kstat) {
            this.swapPagesOut += ParseUtil.parseLastLong(s, 0L);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateSwap() {
        String swapInfo = ExecutingCommand.getAnswerAt("swap -lk", 1);
        Matcher m = SWAPINFO.matcher(swapInfo);
        if (m.matches()) {
            this.swapTotal = ParseUtil.parseLongOrDefault(m.group(1), 0L) << 10;
            this.swapUsed = this.swapTotal - (ParseUtil.parseLongOrDefault(m.group(2), 0L) << 10);
        }
    }
}
