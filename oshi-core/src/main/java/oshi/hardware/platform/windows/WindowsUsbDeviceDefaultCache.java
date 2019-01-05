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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.hardware.platform.windows.WindowsUsbDevice.USBControllerProperty;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * Default static and thread-safe {@link WindowsUsbDeviceCache} implementation
 * which queries the USB Controller PnP Device IDs only once.
 *
 * @see WindowsUsbDeviceCache
 * @see WindowsHardwareAbstractionLayer#createUsbDeviceCache()
 */
public class WindowsUsbDeviceDefaultCache extends WindowsUsbDeviceCache {

    private static final Object LOCK = new Object();

    // @GuardedBy("LOCK")
    private static volatile List<String> PNP_DEVICE_IDS;

    @Override
    public List<String> getPnpDeviceIds() {
        if (PNP_DEVICE_IDS == null) {
            synchronized (LOCK) {
                if (PNP_DEVICE_IDS == null) {
                    buildCache();
                }
            }
        }
        return PNP_DEVICE_IDS;
    }

    private void buildCache() {
        // One time lookup of USB Controller PnP Device IDs which don't change
        WmiQuery<USBControllerProperty> usbControllerQuery = new WmiQuery<>("Win32_USBController",
                USBControllerProperty.class);
        WmiResult<USBControllerProperty> usbController = WmiQueryHandler.getInstance().queryWMI(usbControllerQuery);
        List<String> pnpDeviceIds = new ArrayList<>(usbController.getResultCount());
        for (int i = 0; i < usbController.getResultCount(); i++) {
            pnpDeviceIds.add(WmiUtil.getString(usbController, USBControllerProperty.PNPDEVICEID, i));
        }
        PNP_DEVICE_IDS = Collections.unmodifiableList(pnpDeviceIds);
    }
}
