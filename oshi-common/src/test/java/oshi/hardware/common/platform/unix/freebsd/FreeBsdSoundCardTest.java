/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import oshi.hardware.SoundCard;

class FreeBsdSoundCardTest {

    /**
     * Answers each {@code sysctl -i} call from a fixed tree, as the real command does: it prints the names it knows and
     * skips the rest. Records each call so a test can count them.
     */
    private static Function<List<String>, List<String>> sysctl(Map<String, String> tree, List<List<String>> calls) {
        return oids -> {
            calls.add(oids);
            List<String> out = new ArrayList<>();
            for (String oid : oids) {
                tree.forEach((name, value) -> {
                    if (name.equals(oid) || name.startsWith(oid + ".")) {
                        out.add(name + ": " + value);
                    }
                });
            }
            return out;
        };
    }

    @Test
    void testParseSoundCardsDummy() {
        // Captured on a vmactions FreeBSD 15.1 VM after "kldload snd_dummy": a pcm device attached to nexus0
        Map<String, String> tree = Map.of("kern.osrelease", "15.1-RELEASE", "dev.pcm.0.mode", "7",
                "dev.pcm.0.play.vchans", "1", "dev.pcm.0.%parent", "nexus0", "dev.pcm.0.%driver", "pcm",
                "dev.pcm.0.%desc", "Dummy Audio Device", "dev.pcm.%parent", "");
        List<List<String>> calls = new ArrayList<>();
        List<SoundCard> cards = FreeBsdSoundCard.parseSoundCards(sysctl(tree, calls));
        assertThat(cards, hasSize(1));
        assertThat(cards.get(0).getName(), is("Dummy Audio Device"));
        assertThat(cards.get(0).getCodec(), is("Dummy Audio Device"));
        assertThat(cards.get(0).getDriverVersion(), is("15.1-RELEASE"));
        // nexus0 is a bus, so nothing beyond the first call
        assertThat(calls, hasSize(1));
    }

    @Test
    void testParseSoundCardsHda() {
        // The tree snd_hda builds: two endpoints on two function groups of one codec on one controller. The controller
        // line is from cfarm430 (FreeBSD 16.0-CURRENT, a QEMU intel-hda with no codec attached)
        Map<String, String> tree = Map.ofEntries(Map.entry("kern.osrelease", "14.3-RELEASE"),
                Map.entry("dev.pcm.0.%desc", "Realtek ALC892 (Rear Analog)"), Map.entry("dev.pcm.0.%parent", "hdaa0"),
                Map.entry("dev.pcm.1.%desc", "Realtek ALC892 (Front Analog Headphones)"),
                Map.entry("dev.pcm.1.%parent", "hdaa0"),
                Map.entry("dev.pcm.10.%desc", "Intel Cougar Point (HDMI/DP 8ch)"),
                Map.entry("dev.pcm.10.%parent", "hdaa1"),
                Map.entry("dev.hdaa.0.%desc", "Realtek ALC892 Audio Function Group"),
                Map.entry("dev.hdaa.0.%parent", "hdacc0"),
                Map.entry("dev.hdaa.1.%desc", "Intel Cougar Point Audio Function Group"),
                Map.entry("dev.hdaa.1.%parent", "hdacc1"), Map.entry("dev.hdacc.0.%desc", "Realtek ALC892 HDA CODEC"),
                Map.entry("dev.hdacc.0.%parent", "hdac0"),
                Map.entry("dev.hdacc.1.%desc", "Intel Cougar Point HDA CODEC"),
                Map.entry("dev.hdacc.1.%parent", "hdac0"), Map.entry("dev.hdac.0.%desc", "Intel 82801I HDA Controller"),
                Map.entry("dev.hdac.0.%parent", "pci0"));
        List<List<String>> calls = new ArrayList<>();
        List<SoundCard> cards = FreeBsdSoundCard.parseSoundCards(sysctl(tree, calls));
        assertThat(cards.stream().map(SoundCard::getCodec).toList(), contains("Realtek ALC892 (Rear Analog)",
                "Realtek ALC892 (Front Analog Headphones)", "Intel Cougar Point (HDMI/DP 8ch)"));
        assertThat(cards.stream().map(SoundCard::getName).distinct().toList(), contains("Intel 82801I HDA Controller"));
        // One call for the pcm devices, then one for each of hdaa, hdacc and hdac
        assertThat(calls, hasSize(4));
    }

    @Test
    void testParseSoundCardsUsb() {
        Map<String, String> tree = Map.of("kern.osrelease", "14.3-RELEASE", "dev.pcm.2.%desc", "USB audio",
                "dev.pcm.2.%parent", "uaudio0", "dev.uaudio.0.%desc",
                "C-Media Electronics Inc. USB Audio Device, class 0/0, rev 1.10/0.10, addr 3", "dev.uaudio.0.%parent",
                "uhub2");
        List<SoundCard> cards = FreeBsdSoundCard.parseSoundCards(sysctl(tree, new ArrayList<>()));
        assertThat(cards, hasSize(1));
        assertThat(cards.get(0).getName(),
                is("C-Media Electronics Inc. USB Audio Device, class 0/0, rev 1.10/0.10, addr 3"));
        assertThat(cards.get(0).getCodec(), is("USB audio"));
    }

    @Test
    void testParseSoundCardsMissingParent() {
        // A parent sysctl does not know is asked for once, and the endpoint names the card
        Map<String, String> tree = Map.of("dev.pcm.0.%desc", "Some Audio", "dev.pcm.0.%parent", "gone0");
        List<List<String>> calls = new ArrayList<>();
        List<SoundCard> cards = FreeBsdSoundCard.parseSoundCards(sysctl(tree, calls));
        assertThat(cards, hasSize(1));
        assertThat(cards.get(0).getName(), is("Some Audio"));
        assertThat(cards.get(0).getDriverVersion(), is("unknown"));
        assertThat(calls, hasSize(2));
    }

    @Test
    void testParseSoundCardsVanishedParent() {
        // uaudio0 is gone by the time its level is read, but the hdaa0 asked for alongside it still answers
        Map<String, String> tree = Map.of("dev.pcm.0.%desc", "USB audio", "dev.pcm.0.%parent", "uaudio0",
                "dev.pcm.1.%desc", "Realtek ALC892 (Rear Analog)", "dev.pcm.1.%parent", "hdaa0", "dev.hdaa.0.%desc",
                "Realtek ALC892 Audio Function Group", "dev.hdaa.0.%parent", "pci0");
        List<List<String>> calls = new ArrayList<>();
        List<SoundCard> cards = FreeBsdSoundCard.parseSoundCards(sysctl(tree, calls));
        assertThat(cards.stream().map(SoundCard::getName).toList(),
                contains("USB audio", "Realtek ALC892 Audio Function Group"));
        assertThat(calls.get(1),
                contains("dev.uaudio.0.%desc", "dev.uaudio.0.%parent", "dev.hdaa.0.%desc", "dev.hdaa.0.%parent"));
    }

    @Test
    void testParseSoundCardsNone() {
        // cfarm430 and DragonFly BSD: sysctl knows no dev.pcm
        List<List<String>> calls = new ArrayList<>();
        assertThat(FreeBsdSoundCard.parseSoundCards(sysctl(Map.of("kern.osrelease", "6.4-RELEASE"), calls)),
                is(empty()));
        assertThat(FreeBsdSoundCard.parseSoundCards(oids -> Collections.emptyList()), is(empty()));
        assertThat(calls, hasSize(1));
    }
}
