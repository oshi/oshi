/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.SoundCard;

class FreeBsdSoundCardTest {

    @Test
    void testParseSoundCards() {
        // A pcm-driven node carries the product/vendor; other nodes (e.g. the parent hdac) are ignored.
        List<String> lshal = Arrays.asList(//
                "udi = '/org/freedesktop/Hal/devices/pci_8086_a348'", //
                "  info.product = 'Cannon Lake PCH cAVS'  (string)", //
                "udi = '/org/freedesktop/Hal/devices/pcm0'", //
                "  info.product = 'Realtek ALC892 (Analog)'  (string)", //
                "  info.vendor = 'Realtek'  (string)", //
                "  freebsd.driver = 'pcm'  (string)");
        List<SoundCard> cards = FreeBsdSoundCard.parseSoundCards(lshal);
        assertThat(cards, hasSize(1));
        SoundCard card = cards.get(0);
        assertThat(card.getName(), is("Realtek Realtek ALC892 (Analog)"));
        assertThat(card.getCodec(), is("Realtek ALC892 (Analog)"));
    }

    @Test
    void testParseSoundCardsMissingVendor() {
        // With no info.vendor, the name is just the product (leading space from the "vendor product" join).
        List<String> lshal = Arrays.asList(//
                "udi = '/org/freedesktop/Hal/devices/pcm0'", //
                "  info.product = 'HDA Generic'  (string)", //
                "  freebsd.driver = 'pcm'  (string)");
        List<SoundCard> cards = FreeBsdSoundCard.parseSoundCards(lshal);
        assertThat(cards, hasSize(1));
        assertThat(cards.get(0).getCodec(), is("HDA Generic"));
    }

    @Test
    void testParseSoundCardsEmpty() {
        assertThat(FreeBsdSoundCard.parseSoundCards(Collections.emptyList()), is(empty()));
    }
}
