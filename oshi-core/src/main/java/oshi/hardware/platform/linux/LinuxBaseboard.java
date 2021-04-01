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
package oshi.hardware.platform.linux;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.linux.Sysfs;
import oshi.driver.linux.proc.CpuInfo;
import oshi.hardware.common.AbstractBaseboard;
import oshi.util.Constants;
import oshi.util.tuples.Quartet;

/**
 * Baseboard data obtained by sysfs
 */
@Immutable
final class LinuxBaseboard extends AbstractBaseboard {

    private final Supplier<String> manufacturer = memoize(this::queryManufacturer);
    private final Supplier<String> model = memoize(this::queryModel);
    private final Supplier<String> version = memoize(this::queryVersion);
    private final Supplier<String> serialNumber = memoize(this::querySerialNumber);
    private final Supplier<Quartet<String, String, String, String>> manufacturerModelVersionSerial = memoize(
            CpuInfo::queryBoardInfo);

    @Override
    public String getManufacturer() {
        return manufacturer.get();
    }

    @Override
    public String getModel() {
        return model.get();
    }

    @Override
    public String getVersion() {
        return version.get();
    }

    @Override
    public String getSerialNumber() {
        return serialNumber.get();
    }

    private String queryManufacturer() {
        String result = null;
        if ((result = Sysfs.queryBoardVendor()) == null
                && (result = manufacturerModelVersionSerial.get().getA()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private String queryModel() {
        String result = null;
        if ((result = Sysfs.queryBoardModel()) == null
                && (result = manufacturerModelVersionSerial.get().getB()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private String queryVersion() {
        String result = null;
        if ((result = Sysfs.queryBoardVersion()) == null
                && (result = manufacturerModelVersionSerial.get().getC()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }

    private String querySerialNumber() {
        String result = null;
        if ((result = Sysfs.queryBoardSerial()) == null
                && (result = manufacturerModelVersionSerial.get().getD()) == null) {
            return Constants.UNKNOWN;
        }
        return result;
    }
}
