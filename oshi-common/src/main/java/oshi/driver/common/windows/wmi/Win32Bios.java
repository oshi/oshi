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

    protected Win32Bios() {
    }
}
