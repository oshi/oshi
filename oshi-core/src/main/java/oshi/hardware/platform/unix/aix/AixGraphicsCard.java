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
package oshi.hardware.platform.unix.aix;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;
import oshi.util.ParseUtil;
import oshi.util.Util;

/**
 * Graphics Card info obtained from lscfg
 */
@Immutable
final class AixGraphicsCard extends AbstractGraphicsCard {

    /**
     * Constructor for AixGraphicsCard
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
    AixGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    /**
     * Gets graphics cards
     *
     * @param lscfg
     *            A memoized lscfg list
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
            } else if (display && s.toLowerCase().contains("graphics")) {
                name = s;
            } else if (display && name != null) {
                if (s.startsWith("Manufacture ID")) {
                    vendor = ParseUtil.removeLeadingDots(s.substring(14));
                } else if (s.contains("Level")) {
                    versionInfo.add(s.replaceAll("\\.\\.+", "="));
                } else if (s.startsWith("Hardware Location Code")) {
                    cardList.add(new AixGraphicsCard(name, Constants.UNKNOWN,
                            Util.isBlank(vendor) ? Constants.UNKNOWN : vendor,
                            versionInfo.isEmpty() ? Constants.UNKNOWN : String.join(",", versionInfo), 0L));
                    display = false;
                }
            }
        }
        return cardList;
    }
}
