/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enums for WMI class {@code Win32_BIOS}.
 */
@ThreadSafe
public class Win32Bios {

    /**
     * The WMI class name with WHERE clause for primary BIOS.
     */
    public static final String WIN32_BIOS_WHERE_PRIMARY_BIOS_TRUE = "Win32_BIOS where PrimaryBIOS=true";

    /**
     * Serial number property.
     */
    public enum BiosSerialProperty {
        /** BIOS serial number. */
        SERIALNUMBER;
    }

    /**
     * BIOS description properties.
     */
    public enum BiosProperty {
        /** MANUFACTURER property. */
        MANUFACTURER,
        /** NAME property. */
        NAME,
        /** DESCRIPTION property. */
        DESCRIPTION,
        /** VERSION property. */
        VERSION,
        /** RELEASEDATE property. */
        RELEASEDATE;
    }

    /**
     * Constructor.
     */
    protected Win32Bios() {
    }

    /**
     * Queries the BIOS serial number.
     *
     * @param h An instantiated {@link WmiQueryExecutor}.
     * @return Assigned serial number of the software element.
     */
    public static WmiResult<BiosSerialProperty> querySerialNumber(WmiQueryExecutor h) {
        WmiQuery<BiosSerialProperty> serialNumQuery = new WmiQuery<>(WIN32_BIOS_WHERE_PRIMARY_BIOS_TRUE,
                BiosSerialProperty.class);
        return h.queryWMI(serialNumQuery);
    }

    /**
     * Queries the BIOS description.
     *
     * @param h An instantiated {@link WmiQueryExecutor}.
     * @return BIOS name, description, and related fields.
     */
    public static WmiResult<BiosProperty> queryBiosInfo(WmiQueryExecutor h) {
        WmiQuery<BiosProperty> biosQuery = new WmiQuery<>(WIN32_BIOS_WHERE_PRIMARY_BIOS_TRUE, BiosProperty.class);
        return h.queryWMI(biosQuery);
    }
}
