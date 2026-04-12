/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.W32APITypeMapper;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.common.platform.windows.WindowsPowerSource;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.Struct.CloseableSpDeviceInterfaceData;
import oshi.jna.platform.windows.PowrProf.BATTERY_INFORMATION;
import oshi.jna.platform.windows.PowrProf.BATTERY_MANUFACTURE_DATE;
import oshi.jna.platform.windows.PowrProf.BATTERY_QUERY_INFORMATION;
import oshi.jna.platform.windows.PowrProf.BATTERY_STATUS;
import oshi.jna.platform.windows.PowrProf.BATTERY_WAIT_STATUS;
import oshi.util.Constants;

/**
 * JNA-based Windows power source implementation.
 */
@ThreadSafe
public final class WindowsPowerSourceJNA extends WindowsPowerSource {

    private static final GUID GUID_DEVCLASS_BATTERY = GUID.fromString("{72631E54-78A4-11D0-BCF7-00AA00B7B32A}");
    private static final int CHAR_WIDTH = W32APITypeMapper.DEFAULT == W32APITypeMapper.UNICODE ? 2 : 1;
    private static final boolean X64 = Platform.is64Bit();

    public WindowsPowerSourceJNA(String psName, String psDeviceName, double psRemainingCapacityPercent,
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

    @Override
    protected List<PowerSource> queryPowerSources() {
        return getPowerSources();
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

        // Note: CallNtPowerInformation(SystemBatteryState) was previously used as a
        // first pass here but no longer returns accurate data as of Windows 11 23H2.
        // All battery information is now sourced exclusively via DeviceIoControl.
        //
        // Enumerate batteries and ask each one for information
        // Ported from:
        // https://docs.microsoft.com/en-us/windows/win32/power/enumerating-battery-devices

        HANDLE hdev = SetupApi.INSTANCE.SetupDiGetClassDevs(GUID_DEVCLASS_BATTERY, null, null,
                SetupApi.DIGCF_PRESENT | SetupApi.DIGCF_DEVICEINTERFACE);
        if (!WinBase.INVALID_HANDLE_VALUE.equals(hdev)) {
            boolean batteryFound = false;
            // Limit search to 100 batteries max
            for (int idev = 0; !batteryFound && idev < 100; idev++) {
                try (CloseableSpDeviceInterfaceData did = new CloseableSpDeviceInterfaceData();
                        CloseableIntByReference requiredSize = new CloseableIntByReference();
                        CloseableIntByReference dwWait = new CloseableIntByReference();
                        CloseableIntByReference dwTag = new CloseableIntByReference();
                        CloseableIntByReference dwOut = new CloseableIntByReference()) {
                    did.cbSize = did.size();
                    if (SetupApi.INSTANCE.SetupDiEnumDeviceInterfaces(hdev, null, GUID_DEVCLASS_BATTERY, idev, did)) {
                        SetupApi.INSTANCE.SetupDiGetDeviceInterfaceDetail(hdev, did, null, 0, requiredSize, null);
                        if (WinError.ERROR_INSUFFICIENT_BUFFER == Kernel32.INSTANCE.GetLastError()) {
                            // PSP_DEVICE_INTERFACE_DETAIL_DATA: int size + TCHAR array
                            try (Memory pdidd = new Memory(requiredSize.getValue())) {
                                // pdidd->cbSize is defined as sizeof(*pdidd)
                                // On 64 bit, cbSize is 8. On 32-bit it's 5 or 6 based on char size
                                // This must be set properly for the method to work but is otherwise ignored
                                pdidd.setInt(0, Integer.BYTES + (X64 ? 4 : CHAR_WIDTH));
                                // Regardless of this setting the string portion starts after one byte
                                if (SetupApi.INSTANCE.SetupDiGetDeviceInterfaceDetail(hdev, did, pdidd,
                                        (int) pdidd.size(), requiredSize, null)) {
                                    // Enumerated a battery. Ask it for information.
                                    String devicePath = CHAR_WIDTH > 1 ? pdidd.getWideString(Integer.BYTES)
                                            : pdidd.getString(Integer.BYTES);
                                    HANDLE hBattery = Kernel32.INSTANCE.CreateFile(devicePath, // pdidd->DevicePath
                                            WinNT.GENERIC_READ | WinNT.GENERIC_WRITE,
                                            WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE, null, WinNT.OPEN_EXISTING,
                                            WinNT.FILE_ATTRIBUTE_NORMAL, null);
                                    if (!WinBase.INVALID_HANDLE_VALUE.equals(hBattery)) {
                                        try (BATTERY_QUERY_INFORMATION bqi = new BATTERY_QUERY_INFORMATION();
                                                BATTERY_INFORMATION bi = new BATTERY_INFORMATION();
                                                BATTERY_WAIT_STATUS bws = new BATTERY_WAIT_STATUS();
                                                BATTERY_STATUS bs = new BATTERY_STATUS();
                                                BATTERY_MANUFACTURE_DATE bmd = new BATTERY_MANUFACTURE_DATE()) {
                                            // Ask the battery for its tag.
                                            if (Kernel32.INSTANCE.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_TAG,
                                                    dwWait.getPointer(), Integer.BYTES, dwTag.getPointer(),
                                                    Integer.BYTES, dwOut, null)) {
                                                bqi.BatteryTag = dwTag.getValue();
                                                if (bqi.BatteryTag > 0) {
                                                    // With the tag, you can query the battery info.
                                                    bqi.InformationLevel = BATTERY_INFORMATION_LEVEL;
                                                    bqi.write();

                                                    if (Kernel32.INSTANCE.DeviceIoControl(hBattery,
                                                            IOCTL_BATTERY_QUERY_INFORMATION, bqi.getPointer(),
                                                            bqi.size(), bi.getPointer(), bi.size(), dwOut, null)) {
                                                        // Only non-UPS system batteries count
                                                        bi.read();
                                                        int maxCapacitySafe = 1;
                                                        if (0 != (bi.Capabilities & BATTERY_SYSTEM_BATTERY)
                                                                && 0 == (bi.Capabilities & BATTERY_IS_SHORT_TERM)) {
                                                            // Capabilities flags non-mWh units
                                                            if (0 == (bi.Capabilities & BATTERY_CAPACITY_RELATIVE)) {
                                                                psCapacityUnits = CapacityUnits.MWH;
                                                            }
                                                            psChemistry = Native.toString(bi.Chemistry,
                                                                    StandardCharsets.US_ASCII);
                                                            psDesignCapacity = bi.DesignedCapacity;
                                                            psMaxCapacity = bi.FullChargedCapacity;
                                                            psCycleCount = bi.CycleCount;
                                                            maxCapacitySafe = psMaxCapacity > 0 ? psMaxCapacity
                                                                    : psDesignCapacity > 0 ? psDesignCapacity : 1;

                                                            // Query the battery status.
                                                            bws.BatteryTag = bqi.BatteryTag;
                                                            bws.write();
                                                            if (Kernel32.INSTANCE.DeviceIoControl(hBattery,
                                                                    IOCTL_BATTERY_QUERY_STATUS, bws.getPointer(),
                                                                    bws.size(), bs.getPointer(), bs.size(), dwOut,
                                                                    null)) {
                                                                bs.read();
                                                                if (0 != (bs.PowerState & BATTERY_POWER_ON_LINE)) {
                                                                    psPowerOnLine = true;
                                                                }
                                                                if (0 != (bs.PowerState & BATTERY_DISCHARGING)) {
                                                                    psDischarging = true;
                                                                }
                                                                if (0 != (bs.PowerState & BATTERY_CHARGING)) {
                                                                    psCharging = true;
                                                                    psTimeRemainingEstimated = -2d;
                                                                }
                                                                psCurrentCapacity = bs.Capacity;
                                                                psVoltage = bs.Voltage > 0 ? bs.Voltage / 1000d
                                                                        : bs.Voltage;
                                                                psPowerUsageRate = bs.Rate;
                                                                if (psVoltage > 0) {
                                                                    psAmperage = psPowerUsageRate / psVoltage;
                                                                }
                                                                psRemainingCapacityPercent = Math.min(1d,
                                                                        (double) psCurrentCapacity / maxCapacitySafe);
                                                            }
                                                        }

                                                        try (Memory nameBuf = new Memory(1024)) {
                                                            psDeviceName = batteryQueryString(hBattery,
                                                                    dwTag.getValue(), BATTERY_DEVICE_NAME_LEVEL,
                                                                    nameBuf);
                                                            psManufacturer = batteryQueryString(hBattery,
                                                                    dwTag.getValue(), BATTERY_MANUFACTURE_NAME_LEVEL,
                                                                    nameBuf);
                                                            psSerialNumber = batteryQueryString(hBattery,
                                                                    dwTag.getValue(), BATTERY_SERIAL_NUMBER_LEVEL,
                                                                    nameBuf);
                                                        }

                                                        bqi.InformationLevel = BATTERY_MANUFACTURE_DATE_LEVEL;
                                                        bqi.write();

                                                        if (Kernel32.INSTANCE.DeviceIoControl(hBattery,
                                                                IOCTL_BATTERY_QUERY_INFORMATION, bqi.getPointer(),
                                                                bqi.size(), bmd.getPointer(), bmd.size(), dwOut,
                                                                null)) {
                                                            bmd.read();
                                                            // If failed, returns -1 for each field
                                                            if (bmd.Year > 1900 && bmd.Month >= 1 && bmd.Month <= 12
                                                                    && bmd.Day >= 1 && bmd.Day <= 31) {
                                                                try {
                                                                    psManufactureDate = LocalDate.of(bmd.Year,
                                                                            bmd.Month, bmd.Day);
                                                                } catch (DateTimeException ignored) {
                                                                    // malformed firmware date — leave null
                                                                }
                                                            }
                                                        }

                                                        bqi.InformationLevel = BATTERY_TEMPERATURE_LEVEL;
                                                        bqi.write();
                                                        try (CloseableIntByReference tempK = new CloseableIntByReference()) {
                                                            // 1/10 degree K
                                                            if (Kernel32.INSTANCE.DeviceIoControl(hBattery,
                                                                    IOCTL_BATTERY_QUERY_INFORMATION, bqi.getPointer(),
                                                                    bqi.size(), tempK.getPointer(), Integer.BYTES,
                                                                    dwOut, null)) {
                                                                psTemperature = tempK.getValue() / 10d - 273.15;
                                                            }
                                                        }

                                                        // Put last because we change the AtRate field
                                                        bqi.InformationLevel = BATTERY_ESTIMATED_TIME_LEVEL;
                                                        if (psPowerUsageRate != 0) {
                                                            bqi.AtRate = psPowerUsageRate;
                                                        }
                                                        bqi.write();
                                                        try (CloseableIntByReference tr = new CloseableIntByReference()) {
                                                            if (Kernel32.INSTANCE.DeviceIoControl(hBattery,
                                                                    IOCTL_BATTERY_QUERY_INFORMATION, bqi.getPointer(),
                                                                    bqi.size(), tr.getPointer(), Integer.BYTES, dwOut,
                                                                    null)) {
                                                                psTimeRemainingInstant = tr.getValue();
                                                            }
                                                        }
                                                        // Fallback if BatteryEstimatedTime query failed
                                                        if (psTimeRemainingInstant <= 0 && psPowerUsageRate != 0) {
                                                            psTimeRemainingInstant = psDischarging
                                                                    ? Math.max(0d,
                                                                            psCurrentCapacity * 3600d
                                                                                    / Math.abs(psPowerUsageRate))
                                                                    : Math.max(0d, (maxCapacitySafe - psCurrentCapacity)
                                                                            * 3600d / Math.abs(psPowerUsageRate));
                                                        }
                                                        if (psDischarging && psTimeRemainingInstant > 0) {
                                                            psTimeRemainingEstimated = psTimeRemainingInstant;
                                                        }
                                                        // Exit loop
                                                        batteryFound = true;
                                                    }
                                                }
                                            }
                                        } finally {
                                            Kernel32.INSTANCE.CloseHandle(hBattery);
                                        }
                                    }
                                }
                            }
                        }
                    } else if (WinError.ERROR_NO_MORE_ITEMS == Kernel32.INSTANCE.GetLastError()) {
                        break; // Enumeration failed - perhaps we're out of items
                    }
                }
            }
            SetupApi.INSTANCE.SetupDiDestroyDeviceInfoList(hdev);
        }

        return new WindowsPowerSourceJNA(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated,
                psTimeRemainingInstant, psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging,
                psDischarging, psCapacityUnits, psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount,
                psChemistry, psManufactureDate, psManufacturer, psSerialNumber, psTemperature);
    }

    private static String batteryQueryString(HANDLE hBattery, int tag, int infoLevel, Memory nameBuf) {
        try (BATTERY_QUERY_INFORMATION bqi = new BATTERY_QUERY_INFORMATION();
                CloseableIntByReference dwOut = new CloseableIntByReference()) {
            bqi.BatteryTag = tag;
            bqi.InformationLevel = infoLevel;
            bqi.write();
            if (!Kernel32.INSTANCE.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_INFORMATION, bqi.getPointer(),
                    bqi.size(), nameBuf, (int) nameBuf.size(), dwOut, null)) {
                return Constants.UNKNOWN;
            }
            String result = CHAR_WIDTH > 1 ? nameBuf.getWideString(0) : nameBuf.getString(0);
            return result.isEmpty() ? Constants.UNKNOWN : result;
        }
    }
}
