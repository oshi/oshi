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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import com.sun.jna.Memory; // NOSONAR
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
public class WindowsPowerSource extends AbstractPowerSource {

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

    private String name;
    private String deviceName;
    private double remainingCapacityPercent;
    private double timeRemainingEstimated;
    private double timeRemainingInstant;
    private double powerUsageRate;
    private double voltage;
    private double amperage;
    private boolean powerOnLine;
    private boolean charging;
    private boolean discharging;
    private CapacityUnits capacityUnits;
    private int currentCapacity;
    private int maxCapacity;
    private int designCapacity;
    private int cycleCount;
    private String chemistry;
    private LocalDate manufactureDate;
    private String manufacturer;
    private String serialNumber;
    private double temperature;

    public WindowsPowerSource(String name, String deviceName, double remainingCapacityPercent,
            double timeRemainingEstimated, double timeRemainingInstant, double powerUsageRate, double voltage,
            double amperage, boolean powerOnLine, boolean charging, boolean discharging, CapacityUnits capacityUnits,
            int currentCapacity, int maxCapacity, int designCapacity, int cycleCount, String chemistry,
            LocalDate manufactureDate, String manufacturer, String serialNumber, double temperature) {
        super();
        this.name = name;
        this.deviceName = deviceName;
        this.remainingCapacityPercent = remainingCapacityPercent;
        this.timeRemainingEstimated = timeRemainingEstimated;
        this.timeRemainingInstant = timeRemainingInstant;
        this.powerUsageRate = powerUsageRate;
        this.voltage = voltage;
        this.amperage = amperage;
        this.powerOnLine = powerOnLine;
        this.charging = charging;
        this.discharging = discharging;
        this.capacityUnits = capacityUnits;
        this.currentCapacity = currentCapacity;
        this.maxCapacity = maxCapacity;
        this.designCapacity = designCapacity;
        this.cycleCount = cycleCount;
        this.chemistry = chemistry;
        this.manufactureDate = manufactureDate;
        this.manufacturer = manufacturer;
        this.serialNumber = serialNumber;
        this.temperature = temperature;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDeviceName() {
        return this.deviceName;
    }

    @Override
    public double getRemainingCapacityPercent() {
        return this.remainingCapacityPercent;
    }

    @Override
    public double getTimeRemainingEstimated() {
        return this.timeRemainingEstimated;
    }

    @Override
    public double getTimeRemainingInstant() {
        return this.timeRemainingInstant;
    }

    @Override
    public double getPowerUsageRate() {
        return this.powerUsageRate;
    }

    @Override
    public double getVoltage() {
        return this.voltage;
    }

    @Override
    public double getAmperage() {
        return this.amperage;
    }

    @Override
    public boolean isPowerOnLine() {
        return this.powerOnLine;
    }

    @Override
    public boolean isCharging() {
        return this.charging;
    }

    @Override
    public boolean isDischarging() {
        return this.discharging;
    }

    @Override
    public CapacityUnits getCapacityUnits() {
        return this.capacityUnits;
    }

    @Override
    public int getCurrentCapacity() {
        return this.currentCapacity;
    }

    @Override
    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    @Override
    public int getDesignCapacity() {
        return this.designCapacity;
    }

    @Override
    public int getCycleCount() {
        return this.cycleCount;
    }

    @Override
    public String getChemistry() {
        return this.chemistry;
    }

    @Override
    public LocalDate getManufactureDate() {
        return this.manufactureDate;
    }

    @Override
    public String getManufacturer() {
        return this.manufacturer;
    }

    @Override
    public String getSerialNumber() {
        return this.serialNumber;
    }

    @Override
    public double getTemperature() {
        return this.temperature;
    }

    @Override
    public boolean updateAttributes() {
        PowerSource[] psArr = getPowerSources();
        for (PowerSource ps : psArr) {
            if (ps.getName().equals(this.name)) {
                this.name = ps.getName();
                this.deviceName = ps.getDeviceName();
                this.remainingCapacityPercent = ps.getRemainingCapacityPercent();
                this.timeRemainingEstimated = ps.getTimeRemainingEstimated();
                this.timeRemainingInstant = ps.getTimeRemainingInstant();
                this.powerUsageRate = ps.getPowerUsageRate();
                this.voltage = ps.getVoltage();
                this.amperage = ps.getAmperage();
                this.powerOnLine = ps.isPowerOnLine();
                this.charging = ps.isCharging();
                this.discharging = ps.isDischarging();
                this.capacityUnits = ps.getCapacityUnits();
                this.currentCapacity = ps.getCurrentCapacity();
                this.maxCapacity = ps.getMaxCapacity();
                this.designCapacity = ps.getDesignCapacity();
                this.cycleCount = ps.getCycleCount();
                this.chemistry = ps.getChemistry();
                this.manufactureDate = ps.getManufactureDate();
                this.manufacturer = ps.getManufacturer();
                this.serialNumber = ps.getSerialNumber();
                this.temperature = ps.getTemperature();
                return true;
            }
        }
        // Didn't find this battery
        return false;
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
        String psName = Constants.UNKNOWN;
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

        // windows PowerSource information comes from two sources: the PowrProf's
        // CallNTPowerInformation function which returns information for a single
        // object, and DeviceIoControl with each battery's (if more than one) handle
        // (which, in theory, return an array of objects but in most cases should return
        // one).
        //
        // We start by fetching the PowrProf information, which will be replicated
        // across all IOCTL entries if there are more than one.

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

        // Kernel32.INSTANCE.DeviceIoControl(hBattery, dwIoControlCode, lpInBuffer,
        // nInBufferSize, lpOutBuffer, nOutBufferSize, lpBytesReturned, lpOverlapped)

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
                // TODO verify units, check capabilities flag
                psMaxCapacity = batteryState.maxCapacity;
                psCurrentCapacity = batteryState.remainingCapacity;
                psRemainingCapacityPercent = Math.min(1d, psCurrentCapacity / psMaxCapacity);
                psPowerUsageRate = batteryState.rate;
            }
        }

        // Enumerate batteries and ask each one for information
        // Ported from:
        // https://docs.microsoft.com/en-us/windows/win32/power/enumerating-battery-devices

        HANDLE hdev = SetupApi.INSTANCE.SetupDiGetClassDevs(GUID_DEVCLASS_BATTERY, null, null,
                SetupApi.DIGCF_PRESENT | SetupApi.DIGCF_DEVICEINTERFACE);
        if (WinBase.INVALID_HANDLE_VALUE != hdev) {
            // Limit search to 100 batteries max
            for (int idev = 0; idev < 100; idev++) {
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
                                        BATTERY_INFORMATION bi = new BATTERY_INFORMATION();
                                        bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryInformation
                                                .ordinal();
                                        bqi.write();

                                        if (Kernel32.INSTANCE.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_INFORMATION,
                                                bqi.getPointer(), bqi.size(), bi.getPointer(), bi.size(), dwOut,
                                                null)) {
                                            // Only non-UPS system batteries count
                                            bi.read();
                                            if (0 != (bi.Capabilities & BATTERY_SYSTEM_BATTERY)) {
                                                // Capabilities flags non-mWh units
                                                if (0 == (bi.Capabilities & BATTERY_CAPACITY_RELATIVE)) {
                                                    psCapacityUnits = CapacityUnits.MWH;
                                                }
                                                psChemistry = new String(bi.Chemistry, StandardCharsets.US_ASCII);
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

                                            bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryDeviceName
                                                    .ordinal();
                                            bqi.write();
                                            Kernel32.INSTANCE.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_INFORMATION,
                                                    bqi.getPointer(), bqi.size(), null, 0, dwOut, null);
                                            int nameLen = dwOut.getValue();
                                            Memory nameBuf = new Memory(nameLen + CHAR_WIDTH);
                                            nameBuf.clear();
                                            if (Kernel32.INSTANCE.DeviceIoControl(hBattery,
                                                    IOCTL_BATTERY_QUERY_INFORMATION, bqi.getPointer(), bqi.size(),
                                                    nameBuf, (int) nameBuf.size(), dwOut, null)) {
                                                psDeviceName = CHAR_WIDTH > 1 ? nameBuf.getWideString(0)
                                                        : nameBuf.getString(0);
                                            }

                                            bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryEstimatedTime
                                                    .ordinal();
                                            bqi.write();
                                            IntByReference tr = new IntByReference();
                                            if (Kernel32.INSTANCE.DeviceIoControl(hBattery,
                                                    IOCTL_BATTERY_QUERY_INFORMATION, bqi.getPointer(), bqi.size(),
                                                    tr.getPointer(), Integer.BYTES, dwOut, null)) {
                                                psTimeRemainingInstant = tr.getValue();
                                            }

                                            bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryEstimatedTime
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

                                            bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryManufactureName
                                                    .ordinal();
                                            bqi.write();
                                            Kernel32.INSTANCE.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_INFORMATION,
                                                    bqi.getPointer(), bqi.size(), null, 0, dwOut, null);
                                            nameLen = dwOut.getValue();
                                            nameBuf = new Memory(nameLen + CHAR_WIDTH);
                                            nameBuf.clear();
                                            if (Kernel32.INSTANCE.DeviceIoControl(hBattery,
                                                    IOCTL_BATTERY_QUERY_INFORMATION, bqi.getPointer(), bqi.size(),
                                                    nameBuf, (int) nameBuf.size(), dwOut, null)) {
                                                psManufacturer = CHAR_WIDTH > 1 ? nameBuf.getWideString(0)
                                                        : nameBuf.getString(0);
                                            }

                                            bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatterySerialNumber
                                                    .ordinal();
                                            bqi.write();
                                            Kernel32.INSTANCE.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_INFORMATION,
                                                    bqi.getPointer(), bqi.size(), null, 0, dwOut, null);
                                            nameLen = dwOut.getValue();
                                            nameBuf = new Memory(nameLen + CHAR_WIDTH);
                                            nameBuf.clear();
                                            if (Kernel32.INSTANCE.DeviceIoControl(hBattery,
                                                    IOCTL_BATTERY_QUERY_INFORMATION, bqi.getPointer(), bqi.size(),
                                                    nameBuf, (int) nameBuf.size(), dwOut, null)) {
                                                psSerialNumber = CHAR_WIDTH > 1 ? nameBuf.getWideString(0)
                                                        : nameBuf.getString(0);
                                            }

                                            bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryTemperature
                                                    .ordinal();
                                            bqi.write();
                                            IntByReference tempK = new IntByReference(); // 1/10 degree K
                                            if (Kernel32.INSTANCE.DeviceIoControl(hBattery,
                                                    IOCTL_BATTERY_QUERY_INFORMATION, bqi.getPointer(), bqi.size(),
                                                    tr.getPointer(), Integer.BYTES, dwOut, null)) {
                                                psTemperature = tempK.getValue() / 10d - 273.15;
                                            }

                                            // Exit loop
                                            break;
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

    public static void main(String[] args) {
        System.out.println(WindowsPowerSource.getPowerSources()[0]);
    }
}
