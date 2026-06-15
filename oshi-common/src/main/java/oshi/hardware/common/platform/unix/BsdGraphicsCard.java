/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;

/**
 * Shared graphics card implementation for BSDs that use {@code pcidump -v} to enumerate PCI devices (NetBSD, OpenBSD).
 */
@Immutable
public final class BsdGraphicsCard extends AbstractGraphicsCard {

    private static final String PCI_CLASS_DISPLAY = "Class: 03 Display";
    private static final String UNKNOWN_PCI_ID = "0x0000";
    private static final Pattern PCI_DUMP_HEADER = Pattern.compile(" \\d+:\\d+:\\d+: (.+)");

    public BsdGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    /**
     * Gets graphics cards by parsing {@code pcidump -v} output.
     *
     * @return List of {@link GraphicsCard} objects.
     */
    public static List<GraphicsCard> getGraphicsCards() {
        List<GraphicsCard> cardList = new ArrayList<>();
        List<String> devices = ExecutingCommand.runNative("pcidump -v");
        if (devices.isEmpty()) {
            return Collections.emptyList();
        }
        String name = "";
        String vendorId = "";
        String productId = "";
        boolean classCodeFound = false;
        String versionInfo = "";
        for (String line : devices) {
            Matcher m = PCI_DUMP_HEADER.matcher(line);
            if (m.matches()) {
                if (classCodeFound) {
                    cardList.add(new BsdGraphicsCard(name.isEmpty() ? Constants.UNKNOWN : name,
                            productId.isEmpty() ? UNKNOWN_PCI_ID : productId,
                            vendorId.isEmpty() ? UNKNOWN_PCI_ID : vendorId,
                            versionInfo.isEmpty() ? Constants.UNKNOWN : versionInfo, 0L));
                }
                name = m.group(1);
                vendorId = "";
                productId = "";
                classCodeFound = false;
                versionInfo = "";
            } else {
                int idx;
                if (!classCodeFound) {
                    idx = line.indexOf("Vendor ID: ");
                    if (idx >= 0 && line.length() >= idx + 15) {
                        vendorId = "0x" + line.substring(idx + 11, idx + 15);
                    }
                    idx = line.indexOf("Product ID: ");
                    if (idx >= 0 && line.length() >= idx + 16) {
                        productId = "0x" + line.substring(idx + 12, idx + 16);
                    }
                    if (line.contains(PCI_CLASS_DISPLAY)) {
                        classCodeFound = true;
                    }
                } else if (versionInfo.isEmpty()) {
                    idx = line.indexOf("Revision: ");
                    if (idx >= 0) {
                        versionInfo = line.substring(idx);
                    }
                }
            }
        }
        if (classCodeFound) {
            cardList.add(new BsdGraphicsCard(name.isEmpty() ? Constants.UNKNOWN : name,
                    productId.isEmpty() ? UNKNOWN_PCI_ID : productId, vendorId.isEmpty() ? UNKNOWN_PCI_ID : vendorId,
                    versionInfo.isEmpty() ? Constants.UNKNOWN : versionInfo, 0L));
        }
        return cardList;
    }
}
