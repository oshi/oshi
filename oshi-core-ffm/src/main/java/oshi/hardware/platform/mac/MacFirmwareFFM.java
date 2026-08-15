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
                                        case "rom":
                                            byte[] data = current.getByteArrayProperty("vendor");
                                            if (data != null) {
                                                manufacturer = ParseUtil.decodeNulTerminated(data,
                                                        StandardCharsets.UTF_8);
                                            }
                                            data = current.getByteArrayProperty("version");
                                            if (data != null) {
                                                version = ParseUtil.decodeNulTerminated(data, StandardCharsets.UTF_8);
                                            }
                                            data = current.getByteArrayProperty("release-date");
                                            if (data != null) {
                                                releaseDate = ParseUtil.decodeNulTerminated(data,
                                                        StandardCharsets.UTF_8);
                                            }
                                            break;
                                        case "chosen":
                                            data = current.getByteArrayProperty("booter-name");
                                            if (data != null) {
                                                name = ParseUtil.decodeNulTerminated(data, StandardCharsets.UTF_8);
                                            }
                                            break;
                                        case "efi":
                                            data = current.getByteArrayProperty("firmware-abi");
                                            if (data != null) {
                                                description = ParseUtil.decodeNulTerminated(data,
                                                        StandardCharsets.UTF_8);
                                            }
                                            break;
                                        default:
                                            if (Util.isBlank(name)) {
                                                name = current.getStringProperty("IONameMatch");
                                            }
                                            break;
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
