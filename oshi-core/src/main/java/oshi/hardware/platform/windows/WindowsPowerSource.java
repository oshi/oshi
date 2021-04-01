/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Memory; // NOSONAR
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.PowrProf.POWER_INFORMATION_LEVEL;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.SetupApi.SP_DEVICE_INTERFACE_DATA;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APITypeMapper;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.jna.platform.windows.PowrProf;
import oshi.jna.platform.windows.PowrProf.BATTERY_INFORMATION;
import oshi.jna.platform.windows.PowrProf.BATTERY_MANUFACTURE_DATE;
import oshi.jna.platform.windows.PowrProf.BATTERY_QUERY_INFORMATION;
import oshi.jna.platform.windows.PowrProf.SystemBatteryState;
import oshi.util.Constants;

/**
 * A Power Source
 */
@ThreadSafe
public final class WindowsPowerSource extends AbstractPowerSource {

    private static final GUID GUID_DEVCLASS_BATTERY = GUID.fromString("{72631E54-78A4-11D0-BCF7-00AA00B7B32A}");
    private static final int CHAR_WIDTH = W32APITypeMapper.DEFAULT == W32APITypeMapper.UNICODE ? 2 : 1;
    private static final boolean X64 = Platform.is64Bit();

    private static final int BATTERY_SYSTEM_BATTERY = 0x80000000;
    private static final int BATTERY_IS_SHORT_TERM = 0x20000000;
    private static final int BATTERY_POWER_ON_LINE = 0x00000001;
    private static final int BATTERY_DISCHARGING = 0x00000002;
    private static final int BATTERY_CHARGING = 0x00000004;
    private static final int BATTERY_CAPACITY_RELATIVE = 0x40000000;

    private static final int IOCTL_BATTERY_QUERY_TAG = 0x294040;
    private static final int IOCTL_BATTERY_QUERY_STATUS = 0x29404c;
    private static final int IOCTL_BATTERY_QUERY_INFORMATION = 0x294044;

    public WindowsPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
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
     * Gets Battery Information.
     *
     * @return A list of PowerSource objects representing batteries, etc.
     */
    public static List<PowerSource> getPowerSources() {
        return Arrays.asList(getPowerSource("System Battery"));
    }

    private static WindowsPowerSource getPowerSource(String name) {
        String psName = name;
        String psDeviceName = Constants.UNKNOWN;
        double psRemainingCapacityPercent = 1d;
        double psTimeRemainingEstimated = -1d; // -1 = unknown, -2 = unlimited
        double psTimeRemainingInstant = 0d;
        int psPowerUsageRate = 0;
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

        // windows PowerSource information comes from two sources: the PowrProf's
        // CallNTPowerInformation function which returns information for a single
        // object, and DeviceIoControl with each battery's (if more than one) handle
        // (which, in theory, return an array of objects but in most cases should return
        // one).
        //
        // We start by fetching the PowrProf information, which will be replicated
        // across all IOCTL entries if there are more than one.

        int size = new SystemBatteryState().size();
        Memory mem = new Memory(size);
        if (0 == PowrProf.INSTANCE.CallNtPowerInformation(POWER_INFORMATION_LEVEL.SystemBatteryState, null, 0, mem,
                size)) {
            SystemBatteryState batteryState = new SystemBatteryState(mem);
            if (batteryState.batteryPresent > 0) {
                if (batteryState.acOnLine == 0 && batteryState.charging == 0 && batteryState.discharging > 0) {
                    psTimeRemainingEstimated = batteryState.estimatedTime;
                } else if (batteryState.charging > 0) {
                    psTimeRemainingEstimated = -2d;
                }
                psMaxCapacity = batteryState.maxCapacity;
                psCurrentCapacity = batteryState.remainingCapacity;
                psRemainingCapacityPercent = Math.min(1d, (double) psCurrentCapacity / psMaxCapacity);
                psPowerUsageRate = batteryState.rate;
            }
        }

        // Enumerate batteries and ask each one for information
        // Ported from:
        // https://docs.microsoft.com/en-us/windows/win32/power/enumerating-battery-devices

        HANDLE hdev = SetupApi.INSTANCE.SetupDiGetClassDevs(GUID_DEVCLASS_BATTERY, null, null,
                SetupApi.DIGCF_PRESENT | SetupApi.DIGCF_DEVICEINTERFACE);
        if (WinBase.INVALID_HANDLE_VALUE != hdev) {
            boolean batteryFound = false;
            // Limit search to 100 batteries max
            for (int idev = 0; !batteryFound && idev < 100; idev++) {
                SP_DEVICE_INTERFACE_DATA did = new SP_DEVICE_INTERFACE_DATA();
                did.cbSize = did.size();

                if (SetupApi.INSTANCE.SetupDiEnumDeviceInterfaces(hdev, null, GUID_DEVCLASS_BATTERY, idev, did)) {
                    IntByReference requiredSize = new IntByReference(0);
                    SetupApi.INSTANCE.SetupDiGetDeviceInterfaceDetail(hdev, did, null, 0, requiredSize, null);
                    if (WinError.ERROR_INSUFFICIENT_BUFFER == Kernel32.INSTANCE.GetLastError()) {
                        // PSP_DEVICE_INTERFACE_DETAIL_DATA: int size + TCHAR array
                        Memory pdidd = new Memory(requiredSize.getValue());
                        // pdidd->cbSize is defined as sizeof(*pdidd)
                        // On 64 bit, cbSize is 8. On 32-bit it's 5 or 6 based on char size
                        // This must be set properly for the method to work but is otherwise ignored
                        pdidd.setInt(0, Integer.BYTES + (X64 ? 4 : CHAR_WIDTH));
                        // Regardless of this setting the string portion starts after one byte
                        if (SetupApi.INSTANCE.SetupDiGetDeviceInterfaceDetail(hdev, did, pdidd, (int) pdidd.size(),
                                requiredSize, null)) {
                            // Enumerated a battery. Ask it for information.
                            String devicePath = CHAR_WIDTH > 1 ? pdidd.getWideString(Integer.BYTES)
                                    : pdidd.getString(Integer.BYTES);
                            HANDLE hBattery = Kernel32.INSTANCE.CreateFile(devicePath, // pdidd->DevicePath
                                    WinNT.GENERIC_READ | WinNT.GENERIC_WRITE,
                                    WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE, null, WinNT.OPEN_EXISTING,
                                    WinNT.FILE_ATTRIBUTE_NORMAL, null);
                            if (!WinBase.INVALID_HANDLE_VALUE.equals(hBattery)) {
                                // Ask the battery for its tag.
                                BATTERY_QUERY_INFORMATION bqi = new PowrProf.BATTERY_QUERY_INFORMATION();
                                IntByReference dwWait = new IntByReference(0);
                                IntByReference dwTag = new IntByReference();
                                IntByReference dwOut = new IntByReference();

                                if (Kernel32.INSTANCE.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_TAG,
                                        dwWait.getPointer(), Integer.BYTES, dwTag.getPointer(), Integer.BYTES, dwOut,
                                        null)) {
                                    bqi.BatteryTag = dwTag.getValue();
                                    if (bqi.BatteryTag > 0) {
                                        // With the tag, you can query the battery info.
                                        bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryInformation
                                                .ordinal();
                                        bqi.write();
                                        BATTERY_INFORMATION bi = new BATTERY_INFORMATION();
                                        if (Kernel32.INSTANCE.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_INFORMATION,
                                                bqi.getPointer(), bqi.size(), bi.getPointer(), bi.size(), dwOut,
                                                null)) {
                                            // Only non-UPS system batteries count
                                            bi.read();
                                            if (0 != (bi.Capabilities & BATTERY_SYSTEM_BATTERY)
                                                    && 0 == (bi.Capabilities & BATTERY_IS_SHORT_TERM)) {
                                                // Capabilities flags non-mWh units
                                                if (0 == (bi.Capabilities & BATTERY_CAPACITY_RELATIVE)) {
                                                    psCapacityUnits = CapacityUnits.MWH;
                                                }
                                                psChemistry = Native.toString(bi.Chemistry, StandardCharsets.US_ASCII);
                                                psDesignCapacity = bi.DesignedCapacity;
                                                psMaxCapacity = bi.FullChargedCapacity;
                                                psCycleCount = bi.CycleCount;

                                                // Query the battery status.
                                                PowrProf.BATTERY_WAIT_STATUS bws = new PowrProf.BATTERY_WAIT_STATUS();
                                                bws.BatteryTag = bqi.BatteryTag;
                                                bws.write();
                                                PowrProf.BATTERY_STATUS bs = new PowrProf.BATTERY_STATUS();
                                                if (Kernel32.INSTANCE.DeviceIoControl(hBattery,
                                                        IOCTL_BATTERY_QUERY_STATUS, bws.getPointer(), bws.size(),
                                                        bs.getPointer(), bs.size(), dwOut, null)) {
                                                    bs.read();
                                                    if (0 != (bs.PowerState & BATTERY_POWER_ON_LINE)) {
                                                        psPowerOnLine = true;
                                                    }
                                                    if (0 != (bs.PowerState & BATTERY_DISCHARGING)) {
                                                        psDischarging = true;
                                                    }
                                                    if (0 != (bs.PowerState & BATTERY_CHARGING)) {
                                                        psCharging = true;
                                                    }
                                                    psCurrentCapacity = bs.Capacity;
                                                    psVoltage = bs.Voltage > 0 ? bs.Voltage / 1000d : bs.Voltage;
                                                    psPowerUsageRate = bs.Rate;
                                                    if (psVoltage > 0) {
                                                        psAmperage = psPowerUsageRate / psVoltage;
                                                    }
                                                }
                                            }

                                            psDeviceName = batteryQueryString(hBattery, dwTag.getValue(),
                                                    PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryDeviceName
                                                            .ordinal());
                                            psManufacturer = batteryQueryString(hBattery, dwTag.getValue(),
                                                    PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryManufactureName
                                                            .ordinal());
                                            psSerialNumber = batteryQueryString(hBattery, dwTag.getValue(),
                                                    PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatterySerialNumber
                                                            .ordinal());

                                            bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryManufactureDate
                                                    .ordinal();
                                            bqi.write();
                                            BATTERY_MANUFACTURE_DATE bmd = new BATTERY_MANUFACTURE_DATE();
                                            if (Kernel32.INSTANCE.DeviceIoControl(hBattery,
                                                    IOCTL_BATTERY_QUERY_INFORMATION, bqi.getPointer(), bqi.size(),
                                                    bmd.getPointer(), bmd.size(), dwOut, null)) {
                                                bmd.read();
                                                // If failed, returns -1 for each field
                                                if (bmd.Year > 1900 && bmd.Month > 0 && bmd.Day > 0) {
                                                    psManufactureDate = LocalDate.of(bmd.Year, bmd.Month, bmd.Day);
                                                }
                                            }

                                            bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryTemperature
                                                    .ordinal();
                                            bqi.write();
                                            IntByReference tempK = new IntByReference(); // 1/10 degree K
                                            if (Kernel32.INSTANCE.DeviceIoControl(hBattery,
                                                    IOCTL_BATTERY_QUERY_INFORMATION, bqi.getPointer(), bqi.size(),
                                                    tempK.getPointer(), Integer.BYTES, dwOut, null)) {
                                                psTemperature = tempK.getValue() / 10d - 273.15;
                                            }

                                            // Put last because we change the AtRate field
                                            bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryEstimatedTime
                                                    .ordinal();
                                            if (psPowerUsageRate != 0) {
                                                bqi.AtRate = psPowerUsageRate;
                                            }
                                            bqi.write();
                                            IntByReference tr = new IntByReference();
                                            if (Kernel32.INSTANCE.DeviceIoControl(hBattery,
                                                    IOCTL_BATTERY_QUERY_INFORMATION, bqi.getPointer(), bqi.size(),
                                                    tr.getPointer(), Integer.BYTES, dwOut, null)) {
                                                psTimeRemainingInstant = tr.getValue();
                                            }
                                            // Fallback
                                            if (psTimeRemainingInstant < 0 && psPowerUsageRate != 0) {
                                                psTimeRemainingInstant = (psMaxCapacity - psCurrentCapacity) * 3600d
                                                        / psPowerUsageRate;
                                                if (psTimeRemainingInstant < 0) {
                                                    psTimeRemainingInstant *= -1;
                                                }
                                            }
                                            // Exit loop
                                            batteryFound = true;
                                        }
                                    }
                                }
                                Kernel32.INSTANCE.CloseHandle(hBattery);
                            }
                        }
                    }
                } else if (WinError.ERROR_NO_MORE_ITEMS == Kernel32.INSTANCE.GetLastError()) {
                    break; // Enumeration failed - perhaps we're out of items
                }
            }
            SetupApi.INSTANCE.SetupDiDestroyDeviceInfoList(hdev);
        }

        return new WindowsPowerSource(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated,
                psTimeRemainingInstant, psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging,
                psDischarging, psCapacityUnits, psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount,
                psChemistry, psManufactureDate, psManufacturer, psSerialNumber, psTemperature);
    }

    private static String batteryQueryString(HANDLE hBattery, int tag, int infoLevel) {
        BATTERY_QUERY_INFORMATION bqi = new PowrProf.BATTERY_QUERY_INFORMATION();
        bqi.BatteryTag = tag;
        bqi.InformationLevel = infoLevel;
        bqi.write();
        IntByReference dwOut = new IntByReference();
        boolean ret = false;
        long bufSize = 0;
        Memory nameBuf;
        do {
            // First increment is probably enough
            bufSize += 256;
            nameBuf = new Memory(bufSize);
            ret = Kernel32.INSTANCE.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_INFORMATION, bqi.getPointer(),
                    bqi.size(), nameBuf, (int) nameBuf.size(), dwOut, null);
        } while (!ret && bufSize < 4096);
        return CHAR_WIDTH > 1 ? nameBuf.getWideString(0) : nameBuf.getString(0);
    }
}
