/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32BaseBoard;
import oshi.driver.common.windows.wmi.Win32BaseBoard.BaseBoardProperty;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_BaseBoard} using FFM.
 */
@ThreadSafe
public final class Win32BaseBoardFFM extends Win32BaseBoard {

    private Win32BaseBoardFFM() {
    }

    /**
     * Queries the Baseboard description.
     *
     * @return Baseboard manufacturer, model, and related fields.
     */
    public static WmiResult<BaseBoardProperty> queryBaseboardInfo() {
        WmiQuery<BaseBoardProperty> baseboardQuery = new WmiQuery<>(WIN32_BASEBOARD, BaseBoardProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(baseboardQuery);
    }
}
