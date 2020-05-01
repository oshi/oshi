/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.hardware.platform.unix.freebsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Graphics Card info obtained from pciconf
 */
@Immutable
final class FreeBsdGraphicsCard extends AbstractGraphicsCard {

    private static final String PCI_CLASS_DISPLAY = "0x03";

    /**
     * Constructor for FreeBsdGraphicsCard
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
    FreeBsdGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    /**
     * public method used by
     * {@link oshi.hardware.common.AbstractHardwareAbstractionLayer} to access the
     * graphics cards.
     *
     * @return List of
     *         {@link oshi.hardware.platform.unix.freebsd.FreeBsdGraphicsCard}
     *         objects.
     */
    public static List<GraphicsCard> getGraphicsCards() {
        List<FreeBsdGraphicsCard> cardList = new ArrayList<>();
        // Enumerate all devices and add if required
        List<String> devices = ExecutingCommand.runNative("pciconf -lv");
        if (devices.isEmpty()) {
            return Collections.emptyList();
        }
        String name = Constants.UNKNOWN;
        String vendorId = Constants.UNKNOWN;
        String productId = Constants.UNKNOWN;
        String classCode = "";
        String versionInfo = Constants.UNKNOWN;
        for (String line : devices) {
            if (line.contains("class=0x")) {
                // Identifies start of a new device. Save previous if it's a graphics card
                if (PCI_CLASS_DISPLAY.equals(classCode)) {
                    cardList.add(new FreeBsdGraphicsCard(name.isEmpty() ? Constants.UNKNOWN : name,
                            productId.isEmpty() ? Constants.UNKNOWN : productId,
                            vendorId.isEmpty() ? Constants.UNKNOWN : vendorId,
                            versionInfo.isEmpty() ? Constants.UNKNOWN : versionInfo, 0L));
                }
                // Parse this line
                String[] split = ParseUtil.whitespaces.split(line);
                for (String s : split) {
                    String[] keyVal = s.split("=");
                    if (keyVal.length > 1) {
                        if (keyVal[0].equals("class") && keyVal[1].length() >= 4) {
                            // class=0x030000
                            classCode = keyVal[1].substring(0, 4);
                        } else if (keyVal[0].equals("chip") && keyVal[1].length() >= 10) {
                            // chip=0x3ea08086
                            productId = keyVal[1].substring(0, 6);
                            vendorId = "0x" + keyVal[1].substring(6, 10);
                        } else if (keyVal[0].contains("rev")) {
                            // rev=0x00
                            versionInfo = s;
                        }
                    }
                }
                // Reset name
                name = Constants.UNKNOWN;
            } else {
                String[] split = line.trim().split("=", 2);
                if (split.length == 2) {
                    String key = split[0].trim();
                    if (key.equals("vendor")) {
                        vendorId = ParseUtil.getSingleQuoteStringValue(line)
                                + (vendorId.equals(Constants.UNKNOWN) ? "" : " (" + vendorId + ")");
                    } else if (key.equals("device")) {
                        name = ParseUtil.getSingleQuoteStringValue(line);
                    }
                }
            }
        }
        // In case we reached end before saving
        if (PCI_CLASS_DISPLAY.equals(classCode)) {
            cardList.add(new FreeBsdGraphicsCard(name.isEmpty() ? Constants.UNKNOWN : name,
                    productId.isEmpty() ? Constants.UNKNOWN : productId,
                    vendorId.isEmpty() ? Constants.UNKNOWN : vendorId,
                    versionInfo.isEmpty() ? Constants.UNKNOWN : versionInfo, 0L));
        }
        return Collections.unmodifiableList(cardList);
    }
}
