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
package oshi.hardware.platform.linux;

import oshi.hardware.common.AbstractFirmware;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;

/**
 * Firmware data obtained by sysfs
 *
 * @author SchiTho1 [at] Securiton AG
 * @author widdis [at] gmail [dot] com
 */
final class LinuxFirmware extends AbstractFirmware {

    private static final long serialVersionUID = 1L;

    // Note: /sys/class/dmi/id symlinks here, but /sys/devices/* is the
    // official/approved path for sysfs information
    private static final String SYSFS_SERIAL_PATH = "/sys/devices/virtual/dmi/id/";

    LinuxFirmware() {
        init();
    }

    private void init() {

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

        // $ ls /sys/devices/virtual/dmi/id/
        // bios_date board_vendor chassis_version product_version
        // bios_vendor board_version modalias subsystem
        // bios_version chassis_asset_tag power sys_vendor
        // board_asset_tag chassis_serial product_name uevent
        // board_name chassis_type product_serial
        // board_serial chassis_vendor product_uuid

        final String biosVendor = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "bios_vendor");
        if (biosVendor != null && !biosVendor.trim().isEmpty()) {
            setManufacturer(biosVendor.trim());
        }

        final String modalias = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "modalias");
        if (modalias != null && !modalias.trim().isEmpty()) {
            setDescription(modalias.trim());
        }

        final String biosVersion = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "bios_version");
        if (biosVersion != null && !biosVersion.trim().isEmpty()) {
            final String marker = "Bios Revision:";
            String biosRevision = null;
            // Requires root, may not return anything
            for (final String checkLine : ExecutingCommand.runNative("dmidecode -t bios")) {
                if (checkLine.contains(marker)) {
                    biosRevision = checkLine.split(marker)[1].trim();
                    break;
                }
            }
            if (biosRevision != null && !biosRevision.trim().isEmpty()) {
                setVersion(biosVersion.trim() + " (revision " + biosRevision + ")");
            } else {
                setVersion(biosVersion.trim());
            }
        }

        final String biosDate = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "bios_date");
        if (biosDate != null && !biosDate.trim().isEmpty()) {
            try {
                // Date is MM-DD-YYYY, convert to YYYY-MM-DD
                setReleaseDate(String.format("%s-%s-%s", biosDate.substring(6, 10), biosDate.substring(0, 2),
                        biosDate.substring(3, 5)));
            } catch (StringIndexOutOfBoundsException e) {
                setReleaseDate(biosDate);
            }
        }

        // name --> not set
    }
}
