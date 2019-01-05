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
package oshi.hardware.platform.unix.solaris;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; // NOSONAR

import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.platform.unix.solaris.KstatUtil;

/**
 * A Power Source
 *
 * @author widdis[at]gmail[dot]com
 */
public class SolarisPowerSource extends AbstractPowerSource {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(SolarisPowerSource.class);

    /*
     * One-time lookup to see which kstat module to use
     */
    private static final String[] KSTAT_BATT_MOD = { null, "battery", "acpi_drv" };

    private static final int KSTAT_BATT_IDX;

    static {
        if (KstatUtil.kstatLookup(KSTAT_BATT_MOD[1], 0, null) != null) {
            KSTAT_BATT_IDX = 1;
        } else if (KstatUtil.kstatLookup(KSTAT_BATT_MOD[2], 0, null) != null) {
            KSTAT_BATT_IDX = 2;
        } else {
            KSTAT_BATT_IDX = 0;
        }
    }

    public SolarisPowerSource(String newName, double newRemainingCapacity, double newTimeRemaining) {
        super(newName, newRemainingCapacity, newTimeRemaining);
        LOG.debug("Initialized SolarisPowerSource");
    }

    /**
     * Gets Battery Information
     *
     * @return An array of PowerSource objects representing batteries, etc.
     */
    public static PowerSource[] getPowerSources() {
        // If no kstat info, return empty
        if (KSTAT_BATT_IDX == 0) {
            return new SolarisPowerSource[0];
        }
        // Get kstat for the battery information
        Kstat ksp = KstatUtil.kstatLookup(KSTAT_BATT_MOD[KSTAT_BATT_IDX], 0, "battery BIF0");
        if (ksp == null) {
            return new SolarisPowerSource[0];
        }

        // Predicted battery capacity when fully charged.
        long energyFull = KstatUtil.kstatDataLookupLong(ksp, "bif_last_cap");
        if (energyFull == 0xffffffff || energyFull <= 0) {
            energyFull = KstatUtil.kstatDataLookupLong(ksp, "bif_design_cap");
        }
        if (energyFull == 0xffffffff || energyFull <= 0) {
            return new SolarisPowerSource[0];
        }

        // Get kstat for the battery state
        ksp = KstatUtil.kstatLookup(KSTAT_BATT_MOD[KSTAT_BATT_IDX], 0, "battery BST0");
        if (ksp == null) {
            return new SolarisPowerSource[0];
        }

        // estimated remaining battery capacity
        long energyNow = KstatUtil.kstatDataLookupLong(ksp, "bst_rem_cap");
        if (energyNow < 0) {
            return new SolarisPowerSource[0];
        }

        // power or current supplied at battery terminal
        long powerNow = KstatUtil.kstatDataLookupLong(ksp, "bst_rate");
        if (powerNow == 0xFFFFFFFF) {
            powerNow = 0L;
        }

        // Battery State:
        // bit 0 = discharging
        // bit 1 = charging
        // bit 2 = critical energy state
        boolean isCharging = (KstatUtil.kstatDataLookupLong(ksp, "bst_state") & 0x10) > 0;

        // Set up single battery in array
        SolarisPowerSource[] ps = new SolarisPowerSource[1];
        double timeRemaining = -2d;
        if (!isCharging) {
            timeRemaining = powerNow > 0 ? 3600d * energyNow / powerNow : -1d;
        }
        ps[0] = new SolarisPowerSource("BAT0", (double) energyNow / energyFull, timeRemaining);
        return ps;
    }
}
