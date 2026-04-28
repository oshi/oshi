/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class MacOperatingSystemTest {

    @Test
    void testResolveVersionUsesSwVersWhenNonEmpty() {
        assertThat(MacOperatingSystem.resolveVersion("10.16", "11.2.3"), is("11.2.3"));
    }

    @Test
    void testResolveVersionFallsBackWhenSwVersEmpty() {
        assertThat(MacOperatingSystem.resolveVersion("10.16", ""), is("10.16"));
    }
}
