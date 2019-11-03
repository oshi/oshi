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

import java.time.LocalDate;

import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.Constants;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * A Power Source
 */
public class FreeBsdPowerSource extends AbstractPowerSource {

    public FreeBsdPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
            double psTimeRemainingEstimated, double psTimeRemainingInstant, double psPowerUsageRate, double psVoltage,
            double psAmperage, boolean psPowerOnLine, boolean psCharging, boolean psDischarging,
            CapacityUnits psCapacityUnits, int psCurrentCapacity, int psMaxCapacity, int psDesignCapacity,
            int psCycleCount, String psChemistry, LocalDate psManufactureDate, String psManufacturer,
            String psSerialNumber, double psTemperature) {
        super(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated, psTimeRemainingInstant,
                psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging, psDischarging, psCapacityUnits,
                psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount, psChemistry, psManufactureDate,
                psManufacturer, psSerialNumber, psTemperature);
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
        String psName = name;
        String psDeviceName = Constants.UNKNOWN;
        double psRemainingCapacityPercent = 1d;
        double psTimeRemainingEstimated = -1d; // -1 = unknown, -2 = unlimited
        double psTimeRemainingInstant = 0d;
        double psPowerUsageRate = 0d;
        double psVoltage = -1d;
        double psAmperage = 0d;
        boolean psPowerOnLine = false;
        boolean psCharging = false;
        boolean psDischarging = false;
        CapacityUnits psCapacityUnits = CapacityUnits.RELATIVE;
        int psCurrentCapacity = 0;
        int psMaxCapacity = 1;
        int psDesignCapacity = 1;
        int psCycleCount = -1;
        String psChemistry = Constants.UNKNOWN;
        LocalDate psManufactureDate = null;
        String psManufacturer = Constants.UNKNOWN;
        String psSerialNumber = Constants.UNKNOWN;
        double psTemperature = 0d;

        // state 0=full, 1=discharging, 2=charging
        int state = BsdSysctlUtil.sysctl("hw.acpi.battery.state", 0);
        if (state == 1) {
            psDischarging = true;
        } else if (state == 2) {
            psCharging = true;
        }
        if (state < 2) {
            // time is in minutes
            int time = BsdSysctlUtil.sysctl("hw.acpi.battery.time", -1);
            psTimeRemainingEstimated = time < 0 ? -1d : 60d * time;
        }
        // life is in percent
        int life = BsdSysctlUtil.sysctl("hw.acpi.battery.life", -1);
        if (life > 0) {
            psRemainingCapacityPercent = life / 100d;
        }
// acpiconf -i 0
//        Design capacity:        8400 mAh
//        Last full capacity:     5695 mAh
//        Technology:secondary (rechargeable)
//        Design voltage:11100 mV
//        Capacity (warn):        840 mAh
//        Capacity (low):254 mAh
//        Low/warn granularity:   84 mAh
//        Warn/full granularity:  84 mAh
//        Model number:DELL 3M19002
//        Serial number:2874
//        Type:LION
//        OEM info:Samsung SDI
//        State:high
//        Remaining capacity:     15%
//        Remaining time:unknown
//        Present rate:1 mA (11 mW)
//        Present voltage:        11248 mV
// without power
//        Design capacity:        44000 mWh
//        Last full capacity:     37930 mWh
//        Technology:             secondary (rechargeable)
//        Design voltage:         11100 mV
//        Capacity (warn):        1896 mWh
//        Capacity (low):         200 mWh
//        Low/warn granularity:   1 mWh
//        Warn/full granularity:  1 mWh
//        Model number:           45N1037
//        Serial number:          28608
//        Type:                   LION
//        OEM info:               SANYO
//        State:                  high
//        Remaining capacity:     100%
//        Remaining time:         2:31
//        Present rate:           0 mW
//        Present voltage:        12492 mV
        // or
//        Design capacity:        31320 mWh
//        Last full capacity:     24510 mWh
//        Technology:             secondary (rechargeable)
//        Design voltage:         10800 mV
//        Capacity (warn):        1225 mWh
//        Capacity (low):         200 mWh
//        Low/warn granularity:   1 mWh
//        Warn/full granularity:  1 mWh
//        Model number:           45N1041
//        Serial number:            260
//        Type:                   LiP
//        OEM info:               SONY
//        State:                  discharging
//        Remaining capacity:     98%
//        Remaining time:         1:36
//        Present rate:           14986 mW
//        Present voltage:        11810 mV
        return new FreeBsdPowerSource(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated,
                psTimeRemainingInstant, psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging,
                psDischarging, psCapacityUnits, psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount,
                psChemistry, psManufactureDate, psManufacturer, psSerialNumber, psTemperature);
    }
}
