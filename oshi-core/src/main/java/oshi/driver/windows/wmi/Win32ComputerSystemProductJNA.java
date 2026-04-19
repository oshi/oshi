/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32ComputerSystemProduct;
import oshi.driver.common.windows.wmi.Win32ComputerSystemProduct.ComputerSystemProductProperty;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_ComputerSystemProduct} using JNA.
 */
@ThreadSafe
public final class Win32ComputerSystemProductJNA extends Win32ComputerSystemProduct {

    private Win32ComputerSystemProductJNA() {
    }

    /**
     * Queries the Computer System Product.
     *
     * @return Assigned serial number and UUID.
     */
    public static WmiResult<ComputerSystemProductProperty> queryIdentifyingNumberUUID() {
        WmiQuery<ComputerSystemProductProperty> identifyingNumberQuery = new WmiQuery<>(WIN32_COMPUTER_SYSTEM_PRODUCT,
                ComputerSystemProductProperty.class);
        return Objects
                .requireNonNull(WmiQueryHandler.createInstance(), "WmiQueryHandler.createInstance() returned null")
                .queryWMI(identifyingNumberQuery);
    }
}
