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
package oshi.driver.linux;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.Constants;
import oshi.util.FileUtil;

/**
 * Utility to read info from {@code sysfs}
 */
@ThreadSafe
public final class Sysfs {

    private Sysfs() {
    }

    /**
     * Query the vendor from sysfs
     *
     * @return The vendor if available, null otherwise
     */
    public static String queryVendor() {
        final String sysVendor = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "sys_vendor").trim();
        if (!sysVendor.isEmpty()) {
            return sysVendor;
        }
        return null;
    }

    /**
     * Query the model from sysfs
     *
     * @return The model if available, null otherwise
     */
    public static String queryModel() {
        final String productName = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "product_name").trim();
        final String productVersion = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "product_version")
                .trim();
        if (productName.isEmpty()) {
            if (!productVersion.isEmpty()) {
                return productVersion;
            }
        } else {
            if (!productVersion.isEmpty() && !"None".equals(productVersion)) {
                return productName + " (version: " + productVersion + ")";
            } else {
                return productName;
            }
        }
        return null;
    }

    /**
     * Query the serial number from sysfs
     *
     * @return The serial number if available, null otherwise
     */
    public static String querySerialNumber() {
        // These sysfs files accessible by root, or can be chmod'd at boot time
        // to enable access without root
        String serial = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "product_serial");
        if (!serial.isEmpty() && !"None".equals(serial)) {
            return serial;
        }
        serial = FileUtil.getStringFromFile(Constants.SYSFS_SERIAL_PATH + "board_serial");
        if (!serial.isEmpty() && !"None".equals(serial)) {
            return serial;
        }
        return null;
    }
}
