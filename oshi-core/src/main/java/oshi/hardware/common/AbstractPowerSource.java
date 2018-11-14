/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.common;

import oshi.hardware.PowerSource;

/**
 * A Power Source
 *
 * @author widdis[at]gmail[dot]com
 */
public abstract class AbstractPowerSource implements PowerSource {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected double remainingCapacity;

    protected double timeRemaining;

    /**
     * Super constructor used by platform-specific implementations of
     * PowerSource
     *
     * @param newName
     *            The name to assign
     * @param newRemainingCapacity
     *            Fraction of remaining capacity
     * @param newTimeRemaining
     *            Seconds of time remaining
     */
    public AbstractPowerSource(String newName, double newRemainingCapacity, double newTimeRemaining) {
        this.name = newName;
        this.remainingCapacity = newRemainingCapacity;
        this.timeRemaining = newTimeRemaining;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRemainingCapacity() {
        return this.remainingCapacity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getTimeRemaining() {
        return this.timeRemaining;
    }
}
