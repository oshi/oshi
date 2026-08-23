/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;
import oshi.util.ParseUtil;

/**
 * Graphics Card info obtained from lscfg
 */
@Immutable
public final class AixGraphicsCard extends AbstractGraphicsCard {

    /** The run of dots lscfg uses to pad a label out to its value. */
    private static final Pattern DOT_RUN = Pattern.compile("\\.\\.+");

    /**
     * Constructor for AixGraphicsCard
     *
     * @param name        The name
     * @param deviceId    The device ID
     * @param vendor      The vendor
     * @param versionInfo The version info
     * @param vram        The VRAM
     */
    AixGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    /**
     * Gets graphics cards
     *
     * @param lscfg A memoized lscfg list
     *
     * @return List of graphics cards
     */
    public static List<GraphicsCard> getGraphicsCards(Supplier<List<String>> lscfg) {
        List<GraphicsCard> cardList = new ArrayList<>();
        boolean display = false;
        String name = null;
        String vendor = null;
        List<String> versionInfo = new ArrayList<>();
        for (String line : lscfg.get()) {
            String s = line.trim();
            if (s.startsWith("Name:") && s.contains("display")) {
                display = true;
            } else if (display && s.toLowerCase(Locale.ROOT).contains("graphics")) {
                name = s;
            } else if (display && name != null) {
                if (s.startsWith("Manufacture ID")) {
                    vendor = ParseUtil.removeLeadingDots(s.substring(14));
                } else if (s.contains("Level")) {
                    versionInfo.add(DOT_RUN.matcher(s).replaceAll("="));
                } else if (s.startsWith("Hardware Location Code")) {
                    cardList.add(new AixGraphicsCard(name, Constants.UNKNOWN, ParseUtil.getStringValueOrUnknown(vendor),
                            versionInfo.isEmpty() ? Constants.UNKNOWN : String.join(",", versionInfo), 0L));
                    display = false;
                }
            }
        }
        return cardList;
    }
}
