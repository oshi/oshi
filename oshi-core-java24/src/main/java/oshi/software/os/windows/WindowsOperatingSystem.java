/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import oshi.software.os.common.AbstractOperatingSystem;

public class WindowsOperatingSystem extends AbstractOperatingSystem {

    private final oshi.software.os.OperatingSystem delegate;

    public WindowsOperatingSystem() {
        this.delegate = new oshi.software.os.windows.WindowsOperatingSystem();
    }

}
