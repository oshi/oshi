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
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Graphics card info obtained by system_profiler SPDisplaysDataType.
 */
@Immutable
final class MacGraphicsCard extends AbstractGraphicsCard {

    /**
     * Constructor for MacGraphicsCard
     *
     * @param name
     *            The name
     * @param deviceId
     *            The device ID
     * @param vendor
     *            The vendor
     * @param versionInfo
     *            The version info
     * @param vram
     *            The VRAM
     */
    MacGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    /**
     * public method used by
     * {@link oshi.hardware.common.AbstractHardwareAbstractionLayer} to access the
     * graphics cards.
     *
     * @return List of {@link oshi.hardware.platform.mac.MacGraphicsCard} objects.
     */
    public static List<GraphicsCard> getGraphicsCards() {
        List<GraphicsCard> cardList = new ArrayList<>();
        List<String> sp = ExecutingCommand.runNative("system_profiler SPDisplaysDataType");
        String name = Constants.UNKNOWN;
        String deviceId = Constants.UNKNOWN;
        String vendor = Constants.UNKNOWN;
        List<String> versionInfoList = new ArrayList<>();
        long vram = 0;
        int cardNum = 0;
        for (String line : sp) {
            String[] split = line.trim().split(":", 2);
            if (split.length == 2) {
                String prefix = split[0].toLowerCase();
                if (prefix.equals("chipset model")) {
                    // Save previous card
                    if (cardNum++ > 0) {
                        cardList.add(new MacGraphicsCard(name, deviceId, vendor,
                                versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList),
                                vram));
                        versionInfoList.clear();
                    }
                    name = split[1].trim();
                } else if (prefix.equals("device id")) {
                    deviceId = split[1].trim();
                } else if (prefix.equals("vendor")) {
                    vendor = split[1].trim();
                } else if (prefix.contains("version") || prefix.contains("revision")) {
                    versionInfoList.add(line.trim());
                } else if (prefix.startsWith("vram")) {
                    vram = ParseUtil.parseDecimalMemorySizeToBinary(split[1].trim());
                }
            }
        }
        cardList.add(new MacGraphicsCard(name, deviceId, vendor,
                versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList), vram));
        return cardList;
    }
}
