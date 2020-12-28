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
import oshi.util.platform.unix.openbsd.OpenBSDSysctlUtil;

public class OpenBSDComputerSystem extends AbstractComputerSystem {

    @Override
    public String getManufacturer() {
        return OpenBSDSysctlUtil.sysctl("hw.vendor", "unknown");
    }

    @Override
    public String getModel() {
        // or version
        return OpenBSDSysctlUtil.sysctl("hw.product", "unknown");
    }

    @Override
    public String getSerialNumber() {
        // could also use uuid
        return OpenBSDSysctlUtil.sysctl("hw.serialno", "unknown");
    }

    @Override
    protected Firmware createFirmware() {
        return new OpenBSDFirmware();
    }

    @Override
    protected Baseboard createBaseboard() {
        // TODO implement
        return new OpenBSDBaseboard(Constants.UNKNOWN, Constants.UNKNOWN, Constants.UNKNOWN, Constants.UNKNOWN);
    }
}
