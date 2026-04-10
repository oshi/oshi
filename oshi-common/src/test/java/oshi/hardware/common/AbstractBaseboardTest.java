/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

class AbstractBaseboardTest {

    private static final AbstractBaseboard BOARD = new AbstractBaseboard() {
        @Override
        public String getManufacturer() {
            return "ASUS";
        }

        @Override
        public String getModel() {
            return "ROG";
        }

        @Override
        public String getVersion() {
            return "1.0";
        }

        @Override
        public String getSerialNumber() {
            return "ABC123";
        }
    };

    @Test
    void testToString() {
        String s = BOARD.toString();
        assertThat(s, containsString("ASUS"));
        assertThat(s, containsString("ROG"));
        assertThat(s, containsString("1.0"));
        assertThat(s, containsString("ABC123"));
    }
}
