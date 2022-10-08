/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;
import oshi.util.Constants;
import oshi.util.ParseUtil;

/**
 * AIX Sound Card.
 */
@Immutable
final class AixSoundCard extends AbstractSoundCard {

    /**
     * Constructor for AixSoundCard.
     *
     * @param kernelVersion The version
     * @param name          The name
     * @param codec         The codec
     */
    AixSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    /**
     * Gets sound cards
     *
     * @param lscfg a memoized lscfg object
     *
     * @return sound cards
     */
    public static List<SoundCard> getSoundCards(Supplier<List<String>> lscfg) {
        List<SoundCard> soundCards = new ArrayList<>();
        for (String line : lscfg.get()) {
            String s = line.trim();
            if (s.startsWith("paud")) {
                String[] split = ParseUtil.whitespaces.split(s, 3);
                if (split.length == 3) {
                    soundCards.add(new AixSoundCard(Constants.UNKNOWN, split[2], Constants.UNKNOWN));
                }
            }
        }
        return soundCards;
    }
}
