package oshi.hardware.platform.linux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.hardware.common.AbstractFirmware;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: SchiTho1
 * Date: 14.10.2016
 * Time: 07:59
 * <p>
 * Copyright 2008-2013 - Securiton AG all rights reserved
 */
final class LinuxFirmware extends AbstractFirmware {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxFirmware.class);

    // Note: /sys/class/dmi/id symlinks here, but /sys/devices/* is the
    // official/approved path for sysfs information
    private static final String SYSFS_SERIAL_PATH = "/sys/devices/virtual/dmi/id/";

    LinuxFirmware() {

        init();
    }

    private void init() {

//        $ sudo dmidecode | grep -C 3 -i date
//        BIOS Information
//                Vendor: Phoenix Technologies LTD
//                Version: 6.00
//                Release Date: 07/02/2015
//                Address: 0xEA5E0
//                Runtime Size: 88608 bytes
//                ROM Size: 64 kB


//        $ sudo dmidecode -t bios
//        # dmidecode 2.11
//        SMBIOS 2.4 present.
//
//        Handle 0x0000, DMI type 0, 24 bytes
//        BIOS Information
//            Vendor: Phoenix Technologies LTD
//            Version: 6.00
//            Release Date: 07/02/2015
//            Address: 0xEA5E0
//            Runtime Size: 88608 bytes
//            ROM Size: 64 kB
//            Characteristics:
//                ISA is supported
//                PCI is supported
//                PC Card (PCMCIA) is supported
//                PNP is supported
//                APM is supported
//                BIOS is upgradeable
//                BIOS shadowing is allowed
//                ESCD support is available
//                Boot from CD is supported
//                Selectable boot is supported
//                EDD is supported
//                Print screen service is supported (int 5h)
//                8042 keyboard services are supported (int 9h)
//                Serial services are supported (int 14h)
//                Printer services are supported (int 17h)
//                CGA/mono video services are supported (int 10h)
//                ACPI is supported
//                Smart battery is supported
//                BIOS boot specification is supported
//                Function key-initiated network boot is supported
//                Targeted content distribution is supported
//            BIOS Revision: 4.6
//            Firmware Revision: 0.0


//        $ ls /sys/devices/virtual/dmi/id/
//        bios_date        board_vendor       chassis_version  product_version
//        bios_vendor      board_version      modalias         subsystem
//        bios_version     chassis_asset_tag  power            sys_vendor
//        board_asset_tag  chassis_serial     product_name     uevent
//        board_name       chassis_type       product_serial
//        board_serial     chassis_vendor     product_uuid

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

            final String biosRevision = parseCommandOutput("dmidecode -t bios", "BIOS Revision:");

            if (biosRevision != null && ! biosRevision.isEmpty()) {
                setVersion(biosVersion.trim() + " (revision " + biosRevision+")");
            } else {
                setVersion(biosVersion.trim());
            }
        }

        final String biosDate = FileUtil.getStringFromFile(SYSFS_SERIAL_PATH + "bios_date");
        if (biosDate != null && !biosDate.trim().isEmpty()) {

            // TODO: Is that really not language-dependent?
            final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
            try {
                final Date result = dateFormat.parse(biosDate.trim());
                if (result != null) {
                    setReleaseDate(result);
                }
            } catch (final ParseException e) {
                LOG.warn("could not parse date string: " + biosDate, e);
            }
        }

        // name --> not set
    }

    private String parseCommandOutput(final String nativeCall, final String marker) {
        for (final String checkLine : ExecutingCommand.runNative(nativeCall)) {
            if (checkLine.contains(marker)) {
                return checkLine.split(marker)[1].trim();
            }
        }

        return null;
    }
}
