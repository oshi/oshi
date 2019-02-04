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
package oshi.hardware.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; // NOSONAR

import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.jna.platform.windows.PowrProf;
import oshi.jna.platform.windows.PowrProf.POWER_INFORMATION_LEVEL;
import oshi.jna.platform.windows.PowrProf.SystemBatteryState;
import oshi.util.FormatUtil;

/**
 * A Power Source
 *
 * @author widdis[at]gmail[dot]com
 */
public class WindowsPowerSource extends AbstractPowerSource {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsPowerSource.class);

    public WindowsPowerSource(String newName, double newRemainingCapacity, double newTimeRemaining) {
        super(newName, newRemainingCapacity, newTimeRemaining);
        LOG.debug("Initialized WindowsPowerSource");
    }

    /**
     * Gets Battery Information.
     *
     * @return An array of PowerSource objects representing batteries, etc.
     */
    public static PowerSource[] getPowerSources() {
        // Windows provides a single unnamed battery
        String name = "System Battery";
        WindowsPowerSource[] psArray = new WindowsPowerSource[1];
        // Get structure
        int size = new SystemBatteryState().size();
        Memory mem = new Memory(size);
        if (0 == PowrProf.INSTANCE.CallNtPowerInformation(POWER_INFORMATION_LEVEL.SYSTEM_BATTERY_STATE, null, 0, mem,
                size)) {
            SystemBatteryState batteryState = new SystemBatteryState(mem);
            if (batteryState.batteryPresent > 0) {
                int estimatedTime = -2; // -1 = unknown, -2 = unlimited
                if (batteryState.acOnLine == 0 && batteryState.charging == 0 && batteryState.discharging > 0) {
                    estimatedTime = batteryState.estimatedTime;
                }
                long maxCapacity = FormatUtil.getUnsignedInt(batteryState.maxCapacity);
                long remainingCapacity = FormatUtil.getUnsignedInt(batteryState.remainingCapacity);
                psArray[0] = new WindowsPowerSource(name, (double) remainingCapacity / maxCapacity, estimatedTime);
            }
        }
        if (psArray[0] == null) {
            psArray[0] = new WindowsPowerSource("Unknown", 0d, -1d);
        }
        return psArray;
    }
}
