/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.GraphicsCard;
import oshi.util.Constants;

class NetBsdGraphicsCardTest {

    @Test
    void testVgaWithIdsOnly() {
        // NetBSD 10.1 amd64 under QEMU: the standard VGA has no name in the PCI database
        List<String> dmesg = """
                pchb0 at pci0 dev 0 function 0: Intel 82441FX (PMC) PCI and Memory Controller (rev. 0x02)
                vga0 at pci0 dev 2 function 0: vendor 1234 product 1111 (rev. 0x02)
                wsdisplay0 at vga0 kbdmux 1: console (80x25, vt100 emulation), using wskbd0
                wsmux1: connecting to wsdisplay0
                drm at vga0 not configured
                wm0 at pci0 dev 3 function 0, 64-bit DMA: Intel i82540EM 1000BASE-T Ethernet (rev. 0x03)
                wsdisplay0: screen 1 added (80x25, vt100 emulation)
                """.lines().toList();
        List<GraphicsCard> cards = NetBsdGraphicsCard.parseDmesg(dmesg);
        assertThat(cards, hasSize(1));
        GraphicsCard card = cards.get(0);
        assertThat(card.getName(), is(Constants.UNKNOWN));
        assertThat(card.getVendor(), is("0x1234"));
        assertThat(card.getDeviceId(), is("0x1111"));
        assertThat(card.getVersionInfo(), is("Revision: 0x02"));
        assertThat(card.getVRam(), is(0L));
    }

    @Test
    void testDrmFramebufferChain() {
        List<String> dmesg = """
                i915drmkms0 at pci0 dev 2 function 0: Intel UHD Graphics 620 (rev. 0x07)
                intelfb0 at i915drmkms0
                wsdisplay0 at intelfb0 kbdmux 1: console (default, vt100 emulation), using wskbd0
                radeon0 at pci1 dev 0 function 0: ATI Technologies Radeon RX 550 (rev. 0xc7)
                """.lines().toList();
        List<GraphicsCard> cards = NetBsdGraphicsCard.parseDmesg(dmesg);
        // The second adapter drives no console display, so it is not identified as one
        assertThat(cards, hasSize(1));
        assertThat(cards.get(0).getName(), is("Intel UHD Graphics 620"));
        assertThat(cards.get(0).getVendor(), is(Constants.UNKNOWN));
        assertThat(cards.get(0).getVersionInfo(), is("Revision: 0x07"));
    }

    @Test
    void testNoDisplay() {
        List<String> dmesg = """
                ehci0 at pci0 dev 1 function 0: vendor 8086 product 24cd (rev. 0x10)
                virtio32 at pci0 dev 3 function 0
                """.lines().toList();
        assertThat(NetBsdGraphicsCard.parseDmesg(dmesg), is(empty()));
    }
}
