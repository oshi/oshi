/*
 * Copyright 2018-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Solaris Sound Card.
 */
@Immutable
final class SolarisSoundCard extends AbstractSoundCard {

    private static final String LSHAL = "lshal";
    private static final String DEFAULT_AUDIO_DRIVER = "audio810";

    /**
     * Constructor for SolarisSoundCard.
     *
     * @param kernelVersion The version
     * @param name          The name
     * @param codec         The codec
     */
    SolarisSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    /**
     * <p>
     * getSoundCards.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public static List<SoundCard> getSoundCards() {
        Map<String, String> vendorMap = new HashMap<>();
        Map<String, String> productMap = new HashMap<>();
        List<String> sounds = new ArrayList<>();
        String key = "";
        for (String line : ExecutingCommand.runNative(LSHAL)) {
            line = line.trim();
            if (line.startsWith("udi =")) {
                // we have the key.
                key = ParseUtil.getSingleQuoteStringValue(line);
            } else if (!key.isEmpty() && !line.isEmpty()) {
                if (line.contains("info.solaris.driver =")
                        && DEFAULT_AUDIO_DRIVER.equals(ParseUtil.getSingleQuoteStringValue(line))) {
                    sounds.add(key);
                } else if (line.contains("info.product")) {
                    productMap.put(key, ParseUtil.getStringBetween(line, '\''));
                } else if (line.contains("info.vendor")) {
                    vendorMap.put(key, ParseUtil.getStringBetween(line, '\''));
                }
            }
        }
        List<SoundCard> soundCards = new ArrayList<>();
        for (String s : sounds) {
            soundCards.add(new SolarisSoundCard(productMap.get(s) + " " + DEFAULT_AUDIO_DRIVER,
                    vendorMap.get(s) + " " + productMap.get(s), productMap.get(s)));
        }
        return soundCards;
    }
}
