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
package oshi.hardware.platform.unix.solaris;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * A Power Source
 * 
 * @author widdis[at]gmail[dot]com
 */
public class SolarisPowerSource extends AbstractPowerSource {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(SolarisPowerSource.class);

    public SolarisPowerSource(String newName, double newRemainingCapacity, double newTimeRemaining) {
        super(newName, newRemainingCapacity, newTimeRemaining);
        LOG.debug("Initialized LinuxPowerSource");
    }

    /**
     * Gets Battery Information
     * 
     * @return An array of PowerSource objects representing batteries, etc.
     */
    public static PowerSource[] getPowerSources() {
        SolarisPowerSource[] ps = new SolarisPowerSource[1];
        ArrayList<String> batInfo = ExecutingCommand.runNative("kstat -m acpi_drv");
        if (batInfo.isEmpty()) {
            batInfo = ExecutingCommand.runNative("kstat -m battery");
        }
        // If still empty...
        if (batInfo.isEmpty()) {
            return new SolarisPowerSource[0];
        }
        boolean isCharging = false;
        String name = "BST0";
        int energyNow = -1;
        // defaults to avoid divide by zero
        int energyFull = 1;
        int powerNow = 1;
        for (String line : batInfo) {
            String[] splitLine = line.trim().split("\\s+");
            if (splitLine.length < 2) {
                break;
            }
            switch (splitLine[0]) {
            case "bst_rate":
                // int rate in mA or mW
                powerNow = ParseUtil.parseIntOrDefault(splitLine[1], 1);
                break;
            case "bif_last_cap":
                // full capacity in mAh or mWh
                energyFull = ParseUtil.parseIntOrDefault(splitLine[1], 1);
                break;
            case "bif_rem_cap":
                // remaining capacity in mAh or mWh
                energyNow = ParseUtil.parseIntOrDefault(splitLine[1], 0);
                break;
            case "bst_state":
                // bit 0 = discharging
                // bit 1 = charging
                // bit 2 = critical energy state
                isCharging = (ParseUtil.parseIntOrDefault(splitLine[1], 0) & 0x10) > 0;
                break;
            default:
                // case "bif_unit"
                // 0 -> mW(h), 1 -> mA(h)
                // Math is the same in either case so we ignore it
            }
        }
        if (energyNow < 0) {
            return new SolarisPowerSource[0];
        }
        ps[0] = new SolarisPowerSource(name, (double) energyNow / energyFull,
                isCharging ? -2d : 3600d * energyNow / powerNow);
        return ps;
    }
}
