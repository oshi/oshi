/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_ComputerSystem}.
 */
@ThreadSafe
public class Win32ComputerSystem {

    /**
     * The WMI class name.
     */
    public static final String WIN32_COMPUTER_SYSTEM = "Win32_ComputerSystem";

    /**
     * Computer System properties.
     */
    public enum ComputerSystemProperty {
        /** MANUFACTURER property. */
        MANUFACTURER,
        /** MODEL property. */
        MODEL;
    }

    /**
     * Constructor.
     */
    protected Win32ComputerSystem() {
    }

    /**
     * Queries computer system information.
     *
     * @param h An instantiated {@link WmiQueryExecutor}.
     * @return Computer system information.
     */
    public static WmiResult<ComputerSystemProperty> queryComputerSystem(WmiQueryExecutor h) {
        WmiQuery<ComputerSystemProperty> csQuery = new WmiQuery<>(WIN32_COMPUTER_SYSTEM, ComputerSystemProperty.class);
        return h.queryWMI(csQuery);
    }
}
