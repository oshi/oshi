/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
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

    public AbstractGlobalMemory() {}

    public AbstractGlobalMemory(long memTotal, long memAvailable, long swapTotal, long swapUsed) {
        this.memTotal = memTotal;
        this.memAvailable = memAvailable;
        this.swapTotal = swapTotal;
        this.swapUsed = swapUsed;
    }

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
    public long getMemAvailable() {
        updateMeminfo();
        return this.memAvailable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMemTotal() {
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
    public void setSwapTotal(long swapTotal) {
        this.swapTotal = swapTotal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSwapUsed(long swapUsed) {
        this.swapUsed = swapUsed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMemTotal(long memTotal) {
        this.memTotal = memTotal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMemAvailable(long memAvailable) {
        this.memAvailable = memAvailable;
    }
}
