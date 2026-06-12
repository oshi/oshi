/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;
import oshi.util.ExecutingCommand;

/**
 * Shared sound card implementation for BSDs that use {@code dmesg} audio/pci patterns (NetBSD, OpenBSD).
 */
@Immutable
public final class BsdSoundCard extends AbstractSoundCard {

    private static final Pattern AUDIO_AT = Pattern.compile("audio\\d+ at (.+)");
    private static final Pattern PCI_AT = Pattern
            .compile("(.+) at pci\\d+ dev \\d+ function \\d+ \"(.*)\" (rev .+):.*");

    public BsdSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    /**
     * Gets sound cards by parsing {@code dmesg} output.
     *
     * @return a {@link java.util.List} object.
     */
    public static List<SoundCard> getSoundCards() {
        List<String> dmesg = ExecutingCommand.runNative("dmesg");
        Set<String> names = new HashSet<>();
        for (String line : dmesg) {
            Matcher m = AUDIO_AT.matcher(line);
            if (m.matches()) {
                names.add(m.group(1));
            }
        }
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> codecMap = new HashMap<>();
        Map<String, String> versionMap = new HashMap<>();
        String key = "";
        for (String line : dmesg) {
            Matcher m = PCI_AT.matcher(line);
            if (m.matches() && names.contains(m.group(1))) {
                key = m.group(1);
                nameMap.put(key, m.group(2));
                versionMap.put(key, m.group(3));
            } else if (!key.isEmpty()) {
                int idx = line.indexOf("codec");
                if (idx >= 0) {
                    idx = line.indexOf(':');
                    codecMap.put(key, line.substring(idx + 1).trim());
                }
                key = "";
            }
        }
        List<SoundCard> soundCards = new ArrayList<>();
        for (Entry<String, String> entry : nameMap.entrySet()) {
            soundCards.add(
                    new BsdSoundCard(versionMap.get(entry.getKey()), entry.getValue(), codecMap.get(entry.getKey())));
        }
        return soundCards;
    }
}
