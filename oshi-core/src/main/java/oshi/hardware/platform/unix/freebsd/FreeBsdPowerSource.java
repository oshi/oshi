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
package oshi.hardware.platform.unix.freebsd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * A Power Source
 *
 * @author widdis[at]gmail[dot]com
 */
public class FreeBsdPowerSource extends AbstractPowerSource {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdPowerSource.class);

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
        // state 0=full, 1=discharging, 2=charging
        int state = BsdSysctlUtil.sysctl("hw.acpi.battery.state", 0);
        // time is in minutes
        int time = BsdSysctlUtil.sysctl("hw.acpi.battery.time", -1);
        // life is in percent
        int life = BsdSysctlUtil.sysctl("hw.acpi.battery.life", 100);
        String name = "BAT0";
        double timeRemaining = -2d;
        if (state < 2) {
            timeRemaining = time < 0 ? -1d : 60d * time;
        }
        ps[0] = new FreeBsdPowerSource(name, life / 100d, timeRemaining);
        return ps;
    }
}
