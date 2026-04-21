/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.OhmHardware;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query Open Hardware Monitor WMI data for Hardware using FFM.
 */
@ThreadSafe
public final class OhmHardwareFFM extends OhmHardware {

    private OhmHardwareFFM() {
    }

    /**
     * Queries the hardware identifiers for a monitored type.
     *
     * @param h           An instantiated {@link WmiQueryHandlerFFM}. User should have already initialized COM.
     * @param typeToQuery which type to filter based on
     * @param typeName    the name of the type
     * @return WmiResult of hardware identifier properties.
     */
    public static WmiResult<IdentifierProperty> queryHwIdentifier(WmiQueryHandlerFFM h, String typeToQuery,
            String typeName) {
        WmiQuery<IdentifierProperty> hwIdentifierQuery = new WmiQuery<>(OHM_NAMESPACE,
                buildHardwareWmiClassNameWithWhere(typeToQuery, typeName), IdentifierProperty.class);
        return h.queryWMI(hwIdentifierQuery, false);
    }
}
