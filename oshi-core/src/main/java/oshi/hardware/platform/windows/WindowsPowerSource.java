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
import com.sun.jna.platform.win32.PowrProf.POWER_INFORMATION_LEVEL;

import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.jna.platform.windows.PowrProf;
import oshi.jna.platform.windows.PowrProf.SystemBatteryState;
import oshi.util.FormatUtil;

/**
 * A Power Source
 */
public class WindowsPowerSource extends AbstractPowerSource {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsPowerSource.class);

    /**
     * <p>
     * Constructor for WindowsPowerSource.
     * </p>
     *
     * @param newName
     *            a {@link java.lang.String} object.
     * @param newRemainingCapacity
     *            a double.
     * @param newTimeRemaining
     *            a double.
     */
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
        WindowsPowerSource[] psArray = new WindowsPowerSource[1];
        psArray[0] = getPowerSource("System Battery");
        return psArray;
    }

    private static WindowsPowerSource getPowerSource(String name) {
        long maxCapacity = -1; // mWh
        long remainingCapacity = -1; // mWh
        long estimatedTimeRaw = -1; // Seconds
        long rate = 0; // mW, signed, charging or discharging
        // AcOnLine
        // BatteryPresent
        // Charging
        // Discharging
        // TODO: IOCTL_BATTERY_QUERY_INFORMATION
        // Capabilities -- units flag
        // Chemistry (NiMH etc.)
        // Design & FullCharge
        // CycleCount
        // TODO:
        // Manufacture Date
        // Manufacturer
        // SerialNumber
        // Temperature
        // #define FILE_DEVICE_BATTERY 0x00000029
        // #define METHOD_BUFFERED 0
        // #define FILE_READ_ACCESS 0x0001
        // CTL_CODE (((DeviceType) << 16) | ((Access) << 14) | ((Function) << 2) |
        // (Method))
        // IOCTL_BATTERY_QUERY_INFORMATION
        // #define IOCTL_BATTERY_QUERY_TAG CTL_CODE(FILE_DEVICE_BATTERY, 0x10,
        // METHOD_BUFFERED, FILE_READ_ACCESS)
        // #define IOCTL_BATTERY_QUERY_INFORMATION CTL_CODE(FILE_DEVICE_BATTERY, 0x11,
        // METHOD_BUFFERED, FILE_READ_ACCESS)
        // #define IOCTL_BATTERY_SET_INFORMATION CTL_CODE(FILE_DEVICE_BATTERY, 0x12,
        // METHOD_BUFFERED, FILE_WRITE_ACCESS)
        // #define IOCTL_BATTERY_QUERY_STATUS CTL_CODE(FILE_DEVICE_BATTERY, 0x13,
        // METHOD_BUFFERED, FILE_READ_ACCESS)

        // Kernel32.INSTANCE.DeviceIoControl(hBattery, dwIoControlCode, lpInBuffer,
        // nInBufferSize, lpOutBuffer, nOutBufferSize, lpBytesReturned, lpOverlapped)

        // Enumerate battery services:
        // https://docs.microsoft.com/en-us/windows/win32/power/enumerating-battery-devices
        int size = new SystemBatteryState().size();
        Memory mem = new Memory(size);
        if (0 == PowrProf.INSTANCE.CallNtPowerInformation(POWER_INFORMATION_LEVEL.SystemBatteryState, null, 0, mem,
                size)) {
            SystemBatteryState batteryState = new SystemBatteryState(mem);
            if (batteryState.batteryPresent > 0) {
                int estimatedTime = -2; // -1 = unknown, -2 = unlimited
                if (batteryState.acOnLine == 0 && batteryState.charging == 0 && batteryState.discharging > 0) {
                    estimatedTime = batteryState.estimatedTime;
                }
                maxCapacity = FormatUtil.getUnsignedInt(batteryState.maxCapacity);
                remainingCapacity = FormatUtil.getUnsignedInt(batteryState.remainingCapacity);
                estimatedTimeRaw = FormatUtil.getUnsignedInt(batteryState.estimatedTime);
                rate = batteryState.rate;
                return new WindowsPowerSource(name, (double) remainingCapacity / maxCapacity, estimatedTime);
            }
        }
        return new WindowsPowerSource("Unknown", 0d, -1d);
    }

    /** {@inheritDoc} */
    @Override
    public void updateAttributes() {
        PowerSource ps = getPowerSource(this.name);
        this.remainingCapacity = ps.getRemainingCapacity();
        this.timeRemaining = ps.getTimeRemaining();
    }
}
