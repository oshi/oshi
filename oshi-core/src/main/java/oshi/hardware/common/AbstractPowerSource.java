/**
 * Oshi (https://github.com/oshi/oshi)
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
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.common;

import oshi.hardware.PowerSource;

/**
 * A Power Source
 *
 * @author widdis[at]gmail[dot]com
 * @author ethanjaszewski[at]yahoo[dot]com
 */
public abstract class AbstractPowerSource implements PowerSource {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected double remainingCapacity;

    protected double timeRemaining;
    
    protected double health;

    protected long maximumCharge;
    
    protected long remainingCharge;
    
    protected long power;
    
    /**
     * Super constructor formerly used by platform-specific implementations
     * of PowerSource
     *
     * @param newName
     *            The name to assign
     * @param newRemainingCapacity
     *            Fraction of remaining capacity
     * @param newTimeRemaining
     *            Seconds of time remaining
     */
    @Deprecated
    public AbstractPowerSource(String newName, double newRemainingCapacity, double newTimeRemaining) {
        this.name = newName;
        this.remainingCapacity = newRemainingCapacity;
        this.timeRemaining = newTimeRemaining;
    }
    
    /**
     * Super constructor used by platform specific implementations of
     * PowerSource. Initializes all values to defaults, which are then set
     * using the setter methods.
     */
    public AbstractPowerSource() {
        this.name = "unknown";
        this.remainingCapacity = 0;
        this.timeRemaining = -1;
        this.health = 1d;
        this.maximumCharge = 0;
        this.remainingCharge = 0;
        this.power = 1;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public double getHealth() {
        return health;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMaximumCharge() {
        return maximumCharge;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRemainingCharge() {
        return remainingCharge;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPower() {
        return power;
    }

    /**
     * @param name 
     *             the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param remainingCapacity 
     *             the remainingCapacity to set
     */
    public void setRemainingCapacity(double remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    /**
     * @param timeRemaining
     *             the timeRemaining to set
     */
    public void setTimeRemaining(double timeRemaining) {
        this.timeRemaining = timeRemaining;
    }

    /**
     * @param health 
     *             the health to set
     */
    public void setHealth(double health) {
        this.health = health;
    }

    /**
     * @param maximumCharge 
     *             the maximumCharge to set
     */
    public void setMaximumCharge(long maximumCharge) {
        this.maximumCharge = maximumCharge;
    }

    /**
     * @param remainingCharge
     *             the remainingCharge to set
     */
    public void setRemainingCharge(long remainingCharge) {
        this.remainingCharge = remainingCharge;
    }

    /**
     * @param power
     *             the power to set
     */
    public void setPower(long power) {
        this.power = power;
    }
    
}
