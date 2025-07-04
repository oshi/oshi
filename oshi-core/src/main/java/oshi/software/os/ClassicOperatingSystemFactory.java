/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

public class ClassicOperatingSystemFactory implements OperatingSystemFactory {
    @Override
    public int getMinimumSupportedJavaVersion() {
        return 8;
    }
}
