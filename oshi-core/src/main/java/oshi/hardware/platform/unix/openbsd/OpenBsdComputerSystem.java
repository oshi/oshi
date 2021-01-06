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
package oshi.hardware.platform.unix.openbsd;

import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.Constants;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;

public class OpenBsdComputerSystem extends AbstractComputerSystem {

    @Override
    public String getManufacturer() {
        return OpenBsdSysctlUtil.sysctl("hw.vendor", Constants.UNKNOWN);
    }

    @Override
    public String getModel() {
        return OpenBsdSysctlUtil.sysctl("hw.version", Constants.UNKNOWN);
    }

    @Override
    public String getSerialNumber() {
        return OpenBsdSysctlUtil.sysctl("hw.serialno", Constants.UNKNOWN);
    }

    @Override
    protected Firmware createFirmware() {
        return new OpenBsdFirmware();
    }

    @Override
    protected Baseboard createBaseboard() {
        return new OpenBsdBaseboard( OpenBsdSysctlUtil.sysctl("hw.vendor", Constants.UNKNOWN),
            OpenBsdSysctlUtil.sysctl("hw.product", Constants.UNKNOWN),
            OpenBsdSysctlUtil.sysctl("hw.serialno", Constants.UNKNOWN),
            OpenBsdSysctlUtil.sysctl("hw.version", Constants.UNKNOWN)
        );
    }
}
