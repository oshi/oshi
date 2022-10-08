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
 * Utility to query WMI class {@code Win32_ComputerSystemProduct}
 */
@ThreadSafe
public final class Win32ComputerSystemProduct {

    private static final String WIN32_COMPUTER_SYSTEM_PRODUCT = "Win32_ComputerSystemProduct";

    /**
     * Computer System ID number
     */
    public enum ComputerSystemProductProperty {
        IDENTIFYINGNUMBER, UUID;
    }

    private Win32ComputerSystemProduct() {
    }

    /**
     * Queries the Computer System Product.
     *
     * @return Assigned serial number and UUID.
     */
    public static WmiResult<ComputerSystemProductProperty> queryIdentifyingNumberUUID() {
        WmiQuery<ComputerSystemProductProperty> identifyingNumberQuery = new WmiQuery<>(WIN32_COMPUTER_SYSTEM_PRODUCT,
                ComputerSystemProductProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(identifyingNumberQuery);
    }
}
