/*
 * Copyright 2020-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.UserGroupInfo;
import oshi.util.tuples.Pair;

/**
 * Utility to read info from {@code dmidecode}
 */
@ThreadSafe
public final class Dmidecode {

    private Dmidecode() {
    }

    // $ sudo dmidecode -t bios
    // # dmidecode 2.11
    // SMBIOS 2.4 present.
    //
    // Handle 0x0000, DMI type 0, 24 bytes
    // BIOS Information
    // Vendor: Phoenix Technologies LTD
    // Version: 6.00
    // Release Date: 07/02/2015
    // Address: 0xEA5E0
    // Runtime Size: 88608 bytes
    // ROM Size: 64 kB
    // Characteristics:
    // ISA is supported
    // PCI is supported
    // PC Card (PCMCIA) is supported
    // PNP is supported
    // APM is supported
    // BIOS is upgradeable
    // BIOS shadowing is allowed
    // ESCD support is available
    // Boot from CD is supported
    // Selectable boot is supported
    // EDD is supported
    // Print screen service is supported (int 5h)
    // 8042 keyboard services are supported (int 9h)
    // Serial services are supported (int 14h)
    // Printer services are supported (int 17h)
    // CGA/mono video services are supported (int 10h)
    // ACPI is supported
    // Smart battery is supported
    // BIOS boot specification is supported
    // Function key-initiated network boot is supported
    // Targeted content distribution is supported
    // BIOS Revision: 4.6
    // Firmware Revision: 0.0

    /**
     * Query the serial number from dmidecode
     *
     * @return The serial number if available, null otherwise
     */
    public static String querySerialNumber() {
        // If root privileges this will work
        if (UserGroupInfo.isElevated()) {
            String marker = "Serial Number:";
            for (String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
                if (checkLine.contains(marker)) {
                    return checkLine.split(marker)[1].trim();
                }
            }
        }
        return null;
    }

    /**
     * Query the UUID from dmidecode
     *
     * @return The UUID if available, null otherwise
     */
    public static String queryUUID() {
        // If root privileges this will work
        if (UserGroupInfo.isElevated()) {
            String marker = "UUID:";
            for (String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
                if (checkLine.contains(marker)) {
                    return checkLine.split(marker)[1].trim();
                }
            }
        }
        return null;
    }

    /**
     * Query the name and revision from dmidecode
     *
     * @return The a pair containing the name and revision if available, null values in the pair otherwise
     */
    public static Pair<String, String> queryBiosNameRev() {
        String biosName = null;
        String revision = null;

        // Requires root, may not return anything
        if (UserGroupInfo.isElevated()) {
            final String biosMarker = "SMBIOS";
            final String revMarker = "Bios Revision:";

            for (final String checkLine : ExecutingCommand.runNative("dmidecode -t bios")) {
                if (checkLine.contains(biosMarker)) {
                    String[] biosArr = ParseUtil.whitespaces.split(checkLine);
                    if (biosArr.length >= 2) {
                        biosName = biosArr[0] + " " + biosArr[1];
                    }
                }
                if (checkLine.contains(revMarker)) {
                    revision = checkLine.split(revMarker)[1].trim();
                    // SMBIOS should be first line so if we're here we are done iterating
                    break;
                }
            }
        }
        return new Pair<>(biosName, revision);
    }
}
