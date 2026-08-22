/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.SoundCard;
import oshi.util.Constants;

class BsdSoundCardTest {

    // Representative NetBSD/OpenBSD dmesg output with audio devices
    private static final List<String> DMESG = """
            azalia0 at pci0 dev 27 function 0 "Intel 82801I HD Audio" rev 0x03: msi
            azalia0: codec[0]: Realtek ALC888
            audio0 at azalia0
            hdaudio0 at pci0 dev 3 function 0 "Intel HD Graphics Audio" rev 0x05: msi
            hdaudio0: codec[0]: Intel Haswell HDMI
            audio1 at hdaudio0
            """.lines().toList();

    @Test
    void testParseDmesg() {
        List<SoundCard> cards = BsdSoundCard.parseDmesg(DMESG);
        assertThat(cards, hasSize(2));
        // Find the azalia card
        SoundCard azalia = cards.stream().filter(c -> c.getName().contains("82801I")).findFirst().orElse(null);
        assertNotNull(azalia);
        assertThat(azalia.getName(), is("Intel 82801I HD Audio"));
        // Parser uses indexOf(':') to split — captures everything after the first colon in the codec line
        assertThat(azalia.getCodec(), is("codec[0]: Realtek ALC888"));
        assertThat(azalia.getDriverVersion(), is("rev 0x03"));
    }

    @Test
    void testParseDmesgNoAudio() {
        List<String> dmesg = List.of(//
                "azalia0 at pci0 dev 27 function 0 \"Intel Audio\" rev 0x03: msi", //
                "azalia0: codec[0]: Realtek ALC888");
        // No "audio0 at azalia0" line means azalia0 is not in the names set
        List<SoundCard> cards = BsdSoundCard.parseDmesg(dmesg);
        assertThat(cards, is(empty()));
    }

    @Test
    void testParseDmesgEmpty() {
        List<SoundCard> cards = BsdSoundCard.parseDmesg(Collections.emptyList());
        assertThat(cards, is(empty()));
    }

    @Test
    void testParseDmesgNoCodec() {
        // Audio device is present but no codec line follows the PCI match
        List<String> dmesg = """
                audio0 at azalia0
                azalia0 at pci0 dev 27 function 0 "Intel Audio" rev 0x01: msi
                unrelated line here
                """.lines().toList();
        List<SoundCard> cards = BsdSoundCard.parseDmesg(dmesg);
        assertThat(cards, hasSize(1));
        assertThat(cards.get(0).getName(), is("Intel Audio"));
        // No codec line found; the absent value is normalized to the unknown sentinel
        assertThat(cards.get(0).getCodec(), is(Constants.UNKNOWN));
    }
}
