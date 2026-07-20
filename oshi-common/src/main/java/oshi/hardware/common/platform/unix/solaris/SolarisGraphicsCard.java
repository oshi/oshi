/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Graphics Card info obtained from prtconf
 */
@Immutable
public class SolarisGraphicsCard extends AbstractGraphicsCard {

    private static final String PCI_CLASS_DISPLAY = "0003";

    /**
     * Constructor for SolarisGraphicsCard
     *
     * @param name        The name
     * @param deviceId    The device ID
     * @param vendor      The vendor
     * @param versionInfo The version info
     * @param vram        The VRAM
     */
    SolarisGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    /**
     * public method used by {@code AbstractHardwareAbstractionLayer} to access the graphics cards.
     *
     * @return List of {@link SolarisGraphicsCard} objects.
     */
    public static List<GraphicsCard> getGraphicsCards() {
        return parsePrtconf(ExecutingCommand.runNative("prtconf -pv"));
    }

    /**
     * Parses the output of {@code prtconf -pv} to extract display-class PCI devices as graphics cards.
     *
     * @param devices the lines emitted by {@code prtconf -pv}
     * @return a list of {@link GraphicsCard} objects for each display device found
     */
    static List<GraphicsCard> parsePrtconf(List<String> devices) {
        List<GraphicsCard> cardList = new ArrayList<>();
        if (devices.isEmpty()) {
            return cardList;
        }
        String name = "";
        String vendorId = "";
        String productId = "";
        String classCode = "";
        List<String> versionInfoList = new ArrayList<>();
        for (String line : devices) {
            // Node 0x... identifies start of a new device. Save previous if it's a graphics
            // card
            if (line.contains("Node 0x")) {
                if (PCI_CLASS_DISPLAY.equals(classCode)) {
                    cardList.add(new SolarisGraphicsCard(name.isEmpty() ? Constants.UNKNOWN : name,
                            productId.isEmpty() ? Constants.UNKNOWN : productId,
                            vendorId.isEmpty() ? Constants.UNKNOWN : vendorId,
                            versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList), 0L));
                }
                // Reset strings
                name = "";
                vendorId = Constants.UNKNOWN;
                productId = Constants.UNKNOWN;
                classCode = "";
                versionInfoList.clear();
            } else {
                String[] split = line.trim().split(":", 2);
                if (split.length == 2) {
                    if (split[0].equals("model")) {
                        // This is preferred, always set it
                        name = ParseUtil.getSingleQuoteStringValue(line);
                    } else if (split[0].equals("name")) {
                        // Name is backup for model if model doesn't exist, so only
                        // put if name blank
                        if (name.isEmpty()) {
                            name = ParseUtil.getSingleQuoteStringValue(line);
                        }
                    } else if (split[0].equals("vendor-id")) {
                        // Format: vendor-id: 00008086
                        vendorId = "0x" + line.substring(line.length() - 4);
                    } else if (split[0].equals("device-id")) {
                        // Format: device-id: 00002440
                        productId = "0x" + line.substring(line.length() - 4);
                    } else if (split[0].equals("revision-id")) {
                        // Format: revision-id: 00000002
                        versionInfoList.add(line.trim());
                    } else if (split[0].equals("class-code")) {
                        // Format: 00030000
                        // Display class is 0003xx, first 6 bytes of this code
                        classCode = line.substring(line.length() - 8, line.length() - 4);
                    }
                }
            }
        }
        // In case we reached end before saving
        if (PCI_CLASS_DISPLAY.equals(classCode)) {
            cardList.add(new SolarisGraphicsCard(name.isEmpty() ? Constants.UNKNOWN : name,
                    productId.isEmpty() ? Constants.UNKNOWN : productId,
                    vendorId.isEmpty() ? Constants.UNKNOWN : vendorId,
                    versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList), 0L));
        }
        return cardList;
    }
}
