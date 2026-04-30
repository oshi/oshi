/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Bios;
import oshi.driver.common.windows.wmi.Win32Bios.BiosProperty;
import oshi.driver.common.windows.wmi.Win32Bios.BiosSerialProperty;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_BIOS} using FFM.
 */
@ThreadSafe
public final class Win32BiosFFM extends Win32Bios {

    private Win32BiosFFM() {
    }

    /**
     * Queries the BIOS serial number.
     *
     * @return Assigned serial number of the software element.
     */
    public static WmiResult<BiosSerialProperty> querySerialNumber() {
        WmiQuery<BiosSerialProperty> serialNumQuery = new WmiQuery<>(WIN32_BIOS_WHERE_PRIMARY_BIOS_TRUE,
                BiosSerialProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(serialNumQuery);
    }

    /**
     * Queries the BIOS description.
     *
     * @return BIOS name, description, and related fields.
     */
    public static WmiResult<BiosProperty> queryBiosInfo() {
        WmiQuery<BiosProperty> biosQuery = new WmiQuery<>(WIN32_BIOS_WHERE_PRIMARY_BIOS_TRUE, BiosProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(biosQuery);
    }
}
