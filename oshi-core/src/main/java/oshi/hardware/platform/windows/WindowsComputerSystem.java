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
import oshi.driver.windows.wmi.Win32Bios;
import oshi.driver.windows.wmi.Win32Bios.BiosSerialProperty;
import oshi.driver.windows.wmi.Win32ComputerSystem;
import oshi.driver.windows.wmi.Win32ComputerSystem.ComputerSystemProperty;
import oshi.driver.windows.wmi.Win32ComputerSystemProduct;
import oshi.driver.windows.wmi.Win32ComputerSystemProduct.ComputerSystemProductProperty;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;

/**
 * Hardware data obtained from WMI.
 */
@Immutable
final class WindowsComputerSystem extends AbstractComputerSystem {

    private final Supplier<Pair<String, String>> manufacturerModel = memoize(
            WindowsComputerSystem::queryManufacturerModel);
    private final Supplier<String> serialNumber = memoize(WindowsComputerSystem::querySystemSerialNumber);

    @Override
    public String getManufacturer() {
        return manufacturerModel.get().getA();
    }

    @Override
    public String getModel() {
        return manufacturerModel.get().getB();
    }

    @Override
    public String getSerialNumber() {
        return serialNumber.get();
    }

    @Override
    public Firmware createFirmware() {
        return new WindowsFirmware();
    }

    @Override
    public Baseboard createBaseboard() {
        return new WindowsBaseboard();
    }

    private static Pair<String, String> queryManufacturerModel() {
        String manufacturer = null;
        String model = null;
        WmiResult<ComputerSystemProperty> win32ComputerSystem = Win32ComputerSystem.queryComputerSystem();
        if (win32ComputerSystem.getResultCount() > 0) {
            manufacturer = WmiUtil.getString(win32ComputerSystem, ComputerSystemProperty.MANUFACTURER, 0);
            model = WmiUtil.getString(win32ComputerSystem, ComputerSystemProperty.MODEL, 0);
        }
        return new Pair<>(Util.isBlank(manufacturer) ? Constants.UNKNOWN : manufacturer,
                Util.isBlank(model) ? Constants.UNKNOWN : model);
    }

    private static String querySystemSerialNumber() {
        String result;
        if (((result = querySerialFromBios()) != null || (result = querySerialFromCsProduct()) != null)
                && !Util.isBlank(result)) {
            return result;
        }
        return Constants.UNKNOWN;
    }

    private static String querySerialFromBios() {
        WmiResult<BiosSerialProperty> serialNum = Win32Bios.querySerialNumber();
        if (serialNum.getResultCount() > 0) {
            return WmiUtil.getString(serialNum, BiosSerialProperty.SERIALNUMBER, 0);
        }
        return null;
    }

    private static String querySerialFromCsProduct() {
        WmiResult<ComputerSystemProductProperty> identifyingNumber = Win32ComputerSystemProduct
                .queryIdentifyingNumber();
        if (identifyingNumber.getResultCount() > 0) {
            return WmiUtil.getString(identifyingNumber, ComputerSystemProductProperty.IDENTIFYINGNUMBER, 0);
        }
        return null;
    }
}
