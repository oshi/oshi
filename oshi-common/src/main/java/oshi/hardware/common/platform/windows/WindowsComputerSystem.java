/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.windows.wmi.Win32Bios;
import oshi.driver.common.windows.wmi.Win32Bios.BiosSerialProperty;
import oshi.driver.common.windows.wmi.Win32ComputerSystem;
import oshi.driver.common.windows.wmi.Win32ComputerSystem.ComputerSystemProperty;
import oshi.driver.common.windows.wmi.Win32ComputerSystemProduct;
import oshi.driver.common.windows.wmi.Win32ComputerSystemProduct.ComputerSystemProductProperty;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.tuples.Pair;

/**
 * Hardware data obtained from WMI. Subclasses provide the platform-specific {@link WmiQueryExecutor}.
 */
@Immutable
public abstract class WindowsComputerSystem extends AbstractComputerSystem {

    /** Default constructor. */
    protected WindowsComputerSystem() {
    }

    private final Supplier<Pair<String, String>> manufacturerModel = memoize(this::queryManufacturerModel);
    private final Supplier<Pair<String, String>> serialNumberUUID = memoize(this::querySystemSerialNumberUUID);

    /**
     * Returns the WMI query executor for this platform.
     *
     * @return a non-null {@link WmiQueryExecutor}
     */
    protected abstract WmiQueryExecutor getWmiQueryExecutor();

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

    private Pair<String, String> queryManufacturerModel() {
        String manufacturer = null;
        String model = null;
        WmiResult<ComputerSystemProperty> win32ComputerSystem = Win32ComputerSystem
                .queryComputerSystem(getWmiQueryExecutor());
        if (win32ComputerSystem.getResultCount() > 0) {
            manufacturer = WmiUtil.getString(win32ComputerSystem, ComputerSystemProperty.MANUFACTURER, 0);
            model = WmiUtil.getString(win32ComputerSystem, ComputerSystemProperty.MODEL, 0);
        }
        return new Pair<>(Util.isBlank(manufacturer) ? Constants.UNKNOWN : manufacturer,
                Util.isBlank(model) ? Constants.UNKNOWN : model);
    }

    private Pair<String, String> querySystemSerialNumberUUID() {
        String serialNumber = null;
        String uuid = null;
        WmiResult<ComputerSystemProductProperty> win32ComputerSystemProduct = Win32ComputerSystemProduct
                .queryIdentifyingNumberUUID(getWmiQueryExecutor());
        if (win32ComputerSystemProduct.getResultCount() > 0) {
            serialNumber = WmiUtil.getString(win32ComputerSystemProduct,
                    ComputerSystemProductProperty.IDENTIFYINGNUMBER, 0);
            uuid = WmiUtil.getString(win32ComputerSystemProduct, ComputerSystemProductProperty.UUID, 0);
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

    String querySerialFromBios() {
        WmiResult<BiosSerialProperty> serialNum = Win32Bios.querySerialNumber(getWmiQueryExecutor());
        if (serialNum.getResultCount() > 0) {
            return WmiUtil.getString(serialNum, BiosSerialProperty.SERIALNUMBER, 0);
        }
        return null;
    }
}
