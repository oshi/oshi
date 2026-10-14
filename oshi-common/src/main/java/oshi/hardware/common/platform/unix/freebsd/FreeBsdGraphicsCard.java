/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Graphics Card info obtained from pciconf
 */
@Immutable
public class FreeBsdGraphicsCard extends AbstractGraphicsCard {

    private static final String PCI_CLASS_DISPLAY = "0x03";

    /**
     * Constructor for FreeBsdGraphicsCard
     *
     * @param name        The name
     * @param deviceId    The device ID
     * @param vendor      The vendor
     * @param versionInfo The version info
     * @param vram        The VRAM
     */
    FreeBsdGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    /**
     * public method used by {@code AbstractHardwareAbstractionLayer} to access the graphics cards.
     *
     * @return List of {@link FreeBsdGraphicsCard} objects.
     */
    public static List<GraphicsCard> getGraphicsCards() {
        return parsePciconf(ExecutingCommand.runNative("pciconf -lv"));
    }

    /**
     * Parses the output of {@code pciconf -lv} for display-class devices. Each device starts with a header line giving
     * its class, revision and IDs, followed by indented lines naming its vendor and device where the PCI ID database
     * knows them. The header gives the IDs as {@code vendor=0x8086 device=0x3ea0}, or as {@code chip=0x3ea08086} in
     * older FreeBSD releases and on DragonFly BSD.
     *
     * @param devices the lines of output from {@code pciconf -lv}
     * @return the graphics cards
     */
    static List<GraphicsCard> parsePciconf(List<String> devices) {
        List<GraphicsCard> cardList = new ArrayList<>();
        PciDevice device = null;
        for (String line : devices) {
            if (line.contains("class=0x") && !Character.isWhitespace(line.charAt(0))) {
                addIfDisplay(cardList, device);
                device = new PciDevice();
                for (String s : ParseUtil.whitespaces.split(line, -1)) {
                    String[] keyVal = s.split("=", 2);
                    if (keyVal.length < 2) {
                        continue;
                    }
                    switch (keyVal[0]) {
                        case "class":
                            // class=0x030000
                            device.isDisplay = keyVal[1].startsWith(PCI_CLASS_DISPLAY);
                            break;
                        case "chip":
                            // chip=0x3ea08086: device ID, then vendor ID
                            if (keyVal[1].length() >= 10) {
                                device.deviceId = keyVal[1].substring(0, 6);
                                device.vendorId = "0x" + keyVal[1].substring(6, 10);
                            }
                            break;
                        case "vendor":
                            device.vendorId = keyVal[1];
                            break;
                        case "device":
                            device.deviceId = keyVal[1];
                            break;
                        case "rev":
                            device.versionInfo = s;
                            break;
                        default:
                            break;
                    }
                }
            } else if (device != null) {
                String[] split = line.trim().split("=", 2);
                if (split.length == 2) {
                    String key = split[0].trim();
                    if (key.equals("vendor")) {
                        device.vendorName = ParseUtil.getSingleQuoteStringValue(line);
                    } else if (key.equals("device")) {
                        device.name = ParseUtil.getSingleQuoteStringValue(line);
                    }
                }
            }
        }
        addIfDisplay(cardList, device);
        return Collections.unmodifiableList(cardList);
    }

    private static void addIfDisplay(List<GraphicsCard> cardList, @Nullable PciDevice device) {
        if (device != null && device.isDisplay) {
            String vendor;
            if (device.vendorName.isEmpty()) {
                vendor = device.vendorId;
            } else if (Constants.UNKNOWN.equals(device.vendorId)) {
                vendor = device.vendorName;
            } else {
                vendor = device.vendorName + " (" + device.vendorId + ")";
            }
            cardList.add(new FreeBsdGraphicsCard(ParseUtil.getStringValueOrUnknown(device.name), device.deviceId,
                    vendor, device.versionInfo, 0L));
        }
    }

    /** The fields of one device collected from its lines of {@code pciconf} output. */
    private static final class PciDevice {
        private boolean isDisplay;
        private String vendorId = Constants.UNKNOWN;
        private String deviceId = Constants.UNKNOWN;
        private String versionInfo = Constants.UNKNOWN;
        private String vendorName = "";
        private String name = "";
    }
}
