/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors:
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
package oshi.hardware.platform.unix.freebsd;

import oshi.hardware.common.AbstractFirmware;
import oshi.util.ExecutingCommand;

final class FreeBsdFirmware extends AbstractFirmware {

    private static final long serialVersionUID = 1L;

    FreeBsdFirmware() {
        init();
    }

    private void init() {

        // $ sudo dmidecode -t bios
        // # dmidecode 3.0
        // Scanning /dev/mem for entry point.
        // SMBIOS 2.7 present.
        //
        // Handle 0x0000, DMI type 0, 24 bytes
        // BIOS Information
        // Vendor: Parallels Software International Inc.
        // Version: 11.2.1 (32626)
        // Release Date: 07/15/2016
        // ... <snip> ...
        // BIOS Revision: 11.2
        // Firmware Revision: 11.2

        String manufacturer = "";
        final String manufacturerMarker = "Vendor:";
        String version = "";
        final String versionMarker = "Version:";
        String releaseDate = "";
        final String releaseDateMarker = "Release Date:";

        // Only works with root permissions but it's all we've got
        for (final String checkLine : ExecutingCommand.runNative("dmidecode -t bios")) {
            if (checkLine.contains(manufacturerMarker)) {
                manufacturer = checkLine.split(manufacturerMarker)[1].trim();
            } else if (checkLine.contains(versionMarker)) {
                version = checkLine.split(versionMarker)[1].trim();
            } else if (checkLine.contains(releaseDateMarker)) {
                releaseDate = checkLine.split(releaseDateMarker)[1].trim();
            }
        }
        if (!manufacturer.isEmpty()) {
            setManufacturer(manufacturer);
        }
        if (!version.isEmpty()) {
            setVersion(version);
        }
        if (!releaseDate.isEmpty()) {
            try {
                // Date is MM-DD-YYYY, convert to YYYY-MM-DD
                setReleaseDate(String.format("%s-%s-%s", releaseDate.substring(6, 10), releaseDate.substring(0, 2),
                        releaseDate.substring(3, 5)));
            } catch (StringIndexOutOfBoundsException e) {
                setReleaseDate(releaseDate);
            }
        }
    }
}
