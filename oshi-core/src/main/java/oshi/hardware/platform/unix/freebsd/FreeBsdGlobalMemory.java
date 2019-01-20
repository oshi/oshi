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

import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * Memory obtained by sysctl vm.stats
 */
public class FreeBsdGlobalMemory extends AbstractGlobalMemory {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAvailable() {
        if (this.memAvailable < 0) {
            long inactive = BsdSysctlUtil.sysctl("vm.stats.vm.v_inactive_count", 0L);
            long cache = BsdSysctlUtil.sysctl("vm.stats.vm.v_cache_count", 0L);
            long free = BsdSysctlUtil.sysctl("vm.stats.vm.v_free_count", 0L);
            this.memAvailable = (inactive + cache + free) * getPageSize();
        }
        return this.memAvailable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotal() {
        if (this.memTotal < 0) {
            this.memTotal = BsdSysctlUtil.sysctl("hw.physmem", 0L);
        }
        return this.memTotal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPageSize() {
        if (this.pageSize < 0) {
            this.pageSize = BsdSysctlUtil.sysctl("hw.pagesize", 4096L);
        }
        return this.pageSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VirtualMemory getVirtualMemory() {
        if (this.virtualMemory == null) {
            this.virtualMemory = new FreeBsdVirtualMemory();
        }
        return this.virtualMemory;
    }
}
