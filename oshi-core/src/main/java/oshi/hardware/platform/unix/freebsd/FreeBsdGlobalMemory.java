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

import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * Memory obtained by /proc/meminfo and sysinfo.totalram
 *
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class FreeBsdGlobalMemory extends AbstractGlobalMemory {

    private static final long serialVersionUID = 1L;

    public FreeBsdGlobalMemory() {
        this.pageSize = BsdSysctlUtil.sysctl("hw.pagesize", 4096L);
        this.memTotal = BsdSysctlUtil.sysctl("hw.physmem", 0L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateMeminfo() {
        long inactive = BsdSysctlUtil.sysctl("vm.stats.vm.v_inactive_count", 0L);
        long cache = BsdSysctlUtil.sysctl("vm.stats.vm.v_cache_count", 0L);
        long free = BsdSysctlUtil.sysctl("vm.stats.vm.v_free_count", 0L);
        this.memAvailable = (inactive + cache + free) * this.pageSize;
        this.swapPagesIn = BsdSysctlUtil.sysctl("vm.stats.vm.v_swappgsin", 0L);
        this.swapPagesOut = BsdSysctlUtil.sysctl("vm.stats.vm.v_swappgsout", 0L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateSwap() {
        this.swapTotal = BsdSysctlUtil.sysctl("vm.swap_total", 0L);
        String swapInfo = ExecutingCommand.getAnswerAt("swapinfo -k", 1);
        String[] split = ParseUtil.whitespaces.split(swapInfo);
        if (split.length < 5) {
            return;
        }
        this.swapUsed = ParseUtil.parseLongOrDefault(split[2], 0L) << 10;
    }
}
