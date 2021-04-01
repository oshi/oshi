/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery; //NOSONAR squid:S1191
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_BIOS}
 */
@ThreadSafe
public final class Win32Bios {

    private static final String WIN32_BIOS_WHERE_PRIMARY_BIOS_TRUE = "Win32_BIOS where PrimaryBIOS=true";

    /**
     * Serial number property.
     */
    public enum BiosSerialProperty {
        SERIALNUMBER;
    }

    /**
     * BIOS description properties.
     */
    public enum BiosProperty {
        MANUFACTURER, NAME, DESCRIPTION, VERSION, RELEASEDATE;
    }

    private Win32Bios() {
    }

    /**
     * Queries the BIOS serial number.
     *
     * @return Assigned serial number of the software element.
     */
    public static WmiResult<BiosSerialProperty> querySerialNumber() {
        WmiQuery<BiosSerialProperty> serialNumQuery = new WmiQuery<>(WIN32_BIOS_WHERE_PRIMARY_BIOS_TRUE,
                BiosSerialProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(serialNumQuery);
    }

    /**
     * Queries the BIOS description.
     *
     * @return BIOS name, description, and related fields.
     */
    public static WmiResult<BiosProperty> queryBiosInfo() {
        WmiQuery<BiosProperty> biosQuery = new WmiQuery<>(WIN32_BIOS_WHERE_PRIMARY_BIOS_TRUE, BiosProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(biosQuery);
    }
}
