/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import oshi.hardware.VirtualMemory;

class AbstractGlobalMemoryTest {

    @Test
    void testToString() {
        AbstractGlobalMemory mem = new AbstractGlobalMemory() {
            @Override
            public long getTotal() {
                return 8L * 1024 * 1024 * 1024;
            }

            @Override
            public long getAvailable() {
                return 4L * 1024 * 1024 * 1024;
            }

            @Override
            public long getPageSize() {
                return 4096L;
            }

            @Override
            public VirtualMemory getVirtualMemory() {
                return null;
            }
        };
        String s = mem.toString();
        assertThat(s, containsString("Available:"));
        assertThat(s, containsString("4 GiB"));
        assertThat(s, containsString("8 GiB"));
    }
}
