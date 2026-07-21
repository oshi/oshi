/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.spi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Validates SystemInfoFactory behavior when only oshi-common is on the classpath (no oshi-core or oshi-core-ffm).
 */
class SystemInfoFactoryTest {

    /**
     * Whether the native-free provider supports the current platform. Delegates to the production
     * {@code oshi.nativefree.SystemInfo#isAvailable()} so the test enablement cannot drift from the provider's own
     * gating. ({@code @EnabledIf} requires a static method and cannot reference the instance method directly.)
     *
     * @return {@code true} on Linux or NetBSD
     */
    static boolean isNativeFreePlatform() {
        return new oshi.nativefree.SystemInfo().isAvailable();
    }

    @Test
    @EnabledIf("isNativeFreePlatform")
    void factoryReturnsNativeFreeProviderOnNativeFreePlatform() {
        SystemInfoProvider provider = SystemInfoFactory.create();
        assertThat(provider, is(instanceOf(oshi.nativefree.SystemInfo.class)));
        assertThat(provider.getPriority(), is(0));
    }

    @Test
    @DisabledIf("isNativeFreePlatform")
    void factoryThrowsOnUnsupportedPlatform() {
        assertThrows(IllegalStateException.class, SystemInfoFactory::create);
    }
}
