/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_BaseBoard}
 */
@ThreadSafe
public final class Win32BaseBoard {

    private static final String WIN32_BASEBOARD = "Win32_BaseBoard";

    /**
     * Baseboard description properties.
     */
    public enum BaseBoardProperty {
        MANUFACTURER, MODEL, VERSION, SERIALNUMBER;
    }

    private Win32BaseBoard() {
    }

    /**
     * Queries the Baseboard description.
     *
     * @return Baseboard manufacturer, model, and related fields.
     */
    public static WmiResult<BaseBoardProperty> queryBaseboardInfo() {
        WmiQuery<BaseBoardProperty> baseboardQuery = new WmiQuery<>(WIN32_BASEBOARD, BaseBoardProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(baseboardQuery);
    }
}
