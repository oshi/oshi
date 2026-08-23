/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Hardware data obtained from lsattr
 */
@Immutable
public final class AixComputerSystem extends AbstractComputerSystem {

    private final Supplier<LsattrStrings> lsattrStrings = memoize(AixComputerSystem::readLsattr);
    private final Supplier<List<String>> lscfg;

    /**
     * Constructs a new {@code AixComputerSystem}.
     *
     * @param lscfg a supplier of {@code lscfg} output
     */
    public AixComputerSystem(Supplier<List<String>> lscfg) {
        this.lscfg = lscfg;
    }

    @Override
    public String getManufacturer() {
        return ParseUtil.getStringValueOrUnknown(lsattrStrings.get().manufacturer);
    }

    @Override
    public String getModel() {
        return ParseUtil.getStringValueOrUnknown(lsattrStrings.get().model);
    }

    @Override
    public String getSerialNumber() {
        return ParseUtil.getStringValueOrUnknown(lsattrStrings.get().serialNumber);
    }

    @Override
    public String getHardwareUUID() {
        return ParseUtil.getStringValueOrUnknown(lsattrStrings.get().uuid);
    }

    @Override
    public Firmware createFirmware() {
        return new AixFirmware(ParseUtil.getStringValueOrUnknown(lsattrStrings.get().biosVendor),
                ParseUtil.getStringValueOrUnknown(lsattrStrings.get().biosPlatformVersion),
                ParseUtil.getStringValueOrUnknown(lsattrStrings.get().biosVersion));
    }

    @Override
    public Baseboard createBaseboard() {
        return new AixBaseboard(lscfg);
    }

    private static LsattrStrings readLsattr() {
        return parseLsattr(ExecutingCommand.runNative("lsattr -El sys0"), ExecutingCommand.runNative("lsmcode -c"));
    }

    /**
     * Parses {@code lsattr -El sys0} and {@code lsmcode -c} output into the system's firmware and identity strings.
     *
     * @param lsattr  the lines of {@code lsattr -El sys0} output
     * @param lsmcode the lines of {@code lsmcode -c} output
     * @return the parsed firmware and identity strings (blank fields defaulted to {@link Constants#UNKNOWN})
     */
    static LsattrStrings parseLsattr(List<String> lsattr, List<String> lsmcode) {
        String fwVendor = "IBM";
        String fwVersion = null;
        String fwPlatformVersion = null;

        String manufacturer = fwVendor;
        String model = null;
        String serialNumber = null;
        String uuid = null;

        /*-
        fwversion       IBM,RG080425_d79e22_r                Firmware version and revision levels                False
        modelname       IBM,9114-275                         Machine name                                        False
        os_uuid         789f930f-b15c-4639-b842-b42603862704 N/A                                                 True
        rtasversion     1                                    Open Firmware RTAS version                          False
        systemid        IBM,0110ACFDE                        Hardware system identifier                          False
        */

        final String fwVersionMarker = "fwversion";
        final String modelMarker = "modelname";
        final String systemIdMarker = "systemid";
        final String uuidMarker = "os_uuid";
        final String fwPlatformVersionMarker = "Platform Firmware level is";

        for (final String checkLine : lsattr) {
            if (checkLine.startsWith(fwVersionMarker)) {
                fwVersion = ParseUtil.getTextAfterString(checkLine, fwVersionMarker).trim();
                int comma = fwVersion.indexOf(',');
                if (comma > 0 && fwVersion.length() > comma) {
                    fwVendor = fwVersion.substring(0, comma);
                    fwVersion = fwVersion.substring(comma + 1);
                }
                fwVersion = ParseUtil.whitespaces.split(fwVersion, -1)[0];
            } else if (checkLine.startsWith(modelMarker)) {
                model = ParseUtil.getTextAfterString(checkLine, modelMarker).trim();
                int comma = model.indexOf(',');
                if (comma > 0 && model.length() > comma) {
                    manufacturer = model.substring(0, comma);
                    model = model.substring(comma + 1);
                }
                model = ParseUtil.whitespaces.split(model, -1)[0];
            } else if (checkLine.startsWith(systemIdMarker)) {
                serialNumber = ParseUtil.getTextAfterString(checkLine, systemIdMarker).trim();
                serialNumber = ParseUtil.whitespaces.split(serialNumber, -1)[0];
            } else if (checkLine.startsWith(uuidMarker)) {
                uuid = ParseUtil.getTextAfterString(checkLine, uuidMarker).trim();
                uuid = ParseUtil.whitespaces.split(uuid, -1)[0];
            }
        }
        for (final String checkLine : lsmcode) {
            /*-
             Platform Firmware level is 3F080425
             System Firmware level is RG080425_d79e22_regatta
             */
            if (checkLine.startsWith(fwPlatformVersionMarker)) {
                fwPlatformVersion = ParseUtil.getTextAfterString(checkLine, fwPlatformVersionMarker).trim();
                break;
            }
        }
        return new LsattrStrings(fwVendor, fwPlatformVersion, fwVersion, manufacturer, model, serialNumber, uuid);
    }

    static final class LsattrStrings {
        private final String biosVendor;
        private final String biosPlatformVersion;
        private final String biosVersion;

        private final String manufacturer;
        private final String model;
        private final String serialNumber;
        private final String uuid;

        private LsattrStrings(@Nullable String biosVendor, @Nullable String biosPlatformVersion,
                @Nullable String biosVersion, @Nullable String manufacturer, @Nullable String model,
                @Nullable String serialNumber, @Nullable String uuid) {
            this.biosVendor = ParseUtil.getStringValueOrUnknown(biosVendor);
            this.biosPlatformVersion = ParseUtil.getStringValueOrUnknown(biosPlatformVersion);
            this.biosVersion = ParseUtil.getStringValueOrUnknown(biosVersion);

            this.manufacturer = ParseUtil.getStringValueOrUnknown(manufacturer);
            this.model = ParseUtil.getStringValueOrUnknown(model);
            this.serialNumber = ParseUtil.getStringValueOrUnknown(serialNumber);
            this.uuid = ParseUtil.getStringValueOrUnknown(uuid);
        }

        String biosVendor() {
            return biosVendor;
        }

        String biosPlatformVersion() {
            return biosPlatformVersion;
        }

        String biosVersion() {
            return biosVersion;
        }

        String manufacturer() {
            return manufacturer;
        }

        String model() {
            return model;
        }

        String serialNumber() {
            return serialNumber;
        }

        String uuid() {
            return uuid;
        }
    }
}
