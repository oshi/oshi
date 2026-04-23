/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class WindowsCentralProcessorTest {

    @Test
    void testParseIdentifier() {
        String id = "Intel64 Family 6 Model 142 Stepping 12";
        assertThat(WindowsCentralProcessor.parseIdentifier(id, "Family"), is("6"));
        assertThat(WindowsCentralProcessor.parseIdentifier(id, "Model"), is("142"));
        assertThat(WindowsCentralProcessor.parseIdentifier(id, "Stepping"), is("12"));
    }

    @Test
    void testParseIdentifierNotFound() {
        assertThat(WindowsCentralProcessor.parseIdentifier("Intel64 Family 6", "Model"), is(""));
    }

    @Test
    void testParseIdentifierKeyAtEnd() {
        // Key is the last token — no value follows
        assertThat(WindowsCentralProcessor.parseIdentifier("Family 6 Model", "Model"), is(""));
    }
}
