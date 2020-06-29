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
package oshi.hardware.platform.unix.aix;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractBaseboard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.Util;

/**
 * Baseboard data obtained by lscfg
 */
@Immutable
final class AixBaseboard extends AbstractBaseboard {

    private final String manufacturer = "IBM";
    private final String model;
    private final String serialNumber;
    private final String version;

    AixBaseboard() {
        String bbModel = null;
        String bbSerialNumber = null;
        String bbVersion = null;

        final String planeMarker = "BACKPLANE";
        final String modelMarker = "Part Number";
        final String serialMarker = "Serial Number";
        final String versionMarker = "Version";
        final String locationMarker = "Physical Location";

        // 1 WAY BACKPLANE :
        // Serial Number...............YL10243490FB
        // Part Number.................80P4315
        // Customer Card ID Number.....26F4
        // CCIN Extender...............1
        // FRU Number.................. 80P4315
        // Version.....................RS6K
        // Hardware Location Code......U0.1-P1
        // Physical Location: U0.1-P1

        boolean planeFlag = false;
        for (final String checkLine : ExecutingCommand.runNative("lscfg -vp")) {
            if (!planeFlag && checkLine.contains(planeMarker)) {
                planeFlag = true;
            } else if (planeFlag) {
                if (checkLine.contains(modelMarker)) {
                    bbModel = removeLeadingDots(checkLine.split(modelMarker)[1].trim());
                } else if (checkLine.contains(serialMarker)) {
                    bbSerialNumber = removeLeadingDots(checkLine.split(serialMarker)[1].trim());
                } else if (checkLine.contains(versionMarker)) {
                    bbSerialNumber = removeLeadingDots(checkLine.split(versionMarker)[1].trim());
                } else if (checkLine.contains(locationMarker)) {
                    break;
                }
            }
        }

        this.model = Util.isBlank(bbModel) ? Constants.UNKNOWN : bbModel;
        this.serialNumber = Util.isBlank(bbSerialNumber) ? Constants.UNKNOWN : bbSerialNumber;
        this.version = Util.isBlank(versionMarker) ? Constants.UNKNOWN : bbVersion;
    }

    private String removeLeadingDots(String dotPrefixedStr) {
        int pos = 0;
        while (pos < dotPrefixedStr.length() && dotPrefixedStr.charAt(pos) == '.') {
            pos++;
        }
        return pos < dotPrefixedStr.length() ? dotPrefixedStr.substring(pos) : null;
    }

    @Override
    public String getManufacturer() {
        return this.manufacturer;
    }

    @Override
    public String getModel() {
        return this.model;
    }

    @Override
    public String getSerialNumber() {
        return this.serialNumber;
    }

    @Override
    public String getVersion() {
        return this.version;
    }
}
