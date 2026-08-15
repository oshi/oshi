/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

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
import oshi.util.ParseUtil;
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

    private final Supplier<Pair<@Nullable String, @Nullable String>> manufacturerModel = memoize(
            this::queryManufacturerModel);
    private final Supplier<Pair<@Nullable String, @Nullable String>> serialNumberUUID = memoize(
            this::querySystemSerialNumberUUID);

    /**
     * Returns the WMI query executor for this platform.
     *
     * @return a non-null {@link WmiQueryExecutor}
     */
    protected abstract WmiQueryExecutor getWmiQueryExecutor();

    @Override
    public String getManufacturer() {
        return ParseUtil.getStringValueOrUnknown(manufacturerModel.get().getA());
    }

    @Override
    public String getModel() {
        return ParseUtil.getStringValueOrUnknown(manufacturerModel.get().getB());
    }

    @Override
    public String getSerialNumber() {
        return ParseUtil.getStringValueOrUnknown(serialNumberUUID.get().getA());
    }

    @Override
    public String getHardwareUUID() {
        return ParseUtil.getStringValueOrUnknown(serialNumberUUID.get().getB());
    }

    private Pair<@Nullable String, @Nullable String> queryManufacturerModel() {
        String manufacturer = null;
        String model = null;
        WmiResult<ComputerSystemProperty> win32ComputerSystem = Win32ComputerSystem
                .queryComputerSystem(getWmiQueryExecutor());
        if (win32ComputerSystem.getResultCount() > 0) {
            manufacturer = WmiUtil.getString(win32ComputerSystem, ComputerSystemProperty.MANUFACTURER, 0);
            model = WmiUtil.getString(win32ComputerSystem, ComputerSystemProperty.MODEL, 0);
        }
        return new Pair<>(ParseUtil.getStringValueOrUnknown(manufacturer), ParseUtil.getStringValueOrUnknown(model));
    }

    private Pair<@Nullable String, @Nullable String> querySystemSerialNumberUUID() {
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

    @Nullable
    String querySerialFromBios() {
        WmiResult<BiosSerialProperty> serialNum = Win32Bios.querySerialNumber(getWmiQueryExecutor());
        if (serialNum.getResultCount() > 0) {
            return WmiUtil.getString(serialNum, BiosSerialProperty.SERIALNUMBER, 0);
        }
        return null;
    }
}
