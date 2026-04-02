/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static oshi.util.Memoizer.memoize;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.mac.IOKit.IOIterator;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.hardware.common.AbstractFirmware;
import oshi.util.Constants;
import oshi.util.Util;
import oshi.util.platform.mac.IOKitUtilFFM;
import oshi.util.tuples.Quintet;

/**
 * Firmware data obtained from ioreg.
 */
@Immutable
final class MacFirmwareFFM extends AbstractFirmware {

    private final Supplier<Quintet<String, String, String, String, String>> manufNameDescVersRelease = memoize(
            MacFirmwareFFM::queryEfi);

    @Override
    public String getManufacturer() {
        return manufNameDescVersRelease.get().getA();
    }

    @Override
    public String getName() {
        return manufNameDescVersRelease.get().getB();
    }

    @Override
    public String getDescription() {
        return manufNameDescVersRelease.get().getC();
    }

    @Override
    public String getVersion() {
        return manufNameDescVersRelease.get().getD();
    }

    @Override
    public String getReleaseDate() {
        return manufNameDescVersRelease.get().getE();
    }

    private static Quintet<String, String, String, String, String> queryEfi() {
        String manufacturer = null;
        String name = null;
        String description = null;
        String version = null;
        String releaseDate = null;

        IORegistryEntry platformExpert = IOKitUtilFFM.getMatchingService("IOPlatformExpertDevice");
        if (platformExpert != null) {
            try {
                IOIterator iter = platformExpert.getChildIterator("IODeviceTree");
                if (iter != null) {
                    try {
                        IORegistryEntry entry = iter.next();
                        while (entry != null) {
                            try {
                                String entryName = entry.getName();
                                if (entryName != null) {
                                    switch (entryName) {
                                        case "rom":
                                            byte[] data = entry.getByteArrayProperty("vendor");
                                            if (data != null) {
                                                manufacturer = toUtf8(data);
                                            }
                                            data = entry.getByteArrayProperty("version");
                                            if (data != null) {
                                                version = toUtf8(data);
                                            }
                                            data = entry.getByteArrayProperty("release-date");
                                            if (data != null) {
                                                releaseDate = toUtf8(data);
                                            }
                                            break;
                                        case "chosen":
                                            data = entry.getByteArrayProperty("booter-name");
                                            if (data != null) {
                                                name = toUtf8(data);
                                            }
                                            break;
                                        case "efi":
                                            data = entry.getByteArrayProperty("firmware-abi");
                                            if (data != null) {
                                                description = toUtf8(data);
                                            }
                                            break;
                                        default:
                                            if (Util.isBlank(name)) {
                                                name = entry.getStringProperty("IONameMatch");
                                            }
                                            break;
                                    }
                                }
                            } finally {
                                entry.release();
                            }
                            entry = iter.next();
                        }
                    } finally {
                        iter.release();
                    }
                }
                if (Util.isBlank(manufacturer)) {
                    byte[] data = platformExpert.getByteArrayProperty("manufacturer");
                    if (data != null) {
                        manufacturer = toUtf8(data);
                    }
                }
                if (Util.isBlank(version)) {
                    byte[] data = platformExpert.getByteArrayProperty("target-type");
                    if (data != null) {
                        version = toUtf8(data);
                    }
                }
                if (Util.isBlank(name)) {
                    byte[] data = platformExpert.getByteArrayProperty("device_type");
                    if (data != null) {
                        name = toUtf8(data);
                    }
                }
            } finally {
                platformExpert.release();
            }
        }
        return new Quintet<>(Util.isBlank(manufacturer) ? Constants.UNKNOWN : manufacturer,
                Util.isBlank(name) ? Constants.UNKNOWN : name,
                Util.isBlank(description) ? Constants.UNKNOWN : description,
                Util.isBlank(version) ? Constants.UNKNOWN : version,
                Util.isBlank(releaseDate) ? Constants.UNKNOWN : releaseDate);
    }

    private static String toUtf8(byte[] data) {
        return new String(data, StandardCharsets.UTF_8).trim().replace("\0", "");
    }
}
