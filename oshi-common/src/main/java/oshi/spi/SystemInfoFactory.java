/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.spi;

import java.util.ServiceLoader;

import oshi.annotation.PublicApi;

/**
 * Factory that uses {@link ServiceLoader} to discover and select the best available {@link SystemInfoProvider}.
 * <p>
 * When both {@code oshi-core} (JNA) and {@code oshi-core-ffm} (FFM) are on the classpath/module path, this factory
 * selects the provider with the highest {@linkplain SystemInfoProvider#getPriority() priority} that is
 * {@linkplain SystemInfoProvider#isAvailable() available} in the current runtime environment.
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

    private SystemInfoFactory() {
    }

    /**
     * Creates a new {@link SystemInfoProvider} instance using the best available provider.
     * <p>
     * Providers are discovered via {@link ServiceLoader} and filtered by {@link SystemInfoProvider#isAvailable()}.
     * Among available providers, the one with the highest {@link SystemInfoProvider#getPriority()} is selected.
     *
     * @return a new instance of the best available {@link SystemInfoProvider}
     * @throws IllegalStateException if no available provider is found on the classpath/module path
     */
    public static SystemInfoProvider create() {
        SystemInfoProvider best = null;
        for (SystemInfoProvider provider : ServiceLoader.load(SystemInfoProvider.class)) {
            if (provider.isAvailable()) {
                if (best == null || provider.getPriority() > best.getPriority()) {
                    best = provider;
                }
            }
        }
        if (best == null) {
            throw new IllegalStateException(
                    "No SystemInfoProvider found. Add oshi-core or oshi-core-ffm to your classpath.");
        }
        return best;
    }
}
