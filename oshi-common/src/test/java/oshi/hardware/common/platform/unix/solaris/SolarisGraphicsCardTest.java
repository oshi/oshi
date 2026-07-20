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

import oshi.hardware.GraphicsCard;

class SolarisGraphicsCardTest {

    @Test
    void testParsePrtconf() {
        // prtconf -pv output with a display-class device (class-code 0003xxxx)
        List<String> prtconf = Arrays.asList(//
                "    Node 0x1f8e610", //
                "      name:  'pci8086,2a42'", //
                "      model:  'Intel GM45 Integrated Graphics'", //
                "      vendor-id:  00008086", //
                "      device-id:  00002a42", //
                "      revision-id:  00000007", //
                "      class-code:  00030000");
        List<GraphicsCard> cards = SolarisGraphicsCard.parsePrtconf(prtconf);
        assertThat(cards, hasSize(1));
        GraphicsCard card = cards.get(0);
        assertThat(card.getName(), is("Intel GM45 Integrated Graphics"));
        assertThat(card.getVendor(), is("0x8086"));
        assertThat(card.getDeviceId(), is("0x2a42"));
        assertThat(card.getVersionInfo(), is("revision-id:  00000007"));
        assertThat(card.getVRam(), is(0L));
    }

    @Test
    void testParsePrtconfNoDisplayDevice() {
        // prtconf -pv output with NO display-class device
        List<String> prtconf = Arrays.asList(//
                "    Node 0x1f8e610", //
                "      name:  'pci8086,29c0'", //
                "      vendor-id:  00008086", //
                "      device-id:  000029c0", //
                "      class-code:  00060000"); // class 0006 = Bridge, not Display
        List<GraphicsCard> cards = SolarisGraphicsCard.parsePrtconf(prtconf);
        assertThat(cards, is(empty()));
    }

    @Test
    void testParsePrtconfEmpty() {
        List<GraphicsCard> cards = SolarisGraphicsCard.parsePrtconf(Collections.emptyList());
        assertThat(cards, is(empty()));
    }

    @Test
    void testParsePrtconfDisplayAtEnd() {
        // Display device at end of output (no following Node header to flush)
        List<String> prtconf = Arrays.asList(//
                "    Node 0xaabbcc", //
                "      name:  'pci1002,67df'", //
                "      model:  'AMD Radeon RX 580'", //
                "      vendor-id:  00001002", //
                "      device-id:  000067df", //
                "      revision-id:  000000e7", //
                "      class-code:  00030000");
        List<GraphicsCard> cards = SolarisGraphicsCard.parsePrtconf(prtconf);
        assertThat(cards, hasSize(1));
        assertThat(cards.get(0).getName(), is("AMD Radeon RX 580"));
        assertThat(cards.get(0).getVendor(), is("0x1002"));
        assertThat(cards.get(0).getDeviceId(), is("0x67df"));
    }
}
