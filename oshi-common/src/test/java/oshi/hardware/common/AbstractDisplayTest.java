/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

class AbstractDisplayTest {

    @Test
    void testEdidDefensiveCopy() {
        byte[] edid = new byte[] { 0x00, 0x01, 0x02 };
        AbstractDisplay display = new AbstractDisplay(edid) {
        };
        edid[0] = (byte) 0xFF;
        assertThat(display.getEdid()[0], is((byte) 0x00));
    }

    @Test
    void testGetEdidReturnsCopy() {
        AbstractDisplay display = new AbstractDisplay(new byte[] { 0x00, 0x01 }) {
        };
        display.getEdid()[0] = (byte) 0xFF;
        assertThat(display.getEdid()[0], is((byte) 0x00));
    }

    @Test
    void testToString() {
        AbstractDisplay display = new AbstractDisplay(new byte[128]) {
        };
        assertThat(display.toString(), is(notNullValue()));
    }
}
