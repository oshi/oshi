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
 * Utility to query WMI class {@code Win32_VideoController}
 */
@ThreadSafe
public final class Win32VideoController {

    private static final String WIN32_VIDEO_CONTROLLER = "Win32_VideoController";

    /**
     * Video Controller properties
     */
    public enum VideoControllerProperty {
        ADAPTERCOMPATIBILITY, ADAPTERRAM, DRIVERVERSION, NAME, PNPDEVICEID;
    }

    private Win32VideoController() {
    }

    /**
     * Queries video controller info for Vista and later.
     *
     * @return Information regarding video controllers
     */
    public static WmiResult<VideoControllerProperty> queryVideoController() {
        WmiQuery<VideoControllerProperty> videoControllerQuery = new WmiQuery<>(WIN32_VIDEO_CONTROLLER,
                VideoControllerProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(videoControllerQuery);
    }
}
