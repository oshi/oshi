/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.GraphicsCard;

class BsdGraphicsCardTest {

    // Representative pcidump -v output from OpenBSD with one display and one network device
    private static final List<String> PCIDUMP = Arrays.asList(//
            " 0:0:0: Intel 82G33/G31/P35/P31 DRAM Controller", //
            "\tVendor ID: 8086", //
            "\tProduct ID: 29c0", //
            "\tClass: 06 Bridge, Host", //
            " 0:2:0: Intel 82G33/G31 Integrated Graphics Controller", //
            "\tVendor ID: 8086", //
            "\tProduct ID: 29c2", //
            "\tClass: 03 Display, VGA", //
            "\tRevision: 02", //
            " 0:25:0: Intel 82801I (ICH9) LAN Controller", //
            "\tVendor ID: 8086", //
            "\tProduct ID: 10f0", //
            "\tClass: 02 Network, Ethernet");

    @Test
    void testParsePciDump() {
        List<GraphicsCard> cards = BsdGraphicsCard.parsePciDump(PCIDUMP);
        assertThat(cards, hasSize(1));
        GraphicsCard card = cards.get(0);
        assertThat(card.getName(), is("Intel 82G33/G31 Integrated Graphics Controller"));
        assertThat(card.getDeviceId(), is("0x29c2"));
        assertThat(card.getVendor(), is("0x8086"));
        assertThat(card.getVersionInfo(), is("Revision: 02"));
        assertThat(card.getVRam(), is(0L));
    }

    @Test
    void testParsePciDumpMultipleDisplayDevices() {
        List<String> pcidump = Arrays.asList(//
                " 0:2:0: NVIDIA GeForce GTX 1080", //
                "\tVendor ID: 10de", //
                "\tProduct ID: 1b80", //
                "\tClass: 03 Display, VGA", //
                "\tRevision: a1", //
                " 0:3:0: NVIDIA GeForce GTX 1080 Audio", //
                "\tVendor ID: 10de", //
                "\tProduct ID: 10f0", //
                "\tClass: 04 Multimedia, Audio", //
                " 0:5:0: AMD Radeon RX 580", //
                "\tVendor ID: 1002", //
                "\tProduct ID: 67df", //
                "\tClass: 03 Display, VGA", //
                "\tRevision: e7");
        List<GraphicsCard> cards = BsdGraphicsCard.parsePciDump(pcidump);
        assertThat(cards, hasSize(2));
        assertThat(cards.get(0).getName(), is("NVIDIA GeForce GTX 1080"));
        assertThat(cards.get(0).getDeviceId(), is("0x1b80"));
        assertThat(cards.get(1).getName(), is("AMD Radeon RX 580"));
        assertThat(cards.get(1).getDeviceId(), is("0x67df"));
        assertThat(cards.get(1).getVersionInfo(), is("Revision: e7"));
    }

    @Test
    void testParsePciDumpNoDisplayDevices() {
        List<String> pcidump = Arrays.asList(//
                " 0:0:0: Intel Host Bridge", //
                "\tVendor ID: 8086", //
                "\tProduct ID: 0001", //
                "\tClass: 06 Bridge, Host");
        List<GraphicsCard> cards = BsdGraphicsCard.parsePciDump(pcidump);
        assertThat(cards, is(empty()));
    }

    @Test
    void testParsePciDumpEmpty() {
        List<GraphicsCard> cards = BsdGraphicsCard.parsePciDump(Collections.emptyList());
        assertThat(cards, is(empty()));
    }

    @Test
    void testParsePciDumpDisplayAtEndOfOutput() {
        // Display device is the last entry (no following header to flush it)
        List<String> pcidump = Arrays.asList(//
                " 0:2:0: VGA Compatible Controller", //
                "\tVendor ID: abcd", //
                "\tProduct ID: 1234", //
                "\tClass: 03 Display, VGA", //
                "\tRevision: ff");
        List<GraphicsCard> cards = BsdGraphicsCard.parsePciDump(pcidump);
        assertThat(cards, hasSize(1));
        assertThat(cards.get(0).getVendor(), is("0xabcd"));
        assertThat(cards.get(0).getDeviceId(), is("0x1234"));
    }
}
