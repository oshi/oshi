/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import oshi.spi.SystemInfoFactory;
import oshi.spi.SystemInfoProvider;

class SystemInfoFactoryTest {

    @Test
    void testFactoryFindsProvider() {
        SystemInfoProvider provider = SystemInfoFactory.create();
        assertThat(provider, is(notNullValue()));
        assertThat(provider, instanceOf(SystemInfo.class));
    }

    @Test
    void testProviderIsAvailable() {
        SystemInfo si = new SystemInfo();
        assertThat(si.isAvailable(), is(true));
        assertThat(si.getPriority(), is(0));
    }

    @Test
    void testProviderReturnsHardwareAndOs() {
        SystemInfoProvider provider = SystemInfoFactory.create();
        assertThat(provider.getHardware(), is(notNullValue()));
        assertThat(provider.getOperatingSystem(), is(notNullValue()));
    }
}
