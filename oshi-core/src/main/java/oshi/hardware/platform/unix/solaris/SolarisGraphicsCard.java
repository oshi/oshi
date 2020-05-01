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
package oshi.hardware.platform.unix.solaris;

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
 * Graphics Card info obtained from prtconf
 */
@Immutable
final class SolarisGraphicsCard extends AbstractGraphicsCard {

    private static final String PCI_CLASS_DISPLAY = "0003";

    /**
     * Constructor for SolarisGraphicsCard
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
    SolarisGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    /**
     * public method used by
     * {@link oshi.hardware.common.AbstractHardwareAbstractionLayer} to access the
     * graphics cards.
     *
     * @return List of
     *         {@link oshi.hardware.platform.unix.solaris.SolarisGraphicsCard}
     *         objects.
     */
    public static List<GraphicsCard> getGraphicsCards() {
        List<GraphicsCard> cardList = new ArrayList<>();
        // Enumerate all devices and add if required
        List<String> devices = ExecutingCommand.runNative("prtconf -pv");
        if (devices.isEmpty()) {
            return cardList;
        }
        String name = "";
        String vendorId = "";
        String productId = "";
        String classCode = "";
        List<String> versionInfoList = new ArrayList<>();
        for (String line : devices) {
            // Node 0x... identifies start of a new device. Save previous if it's a graphics
            // card
            if (line.contains("Node 0x")) {
                if (PCI_CLASS_DISPLAY.equals(classCode)) {
                    cardList.add(new SolarisGraphicsCard(name.isEmpty() ? Constants.UNKNOWN : name,
                            productId.isEmpty() ? Constants.UNKNOWN : productId,
                            vendorId.isEmpty() ? Constants.UNKNOWN : vendorId,
                            versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList), 0L));
                }
                // Reset strings
                name = "";
                vendorId = Constants.UNKNOWN;
                productId = Constants.UNKNOWN;
                classCode = "";
                versionInfoList.clear();
            } else {
                String[] split = line.trim().split(":", 2);
                if (split.length == 2) {
                    if (split[0].equals("model")) {
                        // This is preferred, always set it
                        name = ParseUtil.getSingleQuoteStringValue(line);
                    } else if (split[0].equals("name")) {
                        // Name is backup for model if model doesn't exist, so only
                        // put if name blank
                        if (name.isEmpty()) {
                            name = ParseUtil.getSingleQuoteStringValue(line);
                        }
                    } else if (split[0].equals("vendor-id")) {
                        // Format: vendor-id: 00008086
                        vendorId = "0x" + line.substring(line.length() - 4);
                    } else if (split[0].equals("device-id")) {
                        // Format: device-id: 00002440
                        productId = "0x" + line.substring(line.length() - 4);
                    } else if (split[0].equals("revision-id")) {
                        // Format: revision-id: 00000002
                        versionInfoList.add(line.trim());
                    } else if (split[0].equals("class-code")) {
                        // Format: 00030000
                        // Display class is 0003xx, first 6 bytes of this code
                        classCode = line.substring(line.length() - 8, line.length() - 4);
                    }
                }
            }
        }
        // In case we reached end before saving
        if (PCI_CLASS_DISPLAY.equals(classCode)) {
            cardList.add(new SolarisGraphicsCard(name.isEmpty() ? Constants.UNKNOWN : name,
                    productId.isEmpty() ? Constants.UNKNOWN : productId,
                    vendorId.isEmpty() ? Constants.UNKNOWN : vendorId,
                    versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList), 0L));
        }
        return Collections.unmodifiableList(cardList);
    }
}
