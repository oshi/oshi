/*
 * MIT License
 *
 * Copyright (c) 2018-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;

/**
 * Test USB device
 */
class UsbDeviceTest {
    /**
     * Test USB Devices
     */
    @Test
    void testUsbDevices() {
        SystemInfo si = new SystemInfo();
        List<UsbDevice> usbList = si.getHardware().getUsbDevices(true);
        for (UsbDevice usb : usbList) {
            assertThat("USB object shouldn't be null", usb, is(notNullValue()));
            testUsbRecursive(usb);
        }
    }

    private void testUsbRecursive(UsbDevice usb) {
        assertThat("USB name shouldn't be blank", usb.getName(), is(not(emptyString())));
        assertThat("USB vendor shouldn't be null", usb.getVendor(), is(notNullValue()));
        assertThat("USB product ID shouldn't be null", usb.getProductId(), is(notNullValue()));
        assertThat("USB vendor ID shouldn't be null", usb.getVendorId(), is(notNullValue()));
        assertThat("USB serial number shouldn't be null", usb.getSerialNumber(), is(notNullValue()));

        for (UsbDevice nested : usb.getConnectedDevices()) {
            testUsbRecursive(nested);
        }
    }
}
