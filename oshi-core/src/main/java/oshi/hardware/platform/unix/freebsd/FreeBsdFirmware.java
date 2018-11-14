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
