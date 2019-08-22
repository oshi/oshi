/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.windows;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.hardware.common.AbstractBaseboard;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * Baseboard data obtained from WMI
 */
final class WindowsBaseboard extends AbstractBaseboard {
    private final Supplier<WmiStrings> wmi = memoize(this::queryWmi);

    @Override
    public String getManufacturer() {
        return wmi.get().manufacturer;
    }

    @Override
    public String getModel() {
        return wmi.get().model;
    }

    @Override
    public String getVersion() {
        return wmi.get().version;
    }

    @Override
    public String getSerialNumber() {
        return wmi.get().serialNumber;
    }

    private WmiStrings queryWmi() {
        WmiQuery<BaseboardProperty> baseboardQuery = new WmiQuery<>("Win32_BaseBoard", BaseboardProperty.class);
        WmiResult<BaseboardProperty> win32BaseBoard = WmiQueryHandler.createInstance().queryWMI(baseboardQuery);
        if (win32BaseBoard.getResultCount() > 0) {
            return new WmiStrings(WmiUtil.getString(win32BaseBoard, BaseboardProperty.MANUFACTURER, 0),
                    WmiUtil.getString(win32BaseBoard, BaseboardProperty.MODEL, 0),
                    WmiUtil.getString(win32BaseBoard, BaseboardProperty.VERSION, 0),
                    WmiUtil.getString(win32BaseBoard, BaseboardProperty.SERIALNUMBER, 0));
        }
        return new WmiStrings(Constants.UNKNOWN, Constants.UNKNOWN, Constants.UNKNOWN, Constants.UNKNOWN);
    }

    enum BaseboardProperty {
        MANUFACTURER, MODEL, VERSION, SERIALNUMBER;
    }

    private static final class WmiStrings {
        private final String manufacturer;
        private final String model;
        private final String version;
        private final String serialNumber;

        private WmiStrings(String manufacturer, String model, String version, String serialNumber) {
            this.manufacturer = Util.isBlank(manufacturer) ? Constants.UNKNOWN : manufacturer;
            this.model = Util.isBlank(model) ? Constants.UNKNOWN : model;
            this.version = Util.isBlank(version) ? Constants.UNKNOWN : version;
            this.serialNumber = Util.isBlank(serialNumber) ? Constants.UNKNOWN : serialNumber;
        }
    }

}
