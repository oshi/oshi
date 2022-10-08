/*
 * Copyright 2018-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;

/**
 * Test SoundCard
 */
class SoundCardTest {

    /**
     * Testing sound cards , each attribute.
     */
    @Test
    void testSoundCards() {
        SystemInfo info = new SystemInfo();
        for (SoundCard soundCard : info.getHardware().getSoundCards()) {
            assertThat("Sound card's codec should not be null", soundCard.getCodec(), is(notNullValue()));
            assertThat("Sound card's driver should not be null", soundCard.getDriverVersion(), is(notNullValue()));
            assertThat("Sound card's name should not be null", soundCard.getName(), is(notNullValue()));
        }
    }

}
