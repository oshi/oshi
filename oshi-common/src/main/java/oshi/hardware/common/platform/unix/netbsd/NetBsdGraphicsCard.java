/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;
import oshi.util.FileUtil;

/**
 * Graphics card info obtained from the NetBSD boot messages.
 * <p>
 * Listing PCI devices with their class ({@code pcictl}) requires root, so a display adapter is identified instead as
 * the PCI device that a {@code wsdisplay} console attaches to, directly ({@code vga}, {@code genfb}) or through a
 * framebuffer driver ({@code i915drmkms} and similar).
 */
@Immutable
final class NetBsdGraphicsCard extends AbstractGraphicsCard {

    private static final String DMESG_BOOT = "/var/run/dmesg.boot";

    // wsdisplay0 at vga0 kbdmux 1: console (80x25, vt100 emulation), using wskbd0
    private static final Pattern ATTACH = Pattern.compile("^(\\w+\\d+) at (\\w+\\d+)\\b.*");
    // vga0 at pci0 dev 2 function 0: vendor 1234 product 1111 (rev. 0x02)
    private static final Pattern PCI_ATTACH = Pattern
            .compile("^(\\w+\\d+) at pci\\d+ dev \\d+ function \\d+[^:]*: (.+?)(?: \\(rev\\. (0x\\p{XDigit}+)\\))?$");
    private static final Pattern PCI_IDS = Pattern.compile("vendor (\\p{XDigit}{4}) product (\\p{XDigit}{4})");

    NetBsdGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    /**
     * Gets the graphics cards that a console display is attached to.
     *
     * @return the graphics cards
     */
    static List<GraphicsCard> getGraphicsCards() {
        return parseDmesg(FileUtil.readFile(DMESG_BOOT, false));
    }

    /**
     * Parses boot messages for the PCI devices that a {@code wsdisplay} attaches to.
     *
     * @param dmesg the lines of {@code /var/run/dmesg.boot}
     * @return a graphics card for each such device, in attach order
     */
    static List<GraphicsCard> parseDmesg(List<String> dmesg) {
        Map<String, String> parents = new HashMap<>();
        Map<String, Matcher> pciDevices = new HashMap<>();
        List<String> displays = new ArrayList<>();
        for (String line : dmesg) {
            Matcher pci = PCI_ATTACH.matcher(line);
            if (pci.matches()) {
                pciDevices.put(pci.group(1), pci);
                continue;
            }
            Matcher m = ATTACH.matcher(line);
            if (m.matches()) {
                parents.putIfAbsent(m.group(1), m.group(2));
                if (m.group(1).startsWith("wsdisplay")) {
                    displays.add(m.group(1));
                }
            }
        }
        // Walk each display up to the PCI device it hangs from
        Map<String, Matcher> adapters = new LinkedHashMap<>();
        for (String display : displays) {
            String device = parents.get(display);
            for (int depth = 0; device != null && depth < 8; depth++) {
                Matcher pci = pciDevices.get(device);
                if (pci != null) {
                    adapters.putIfAbsent(device, pci);
                    break;
                }
                device = parents.get(device);
            }
        }
        List<GraphicsCard> cards = new ArrayList<>();
        for (Matcher pci : adapters.values()) {
            cards.add(toGraphicsCard(pci));
        }
        return Collections.unmodifiableList(cards);
    }

    private static GraphicsCard toGraphicsCard(Matcher pci) {
        String description = pci.group(2);
        String revision = pci.group(3);
        String versionInfo = revision == null ? Constants.UNKNOWN : "Revision: " + revision;
        Matcher ids = PCI_IDS.matcher(description);
        if (ids.find()) {
            // No name in the kernel's PCI database: only the IDs are known
            return new NetBsdGraphicsCard(Constants.UNKNOWN, "0x" + ids.group(2), "0x" + ids.group(1), versionInfo, 0L);
        }
        // The kernel prints vendor and product names as one string, with no reliable boundary between them
        return new NetBsdGraphicsCard(description, Constants.UNKNOWN, Constants.UNKNOWN, versionInfo, 0L);
    }
}
