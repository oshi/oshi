/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.windows.wmi.Win32Bios;
import oshi.driver.common.windows.wmi.Win32Bios.BiosProperty;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.hardware.common.AbstractFirmware;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.tuples.Quintet;

/**
 * Firmware data obtained from WMI. Subclasses provide the platform-specific {@link WmiQueryExecutor}.
 */
@Immutable
public abstract class WindowsFirmware extends AbstractFirmware {

    /** Default constructor. */
    protected WindowsFirmware() {
    }

    private final Supplier<Quintet<String, String, String, String, String>> manufNameDescVersRelease = memoize(
            this::queryManufNameDescVersRelease);

    /**
     * Returns the WMI query executor for this platform.
     *
     * @return a non-null {@link WmiQueryExecutor}
     */
    protected abstract WmiQueryExecutor getWmiQueryExecutor();

    @Override
    public String getManufacturer() {
        return manufNameDescVersRelease.get().getA();
    }

    @Override
    public String getName() {
        return manufNameDescVersRelease.get().getB();
    }

    @Override
    public String getDescription() {
        return manufNameDescVersRelease.get().getC();
    }

    @Override
    public String getVersion() {
        return manufNameDescVersRelease.get().getD();
    }

    @Override
    public String getReleaseDate() {
        return manufNameDescVersRelease.get().getE();
    }

    private Quintet<String, String, String, String, String> queryManufNameDescVersRelease() {
        String manufacturer = null;
        String name = null;
        String description = null;
        String version = null;
        String releaseDate = null;
        WmiResult<BiosProperty> win32BIOS = Win32Bios.queryBiosInfo(getWmiQueryExecutor());
        if (win32BIOS.getResultCount() > 0) {
            manufacturer = WmiUtil.getString(win32BIOS, BiosProperty.MANUFACTURER, 0);
            name = WmiUtil.getString(win32BIOS, BiosProperty.NAME, 0);
            description = WmiUtil.getString(win32BIOS, BiosProperty.DESCRIPTION, 0);
            version = WmiUtil.getString(win32BIOS, BiosProperty.VERSION, 0);
            releaseDate = WmiUtil.getDateString(win32BIOS, BiosProperty.RELEASEDATE, 0);
        }
        return new Quintet<>(Util.isBlank(manufacturer) ? Constants.UNKNOWN : manufacturer,
                Util.isBlank(name) ? Constants.UNKNOWN : name,
                Util.isBlank(description) ? Constants.UNKNOWN : description,
                Util.isBlank(version) ? Constants.UNKNOWN : version,
                Util.isBlank(releaseDate) ? Constants.UNKNOWN : releaseDate);
    }
}
