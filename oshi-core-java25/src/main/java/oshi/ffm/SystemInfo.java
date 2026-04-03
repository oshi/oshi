/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.PlatformEnum;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.platform.linux.LinuxHardwareAbstractionLayer;
import oshi.hardware.platform.mac.MacHardwareAbstractionLayerFFM;
import oshi.hardware.platform.windows.WindowsHardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.software.os.mac.MacOperatingSystemFFM;
import oshi.software.os.windows.WindowsOperatingSystemFFM;

/**
 * System information. This is the main FFM entry point to OSHI.
 * <p>
 * This object provides getters which instantiate the appropriate platform-specific FFM implementations of
 * {@link oshi.software.os.OperatingSystem} (software) and {@link oshi.hardware.HardwareAbstractionLayer} (hardware),
 * falling back to JNA implementations where FFM equivalents are not yet available.
 */
public class SystemInfo {

    private static final String NOT_SUPPORTED = "Unsupported platform: " + PlatformEnum.getCurrentPlatform().getName();

    private final Supplier<OperatingSystem> os = memoize(SystemInfo::createOperatingSystem);
    private final Supplier<HardwareAbstractionLayer> hardware = memoize(SystemInfo::createHardware);

    /**
     * Create a new instance of {@link SystemInfo}.
     */
    public SystemInfo() {
        // Intentionally empty, here to enable the constructor javadoc.
    }

    private static OperatingSystem createOperatingSystem() {
        switch (PlatformEnum.getCurrentPlatform()) {
            case MACOS:
                return new MacOperatingSystemFFM();
            case LINUX:
                return new LinuxOperatingSystem();
            case WINDOWS:
                return new WindowsOperatingSystemFFM();
            default:
                throw new UnsupportedOperationException(NOT_SUPPORTED);
        }
    }

    private static HardwareAbstractionLayer createHardware() {
        switch (PlatformEnum.getCurrentPlatform()) {
            case MACOS:
                return new MacHardwareAbstractionLayerFFM();
            case LINUX:
                return new LinuxHardwareAbstractionLayer();
            case WINDOWS:
                return new WindowsHardwareAbstractionLayer();
            default:
                throw new UnsupportedOperationException(NOT_SUPPORTED);
        }
    }

    /**
     * Creates a new instance of the appropriate platform-specific {@link OperatingSystem}.
     *
     * @return A new platform-specific instance implementing {@link OperatingSystem}.
     */
    public OperatingSystem getOperatingSystem() {
        return os.get();
    }

    /**
     * Creates a new instance of the appropriate platform-specific {@link HardwareAbstractionLayer}.
     *
     * @return A new platform-specific instance implementing {@link HardwareAbstractionLayer}.
     */
    public HardwareAbstractionLayer getHardware() {
        return hardware.get();
    }
}
