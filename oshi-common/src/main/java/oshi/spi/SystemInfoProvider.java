/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.spi;

import oshi.annotation.PublicApi;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

/**
 * Service Provider Interface for OSHI's {@code SystemInfo} entry points.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}. When multiple providers are available (e.g., both
 * {@code oshi-core} and {@code oshi-core-ffm} on the classpath), {@link SystemInfoFactory} selects the best available
 * provider based on {@link #getPriority()} and {@link #isAvailable()}.
 *
 * @see SystemInfoFactory
 */
@PublicApi
public interface SystemInfoProvider {

    /**
     * Creates a new instance of the appropriate platform-specific {@link OperatingSystem}.
     *
     * @return A new instance of {@link OperatingSystem}.
     */
    OperatingSystem getOperatingSystem();

    /**
     * Creates a new instance of the appropriate platform-specific {@link HardwareAbstractionLayer}.
     *
     * @return A new instance of {@link HardwareAbstractionLayer}.
     */
    HardwareAbstractionLayer getHardware();

    /**
     * Returns the priority of this provider. Higher values indicate higher priority. When multiple providers are
     * available, the one with the highest priority is selected.
     * <p>
     * Priority 0 is the native-free fallback ({@code oshi-common} alone, on Linux or NetBSD). The JNA-based provider
     * ({@code oshi-core}) returns 10. The FFM-based provider ({@code oshi-core-ffm}) returns 20 (preferred when
     * available).
     *
     * @return the priority of this provider
     */
    int getPriority();

    /**
     * Returns whether this provider is available in the current runtime environment.
     * <p>
     * A provider may be unavailable if the current platform is not supported or if required runtime features (such as
     * the FFM API on JDK 25+) are not present.
     *
     * @return {@code true} if this provider can be used in the current environment
     */
    boolean isAvailable();
}
