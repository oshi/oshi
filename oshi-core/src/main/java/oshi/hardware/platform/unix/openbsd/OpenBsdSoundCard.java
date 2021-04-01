/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.unix.openbsd;

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
 * OpenBSD soundcard.
 */
@Immutable
final class OpenBsdSoundCard extends AbstractSoundCard {

    private static final Pattern AUDIO_AT = Pattern.compile("audio\\d+ at (.+)");
    private static final Pattern PCI_AT = Pattern
            .compile("(.+) at pci\\d+ dev \\d+ function \\d+ \"(.*)\" (rev .+):.*");

    /**
     * Constructor for OpenBsdSoundCard.
     *
     * @param kernelVersion
     *            The version
     * @param name
     *            The name
     * @param codec
     *            The codec
     */
    OpenBsdSoundCard(String kernelVersion, String name, String codec) {
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
        List<String> dmesg = ExecutingCommand.runNative("dmesg");
        // Iterate dmesg once to collect location of audioN
        Set<String> names = new HashSet<>();
        for (String line : dmesg) {
            Matcher m = AUDIO_AT.matcher(line);
            if (m.matches()) {
                names.add(m.group(1));
            }
        }
        // Iterate again and add cards when they match the name
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
                // Codec is on the next line
                int idx = line.indexOf("codec");
                if (idx >= 0) {
                    idx = line.indexOf(':');
                    codecMap.put(key, line.substring(idx + 1).trim());
                }
                // clear key so we don't keep looking
                key = "";
            }
        }
        List<SoundCard> soundCards = new ArrayList<>();
        for (Entry<String, String> entry : nameMap.entrySet()) {
            soundCards.add(new OpenBsdSoundCard(versionMap.get(entry.getKey()), entry.getValue(),
                    codecMap.get(entry.getKey())));
        }
        return soundCards;
    }
}
