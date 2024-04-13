/*
 * Copyright 2020-2024 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;
import oshi.util.platform.linux.SysPath;

/**
 * Utility to read info from the devicetree
 */
@ThreadSafe
public final class Devicetree {

    private Devicetree() {
    }

    /**
     * Query the model from the devicetree
     *
     * @return The model if available, null otherwise
     */
    public static String queryModel() {
        String modelStr = FileUtil.getStringFromFile(SysPath.MODEL);
        if (!modelStr.isEmpty()) {
            return modelStr.replace("Machine: ", "");
        }
        return null;
    }
}
