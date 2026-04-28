/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import oshi.hardware.GraphicsCard;
import oshi.util.tuples.Triplet;

class LinuxGraphicsCardTest {

    /**
     * Minimal concrete subclass for testing the abstract LinuxGraphicsCard.
     */
    private static class StubGraphicsCard extends LinuxGraphicsCard {
        StubGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram,
                String drmDevicePath, String driverName, String pciBusId) {
            super(name, deviceId, vendor, versionInfo, vram, drmDevicePath, driverName, pciBusId);
        }
    }

    @Test
    void testConstructorAndGetters() {
        StubGraphicsCard card = new StubGraphicsCard("RTX 4090", "0x2684", "NVIDIA", "Rev: 01", 24576L,
                "/sys/class/drm/card0/device", "nvidia", "0000:01:00.0");
        assertThat(card.getName(), is("RTX 4090"));
        assertThat(card.getDeviceId(), is("0x2684"));
        assertThat(card.getVendor(), is("NVIDIA"));
        assertThat(card.getVersionInfo(), is("Rev: 01"));
        assertThat(card.getVRam(), is(24576L));
        assertThat(card.getDrmDevicePath(), is("/sys/class/drm/card0/device"));
        assertThat(card.getDriverName(), is("nvidia"));
        assertThat(card.getPciBusId(), is("0000:01:00.0"));
    }

    @Test
    void testAttrsConstructorAndGetters() {
        LinuxGraphicsCard.Attrs attrs = new LinuxGraphicsCard.Attrs("RX 7900", "0x744c", "AMD", "Rev: c1", 20480L,
                "/sys/class/drm/card1/device", "amdgpu", "0000:03:00.0");
        assertThat(attrs.getName(), is("RX 7900"));
        assertThat(attrs.getDeviceId(), is("0x744c"));
        assertThat(attrs.getVendor(), is("AMD"));
        assertThat(attrs.getVersionInfo(), is("Rev: c1"));
        assertThat(attrs.getVram(), is(20480L));
        assertThat(attrs.getDrmDevicePath(), is("/sys/class/drm/card1/device"));
        assertThat(attrs.getDriverName(), is("amdgpu"));
        assertThat(attrs.getPciBusId(), is("0000:03:00.0"));
    }

    @Test
    void testFindDrmInfoNonexistentPath(@TempDir Path tempDir) {
        Triplet<String, String, String> result = LinuxGraphicsCard.findDrmInfo("01:00.0",
                tempDir.resolve("does-not-exist").toString());
        assertThat(result.getA(), is(""));
        assertThat(result.getB(), is(""));
        assertThat(result.getC(), is(""));
    }

    @Test
    void testFindDrmInfoEmptyDir(@TempDir Path tempDir) {
        Triplet<String, String, String> result = LinuxGraphicsCard.findDrmInfo("01:00.0", tempDir.toString());
        assertThat(result.getA(), is(""));
        assertThat(result.getB(), is(""));
        assertThat(result.getC(), is(""));
    }

    @Test
    void testFindDrmInfoNoDriverSymlink(@TempDir Path tempDir) throws IOException {
        // card0/device exists but no driver symlink
        Files.createDirectories(tempDir.resolve("card0/device"));
        Triplet<String, String, String> result = LinuxGraphicsCard.findDrmInfo("01:00.0", tempDir.toString());
        assertThat(result.getA(), is(""));
        assertThat(result.getB(), is(""));
        assertThat(result.getC(), is(""));
    }

    @Test
    void testFindDrmInfoMatchesPciSlot(@TempDir Path tempDir) throws IOException {
        createCardWithDriver(tempDir, "card0", "amdgpu", "0000:01:00.0");
        createCardWithDriver(tempDir, "card1", "i915", "0000:00:02.0");

        Triplet<String, String, String> result = LinuxGraphicsCard.findDrmInfo("00:02.0", tempDir.toString());
        assertThat(result.getB(), is("i915"));
        assertThat(result.getC(), is("0000:00:02.0"));
    }

    @Test
    void testFindDrmInfoFallsBackToFirstDriver(@TempDir Path tempDir) throws IOException {
        createCardWithDriver(tempDir, "card0", "amdgpu", "0000:01:00.0");

        // No slot match — should fall back to first card with a driver
        Triplet<String, String, String> result = LinuxGraphicsCard.findDrmInfo("99:00.0", tempDir.toString());
        assertThat(result.getB(), is("amdgpu"));
        assertThat(result.getC(), is("0000:01:00.0"));
    }

    @Test
    void testFindDrmInfoNullPciSlotFallback(@TempDir Path tempDir) throws IOException {
        createCardWithDriver(tempDir, "card0", "xe", "0000:00:02.0");

        Triplet<String, String, String> result = LinuxGraphicsCard.findDrmInfo(null, tempDir.toString());
        assertThat(result.getB(), is("xe"));
        assertThat(result.getC(), is("0000:00:02.0"));
    }

    @Test
    void testFindDrmInfoIgnoresNonCardDirs(@TempDir Path tempDir) throws IOException {
        // "renderD128" should not match the card\d+ pattern
        Files.createDirectories(tempDir.resolve("renderD128/device"));
        createCardWithDriver(tempDir, "card0", "nvidia", "0000:01:00.0");

        Triplet<String, String, String> result = LinuxGraphicsCard.findDrmInfo(null, tempDir.toString());
        assertThat(result.getB(), is("nvidia"));
    }

    @Test
    void testReadUeventValue(@TempDir Path tempDir) throws IOException {
        Path uevent = tempDir.resolve("uevent");
        String content = "DRIVER=amdgpu\nPCI_SLOT_NAME=0000:01:00.0\nPCI_ID=1002:744C\n";
        Files.write(uevent, content.getBytes(StandardCharsets.UTF_8));

        assertThat(LinuxGraphicsCard.readUeventValue(uevent.toString(), "PCI_SLOT_NAME"), is("0000:01:00.0"));
        assertThat(LinuxGraphicsCard.readUeventValue(uevent.toString(), "DRIVER"), is("amdgpu"));
        assertThat(LinuxGraphicsCard.readUeventValue(uevent.toString(), "MISSING_KEY"), is(""));
    }

    @Test
    void testReadUeventValueMissingFile(@TempDir Path tempDir) {
        assertThat(LinuxGraphicsCard.readUeventValue(tempDir.resolve("no-uevent").toString(), "KEY"), is(""));
    }

    @Test
    void testReadDriverNameSymlink(@TempDir Path tempDir) throws IOException {
        // Create a fake driver directory and symlink to it
        Path driverDir = tempDir.resolve("bus/pci/drivers/amdgpu");
        Files.createDirectories(driverDir);
        Path symlink = tempDir.resolve("driver");
        Files.createSymbolicLink(symlink, driverDir);

        assertThat(LinuxGraphicsCard.readDriverName(symlink.toString()), is("amdgpu"));
    }

    @Test
    void testReadDriverNameNoSymlink(@TempDir Path tempDir) {
        assertThat(LinuxGraphicsCard.readDriverName(tempDir.resolve("no-driver").toString()), is(""));
    }

    /**
     * Creates a card directory with a driver symlink and uevent file.
     *
     * @param drmDir     the base DRM directory
     * @param cardName   e.g. "card0"
     * @param driverName e.g. "amdgpu"
     * @param slotName   e.g. "0000:01:00.0"
     * @throws IOException if file creation fails
     */
    private static void createCardWithDriver(Path drmDir, String cardName, String driverName, String slotName)
            throws IOException {
        Path deviceDir = drmDir.resolve(cardName + "/device");
        Files.createDirectories(deviceDir);
        // Create a fake driver target and symlink
        Path driverTarget = drmDir.resolve("drivers/" + driverName);
        Files.createDirectories(driverTarget);
        Files.createSymbolicLink(deviceDir.resolve("driver"), driverTarget);
        // Write uevent with PCI_SLOT_NAME
        Files.write(deviceDir.resolve("uevent"), ("PCI_SLOT_NAME=" + slotName + "\n").getBytes(StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // getGraphicsCardsFromLspci parsing
    // -------------------------------------------------------------------------

    // Fixture: lspci -vnnm output with one VGA card
    private static final List<String> LSPCI_VNNM = Arrays.asList("Slot:\t01:00.0",
            "Class:\tVGA compatible controller [0300]", "Vendor:\tNVIDIA Corporation [10de]",
            "Device:\tGA102 [GeForce RTX 3090] [2204]", "SVendor:\tASUS [1043]",
            "SDevice:\tGA102 [GeForce RTX 3090] [8687]", "Rev:\ta1", "");

    private static final Function<LinuxGraphicsCard.Attrs, GraphicsCard> STUB_FACTORY = attrs -> new StubGraphicsCard(
            attrs.getName(), attrs.getDeviceId(), attrs.getVendor(), attrs.getVersionInfo(), attrs.getVram(),
            attrs.getDrmDevicePath(), attrs.getDriverName(), attrs.getPciBusId());

    // No-op lookups for pure parsing tests
    private static final Function<String, Long> NO_VRAM = slot -> 0L;
    private static final Function<String, Triplet<String, String, String>> NO_DRM = slot -> new Triplet<>("", "", "");

    @Test
    void testGetGraphicsCardsFromLspciSingleCard() {
        List<GraphicsCard> cards = LinuxGraphicsCard.getGraphicsCardsFromLspci(LSPCI_VNNM, STUB_FACTORY, NO_VRAM,
                NO_DRM);
        assertThat(cards.size(), is(1));
        GraphicsCard card = cards.get(0);
        assertThat(card.getName(), is("GA102 [GeForce RTX 3090]"));
        assertThat(card.getDeviceId(), is("0x2204"));
        assertThat(card.getVendor(), is("NVIDIA Corporation (0x10de)"));
        assertThat(card.getVersionInfo(), is("Rev:\ta1"));
    }

    @Test
    void testGetGraphicsCardsFromLspciEmpty() {
        List<GraphicsCard> cards = LinuxGraphicsCard.getGraphicsCardsFromLspci(Collections.emptyList(), STUB_FACTORY,
                NO_VRAM, NO_DRM);
        assertThat(cards, is(empty()));
    }

    @Test
    void testGetGraphicsCardsFromLspciTwoCards() {
        List<String> twoCards = Arrays.asList("Slot:\t01:00.0", "Class:\tVGA compatible controller [0300]",
                "Vendor:\tNVIDIA Corporation [10de]", "Device:\tGA102 [GeForce RTX 3090] [2204]", "Rev:\ta1", "",
                "Slot:\t00:02.0", "Class:\tVGA compatible controller [0300]", "Vendor:\tIntel Corporation [8086]",
                "Device:\tUHD Graphics 630 [3E92]", "Rev:\t00", "");
        List<GraphicsCard> cards = LinuxGraphicsCard.getGraphicsCardsFromLspci(twoCards, STUB_FACTORY, NO_VRAM, NO_DRM);
        assertThat(cards.size(), is(2));
        assertThat(cards.get(0).getName(), is("GA102 [GeForce RTX 3090]"));
        assertThat(cards.get(1).getName(), is("UHD Graphics 630"));
        assertThat(cards.get(1).getDeviceId(), is("0x3E92"));
    }

    @Test
    void testGetGraphicsCardsFromLspci3DController() {
        List<String> threeD = Arrays.asList("Slot:\t01:00.0", "Class:\t3D controller [0302]",
                "Vendor:\tNVIDIA Corporation [10de]", "Device:\tTesla V100 [1db4]", "");
        List<GraphicsCard> cards = LinuxGraphicsCard.getGraphicsCardsFromLspci(threeD, STUB_FACTORY, NO_VRAM, NO_DRM);
        assertThat(cards.size(), is(1));
        assertThat(cards.get(0).getName(), is("Tesla V100"));
    }

    // -------------------------------------------------------------------------
    // queryLspciMemorySize parsing
    // -------------------------------------------------------------------------

    @Test
    void testQueryLspciMemorySize() {
        List<String> lspciV = Arrays.asList("01:00.0 VGA compatible controller: NVIDIA Corporation",
                "\tMemory at f6000000 (32-bit, non-prefetchable) [size=16M]",
                "\tMemory at e0000000 (64-bit, prefetchable) [size=256M]",
                "\tMemory at f0000000 (64-bit, prefetchable) [size=32M]", "\tI/O ports at e000 [size=128]");
        long vram = LinuxGraphicsCard.queryLspciMemorySize(lspciV);
        // 256M + 32M = 288M
        assertThat(vram, is(256L * 1024 * 1024 + 32L * 1024 * 1024));
    }

    @Test
    void testQueryLspciMemorySizeNoPrefetchable() {
        List<String> noPrefetch = Arrays.asList("01:00.0 VGA compatible controller: Intel",
                "\tMemory at f6000000 (32-bit, non-prefetchable) [size=16M]");
        assertThat(LinuxGraphicsCard.queryLspciMemorySize(noPrefetch), is(0L));
    }

    @Test
    void testQueryLspciMemorySizeEmpty() {
        assertThat(LinuxGraphicsCard.queryLspciMemorySize(Collections.emptyList()), is(0L));
    }
}
