/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;

class MacGraphicsCardTest {

    private static final MacGraphicsCard.GraphicsCardFactory FACTORY = TestGraphicsCard::new;
    private static final MacGraphicsCard.SysctlLong SYSCTL = (name, def) -> def;

    @Test
    void testEmptyInput() {
        List<GraphicsCard> cards = MacGraphicsCard.parseGraphicsCards(Collections.emptyList(), FACTORY, SYSCTL);
        assertThat(cards, is(empty()));
    }

    @Test
    void testSingleCard() {
        // Real output from system_profiler SPDisplaysDataType on Apple M3 Pro
        List<String> sp = Arrays.asList("Graphics/Displays:", "", "    Apple M3 Pro:", "",
                "      Chipset Model: Apple M3 Pro", "      Type: GPU", "      Bus: Built-In",
                "      Total Number of Cores: 18", "      Vendor: Apple (0x106b)", "      Metal Support: Metal 4",
                "      Displays:", "        LG ULTRAWIDE:",
                "          Resolution: 3440 x 1440 (UWQHD - Ultra-Wide Quad HD)",
                "          UI Looks like: 3440 x 1440 @ 50.00Hz", "          Main Display: Yes",
                "          Mirror: Off", "          Online: Yes", "          Rotation: Supported", "        Color LCD:",
                "          Display Type: Built-in Liquid Retina XDR Display",
                "          Resolution: 3456 x 2234 Retina", "          Mirror: Off", "          Online: Yes",
                "          Automatically Adjust Brightness: Yes", "          Connection Type: Internal");
        List<GraphicsCard> cards = MacGraphicsCard.parseGraphicsCards(sp, FACTORY, SYSCTL);
        assertThat(cards, hasSize(1));
        assertThat(cards.get(0).getName(), is("Apple M3 Pro"));
        assertThat(cards.get(0).getVendor(), is("Apple (0x106b)"));
        assertThat(cards.get(0).getDeviceId(), is(Constants.UNKNOWN));
    }

    @Test
    void testTwoCards() {
        // Representative output from a 2017 MacBook Pro with Intel + AMD GPUs
        List<String> sp = Arrays.asList("Graphics/Displays:", "", "    Intel HD Graphics 630:", "",
                "      Chipset Model: Intel HD Graphics 630", "      Device ID: 0x591b", "      Vendor: Intel (0x8086)",
                "      VRAM (Dynamic, Max): 1536 MB", "      Revision ID: 0x0004", "", "    Radeon Pro 560:", "",
                "      Chipset Model: Radeon Pro 560", "      Device ID: 0x67ef", "      Vendor: AMD (0x1002)",
                "      VRAM (Total): 4096 MB", "      EFI Driver Version: 01.00.560");
        List<GraphicsCard> cards = MacGraphicsCard.parseGraphicsCards(sp, FACTORY, SYSCTL);
        assertThat(cards, hasSize(2));
        assertThat(cards.get(0).getName(), is("Intel HD Graphics 630"));
        assertThat(cards.get(0).getDeviceId(), is("0x591b"));
        assertThat(cards.get(0).getVRam(), is(1536L * 1024 * 1024));
        assertThat(cards.get(1).getName(), is("Radeon Pro 560"));
        assertThat(cards.get(1).getDeviceId(), is("0x67ef"));
        assertThat(cards.get(1).getVRam(), is(4096L * 1024 * 1024));
    }

    @Test
    void testVersionInfo() {
        List<String> sp = Arrays.asList("    Card:", "      Chipset Model: Test GPU", "      EFI Driver Version: 1.2.3",
                "      Metal Revision: 42");
        List<GraphicsCard> cards = MacGraphicsCard.parseGraphicsCards(sp, FACTORY, SYSCTL);
        assertThat(cards, hasSize(1));
        assertThat(cards.get(0).getVersionInfo(), is("EFI Driver Version: 1.2.3, Metal Revision: 42"));
    }

    @Test
    void testVramFallbackHwMemsize() {
        // Apple Silicon GPUs have no VRAM line; resolveVram falls back to hw.memsize
        long memsize = 36L * 1024 * 1024 * 1024;
        MacGraphicsCard.SysctlLong sysctl = (name, def) -> "hw.memsize".equals(name) ? memsize : def;
        List<String> sp = Arrays.asList("      Chipset Model: Apple M3 Pro", "      Vendor: Apple (0x106b)");
        List<GraphicsCard> cards = MacGraphicsCard.parseGraphicsCards(sp, FACTORY, sysctl);
        assertThat(cards, hasSize(1));
        assertThat(cards.get(0).getVRam(), is(memsize));
    }

    static class TestGraphicsCard extends AbstractGraphicsCard {
        TestGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
            super(name, deviceId, vendor, versionInfo, vram);
        }
    }
}
