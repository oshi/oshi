/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import oshi.software.os.common.AbstractOperatingSystem;

public class MacOperatingSystem extends AbstractOperatingSystem {

    private final oshi.software.os.OperatingSystem delegate;

    public MacOperatingSystem() {
        this.delegate = new oshi.software.os.mac.MacOperatingSystem();
    }

}
