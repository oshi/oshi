/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.hardware.GlobalMemory;
import oshi.json.NullAwareJsonObjectBuilder;

/**
 * Memory obtained by /proc/meminfo and sysinfo.totalram
 *
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public abstract class AbstractGlobalMemory implements GlobalMemory {

    protected long memTotal = 0L;
    protected long memAvailable = 0L;
    protected long swapTotal = 0L;
    protected long swapUsed = 0L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

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
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("available", getAvailable())
                .add("total", getTotal()).add("swapTotal", getSwapTotal()).add("swapUsed", getSwapUsed()).build();
    }
}
