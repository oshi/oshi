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
package oshi.hardware.platform.unix.freebsd;

import oshi.hardware.common.AbstractVirtualMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * Memory obtained by sysctl vm.stats
 */
public class FreeBsdVirtualMemory extends AbstractVirtualMemory {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapUsed() {
        String swapInfo = ExecutingCommand.getAnswerAt("swapinfo -k", 1);
        String[] split = ParseUtil.whitespaces.split(swapInfo);
        if (split.length < 5) {
            return 0L;
        }
        return ParseUtil.parseLongOrDefault(split[2], 0L) << 10;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapTotal() {
        return BsdSysctlUtil.sysctl("vm.swap_total", 0L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapPagesIn() {
        return BsdSysctlUtil.sysctl("vm.stats.vm.v_swappgsin", 0L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapPagesOut() {
        return BsdSysctlUtil.sysctl("vm.stats.vm.v_swappgsout", 0L);
    }
}
