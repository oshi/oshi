/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.spi;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.PublicApi;

/**
 * Factory that uses {@link ServiceLoader} to discover and select the best available {@link SystemInfoProvider}.
 * <p>
 * When both {@code oshi-core} (JNA) and {@code oshi-core-ffm} (FFM) are declared as dependencies, this factory
 * discovers their {@link SystemInfoProvider} implementations via {@link ServiceLoader}, filters by
 * {@linkplain SystemInfoProvider#isAvailable() availability}, and selects the one with the highest
 * {@linkplain SystemInfoProvider#getPriority() priority}.
 * <p>
 * Usage:
 *
 * <pre>{@code
 * SystemInfoProvider si = SystemInfoFactory.create();
 * HardwareAbstractionLayer hal = si.getHardware();
 * OperatingSystem os = si.getOperatingSystem();
 * }</pre>
 *
 * @see SystemInfoProvider
 */
@PublicApi
public final class SystemInfoFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SystemInfoFactory.class);

    private SystemInfoFactory() {
    }

    /**
     * Creates a new {@link SystemInfoProvider} instance using the best available provider.
     * <p>
     * Providers are discovered via {@link ServiceLoader} and filtered by {@link SystemInfoProvider#isAvailable()}.
     * Among available providers, the one with the highest {@link SystemInfoProvider#getPriority()} is selected.
     *
     * @return a new instance of the best available {@link SystemInfoProvider}
     * @throws IllegalStateException if no available provider is found. Ensure {@code oshi-core} or
     *                               {@code oshi-core-ffm} is declared as a dependency.
     */
    public static SystemInfoProvider create() {
        SystemInfoProvider best = null;
        Iterator<SystemInfoProvider> iterator = ServiceLoader.load(SystemInfoProvider.class).iterator();
        while (iterator.hasNext()) {
            try {
                SystemInfoProvider provider = iterator.next();
                if (provider.isAvailable()) {
                    if (best == null || provider.getPriority() > best.getPriority()) {
                        best = provider;
                    }
                }
            } catch (ServiceConfigurationError | RuntimeException e) {
                LOG.debug("Skipping unavailable provider: {}", e.getMessage());
            }
        }
        if (best == null) {
            throw new IllegalStateException(
                    "No SystemInfoProvider found. Add oshi-core or oshi-core-ffm as a dependency.");
        }
        return best;
    }
}
