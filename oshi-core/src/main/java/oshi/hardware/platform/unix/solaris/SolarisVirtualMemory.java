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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.hardware.common.AbstractVirtualMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Memory obtained by kstat and swap
 */
public class SolarisVirtualMemory extends AbstractVirtualMemory {

    private static final long serialVersionUID = 1L;

    private static final Pattern SWAPINFO = Pattern.compile(".+\\s(\\d+)K\\s+(\\d+)K$");

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapUsed() {
        if (this.swapUsed < 0) {
            updateSwapUsed();
        }
        return this.swapUsed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapTotal() {
        if (this.swapTotal < 0) {
            updateSwapUsed();
        }
        return this.swapTotal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapPagesIn() {
        if (this.swapPagesIn < 0) {
            this.swapPagesIn = 0L;
            for (String s : ExecutingCommand.runNative("kstat -p cpu_stat:::pgpgin")) {
                this.swapPagesIn += ParseUtil.parseLastLong(s, 0L);
            }
        }
        return this.swapPagesIn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapPagesOut() {
        if (this.swapPagesOut < 0) {
            this.swapPagesOut = 0L;
            for (String s : ExecutingCommand.runNative("kstat -p cpu_stat:::pgpgout")) {
                this.swapPagesOut += ParseUtil.parseLastLong(s, 0L);
            }
        }
        return this.swapPagesOut;
    }

    private void updateSwapUsed() {
        String swapInfo = ExecutingCommand.getAnswerAt("swap -lk", 1);
        Matcher m = SWAPINFO.matcher(swapInfo);
        if (m.matches()) {
            this.swapTotal = ParseUtil.parseLongOrDefault(m.group(1), 0L) << 10;
            this.swapUsed = this.swapTotal - (ParseUtil.parseLongOrDefault(m.group(2), 0L) << 10);
        }
    }
}
