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
package oshi.hardware.common;

import oshi.hardware.VirtualMemory;

/**
 * Virtual Memory info.
 */
public abstract class AbstractVirtualMemory implements VirtualMemory {

    private static final long serialVersionUID = 1L;

    protected long swapTotal = -1L;
    protected long swapUsed = -1L;
    protected long swapPagesIn = -1L;
    protected long swapPagesOut = -1L;

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapUsed() {
        if (this.swapUsed < 0) {
            updateAttributes();
        }
        return this.swapUsed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapTotal() {
        if (this.swapTotal < 0) {
            updateAttributes();
        }
        return this.swapTotal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapPagesIn() {
        if (this.swapPagesIn < 0) {
            updateAttributes();
        }
        return this.swapPagesIn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSwapPagesOut() {
        if (this.swapPagesOut < 0) {
            updateAttributes();
        }
        return this.swapPagesOut;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateAttributes() {
        this.swapTotal = -1L;
        this.swapUsed = -1L;
        this.swapPagesIn = -1L;
        this.swapPagesOut = -1L;
    }
}
