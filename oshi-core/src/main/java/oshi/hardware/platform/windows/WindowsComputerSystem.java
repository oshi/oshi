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

import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * Hardware data obtained from WMI
 *
 * @author SchiTho1 [at] Securiton AG
 * @author widdis [at] gmail [dot] com
 */
final class WindowsComputerSystem extends AbstractComputerSystem {

    private static final long serialVersionUID = 1L;

    enum ComputerSystemProperty {
        MANUFACTURER, MODEL;
    }

    enum BiosProperty {
        SERIALNUMBER;
    }

    enum ComputerSystemProductProperty {
        IDENTIFYINGNUMBER;
    }

    private static final WmiQuery<ComputerSystemProductProperty> IDENTIFYINGNUMBER_QUERY = new WmiQuery<>(
            "Win32_ComputerSystemProduct", ComputerSystemProductProperty.class);

    private String systemSerialNumber = "";

    WindowsComputerSystem(WmiQueryHandler queryHandler) {
        init(queryHandler);
    }

    private void init(WmiQueryHandler queryHandler) {
        WmiQuery<ComputerSystemProperty> computerSystemQuery = new WmiQuery<>("Win32_ComputerSystem",
                ComputerSystemProperty.class);
        WmiResult<ComputerSystemProperty> win32ComputerSystem = queryHandler.queryWMI(computerSystemQuery);
        if (win32ComputerSystem.getResultCount() > 0) {
            setManufacturer(WmiUtil.getString(win32ComputerSystem, ComputerSystemProperty.MANUFACTURER, 0));
            setModel(WmiUtil.getString(win32ComputerSystem, ComputerSystemProperty.MODEL, 0));
        }

        setSerialNumber(getSystemSerialNumber(queryHandler));
        setFirmware(new WindowsFirmware(queryHandler));
        setBaseboard(new WindowsBaseboard(queryHandler));
    }

    private String getSystemSerialNumber(WmiQueryHandler queryHandler) {
        if (!"".equals(this.systemSerialNumber)) {
            return this.systemSerialNumber;
        }
        // This should always work
        WmiQuery<BiosProperty> serialNumberQuery = new WmiQuery<>("Win32_BIOS where PrimaryBIOS=true",
                BiosProperty.class);
        WmiResult<BiosProperty> serialNumber = queryHandler.queryWMI(serialNumberQuery);
        if (serialNumber.getResultCount() > 0) {
            this.systemSerialNumber = WmiUtil.getString(serialNumber, BiosProperty.SERIALNUMBER, 0);
        }
        // If the above doesn't work, this might
        if ("".equals(this.systemSerialNumber)) {
            WmiResult<ComputerSystemProductProperty> identifyingNumber = queryHandler.queryWMI(IDENTIFYINGNUMBER_QUERY);
            if (identifyingNumber.getResultCount() > 0) {
                this.systemSerialNumber = WmiUtil.getString(identifyingNumber,
                        ComputerSystemProductProperty.IDENTIFYINGNUMBER, 0);
            }
        }
        // Nothing worked. Default.
        if ("".equals(this.systemSerialNumber)) {
            this.systemSerialNumber = "unknown";
        }
        return this.systemSerialNumber;
    }
}
