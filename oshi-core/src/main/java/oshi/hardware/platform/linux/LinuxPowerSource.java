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
package oshi.hardware.platform.linux;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.FileUtil;

/**
 * A Power Source
 *
 * @author widdis[at]gmail[dot]com
 */
public class LinuxPowerSource extends AbstractPowerSource {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LinuxPowerSource.class);

    private static final String PS_PATH = "/sys/class/power_supply/";

    public LinuxPowerSource(String newName, double newRemainingCapacity, double newTimeRemaining) {
        super(newName, newRemainingCapacity, newTimeRemaining);
        LOG.debug("Initialized LinuxPowerSource");
    }

    /**
     * Gets Battery Information
     *
     * @return An array of PowerSource objects representing batteries, etc.
     */
    public static PowerSource[] getPowerSources() {
        // Get list of power source names
        File f = new File(PS_PATH);
        String[] psNames = f.list();
        // Empty directory will give null rather than empty array, so fix
        if (psNames == null) {
            psNames = new String[0];
        }
        List<LinuxPowerSource> psList = new ArrayList<>(psNames.length);
        // For each power source, output various info
        for (String psName : psNames) {
            // Skip if name is ADP* or AC* (AC power supply)
            if (psName.startsWith("ADP") || psName.startsWith("AC")) {
                continue;
            }
            // Skip if can't read uevent file
            List<String> psInfo;
            psInfo = FileUtil.readFile(PS_PATH + psName + "/uevent", false);
            if (psInfo.isEmpty()) {
                continue;
            }
            // Initialize defaults
            boolean isPresent = false;
            boolean isCharging = false;
            String name = "Unknown";
            int energyNow = 0;
            int energyFull = 1;
            int powerNow = 1;
            for (String checkLine : psInfo) {
                if (checkLine.startsWith("POWER_SUPPLY_PRESENT")) {
                    // Skip if not present
                    String[] psSplit = checkLine.split("=");
                    if (psSplit.length > 1) {
                        isPresent = Integer.parseInt(psSplit[1]) > 0;
                    }
                    if (!isPresent) {
                        break;
                    }
                } else if (checkLine.startsWith("POWER_SUPPLY_NAME")) {
                    // Name
                    String[] psSplit = checkLine.split("=");
                    if (psSplit.length > 1) {
                        name = psSplit[1];
                    }
                } else if (checkLine.startsWith("POWER_SUPPLY_ENERGY_NOW")
                        || checkLine.startsWith("POWER_SUPPLY_CHARGE_NOW")) {
                    // Remaining Capacity = energyNow / energyFull
                    String[] psSplit = checkLine.split("=");
                    if (psSplit.length > 1) {
                        energyNow = Integer.parseInt(psSplit[1]);
                    }
                } else if (checkLine.startsWith("POWER_SUPPLY_ENERGY_FULL")
                        || checkLine.startsWith("POWER_SUPPLY_CHARGE_FULL")) {
                    String[] psSplit = checkLine.split("=");
                    if (psSplit.length > 1) {
                        energyFull = Integer.parseInt(psSplit[1]);
                    }
                } else if (checkLine.startsWith("POWER_SUPPLY_STATUS")) {
                    // Check if charging
                    String[] psSplit = checkLine.split("=");
                    if (psSplit.length > 1 && "Charging".equals(psSplit[1])) {
                        isCharging = true;
                    }
                } else if (checkLine.startsWith("POWER_SUPPLY_POWER_NOW")
                        || checkLine.startsWith("POWER_SUPPLY_CURRENT_NOW")) {
                    // Time Remaining = energyNow / powerNow (hours)
                    String[] psSplit = checkLine.split("=");
                    if (psSplit.length > 1) {
                        powerNow = Integer.parseInt(psSplit[1]);
                    }
                    if (powerNow <= 0) {
                        isCharging = true;
                    }
                }
            }
            if (isPresent) {
                psList.add(new LinuxPowerSource(name, (double) energyNow / energyFull,
                        isCharging ? -2d : 3600d * energyNow / powerNow));
            }
        }

        return psList.toArray(new LinuxPowerSource[psList.size()]);
    }
}
