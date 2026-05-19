/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.OhmHardware;
import oshi.driver.common.windows.wmi.OhmHardware.IdentifierProperty;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.driver.common.windows.wmi.WmiResult;

@ThreadSafe
public final class OhmHardwareJNA extends OhmHardware {
    private OhmHardwareJNA() {
    }

    public static WmiResult<IdentifierProperty> queryHwIdentifier(WmiQueryExecutor h, String typeToQuery,
            String typeName) {
        return OhmHardware.queryHwIdentifier(h, typeToQuery, typeName);
    }
}
