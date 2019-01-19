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

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.Constants;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * Hardware data obtained from WMI.
 */
final class WindowsComputerSystem extends AbstractComputerSystem {

    private static final long serialVersionUID = 1L;

    private final transient WmiQueryHandler wmiQueryHandler = WmiQueryHandler.createInstance();

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        if (this.manufacturer == null) {
            queryManufacturerAndModel();
        }
        return super.getManufacturer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModel() {
        if (this.model == null) {
            queryManufacturerAndModel();
        }
        return super.getModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSerialNumber() {
        if (this.serialNumber == null) {
            querySystemSerialNumber();
        }
        return this.serialNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Firmware getFirmware() {
        if (this.firmware == null) {
            this.firmware = new WindowsFirmware();
        }
        return this.firmware;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Baseboard getBaseboard() {
        if (this.baseboard == null) {
            this.baseboard = new WindowsBaseboard();
        }
        return this.baseboard;
    }

    private void queryManufacturerAndModel() {
        WmiQuery<ComputerSystemProperty> computerSystemQuery = new WmiQuery<>("Win32_ComputerSystem",
                ComputerSystemProperty.class);
        WmiResult<ComputerSystemProperty> win32ComputerSystem = wmiQueryHandler.queryWMI(computerSystemQuery);
        if (win32ComputerSystem.getResultCount() > 0) {
            this.manufacturer = WmiUtil.getString(win32ComputerSystem, ComputerSystemProperty.MANUFACTURER, 0);
            this.model = WmiUtil.getString(win32ComputerSystem, ComputerSystemProperty.MODEL, 0);
        }
    }

    private void querySystemSerialNumber() {
        if (!querySerialFromBios() && !querySerialFromCsProduct()) {
            this.serialNumber = Constants.UNKNOWN;
        }
    }

    private boolean querySerialFromBios() {
        WmiQuery<BiosProperty> serialNumberQuery = new WmiQuery<>("Win32_BIOS where PrimaryBIOS=true",
                BiosProperty.class);
        WmiResult<BiosProperty> serialNumber = wmiQueryHandler.queryWMI(serialNumberQuery);
        if (serialNumber.getResultCount() > 0) {
            this.serialNumber = WmiUtil.getString(serialNumber, BiosProperty.SERIALNUMBER, 0);
        }
        return this.serialNumber != null && !this.serialNumber.isEmpty();
    }

    private boolean querySerialFromCsProduct() {
        WmiQuery<ComputerSystemProductProperty> identifyingNumberQuery = new WmiQuery<>("Win32_ComputerSystemProduct",
                ComputerSystemProductProperty.class);
        WmiResult<ComputerSystemProductProperty> identifyingNumber = wmiQueryHandler.queryWMI(identifyingNumberQuery);
        if (identifyingNumber.getResultCount() > 0) {
            this.serialNumber = WmiUtil.getString(identifyingNumber, ComputerSystemProductProperty.IDENTIFYINGNUMBER,
                    0);
        }
        return this.serialNumber != null && !this.serialNumber.isEmpty();
    }

    enum ComputerSystemProperty {
        MANUFACTURER, MODEL;
    }

    enum BiosProperty {
        SERIALNUMBER;
    }

    enum ComputerSystemProductProperty {
        IDENTIFYINGNUMBER;
    }
}
