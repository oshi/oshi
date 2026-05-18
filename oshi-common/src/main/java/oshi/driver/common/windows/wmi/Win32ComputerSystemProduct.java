/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_ComputerSystemProduct}.
 */
@ThreadSafe
public class Win32ComputerSystemProduct {

    /**
     * The WMI class name.
     */
    public static final String WIN32_COMPUTER_SYSTEM_PRODUCT = "Win32_ComputerSystemProduct";

    /**
     * Computer System ID number.
     */
    public enum ComputerSystemProductProperty {
        /** IDENTIFYINGNUMBER property. */
        IDENTIFYINGNUMBER,
        /** UUID property. */
        UUID;
    }

    /**
     * Constructor.
     */
    protected Win32ComputerSystemProduct() {
    }

    /**
     * Queries the UUID and identifying number.
     *
     * @param h An instantiated {@link WmiQueryExecutor}.
     * @return UUID and identifying number.
     */
    public static WmiResult<ComputerSystemProductProperty> queryIdentifyingNumberUUID(WmiQueryExecutor h) {
        WmiQuery<ComputerSystemProductProperty> cspQuery = new WmiQuery<>(WIN32_COMPUTER_SYSTEM_PRODUCT,
                ComputerSystemProductProperty.class);
        return h.queryWMI(cspQuery);
    }
}
