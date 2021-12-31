/*
 * MIT License
 *
 * Copyright (c) 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.hardware.platform.unix.UnixBaseboard;
import oshi.util.Constants;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;

/**
 * OpenBSD ComputerSystem implementation
 */
@Immutable
public class OpenBsdComputerSystem extends AbstractComputerSystem {

    private final Supplier<String> manufacturer = memoize(OpenBsdComputerSystem::queryManufacturer);

    private final Supplier<String> model = memoize(OpenBsdComputerSystem::queryModel);

    private final Supplier<String> serialNumber = memoize(OpenBsdComputerSystem::querySerialNumber);

    private final Supplier<String> uuid = memoize(OpenBsdComputerSystem::queryUUID);

    @Override
    public String getManufacturer() {
        return manufacturer.get();
    }

    @Override
    public String getModel() {
        return model.get();
    }

    @Override
    public String getSerialNumber() {
        return serialNumber.get();
    }

    @Override
    public String getHardwareUUID() {
        return uuid.get();
    }

    @Override
    protected Firmware createFirmware() {
        return new OpenBsdFirmware();
    }

    @Override
    protected Baseboard createBaseboard() {
        return new UnixBaseboard(manufacturer.get(), model.get(), serialNumber.get(),
                OpenBsdSysctlUtil.sysctl("hw.product", Constants.UNKNOWN));
    }

    private static String queryManufacturer() {
        return OpenBsdSysctlUtil.sysctl("hw.vendor", Constants.UNKNOWN);
    }

    private static String queryModel() {
        return OpenBsdSysctlUtil.sysctl("hw.version", Constants.UNKNOWN);
    }

    private static String querySerialNumber() {
        return OpenBsdSysctlUtil.sysctl("hw.serialno", Constants.UNKNOWN);
    }

    private static String queryUUID() {
        return OpenBsdSysctlUtil.sysctl("hw.uuid", Constants.UNKNOWN);
    }
}
