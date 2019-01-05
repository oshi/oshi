/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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

import oshi.hardware.common.AbstractSoundCard;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * Sound card data obtained via system_profiler
 *
 * @author dbwiddis
 */

public class MacSoundCard extends AbstractSoundCard {

    private static final String APPLE = "Apple Inc.";

    public MacSoundCard(String kernelVersion, String name, String codec) {
        super(kernelVersion, name, codec);
    }

    /**
     * public method used by
     * {@link oshi.hardware.common.AbstractHardwareAbstractionLayer} to access
     * the sound cards.
     *
     * @return List of {@link MacSoundCard} objects.
     */
    public static List<MacSoundCard> getSoundCards() {
        List<MacSoundCard> soundCards = new ArrayList<>();

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
