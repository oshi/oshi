/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32BaseBoard;
import oshi.driver.common.windows.wmi.Win32BaseBoard.BaseBoardProperty;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_BaseBoard} using JNA.
 */
@ThreadSafe
public final class Win32BaseBoardJNA extends Win32BaseBoard {

    private Win32BaseBoardJNA() {
    }

    /**
     * Queries the Baseboard description.
     *
     * @return Baseboard manufacturer, model, and related fields.
     */
    public static WmiResult<BaseBoardProperty> queryBaseboardInfo() {
        WmiQuery<BaseBoardProperty> baseboardQuery = new WmiQuery<>(WIN32_BASEBOARD, BaseBoardProperty.class);
        return Objects
                .requireNonNull(WmiQueryHandler.createInstance(), "WmiQueryHandler.createInstance() returned null")
                .queryWMI(baseboardQuery);
    }
}
