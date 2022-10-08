/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_BIOS}
 */
@ThreadSafe
public final class Win32Bios {

    private static final String WIN32_BIOS_WHERE_PRIMARY_BIOS_TRUE = "Win32_BIOS where PrimaryBIOS=true";

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
        MANUFACTURER, NAME, DESCRIPTION, VERSION, RELEASEDATE;
    }

    private Win32Bios() {
    }

    /**
     * Queries the BIOS serial number.
     *
     * @return Assigned serial number of the software element.
     */
    public static WmiResult<BiosSerialProperty> querySerialNumber() {
        WmiQuery<BiosSerialProperty> serialNumQuery = new WmiQuery<>(WIN32_BIOS_WHERE_PRIMARY_BIOS_TRUE,
                BiosSerialProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(serialNumQuery);
    }

    /**
     * Queries the BIOS description.
     *
     * @return BIOS name, description, and related fields.
     */
    public static WmiResult<BiosProperty> queryBiosInfo() {
        WmiQuery<BiosProperty> biosQuery = new WmiQuery<>(WIN32_BIOS_WHERE_PRIMARY_BIOS_TRUE, BiosProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(biosQuery);
    }
}
