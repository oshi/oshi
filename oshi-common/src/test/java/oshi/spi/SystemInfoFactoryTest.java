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
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Validates SystemInfoFactory behavior when only oshi-common is on the classpath (no oshi-core or oshi-core-ffm).
 */
class SystemInfoFactoryTest {

    @Test
    @EnabledOnOs(OS.LINUX)
    void factoryReturnsNativeFreeProviderOnLinux() {
        SystemInfoProvider provider = SystemInfoFactory.create();
        assertThat(provider, is(instanceOf(oshi.nativefree.SystemInfo.class)));
        assertThat(provider.getPriority(), is(0));
    }

    @Test
    @DisabledOnOs(OS.LINUX)
    void factoryThrowsOnNonLinux() {
        assertThrows(IllegalStateException.class, SystemInfoFactory::create);
    }
}
