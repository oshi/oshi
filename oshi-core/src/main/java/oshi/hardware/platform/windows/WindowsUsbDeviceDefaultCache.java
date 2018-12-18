/*
 * WindowsUsbDeviceDefaultCache.java
 *
 * Creator:
 * 18.12.2018 20:34 Reto Merz
 *
 * Maintainer:
 * 18.12.2018 20:34 Reto Merz
 *
 * Last Modification:
 * $Id: $
 *
 * PLEASE DO NOT REFORMAT THIS CODE, I WILL REFORMAT IT BACK ALWAYS IT'S DONE
 *
 * Copyright (c) 2003 Abacus Research AG, All Rights Reserved
 */
package oshi.hardware.platform.windows;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import oshi.hardware.platform.windows.WindowsUsbDevice.USBControllerProperty;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default static and thread-safe {@link WindowsUsbDeviceCache} implementation
 * which queries the USB Controller PnP Device IDs only once.
 *
 * @see WindowsUsbDeviceCache
 * @see WindowsHardwareAbstractionLayer#createUsbDeviceCache()
 */
public class WindowsUsbDeviceDefaultCache extends WindowsUsbDeviceCache {

    private static final Object LOCK = new Object();

    //@GuardedBy("LOCK")
    private static volatile List<String> PNP_DEVICE_IDS;

    public WindowsUsbDeviceDefaultCache(WmiQueryHandler queryHandler) {
        super(queryHandler);
    }

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
        WmiResult<USBControllerProperty> usbController = queryHandler.queryWMI(usbControllerQuery);
        List<String> pnpDeviceIds = new ArrayList<>(usbController.getResultCount());
        for (int i = 0; i < usbController.getResultCount(); i++) {
            pnpDeviceIds.add(WmiUtil.getString(usbController, USBControllerProperty.PNPDEVICEID, i));
        }
        PNP_DEVICE_IDS = Collections.unmodifiableList(pnpDeviceIds);
    }
}
