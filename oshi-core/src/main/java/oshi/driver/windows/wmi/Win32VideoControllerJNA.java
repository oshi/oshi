/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32VideoController;
import oshi.driver.common.windows.wmi.Win32VideoController.VideoControllerProperty;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_VideoController} using JNA.
 */
@ThreadSafe
public final class Win32VideoControllerJNA extends Win32VideoController {

    private Win32VideoControllerJNA() {
    }

    /**
     * Queries video controller info.
     *
     * @return Information regarding video controllers
     */
    public static WmiResult<VideoControllerProperty> queryVideoController() {
        WmiQuery<VideoControllerProperty> videoControllerQuery = new WmiQuery<>(WIN32_VIDEO_CONTROLLER,
                VideoControllerProperty.class);
        return Objects
                .requireNonNull(WmiQueryHandler.createInstance(), "WmiQueryHandler.createInstance() returned null")
                .queryWMI(videoControllerQuery);
    }
}
