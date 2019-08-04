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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * A Power Source
 */
public class FreeBsdPowerSource extends AbstractPowerSource {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdPowerSource.class);

    /**
     * <p>
     * Constructor for FreeBsdPowerSource.
     * </p>
     *
     * @param newName
     *            a {@link java.lang.String} object.
     * @param newRemainingCapacity
     *            a double.
     * @param newTimeRemaining
     *            a double.
     */
    public FreeBsdPowerSource(String newName, double newRemainingCapacity, double newTimeRemaining) {
        super(newName, newRemainingCapacity, newTimeRemaining);
        LOG.debug("Initialized FreeBsdPowerSource");
    }

    /**
     * Gets Battery Information
     *
     * @return An array of PowerSource objects representing batteries, etc.
     */
    public static PowerSource[] getPowerSources() {
        FreeBsdPowerSource[] ps = new FreeBsdPowerSource[1];
        ps[0] = getPowerSource("BAT0");
        return ps;
    }

    private static FreeBsdPowerSource getPowerSource(String name) {
        // state 0=full, 1=discharging, 2=charging
        int state = BsdSysctlUtil.sysctl("hw.acpi.battery.state", 0);
        // time is in minutes
        int time = BsdSysctlUtil.sysctl("hw.acpi.battery.time", -1);
        // life is in percent
        int life = BsdSysctlUtil.sysctl("hw.acpi.battery.life", 100);
        double timeRemaining = -2d;
        if (state < 2) {
            timeRemaining = time < 0 ? -1d : 60d * time;
        }
        return new FreeBsdPowerSource(name, life / 100d, timeRemaining);
    }

    /** {@inheritDoc} */
    @Override
    public void updateAttributes() {
        PowerSource ps = getPowerSource(this.name);
        this.remainingCapacity = ps.getRemainingCapacity();
        this.timeRemaining = ps.getTimeRemaining();
    }
}
