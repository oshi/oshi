/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.windows.wmi.Win32Bios.BiosSerialProperty;
import oshi.driver.common.windows.wmi.Win32ComputerSystem.ComputerSystemProperty;
import oshi.driver.common.windows.wmi.Win32ComputerSystemProduct.ComputerSystemProductProperty;
import oshi.driver.windows.wmi.Win32BiosFFM;
import oshi.driver.windows.wmi.Win32ComputerSystemFFM;
import oshi.driver.windows.wmi.Win32ComputerSystemProductFFM;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiUtilFFM;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.tuples.Pair;

/**
 * Hardware data obtained from WMI using FFM.
 */
@Immutable
final class WindowsComputerSystemFFM extends AbstractComputerSystem {

    private final Supplier<Pair<String, String>> manufacturerModel = memoize(
            WindowsComputerSystemFFM::queryManufacturerModel);
    private final Supplier<Pair<String, String>> serialNumberUUID = memoize(
            WindowsComputerSystemFFM::querySystemSerialNumberUUID);

    @Override
    public String getManufacturer() {
        return manufacturerModel.get().getA();
    }

    @Override
    public String getModel() {
        return manufacturerModel.get().getB();
    }

    @Override
    public String getSerialNumber() {
        return serialNumberUUID.get().getA();
    }

    @Override
    public String getHardwareUUID() {
        return serialNumberUUID.get().getB();
    }

    @Override
    public Firmware createFirmware() {
        return new WindowsFirmwareFFM();
    }

    @Override
    public Baseboard createBaseboard() {
        return new WindowsBaseboardFFM();
    }

    private static Pair<String, String> queryManufacturerModel() {
        String manufacturer = null;
        String model = null;
        WmiResult<ComputerSystemProperty> win32ComputerSystem = Win32ComputerSystemFFM.queryComputerSystem();
        if (win32ComputerSystem.getResultCount() > 0) {
            manufacturer = WmiUtilFFM.getString(win32ComputerSystem, ComputerSystemProperty.MANUFACTURER, 0);
            model = WmiUtilFFM.getString(win32ComputerSystem, ComputerSystemProperty.MODEL, 0);
        }
        return new Pair<>(Util.isBlank(manufacturer) ? Constants.UNKNOWN : manufacturer,
                Util.isBlank(model) ? Constants.UNKNOWN : model);
    }

    private static Pair<String, String> querySystemSerialNumberUUID() {
        String serialNumber = null;
        String uuid = null;
        WmiResult<ComputerSystemProductProperty> win32ComputerSystemProduct = Win32ComputerSystemProductFFM
                .queryIdentifyingNumberUUID();
        if (win32ComputerSystemProduct.getResultCount() > 0) {
            serialNumber = WmiUtilFFM.getString(win32ComputerSystemProduct,
                    ComputerSystemProductProperty.IDENTIFYINGNUMBER, 0);
            uuid = WmiUtilFFM.getString(win32ComputerSystemProduct, ComputerSystemProductProperty.UUID, 0);
        }
        if (Util.isBlank(serialNumber)) {
            serialNumber = querySerialFromBios();
        }
        if (Util.isBlank(serialNumber)) {
            serialNumber = Constants.UNKNOWN;
        }
        if (Util.isBlank(uuid)) {
            uuid = Constants.UNKNOWN;
        }
        return new Pair<>(serialNumber, uuid);
    }

    private static String querySerialFromBios() {
        WmiResult<BiosSerialProperty> serialNum = Win32BiosFFM.querySerialNumber();
        if (serialNum.getResultCount() > 0) {
            return WmiUtilFFM.getString(serialNum, BiosSerialProperty.SERIALNUMBER, 0);
        }
        return null;
    }
}
