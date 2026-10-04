/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

/**
 * Tests the parsing behind {@link Sysfs} queries against fixed input, so it runs on every platform.
 */
class SysfsParseTest {

    @Test
    void testParseBiosVendorReturnsNullWhenBlank() {
        assertThat(Sysfs.parseBiosVendor(""), is(nullValue()));
        assertThat(Sysfs.parseBiosVendor(" \n"), is(nullValue()));
    }

    @Test
    void testParseBiosVendorTrimsValue() {
        assertThat(Sysfs.parseBiosVendor(" American Megatrends Inc.\n"), is("American Megatrends Inc."));
    }
}
