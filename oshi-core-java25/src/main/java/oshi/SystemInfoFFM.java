/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.platform.linux.LinuxHardwareAbstractionLayer;
import oshi.hardware.platform.mac.MacHardwareAbstractionLayerFFM;
import oshi.hardware.platform.windows.WindowsHardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.software.os.mac.MacOperatingSystemFFM;
import oshi.software.os.windows.WindowsOperatingSystemFFM;

import java.util.function.Supplier;

import static oshi.util.Memoizer.memoize;

public class SystemInfoFFM {

    private static final PlatformEnumFFM CURRENT_PLATFORM;

    static {
        String osName = System.getProperty("os.name");
        CURRENT_PLATFORM = switch (osName) {
        case String name when name.startsWith("Linux") -> PlatformEnumFFM.LINUX;
        case String name when name.startsWith("Mac") || name.startsWith("Darwin") -> PlatformEnumFFM.MACOS;
        case String name when name.startsWith("Windows") -> PlatformEnumFFM.WINDOWS;
        default -> PlatformEnumFFM.UNSUPPORTED;
        };
    }

    private static final String NOT_SUPPORTED = "Unsupported platform: " + CURRENT_PLATFORM;

    private final Supplier<OperatingSystem> os = memoize(SystemInfoFFM::createOperatingSystem);
    private final Supplier<HardwareAbstractionLayer> hardware = memoize(SystemInfoFFM::createHardware);

    /**
     * Constructs a new SystemInfo instance.
     */
    public SystemInfoFFM() {
        // Empty constructor for API consistency
    }

    private static OperatingSystem createOperatingSystem() {
        return switch (CURRENT_PLATFORM) {
        case LINUX -> new LinuxOperatingSystem();
        case MACOS -> new MacOperatingSystemFFM();
        case WINDOWS -> new WindowsOperatingSystemFFM();
        default -> throw new UnsupportedOperationException(NOT_SUPPORTED);
        };
    }

    private static HardwareAbstractionLayer createHardware() {
        return switch (CURRENT_PLATFORM) {
        case LINUX -> new LinuxHardwareAbstractionLayer();
        case MACOS -> new MacHardwareAbstractionLayerFFM();
        case WINDOWS -> new WindowsHardwareAbstractionLayer();
        default -> throw new UnsupportedOperationException(NOT_SUPPORTED);
        };
    }

    /**
     * Gets the {@link PlatformEnumFFM} value representing this system.
     *
     * @return Returns the current platform
     */
    public static PlatformEnumFFM getCurrentPlatform() {
        return CURRENT_PLATFORM;
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
