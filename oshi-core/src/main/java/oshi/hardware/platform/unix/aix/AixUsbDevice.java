/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.hardware.platform.unix.aix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.Constants;
import oshi.util.ParseUtil;

/**
 * AIX Usb Device
 */
@Immutable
public class AixUsbDevice extends AbstractUsbDevice {

    public AixUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * {@inheritDoc}
     *
     * @param tree
     *            a boolean.
     * @param lscfg
     *            A memoized lscfg list
     * @return an unmodifiable list of {@link oshi.hardware.UsbDevice} objects.
     */
    public static List<UsbDevice> getUsbDevices(boolean tree, Supplier<List<String>> lscfg) { // NOSONAR squid:S1172
        return Collections.unmodifiableList(getUsbDevices(lscfg.get()));
    }

    private static List<UsbDevice> getUsbDevices(List<String> lsusb) {
        List<UsbDevice> deviceList = new ArrayList<>();
        for (String line : lsusb) {
            String s = line.trim();
            if (s.startsWith("usb")) {
                String[] split = ParseUtil.whitespaces.split(s, 3);
                if (split.length == 3) {
                    deviceList.add(new AixUsbDevice(split[2], Constants.UNKNOWN, Constants.UNKNOWN, Constants.UNKNOWN,
                            Constants.UNKNOWN, split[0], Collections.emptyList()));
                }
            }
        }
        return deviceList;
    }
}
