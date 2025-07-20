/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import oshi.software.os.common.AbstractOperatingSystem;

public class LinuxOperatingSystem extends AbstractOperatingSystem {

    private final oshi.software.os.OperatingSystem delegate;

    public LinuxOperatingSystem() {
        this.delegate = new oshi.software.os.linux.LinuxOperatingSystem();
    }

}
