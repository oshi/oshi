/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.windows.wmi.Win32BaseBoard.BaseBoardProperty;
import oshi.driver.windows.wmi.Win32BaseBoardFFM;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiUtilFFM;
import oshi.hardware.common.AbstractBaseboard;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.tuples.Quartet;

/**
 * Baseboard data obtained from WMI using FFM.
 */
@Immutable
final class WindowsBaseboardFFM extends AbstractBaseboard {

    private final Supplier<Quartet<String, String, String, String>> manufModelVersSerial = memoize(
            WindowsBaseboardFFM::queryManufModelVersSerial);

    @Override
    public String getManufacturer() {
        return manufModelVersSerial.get().getA();
    }

    @Override
    public String getModel() {
        return manufModelVersSerial.get().getB();
    }

    @Override
    public String getVersion() {
        return manufModelVersSerial.get().getC();
    }

    @Override
    public String getSerialNumber() {
        return manufModelVersSerial.get().getD();
    }

    private static Quartet<String, String, String, String> queryManufModelVersSerial() {
        String manufacturer = null;
        String model = null;
        String version = null;
        String serialNumber = null;
        WmiResult<BaseBoardProperty> win32BaseBoard = Win32BaseBoardFFM.queryBaseboardInfo();
        if (win32BaseBoard.getResultCount() > 0) {
            manufacturer = WmiUtilFFM.getString(win32BaseBoard, BaseBoardProperty.MANUFACTURER, 0);
            model = WmiUtilFFM.getString(win32BaseBoard, BaseBoardProperty.MODEL, 0);
            String product = WmiUtilFFM.getString(win32BaseBoard, BaseBoardProperty.PRODUCT, 0);
            if (!Util.isBlank(product)) {
                model = Util.isBlank(model) ? product : (model + " (" + product + ")");
            }
            version = WmiUtilFFM.getString(win32BaseBoard, BaseBoardProperty.VERSION, 0);
            serialNumber = WmiUtilFFM.getString(win32BaseBoard, BaseBoardProperty.SERIALNUMBER, 0);
        }
        return new Quartet<>(Util.isBlank(manufacturer) ? Constants.UNKNOWN : manufacturer,
                Util.isBlank(model) ? Constants.UNKNOWN : model, Util.isBlank(version) ? Constants.UNKNOWN : version,
                Util.isBlank(serialNumber) ? Constants.UNKNOWN : serialNumber);
    }
}
