/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32VideoController;
import oshi.driver.common.windows.wmi.Win32VideoController.VideoControllerProperty;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_VideoController} using FFM.
 */
@ThreadSafe
public final class Win32VideoControllerFFM extends Win32VideoController {

    private Win32VideoControllerFFM() {
    }

    /**
     * Queries video controller info.
     *
     * @return Information regarding video controllers
     */
    public static WmiResult<VideoControllerProperty> queryVideoController() {
        WmiQuery<VideoControllerProperty> videoControllerQuery = new WmiQuery<>(WIN32_VIDEO_CONTROLLER,
                VideoControllerProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(videoControllerQuery);
    }
}
