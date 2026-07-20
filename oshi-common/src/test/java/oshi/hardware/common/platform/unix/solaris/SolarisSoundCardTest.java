/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.SoundCard;

class SolarisSoundCardTest {

    @Test
    void testParseLshal() {
        // Representative lshal output with a sound device
        List<String> lshal = Arrays.asList(//
                "udi = '/org/freedesktop/Hal/devices/pci_8086_293e'", //
                "  info.solaris.driver = 'audio810'  (string)", //
                "  info.product = 'ICH9 HD Audio Controller'  (string)", //
                "  info.vendor = 'Intel Corporation'  (string)", //
                "", //
                "udi = '/org/freedesktop/Hal/devices/pci_8086_0001'", //
                "  info.solaris.driver = 'e1000g'  (string)", //
                "  info.product = 'Network Adapter'  (string)", //
                "  info.vendor = 'Intel Corporation'  (string)");
        List<SoundCard> cards = SolarisSoundCard.parseLshal(lshal);
        assertThat(cards, hasSize(1));
        SoundCard card = cards.get(0);
        assertThat(card.getName(), is("Intel Corporation ICH9 HD Audio Controller"));
        assertThat(card.getCodec(), is("ICH9 HD Audio Controller"));
        assertThat(card.getDriverVersion(), is("ICH9 HD Audio Controller audio810"));
    }

    @Test
    void testParseLshalMultipleAudioDevices() {
        List<String> lshal = Arrays.asList(//
                "udi = '/org/freedesktop/Hal/devices/pci_8086_293e'", //
                "  info.solaris.driver = 'audio810'  (string)", //
                "  info.product = 'HD Audio'  (string)", //
                "  info.vendor = 'Intel'  (string)", //
                "", //
                "udi = '/org/freedesktop/Hal/devices/pci_1002_aa38'", //
                "  info.solaris.driver = 'audio810'  (string)", //
                "  info.product = 'Radeon Audio'  (string)", //
                "  info.vendor = 'AMD'  (string)");
        List<SoundCard> cards = SolarisSoundCard.parseLshal(lshal);
        assertThat(cards, hasSize(2));
        assertThat(cards.get(0).getName(), is("Intel HD Audio"));
        assertThat(cards.get(1).getName(), is("AMD Radeon Audio"));
    }

    @Test
    void testParseLshalEmpty() {
        List<SoundCard> cards = SolarisSoundCard.parseLshal(Collections.emptyList());
        assertThat(cards, is(empty()));
    }

    @Test
    void testParseLshalNoAudioDriver() {
        // Devices exist but none have audio810 driver
        List<String> lshal = Arrays.asList(//
                "udi = '/org/freedesktop/Hal/devices/pci_8086_0001'", //
                "  info.solaris.driver = 'e1000g'  (string)", //
                "  info.product = 'Network Adapter'  (string)", //
                "  info.vendor = 'Intel Corporation'  (string)");
        List<SoundCard> cards = SolarisSoundCard.parseLshal(lshal);
        assertThat(cards, is(empty()));
    }
}
