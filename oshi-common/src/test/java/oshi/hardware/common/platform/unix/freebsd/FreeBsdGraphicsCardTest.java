/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.GraphicsCard;
import oshi.util.Constants;

class FreeBsdGraphicsCardTest {

    // pciconf -lv on a FreeBSD 16 VM, trimmed. The QEMU display has IDs the PCI database does not know, so it has no
    // vendor or device line; the device before it does.
    private static final String PCICONF_FREEBSD = """
            hostb0@pci0:0:0:0:\tclass=0x060000 rev=0x00 hdr=0x00 vendor=0x8086 device=0x29c0 subvendor=0x1af4 subdevice=0x1100
                vendor     = 'Intel Corporation'
                device     = '82G33/G31/P35/P31 Express DRAM Controller'
                class      = bridge
                subclass   = HOST-PCI
            vgapci0@pci0:0:1:0:\tclass=0x030000 rev=0x02 hdr=0x00 vendor=0x1234 device=0x1111 subvendor=0x1af4 subdevice=0x1100
                class      = display
                subclass   = VGA
            uhci0@pci0:0:26:0:\tclass=0x0c0300 rev=0x03 hdr=0x00 vendor=0x8086 device=0x2937 subvendor=0x1af4 subdevice=0x1100
                vendor     = 'Intel Corporation'
                device     = '82801I (ICH9 Family) USB UHCI Controller'
                class      = serial bus
                subclass   = USB
            vgapci1@pci0:0:2:0:\tclass=0x030000 rev=0x00 hdr=0x00 vendor=0x8086 device=0x3ea0 subvendor=0x1028 subdevice=0x08e1
                vendor     = 'Intel Corporation'
                device     = 'WhiskeyLake-U GT2 [UHD Graphics 620]'
                class      = display
                subclass   = VGA
            """;

    // pciconf -lv on DragonFly BSD 6.4, trimmed, which gives the IDs as chip=0xDDDDVVVV
    private static final String PCICONF_DRAGONFLY = """
            none0@pci0:0:1:3:\tclass=0x068000 card=0x11001af4 chip=0x71138086 rev=0x03 hdr=0x00
                vendor     = 'Intel Corporation'
                device     = '82371AB/EB/MB PIIX4 ACPI'
                class      = bridge
            vgapci0@pci0:0:2:0:\tclass=0x030000 card=0x11001af4 chip=0x10501af4 rev=0x01 hdr=0x00
                vendor     = 'Red Hat, Inc.'
                device     = 'Virtio GPU'
                class      = display
                subclass   = VGA
            virtio_pci0@pci0:0:3:0:\tclass=0x020000 card=0x00011af4 chip=0x10001af4 rev=0x00 hdr=0x00
                vendor     = 'Red Hat, Inc.'
                device     = 'Virtio network device'
                class      = network
                subclass   = ethernet
            """;

    @Test
    void testParsePciconfFreeBsd() {
        List<GraphicsCard> cards = FreeBsdGraphicsCard.parsePciconf(PCICONF_FREEBSD.lines().toList());
        assertThat(cards, hasSize(2));

        // Nothing carried over from the device before
        GraphicsCard qemu = cards.get(0);
        assertThat(qemu.getName(), is(Constants.UNKNOWN));
        assertThat(qemu.getVendor(), is("0x1234"));
        assertThat(qemu.getDeviceId(), is("0x1111"));
        assertThat(qemu.getVersionInfo(), is("rev=0x02"));

        GraphicsCard intel = cards.get(1);
        assertThat(intel.getName(), is("WhiskeyLake-U GT2 [UHD Graphics 620]"));
        assertThat(intel.getVendor(), is("Intel Corporation (0x8086)"));
        assertThat(intel.getDeviceId(), is("0x3ea0"));
        assertThat(intel.getVersionInfo(), is("rev=0x00"));
        assertThat(intel.getVRam(), is(0L));
    }

    @Test
    void testParsePciconfDragonFly() {
        List<GraphicsCard> cards = FreeBsdGraphicsCard.parsePciconf(PCICONF_DRAGONFLY.lines().toList());
        assertThat(cards, hasSize(1));
        GraphicsCard virtio = cards.get(0);
        assertThat(virtio.getName(), is("Virtio GPU"));
        assertThat(virtio.getVendor(), is("Red Hat, Inc. (0x1af4)"));
        assertThat(virtio.getDeviceId(), is("0x1050"));
        assertThat(virtio.getVersionInfo(), is("rev=0x01"));
    }

    @Test
    void testParsePciconfEmpty() {
        assertThat(FreeBsdGraphicsCard.parsePciconf(Collections.emptyList()), is(empty()));
        assertThat(FreeBsdGraphicsCard.parsePciconf(List.of("")), is(empty()));
    }
}
