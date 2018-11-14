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
package oshi.hardware;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import oshi.SystemInfo;

/**
 * Test USB device
 */
public class UsbDeviceTest {
    /**
     * Test USB Devices
     */
    @Test
    public void testUsbDevices() {
        SystemInfo si = new SystemInfo();
        for (UsbDevice usb : si.getHardware().getUsbDevices(true)) {
            assertNotNull(usb);
            testUsbRecursive(usb);
        }
    }

    private void testUsbRecursive(UsbDevice usb) {
        assertTrue(usb.getName().length() > 0);
        assertNotNull(usb.getVendor());
        assertNotNull(usb.getProductId());
        assertNotNull(usb.getVendorId());
        assertNotNull(usb.getSerialNumber());

        for (UsbDevice nested : usb.getConnectedDevices()) {
            testUsbRecursive(nested);
        }
    }

}
