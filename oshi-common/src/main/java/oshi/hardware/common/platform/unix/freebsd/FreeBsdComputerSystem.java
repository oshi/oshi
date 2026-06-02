/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.hardware.common.platform.unix.UnixBaseboard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Quintet;

@Immutable
public abstract class FreeBsdComputerSystem extends AbstractComputerSystem {

    private final Supplier<Quintet<String, String, String, String, String>> manufModelSerialUuidVers = memoize(
            this::readDmiDecode);

    @Override
    public String getManufacturer() {
        return manufModelSerialUuidVers.get().getA();
    }

    @Override
    public String getModel() {
        return manufModelSerialUuidVers.get().getB();
    }

    @Override
    public String getSerialNumber() {
        return manufModelSerialUuidVers.get().getC();
    }

    @Override
    public String getHardwareUUID() {
        return manufModelSerialUuidVers.get().getD();
    }

    @Override
    public Firmware createFirmware() {
        return new FreeBsdFirmware();
    }

    @Override
    public Baseboard createBaseboard() {
        return new UnixBaseboard(manufModelSerialUuidVers.get().getA(), manufModelSerialUuidVers.get().getB(),
                manufModelSerialUuidVers.get().getC(), manufModelSerialUuidVers.get().getE());
    }

    /**
     * Reads {@code kern.hostuuid} via the subclass's sysctl mechanism. Used as a fallback when {@code dmidecode} does
     * not yield a UUID (commonly the case without root).
     *
     * @return the host UUID, or {@link oshi.util.Constants#UNKNOWN} if the sysctl read fails
     */
    protected abstract String queryHostUuid();

    private Quintet<String, String, String, String, String> readDmiDecode() {
        String manufacturer = null;
        String model = null;
        String serialNumber = null;
        String uuid = null;
        String version = null;

        final String manufacturerMarker = "Manufacturer:";
        final String productNameMarker = "Product Name:";
        final String serialNumMarker = "Serial Number:";
        final String uuidMarker = "UUID:";
        final String versionMarker = "Version:";

        // Only works with root permissions but it's all we've got
        for (final String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
            if (checkLine.contains(manufacturerMarker)) {
                manufacturer = checkLine.split(manufacturerMarker)[1].trim();
            } else if (checkLine.contains(productNameMarker)) {
                model = checkLine.split(productNameMarker)[1].trim();
            } else if (checkLine.contains(serialNumMarker)) {
                serialNumber = checkLine.split(serialNumMarker)[1].trim();
            } else if (checkLine.contains(uuidMarker)) {
                uuid = checkLine.split(uuidMarker)[1].trim();
            } else if (checkLine.contains(versionMarker)) {
                version = checkLine.split(versionMarker)[1].trim();
            }
        }
        // If we get to end and haven't assigned, use fallback
        if (Util.isBlank(serialNumber)) {
            serialNumber = querySystemSerialNumber();
        }
        if (Util.isBlank(uuid)) {
            uuid = queryHostUuid();
        }
        return new Quintet<>(Util.isBlank(manufacturer) ? Constants.UNKNOWN : manufacturer,
                Util.isBlank(model) ? Constants.UNKNOWN : model,
                Util.isBlank(serialNumber) ? Constants.UNKNOWN : serialNumber,
                Util.isBlank(uuid) ? Constants.UNKNOWN : uuid, Util.isBlank(version) ? Constants.UNKNOWN : version);
    }

    private static String querySystemSerialNumber() {
        String marker = "system.hardware.serial =";
        for (String checkLine : ExecutingCommand.runNative("lshal")) {
            if (checkLine.contains(marker)) {
                return ParseUtil.getSingleQuoteStringValue(checkLine);
            }
        }
        return Constants.UNKNOWN;
    }
}
