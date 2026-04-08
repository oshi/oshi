/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static oshi.ffm.windows.PowrProfFFM.BATTERY_INFORMATION;
import static oshi.ffm.windows.PowrProfFFM.BATTERY_MANUFACTURE_DATE;
import static oshi.ffm.windows.PowrProfFFM.BATTERY_QUERY_INFORMATION;
import static oshi.ffm.windows.PowrProfFFM.BATTERY_STATUS;
import static oshi.ffm.windows.PowrProfFFM.BATTERY_WAIT_STATUS;
import static oshi.ffm.windows.PowrProfFFM.OFF_BI_CAPABILITIES;
import static oshi.ffm.windows.PowrProfFFM.OFF_BI_CHEMISTRY;
import static oshi.ffm.windows.PowrProfFFM.OFF_BI_CYCLE;
import static oshi.ffm.windows.PowrProfFFM.OFF_BI_DESIGNED;
import static oshi.ffm.windows.PowrProfFFM.OFF_BI_FULL;
import static oshi.ffm.windows.PowrProfFFM.OFF_BMD_DAY;
import static oshi.ffm.windows.PowrProfFFM.OFF_BMD_MONTH;
import static oshi.ffm.windows.PowrProfFFM.OFF_BMD_YEAR;
import static oshi.ffm.windows.PowrProfFFM.OFF_BQI_ATRATE;
import static oshi.ffm.windows.PowrProfFFM.OFF_BQI_LEVEL;
import static oshi.ffm.windows.PowrProfFFM.OFF_BQI_TAG;
import static oshi.ffm.windows.PowrProfFFM.OFF_BS_CAPACITY;
import static oshi.ffm.windows.PowrProfFFM.OFF_BS_POWERSTATE;
import static oshi.ffm.windows.PowrProfFFM.OFF_BS_RATE;
import static oshi.ffm.windows.PowrProfFFM.OFF_BS_VOLTAGE;
import static oshi.ffm.windows.PowrProfFFM.OFF_BWS_TAG;
import static oshi.ffm.windows.SetupApiFFM.DIGCF_DEVICEINTERFACE;
import static oshi.ffm.windows.SetupApiFFM.DIGCF_PRESENT;
import static oshi.ffm.windows.SetupApiFFM.SP_DEVICE_INTERFACE_DATA;
import static oshi.ffm.windows.WinNTFFM.FILE_ATTRIBUTE_NORMAL;
import static oshi.ffm.windows.WinNTFFM.FILE_SHARE_READ;
import static oshi.ffm.windows.WinNTFFM.FILE_SHARE_WRITE;
import static oshi.ffm.windows.WinNTFFM.GENERIC_READ;
import static oshi.ffm.windows.WinNTFFM.GENERIC_WRITE;
import static oshi.ffm.windows.WinNTFFM.OPEN_EXISTING;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.windows.Kernel32FFM;
import oshi.ffm.windows.SetupApiFFM;
import oshi.ffm.windows.WindowsForeignFunctions;
import oshi.ffm.windows.com.GuidFFM;
import oshi.hardware.PowerSource;
import oshi.util.Constants;

/**
 * Windows power source implementation using FFM.
 */
@ThreadSafe
public final class WindowsPowerSourceFFM extends WindowsPowerSource {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsPowerSourceFFM.class);

    public WindowsPowerSourceFFM(String psName, String psDeviceName, double psRemainingCapacityPercent,
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
        double psTimeRemainingEstimated = -1d;
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
        // Enumerate batteries and ask each one for information.
        // Ported from:
        // https://docs.microsoft.com/en-us/windows/win32/power/enumerating-battery-devices
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment classGuid = GuidFFM.GUID_DEVCLASS_BATTERY(arena);
            Optional<MemorySegment> hdevOpt = SetupApiFFM.SetupDiGetClassDevs(classGuid,
                    DIGCF_PRESENT | DIGCF_DEVICEINTERFACE);
            if (hdevOpt.isEmpty()) {
                LOG.warn("SetupDiGetClassDevs failed for battery class");
            } else {
                MemorySegment hdev = hdevOpt.get();
                try {
                    boolean batteryFound = false;
                    for (int idev = 0; !batteryFound && idev < 100; idev++) {
                        MemorySegment did = arena.allocate(SP_DEVICE_INTERFACE_DATA);
                        did.set(JAVA_INT, 0, (int) SP_DEVICE_INTERFACE_DATA.byteSize());

                        int enumResult = SetupApiFFM.SetupDiEnumDeviceInterfaces(hdev, classGuid, idev, did);
                        if (enumResult == 0) {
                            break;
                        }
                        if (enumResult < 0) {
                            continue;
                        }

                        int requiredSize = SetupApiFFM.SetupDiGetDeviceInterfaceDetailSize(hdev, did, arena);
                        if (requiredSize == 0) {
                            continue;
                        }

                        Optional<String> devicePathOpt = SetupApiFFM.SetupDiGetDeviceInterfaceDetail(hdev, did,
                                requiredSize, arena);
                        if (devicePathOpt.isEmpty()) {
                            continue;
                        }

                        MemorySegment devicePathSeg = WindowsForeignFunctions.toWideString(arena, devicePathOpt.get());
                        Optional<MemorySegment> hBatteryOpt = Kernel32FFM.CreateFile(devicePathSeg,
                                GENERIC_READ | GENERIC_WRITE, FILE_SHARE_READ | FILE_SHARE_WRITE, OPEN_EXISTING,
                                FILE_ATTRIBUTE_NORMAL);
                        if (hBatteryOpt.isEmpty()) {
                            continue;
                        }
                        MemorySegment hBattery = hBatteryOpt.get();
                        try {
                            // Ask the battery for its tag
                            MemorySegment dwWait = arena.allocate(JAVA_INT);
                            MemorySegment dwTag = arena.allocate(JAVA_INT);
                            if (!Kernel32FFM.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_TAG, dwWait,
                                    (int) dwWait.byteSize(), dwTag, (int) dwTag.byteSize())) {
                                continue;
                            }
                            int batteryTag = dwTag.get(JAVA_INT, 0);
                            if (batteryTag == 0) {
                                continue;
                            }

                            MemorySegment bqi = arena.allocate(BATTERY_QUERY_INFORMATION);
                            bqi.set(JAVA_INT, OFF_BQI_TAG, batteryTag);
                            bqi.set(JAVA_INT, OFF_BQI_LEVEL, BATTERY_INFORMATION_LEVEL);

                            MemorySegment bi = arena.allocate(BATTERY_INFORMATION);
                            if (!Kernel32FFM.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_INFORMATION, bqi,
                                    (int) bqi.byteSize(), bi, (int) bi.byteSize())) {
                                continue;
                            }

                            int capabilities = bi.get(JAVA_INT, OFF_BI_CAPABILITIES);
                            // Only non-UPS system batteries count
                            if (0 == (capabilities & BATTERY_SYSTEM_BATTERY)
                                    || 0 != (capabilities & BATTERY_IS_SHORT_TERM)) {
                                continue;
                            }

                            if (0 == (capabilities & BATTERY_CAPACITY_RELATIVE)) {
                                psCapacityUnits = CapacityUnits.MWH;
                            }
                            byte[] chemBytes = new byte[4];
                            for (int i = 0; i < 4; i++) {
                                chemBytes[i] = bi.get(JAVA_BYTE, OFF_BI_CHEMISTRY + i);
                            }
                            psChemistry = new String(chemBytes, StandardCharsets.US_ASCII).trim();
                            psDesignCapacity = bi.get(JAVA_INT, OFF_BI_DESIGNED);
                            psMaxCapacity = bi.get(JAVA_INT, OFF_BI_FULL);
                            psCycleCount = bi.get(JAVA_INT, OFF_BI_CYCLE);
                            int maxCapacitySafe = psMaxCapacity > 0 ? psMaxCapacity
                                    : psDesignCapacity > 0 ? psDesignCapacity : 1;

                            // Query battery status
                            MemorySegment bws = arena.allocate(BATTERY_WAIT_STATUS);
                            bws.set(JAVA_INT, OFF_BWS_TAG, batteryTag);
                            MemorySegment bs = arena.allocate(BATTERY_STATUS);
                            if (Kernel32FFM.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_STATUS, bws,
                                    (int) bws.byteSize(), bs, (int) bs.byteSize())) {
                                int powerState = bs.get(JAVA_INT, OFF_BS_POWERSTATE);
                                if (0 != (powerState & BATTERY_POWER_ON_LINE)) {
                                    psPowerOnLine = true;
                                }
                                if (0 != (powerState & BATTERY_DISCHARGING)) {
                                    psDischarging = true;
                                }
                                if (0 != (powerState & BATTERY_CHARGING)) {
                                    psCharging = true;
                                    psTimeRemainingEstimated = -2d;
                                }
                                psCurrentCapacity = bs.get(JAVA_INT, OFF_BS_CAPACITY);
                                int voltage = bs.get(JAVA_INT, OFF_BS_VOLTAGE);
                                psVoltage = voltage > 0 ? voltage / 1000d : voltage;
                                psPowerUsageRate = bs.get(JAVA_INT, OFF_BS_RATE);
                                if (psVoltage > 0) {
                                    psAmperage = psPowerUsageRate / psVoltage;
                                }
                                psRemainingCapacityPercent = Math.min(1d, (double) psCurrentCapacity / maxCapacitySafe);
                            }

                            MemorySegment nameBuf = arena.allocate(1024);
                            psDeviceName = batteryQueryString(hBattery, batteryTag, BATTERY_DEVICE_NAME_LEVEL, bqi,
                                    nameBuf);
                            psManufacturer = batteryQueryString(hBattery, batteryTag, BATTERY_MANUFACTURE_NAME_LEVEL,
                                    bqi, nameBuf);
                            psSerialNumber = batteryQueryString(hBattery, batteryTag, BATTERY_SERIAL_NUMBER_LEVEL, bqi,
                                    nameBuf);

                            // Manufacture date
                            bqi.set(JAVA_INT, OFF_BQI_LEVEL, BATTERY_MANUFACTURE_DATE_LEVEL);
                            MemorySegment bmd = arena.allocate(BATTERY_MANUFACTURE_DATE);
                            if (Kernel32FFM.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_INFORMATION, bqi,
                                    (int) bqi.byteSize(), bmd, (int) bmd.byteSize())) {
                                int year = bmd.get(JAVA_SHORT, OFF_BMD_YEAR) & 0xFFFF;
                                int month = bmd.get(JAVA_BYTE, OFF_BMD_MONTH) & 0xFF;
                                int day = bmd.get(JAVA_BYTE, OFF_BMD_DAY) & 0xFF;
                                if (year > 1900 && month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                                    try {
                                        psManufactureDate = LocalDate.of(year, month, day);
                                    } catch (java.time.DateTimeException ignored) {
                                        // malformed firmware date — leave null
                                    }
                                }
                            }

                            // Temperature (1/10 degree K)
                            bqi.set(JAVA_INT, OFF_BQI_LEVEL, BATTERY_TEMPERATURE_LEVEL);
                            MemorySegment tempK = arena.allocate(JAVA_INT);
                            if (Kernel32FFM.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_INFORMATION, bqi,
                                    (int) bqi.byteSize(), tempK, (int) tempK.byteSize())) {
                                psTemperature = tempK.get(JAVA_INT, 0) / 10d - 273.15;
                            }

                            // Estimated time — put last because we change AtRate
                            bqi.set(JAVA_INT, OFF_BQI_LEVEL, BATTERY_ESTIMATED_TIME_LEVEL);
                            if (psPowerUsageRate != 0) {
                                bqi.set(JAVA_INT, OFF_BQI_ATRATE, psPowerUsageRate);
                            }
                            MemorySegment tr = arena.allocate(JAVA_INT);
                            if (Kernel32FFM.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_INFORMATION, bqi,
                                    (int) bqi.byteSize(), tr, (int) tr.byteSize())) {
                                psTimeRemainingInstant = tr.get(JAVA_INT, 0);
                            }
                            // Fallback if BatteryEstimatedTime query failed
                            if (psTimeRemainingInstant <= 0 && psPowerUsageRate != 0) {
                                psTimeRemainingInstant = psDischarging
                                        ? Math.max(0d, psCurrentCapacity * 3600d / Math.abs(psPowerUsageRate))
                                        : Math.max(0d, (maxCapacitySafe - psCurrentCapacity) * 3600d
                                                / Math.abs(psPowerUsageRate));
                            }
                            if (psDischarging && psTimeRemainingInstant > 0) {
                                psTimeRemainingEstimated = psTimeRemainingInstant;
                            }

                            batteryFound = true;
                        } finally {
                            Kernel32FFM.CloseHandle(hBattery);
                        }
                    }
                } finally {
                    SetupApiFFM.SetupDiDestroyDeviceInfoList(hdev);
                }
            }
        }
        return new WindowsPowerSourceFFM(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated,
                psTimeRemainingInstant, psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging,
                psDischarging, psCapacityUnits, psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount,
                psChemistry, psManufactureDate, psManufacturer, psSerialNumber, psTemperature);
    }

    private static String batteryQueryString(MemorySegment hBattery, int tag, int infoLevel, MemorySegment bqi,
            MemorySegment nameBuf) {
        bqi.set(JAVA_INT, OFF_BQI_TAG, tag);
        bqi.set(JAVA_INT, OFF_BQI_LEVEL, infoLevel);
        if (!Kernel32FFM.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_INFORMATION, bqi, (int) bqi.byteSize(), nameBuf,
                (int) nameBuf.byteSize())) {
            return Constants.UNKNOWN;
        }
        String result = WindowsForeignFunctions.readWideString(nameBuf);
        return result.isEmpty() ? Constants.UNKNOWN : result;
    }
}
