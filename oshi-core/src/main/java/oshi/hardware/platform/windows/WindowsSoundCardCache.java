/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import oshi.util.platform.windows.WmiQueryHandler;

import java.util.Map;

/**
 * Used to cache sound-card-specific data.
 *
 * @see WindowsSoundCardDefaultCache
 * @see WindowsHardwareAbstractionLayer#createSoundCardCache()
 */
public abstract class WindowsSoundCardCache {

    protected transient final WmiQueryHandler queryHandler;

    public WindowsSoundCardCache(WmiQueryHandler queryHandler) {
        this.queryHandler = queryHandler;
    }

    protected abstract String getDriverQuery();

    /**
     * @return An immutable map.
     */
    protected abstract Map<String, String> getManufacturerByName();

    /**
     * Creates our Win32_PnPSignedDevice query with the WHERE clause taking the
     * attributes from our map.
     *
     * @return The WHERE clause
     */
    protected static String createClause(Map<String, String> manufacturerByName) {
        StringBuilder sb = new StringBuilder("Win32_PnPSignedDriver");
        boolean first = true;
        for (String key : manufacturerByName.keySet()) {
            if (first) {
                sb.append(" WHERE");
                first = false;
            } else {
                sb.append(" OR");
            }
            sb.append(" DeviceName LIKE \"%").append(key).append("%\"");
        }
        return sb.toString();
    }

}
