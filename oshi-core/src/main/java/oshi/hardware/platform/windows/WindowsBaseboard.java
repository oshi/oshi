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
package oshi.hardware.platform.windows;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult; // NOSONAR squid:S1191

import oshi.annotation.concurrent.Immutable;
import oshi.driver.windows.wmi.Win32BaseBoard;
import oshi.driver.windows.wmi.Win32BaseBoard.BaseBoardProperty;
import oshi.hardware.common.AbstractBaseboard;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Quartet;

/**
 * Baseboard data obtained from WMI
 */
@Immutable
final class WindowsBaseboard extends AbstractBaseboard {

    private final Supplier<Quartet<String, String, String, String>> manufModelVersSerial = memoize(
            WindowsBaseboard::queryManufModelVersSerial);

    @Override
    public String getManufacturer() {
        return manufModelVersSerial.get().getA();
    }

    @Override
    public String getModel() {
        return manufModelVersSerial.get().getB();
    }

    @Override
    public String getVersion() {
        return manufModelVersSerial.get().getC();
    }

    @Override
    public String getSerialNumber() {
        return manufModelVersSerial.get().getD();
    }

    private static Quartet<String, String, String, String> queryManufModelVersSerial() {
        String manufacturer = null;
        String model = null;
        String version = null;
        String serialNumber = null;
        WmiResult<BaseBoardProperty> win32BaseBoard = Win32BaseBoard.queryBaseboardInfo();
        if (win32BaseBoard.getResultCount() > 0) {
            manufacturer = WmiUtil.getString(win32BaseBoard, BaseBoardProperty.MANUFACTURER, 0);
            model = WmiUtil.getString(win32BaseBoard, BaseBoardProperty.MODEL, 0);
            version = WmiUtil.getString(win32BaseBoard, BaseBoardProperty.VERSION, 0);
            serialNumber = WmiUtil.getString(win32BaseBoard, BaseBoardProperty.SERIALNUMBER, 0);
        }
        return new Quartet<>(Util.isBlank(manufacturer) ? Constants.UNKNOWN : manufacturer,
                Util.isBlank(model) ? Constants.UNKNOWN : model, Util.isBlank(version) ? Constants.UNKNOWN : version,
                Util.isBlank(serialNumber) ? Constants.UNKNOWN : serialNumber);
    }
}
