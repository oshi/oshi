/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.nio.charset.StandardCharsets;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.platform.mac.IOKit.IOIterator;
import oshi.ffm.platform.mac.IOKit.IORegistryEntry;
import oshi.ffm.util.platform.mac.IOKitUtilFFM;
import oshi.hardware.common.platform.mac.MacFirmware;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Quintet;

/**
 * Firmware data obtained from ioreg.
 */
@Immutable
final class MacFirmwareFFM extends MacFirmware {

    @Override
    protected Quintet<String, String, String, String, String> queryEfi() {
        String manufacturer = null;
        String name = null;
        String description = null;
        String version = null;
        String releaseDate = null;

        IORegistryEntry platformExpert = IOKitUtilFFM.getMatchingService("IOPlatformExpertDevice");
        if (platformExpert != null) {
            try (platformExpert) {
                IOIterator iter = platformExpert.getChildIterator("IODeviceTree");
                if (iter != null) {
                    try (iter) {
                        IORegistryEntry entry = iter.next();
                        while (entry != null) {
                            try (IORegistryEntry current = entry) {
                                String entryName = current.getName();
                                if (entryName != null) {
                                    switch (entryName) {
                                        case "rom" -> {
                                            byte[] vendor = current.getByteArrayProperty("vendor");
                                            if (vendor != null) {
                                                manufacturer = ParseUtil.decodeNulTerminated(vendor,
                                                        StandardCharsets.UTF_8);
                                            }
                                            byte[] romVersion = current.getByteArrayProperty("version");
                                            if (romVersion != null) {
                                                version = ParseUtil.decodeNulTerminated(romVersion,
                                                        StandardCharsets.UTF_8);
                                            }
                                            byte[] date = current.getByteArrayProperty("release-date");
                                            if (date != null) {
                                                releaseDate = ParseUtil.decodeNulTerminated(date,
                                                        StandardCharsets.UTF_8);
                                            }
                                        }
                                        case "chosen" -> {
                                            byte[] booter = current.getByteArrayProperty("booter-name");
                                            if (booter != null) {
                                                name = ParseUtil.decodeNulTerminated(booter, StandardCharsets.UTF_8);
                                            }
                                        }
                                        case "efi" -> {
                                            byte[] abi = current.getByteArrayProperty("firmware-abi");
                                            if (abi != null) {
                                                description = ParseUtil.decodeNulTerminated(abi,
                                                        StandardCharsets.UTF_8);
                                            }
                                        }
                                        default -> {
                                            if (Util.isBlank(name)) {
                                                name = current.getStringProperty("IONameMatch");
                                            }
                                        }
                                    }
                                }
                            }
                            entry = iter.next();
                        }
                    }
                }
                if (Util.isBlank(manufacturer)) {
                    byte[] data = platformExpert.getByteArrayProperty("manufacturer");
                    if (data != null) {
                        manufacturer = ParseUtil.decodeNulTerminated(data, StandardCharsets.UTF_8);
                    }
                }
                if (Util.isBlank(version)) {
                    byte[] data = platformExpert.getByteArrayProperty("target-type");
                    if (data != null) {
                        version = ParseUtil.decodeNulTerminated(data, StandardCharsets.UTF_8);
                    }
                }
                if (Util.isBlank(name)) {
                    byte[] data = platformExpert.getByteArrayProperty("device_type");
                    if (data != null) {
                        name = ParseUtil.decodeNulTerminated(data, StandardCharsets.UTF_8);
                    }
                }
            }
        }
        return new Quintet<>(ParseUtil.getStringValueOrUnknown(manufacturer), ParseUtil.getStringValueOrUnknown(name),
                ParseUtil.getStringValueOrUnknown(description), ParseUtil.getStringValueOrUnknown(version),
                ParseUtil.getStringValueOrUnknown(releaseDate));
    }

}
