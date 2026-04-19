/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_VideoController}.
 */
@ThreadSafe
public class Win32VideoController {

    /**
     * The WMI class name.
     */
    public static final String WIN32_VIDEO_CONTROLLER = "Win32_VideoController";

    /**
     * Video Controller properties.
     */
    public enum VideoControllerProperty {
        ADAPTERCOMPATIBILITY, ADAPTERRAM, CONFIGMANAGERERRORCODE, DRIVERVERSION, NAME, PNPDEVICEID;
    }

    protected Win32VideoController() {
    }
}
