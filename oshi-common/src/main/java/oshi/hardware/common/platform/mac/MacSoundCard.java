/*
 * Copyright 2018-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * Sound card data obtained via the AppleHDA kext on Intel Macs, and from the I2C audio chips in the IOKit registry on
 * Apple Silicon.
 */
@Immutable
public final class MacSoundCard extends AbstractSoundCard {

    private static final String APPLE = "Apple Inc.";

    private static final String APPLE_HDA_PLIST = "/System/Library/Extensions/AppleHDA.kext/Contents/Info.plist";
    private static final String EMBEDDED_AUDIO_PLIST = "/System/Library/Extensions/AppleEmbeddedAudio.kext/Contents/Info.plist";
    private static final String AUDIO_CONTROL_PREFIX = "audio-control,";

    /**
     * Constructor for MacSoundCard.
     *
     * @param kernelVersion The version
     * @param name          The name
     * @param codec         The codec
     */
    public MacSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    /**
     * public method used by {@code AbstractHardwareAbstractionLayer} to access the sound cards.
     *
     * @param ioKit the IOKit provider used to find the audio chips on Apple Silicon
     * @return List of {@link MacSoundCard} objects.
     */
    public static List<SoundCard> getSoundCards(IOKitProvider ioKit) {
        List<String> hdaPlist = FileUtil.readFile(APPLE_HDA_PLIST, false);
        if (!hdaPlist.isEmpty()) {
            // Intel: the AppleHDA controller and its codec
            String version = plistValue(hdaPlist, "com.apple.driver.AppleHDAController");
            String kernelVersion = version.isEmpty() ? "AppleHDAController" : "AppleHDAController " + version;
            return Collections.singletonList(new MacSoundCard(kernelVersion, APPLE, "AppleHDACodec"));
        }
        // Apple Silicon has no HDA; its codec and speaker amplifiers are I2C devices driven by AppleEmbeddedAudio
        String version = plistValue(FileUtil.readFile(EMBEDDED_AUDIO_PLIST, false), "CFBundleShortVersionString");
        String kernelVersion = version.isEmpty() ? "AppleEmbeddedAudio" : "AppleEmbeddedAudio " + version;
        return queryAudioControlChips(ioKit, kernelVersion);
    }

    /**
     * Lists the audio chips among the I2C devices in the IOKit registry, one sound card per distinct part. Each chip's
     * device tree {@code compatible} value is {@code audio-control,} followed by its part number; a part that appears
     * several times, such as one amplifier per speaker, is reported once under the shortest of its node names.
     *
     * @param ioKit         the IOKit provider
     * @param kernelVersion the audio driver version to report on each card
     * @return the sound cards, empty if no audio chip is found
     */
    static List<SoundCard> queryAudioControlChips(IOKitProvider ioKit, String kernelVersion) {
        Map<String, String> nameByPart = new LinkedHashMap<>();
        ioKit.forEachMatchingService("AppleARMIICDevice", entry -> {
            String compatible = ParseUtil.decodeNulTerminated(entry.getByteArrayProperty("compatible"),
                    StandardCharsets.UTF_8);
            if (compatible.startsWith(AUDIO_CONTROL_PREFIX)) {
                String part = compatible.substring(AUDIO_CONTROL_PREFIX.length());
                String name = ParseUtil.getStringValueOrUnknown(entry.getName());
                String known = nameByPart.get(part);
                // Shortest name wins: audio-speaker rather than audio-speaker-left-woofer-2, whatever the order
                if (!part.isEmpty() && (known == null || name.length() < known.length())) {
                    nameByPart.put(part, name);
                }
            }
        });
        List<SoundCard> soundCards = new ArrayList<>(nameByPart.size());
        for (Map.Entry<String, String> e : nameByPart.entrySet()) {
            soundCards.add(new MacSoundCard(kernelVersion, e.getValue(), e.getKey()));
        }
        return soundCards;
    }

    /**
     * Reads the string value following a key in an XML property list, such as a kext's {@code Info.plist}.
     *
     * @param plist the lines of the property list
     * @param key   the key whose value to read
     * @return the value, or an empty string if the key is absent
     */
    static String plistValue(List<String> plist, String key) {
        String keyMarker = "<key>" + key + "</key>";
        for (int i = 0; i < plist.size() - 1; i++) {
            if (plist.get(i).contains(keyMarker)) {
                return ParseUtil.getTextBetweenStrings(plist.get(i + 1), "<string>", "</string>").trim();
            }
        }
        return "";
    }
}
