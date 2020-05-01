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
package oshi.hardware.platform.linux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Graphics card info obtained by lshw
 */
@Immutable
final class LinuxGraphicsCard extends AbstractGraphicsCard {

    /**
     * Constructor for LinuxGraphicsCard
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
    LinuxGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    /**
     * public method used by
     * {@link oshi.hardware.common.AbstractHardwareAbstractionLayer} to access the
     * graphics cards.
     *
     * @return List of {@link oshi.hardware.platform.linux.LinuxGraphicsCard}
     *         objects.
     */
    public static List<GraphicsCard> getGraphicsCards() {
        List<LinuxGraphicsCard> cardList = getGraphicsCardsFromLspci();
        if (cardList.isEmpty()) {
            cardList = getGraphicsCardsFromLshw();
        }

        return Collections.unmodifiableList(cardList);
    }

    // Faster, use as primary
    private static List<LinuxGraphicsCard> getGraphicsCardsFromLspci() {
        List<LinuxGraphicsCard> cardList = new ArrayList<>();
        // Machine readable version
        List<String> lspci = ExecutingCommand.runNative("lspci -vnnm");
        String name = Constants.UNKNOWN;
        String deviceId = Constants.UNKNOWN;
        String vendor = Constants.UNKNOWN;
        List<String> versionInfoList = new ArrayList<>();
        boolean found = false;
        String lookupDevice = null;
        for (String line : lspci) {
            String[] split = line.trim().split(":", 2);
            String prefix = split[0];
            // Skip until line contains "VGA"
            if (prefix.equals("Class") && line.contains("VGA")) {
                found = true;
            } else if (prefix.equals("Device") && !found && split.length > 1) {
                lookupDevice = split[1].trim();
            }
            if (found) {
                if (split.length < 2) {
                    // Save previous card
                    cardList.add(new LinuxGraphicsCard(name, deviceId, vendor,
                            versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList),
                            queryLspciMemorySize(lookupDevice)));
                    versionInfoList.clear();
                    found = false;
                } else {
                    if (prefix.equals("Device")) {
                        Pair<String, String> pair = ParseUtil.parseLspciMachineReadable(split[1].trim());
                        if (pair != null) {
                            name = pair.getA();
                            deviceId = "0x" + pair.getB();
                        }
                    } else if (prefix.equals("Vendor")) {
                        Pair<String, String> pair = ParseUtil.parseLspciMachineReadable(split[1].trim());
                        if (pair != null) {
                            vendor = pair.getA() + " (0x" + pair.getB() + ")";
                        } else {
                            vendor = split[1].trim();
                        }
                    } else if (prefix.equals("Rev:")) {
                        versionInfoList.add(line.trim());
                    }
                }
            }
        }
        // If we haven't yet written the last card do so now
        if (found) {
            cardList.add(new LinuxGraphicsCard(name, deviceId, vendor,
                    versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList),
                    queryLspciMemorySize(lookupDevice)));
        }
        return cardList;
    }

    private static long queryLspciMemorySize(String lookupDevice) {
        long vram = 0L;
        // Lookup memory
        // Human readable version, includes memory
        List<String> lspciMem = ExecutingCommand.runNative("lspci -v -s " + lookupDevice);
        for (String mem : lspciMem) {
            if (mem.contains(" prefetchable")) {
                vram += ParseUtil.parseLspciMemorySize(mem);
            }
        }
        return vram;
    }

    // Slower, use as backup
    private static List<LinuxGraphicsCard> getGraphicsCardsFromLshw() {
        List<LinuxGraphicsCard> cardList = new ArrayList<>();
        List<String> lshw = ExecutingCommand.runNative("lshw -C display");
        String name = Constants.UNKNOWN;
        String deviceId = Constants.UNKNOWN;
        String vendor = Constants.UNKNOWN;
        List<String> versionInfoList = new ArrayList<>();
        long vram = 0;
        int cardNum = 0;
        for (String line : lshw) {
            String[] split = line.trim().split(":");
            if (split[0].startsWith("*-display")) {
                // Save previous card
                if (cardNum++ > 0) {
                    cardList.add(new LinuxGraphicsCard(name, deviceId, vendor,
                            versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList), vram));
                    versionInfoList.clear();
                }
            } else if (split.length == 2) {
                String prefix = split[0];
                if (prefix.equals("product")) {
                    name = split[1].trim();
                } else if (prefix.equals("vendor")) {
                    vendor = split[1].trim();
                } else if (prefix.equals("version")) {
                    versionInfoList.add(line.trim());
                } else if (prefix.startsWith("resources")) {
                    vram = ParseUtil.parseLshwResourceString(split[1].trim());
                }
            }
        }
        cardList.add(new LinuxGraphicsCard(name, deviceId, vendor,
                versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList), vram));
        return cardList;
    }
}
