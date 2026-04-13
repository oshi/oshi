/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.platform.linux.LinuxHardwareAbstractionLayerFFM;
import oshi.hardware.platform.mac.MacHardwareAbstractionLayerFFM;
import oshi.hardware.platform.windows.WindowsHardwareAbstractionLayerFFM;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxOperatingSystemFFM;
import oshi.software.os.mac.MacOperatingSystemFFM;
import oshi.software.os.windows.WindowsOperatingSystemFFM;
import oshi.util.PlatformEnum;

/**
 * System information. This is the main FFM entry point to OSHI, using the Foreign Function &amp; Memory (FFM) API for
 * native access.
 * <p>
 * This object provides getters which instantiate the appropriate platform-specific FFM implementations of
 * {@link OperatingSystem} (software) and {@link HardwareAbstractionLayer} (hardware).
 * <p>
 * Quick start:
 *
 * <pre>{@code
 * SystemInfo si = new SystemInfo(); // oshi.ffm.SystemInfo
 * HardwareAbstractionLayer hal = si.getHardware();
 * OperatingSystem os = si.getOperatingSystem();
 *
 * // CPU usage (blocks for 1 second)
 * double cpuLoad = hal.getProcessor().getSystemCpuLoad(1000L);
 *
 * // Memory
 * GlobalMemory mem = hal.getMemory();
 * long availableBytes = mem.getAvailable();
 * }</pre>
 *
 * Platform-specific Hardware and Software objects are retrieved via memoized suppliers and cached on the SystemInfo
 * instance. To conserve memory at the cost of additional processing time, create a new SystemInfo for subsequent calls.
 * To conserve processing time at the cost of additional memory usage, re-use the same instance.
 * <p>
 * This implementation requires JDK 25+ and currently supports Windows, macOS, and Linux. It uses the FFM API in place
 * of JNA for native access, which may offer better performance. For broader platform support (including FreeBSD,
 * OpenBSD, Solaris, and AIX), use the JNA-based entry point ({@code oshi.SystemInfo}) in the {@code oshi-core} module.
 * <p>
 * All other imports ({@code oshi.hardware.*}, {@code oshi.software.os.*}) remain the same regardless of which entry
 * point is used. The API is identical; only the underlying native access mechanism differs.
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
                return new LinuxOperatingSystemFFM();
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
                return new LinuxHardwareAbstractionLayerFFM();
            case WINDOWS:
                return new WindowsHardwareAbstractionLayerFFM();
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
