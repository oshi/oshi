/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import oshi.util.ParseUtil;

import java.util.ServiceLoader;

public class OperatingSystemFactoryProvider {
    private static final OperatingSystemFactory INSTANCE = loadInstance();

    private static OperatingSystemFactory loadInstance() {
        int currentVersion = parseJavaVersion(System.getProperty("java.specification.version"));
        OperatingSystemFactory selected = null;
        int highestSupportedMinVersion = Integer.MIN_VALUE;

        for (OperatingSystemFactory factory : ServiceLoader.load(OperatingSystemFactory.class)) {
            int factoryMinVersion = factory.getMinimumSupportedJavaVersion();
            if (factoryMinVersion <= currentVersion && factoryMinVersion > highestSupportedMinVersion) {
                selected = factory;
                highestSupportedMinVersion = factoryMinVersion;
            }
        }

        if (selected == null) {
            throw new IllegalStateException("No compatible OperatingSystemFactory found");
        }

        return selected;
    }
    private static int parseJavaVersion(String version) {
        if (version.startsWith("1.")) {
            return ParseUtil.parseIntOrDefault(version.substring(2), 0);
        }
        return ParseUtil.parseIntOrDefault(version, 0);
    }
    public static OperatingSystemFactory getInstance() {
        return INSTANCE;
    }
}
