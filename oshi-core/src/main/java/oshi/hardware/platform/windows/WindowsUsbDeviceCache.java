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

import java.util.List;

/**
 * Used to cache USB-device-specific data.
 *
 * @see WindowsUsbDeviceDefaultCache
 * @see WindowsHardwareAbstractionLayer#createUsbDeviceCache()
 */
public abstract class WindowsUsbDeviceCache {

    protected transient final WmiQueryHandler queryHandler;

    public WindowsUsbDeviceCache(WmiQueryHandler queryHandler) {
        this.queryHandler = queryHandler;
    }

    /**
     * Get the USB Controller PnP Device IDs.
     *
     * @return Return an immutable list which contains the USB Controller PnP Device IDs.
     */
    public abstract List<String> getPnpDeviceIds();
}
