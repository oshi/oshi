/*
 * Copyright 2018-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * Sound card data obtained via AppleHDA kext
 */
@Immutable
final class MacSoundCard extends AbstractSoundCard {

    private static final String APPLE = "Apple Inc.";

    /**
     * Constructor for MacSoundCard.
     *
     * @param kernelVersion The version
     * @param name          The name
     * @param codec         The codec
     */
    MacSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    /**
     * public method used by {@link oshi.hardware.common.AbstractHardwareAbstractionLayer} to access the sound cards.
     *
     * @return List of {@link oshi.hardware.platform.mac.MacSoundCard} objects.
     */
    public static List<SoundCard> getSoundCards() {
        List<SoundCard> soundCards = new ArrayList<>();

        // /System/Library/Extensions/AppleHDA.kext/Contents/Info.plist

        // ..... snip ....
        // <dict>
        // <key>com.apple.driver.AppleHDAController</key>
        // <string>1.7.2a1</string>

        String manufacturer = APPLE;
        String kernelVersion = "AppleHDAController";
        String codec = "AppleHDACodec";

        boolean version = false;
        String versionMarker = "<key>com.apple.driver.AppleHDAController</key>";

        for (final String checkLine : FileUtil
                .readFile("/System/Library/Extensions/AppleHDA.kext/Contents/Info.plist")) {
            if (checkLine.contains(versionMarker)) {
                version = true;
                continue;
            }
            if (version) {
                kernelVersion = "AppleHDAController "
                        + ParseUtil.getTextBetweenStrings(checkLine, "<string>", "</string>");
                version = false;
            }
        }
        soundCards.add(new MacSoundCard(kernelVersion, manufacturer, codec));

        return soundCards;
    }
}
