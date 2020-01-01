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
package oshi.hardware.common;

import oshi.hardware.GlobalMemory;

/**
 * Memory obtained by /proc/meminfo and sysinfo.totalram
 *
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public abstract class AbstractGlobalMemory implements GlobalMemory {

    private static final long serialVersionUID = 1L;

    protected long memTotal = 0L;
    protected long memAvailable = 0L;
    protected long swapTotal = 0L;
    protected long swapUsed = 0L;
    protected long swapPagesIn = 0L;
    protected long swapPagesOut = 0L;
    protected long pageSize = 0L;

    /**
     * Updates physical memory instance variables.
     */
    protected abstract void updateMeminfo();

    /**
     * Updates virtual memory instance variables.
     */
    protected abstract void updateSwap();

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAvailable() {
        updateMeminfo();
        return this.memAvailable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotal() {
        if (this.memTotal == 0) {
            updateMeminfo();
        }
        return this.memTotal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapUsed() {
        updateSwap();
        return this.swapUsed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapTotal() {
        updateSwap();
        return this.swapTotal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapPagesIn() {
        updateMeminfo();
        return this.swapPagesIn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapPagesOut() {
        updateMeminfo();
        return this.swapPagesOut;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPageSize() {
        if (this.pageSize == 0) {
            updateMeminfo();
        }
        return this.pageSize;
    }
}
