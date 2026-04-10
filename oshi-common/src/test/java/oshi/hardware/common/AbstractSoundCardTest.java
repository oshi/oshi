/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class AbstractSoundCardTest {

    private static final AbstractSoundCard CARD = new AbstractSoundCard("5.4.0", "HDA Intel", "Realtek ALC892") {
    };

    @Test
    void testGetters() {
        assertThat(CARD.getDriverVersion(), is("5.4.0"));
        assertThat(CARD.getName(), is("HDA Intel"));
        assertThat(CARD.getCodec(), is("Realtek ALC892"));
    }

    @Test
    void testToString() {
        assertThat(CARD.toString(), containsString("HDA Intel"));
        assertThat(CARD.toString(), containsString("5.4.0"));
        assertThat(CARD.toString(), containsString("Realtek ALC892"));
    }
}
