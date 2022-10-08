/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
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
