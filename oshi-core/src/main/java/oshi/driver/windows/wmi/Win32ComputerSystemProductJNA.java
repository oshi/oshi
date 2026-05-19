/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32ComputerSystemProduct;
import oshi.driver.common.windows.wmi.Win32ComputerSystemProduct.ComputerSystemProductProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.util.platform.windows.WmiQueryExecutorJNA;

@ThreadSafe
public final class Win32ComputerSystemProductJNA extends Win32ComputerSystemProduct {
    private Win32ComputerSystemProductJNA() {
    }

    public static WmiResult<ComputerSystemProductProperty> queryIdentifyingNumberUUID() {
        return Win32ComputerSystemProduct
                .queryIdentifyingNumberUUID(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }
}
