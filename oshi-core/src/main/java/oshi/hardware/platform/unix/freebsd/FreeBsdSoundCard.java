/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.platform.unix.freebsd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Gets soundcard.
 *
 * @author : BilalAM
 */
public class FreeBsdSoundCard extends AbstractSoundCard {

    private static final String LSHAL = "lshal";

    private static Map<String, String> vendorMap = new HashMap<>();
    private static Map<String, String> productMap = new HashMap<>();

    public FreeBsdSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    public static List<SoundCard> getSoundCards() {
        vendorMap.clear();
        productMap.clear();
        List<String> sounds = new ArrayList<>();
        String key = "";
        for (String line : ExecutingCommand.runNative(LSHAL)) {
            if (line.startsWith("udi =")) {
                // we have the key.
                key = ParseUtil.getSingleQuoteStringValue(line);
                continue;

            } else if (key.isEmpty()) {
                continue;
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            } else if (line.contains("freebsd.driver =") && "pcm".equals(ParseUtil.getSingleQuoteStringValue(line))) {
                sounds.add(key);
            } else if (line.contains("info.product")) {
                productMap.put(key, ParseUtil.getStringBetween(line, '\''));
                continue;
            } else if (line.contains("info.vendor")) {
                vendorMap.put(key, ParseUtil.getStringBetween(line, '\''));
                continue;
            }
        }
        List<SoundCard> soundCards = new ArrayList<>();
        for (String _key : sounds) {
            soundCards.add(new FreeBsdSoundCard(productMap.get(_key), vendorMap.get(_key) + " " + productMap.get(_key),
                    productMap.get(_key)));
        }
        return soundCards;
    }
}
