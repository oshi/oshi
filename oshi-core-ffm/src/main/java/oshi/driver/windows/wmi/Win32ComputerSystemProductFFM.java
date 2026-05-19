/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32ComputerSystemProduct;
import oshi.driver.common.windows.wmi.Win32ComputerSystemProduct.ComputerSystemProductProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryExecutorFFM;

@ThreadSafe
public final class Win32ComputerSystemProductFFM extends Win32ComputerSystemProduct {
    private Win32ComputerSystemProductFFM() {
    }

    public static WmiResult<ComputerSystemProductProperty> queryIdentifyingNumberUUID() {
        return Win32ComputerSystemProduct
                .queryIdentifyingNumberUUID(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()));
    }
}
