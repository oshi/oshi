/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

import org.junit.jupiter.api.Test;

import oshi.hardware.DisplayInfo;
import oshi.hardware.DisplayInfoImpl;

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

    @Test
    void testEdidConstructorNotSynthetic() {
        AbstractDisplay display = new AbstractDisplay(new byte[128]) {
        };
        assertThat(display.getDisplayInfo(), is(notNullValue()));
        assertThat(display.isEdidSynthetic(), is(false));
    }

    @Test
    void testDisplayInfoConstructor() {
        DisplayInfo info = new DisplayInfoImpl(new byte[128]);
        AbstractDisplay display = new AbstractDisplay(info) {
        };
        assertThat(display.getDisplayInfo(), is(sameInstance(info)));
        assertThat(display.isEdidSynthetic(), is(false));
        assertThat(display.getEdid(), is(new byte[128]));
    }

    @Test
    void testSyntheticDisplayInfoConstructor() {
        DisplayInfo info = new DisplayInfoImpl("AUO", "9227", "162C0C25", (byte) 44, 2012, "1.4", true, 60, 34,
                "2560x1440", "Thunderbolt", "C02JM2PFF2GC");
        AbstractDisplay display = new AbstractDisplay(info) {
        };
        assertThat(display.isEdidSynthetic(), is(true));
        assertThat(display.getDisplayInfo().getModel(), is("Thunderbolt"));
    }
}
