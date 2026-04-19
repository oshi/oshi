/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.OhmHardware;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query Open Hardware Monitor WMI data for Hardware using JNA.
 */
@ThreadSafe
public final class OhmHardwareJNA extends OhmHardware {

    private OhmHardwareJNA() {
    }

    /**
     * Queries the hardware identifiers for a monitored type.
     *
     * @param h           An instantiated {@link WmiQueryHandler}. User should have already initialized COM.
     * @param typeToQuery which type to filter based on
     * @param typeName    the name of the type
     * @return WmiResult of hardware identifier properties.
     */
    public static WmiResult<IdentifierProperty> queryHwIdentifier(WmiQueryHandler h, String typeToQuery,
            String typeName) {
        WmiQuery<IdentifierProperty> hwIdentifierQuery = new WmiQuery<>(OHM_NAMESPACE,
                buildHardwareWmiClassNameWithWhere(typeToQuery, typeName), IdentifierProperty.class);
        return h.queryWMI(hwIdentifierQuery, false);
    }
}
