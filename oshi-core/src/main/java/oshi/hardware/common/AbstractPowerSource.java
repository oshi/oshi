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
