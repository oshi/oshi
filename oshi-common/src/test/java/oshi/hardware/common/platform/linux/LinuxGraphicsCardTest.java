/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static oshi.util.TestFileUtil.writeFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import oshi.hardware.GraphicsCard;
import oshi.util.Constants;
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
    void testVramTotalPrefersAmdgpuSysfs(@TempDir Path tempDir) throws IOException {
        Path device = tempDir.resolve("device");
        Files.createDirectories(device);
        writeFile(device.resolve("mem_info_vram_total"), "25753026560\n");

        // The 20480 below is an lspci BAR size, which is the aperture the card exposes rather than the memory it has
        LinuxGraphicsCard.Attrs attrs = new LinuxGraphicsCard.Attrs("RX 7900", "0x744c", "AMD", "Rev: c1", 20480L,
                device.toString(), "amdgpu", "0000:03:00.0");
        assertThat(LinuxGraphicsCard.vramTotal(attrs), is(25753026560L));
    }

    @Test
    void testVramTotalFallsBackToParsedValue(@TempDir Path tempDir) throws IOException {
        Path device = tempDir.resolve("device");
        Files.createDirectories(device);

        LinuxGraphicsCard.Attrs amdNoFile = new LinuxGraphicsCard.Attrs("RX 7900", "0x744c", "AMD", "Rev: c1", 20480L,
                device.toString(), "amdgpu", "0000:03:00.0");
        assertThat(LinuxGraphicsCard.vramTotal(amdNoFile), is(20480L));

        // Only amdgpu publishes this file, so another driver must not be read out of the same path
        writeFile(device.resolve("mem_info_vram_total"), "25753026560\n");
        LinuxGraphicsCard.Attrs intel = new LinuxGraphicsCard.Attrs("Arc A770", "0x56a0", "Intel", "Rev: 08", 20480L,
                device.toString(), "i915", "0000:03:00.0");
        assertThat(LinuxGraphicsCard.vramTotal(intel), is(20480L));

        LinuxGraphicsCard.Attrs noPath = new LinuxGraphicsCard.Attrs("RX 7900", "0x744c", "AMD", "Rev: c1", 20480L, "",
                "amdgpu", "0000:03:00.0");
        assertThat(LinuxGraphicsCard.vramTotal(noPath), is(20480L));
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
    void testFindDrmInfoUnmatchedSlotReturnsEmpty(@TempDir Path tempDir) throws IOException {
        // card0 has a driver, so it is a candidate for the first-with-driver fallback. Because a slot was supplied
        // and no card claimed it, this card has no DRM node (e.g. a GPU bound to vfio-pci) and must report nothing
        // rather than inherit card0's path, driver and metrics.
        createCardWithDriver(tempDir, "card0", "amdgpu", "0000:01:00.0");

        Triplet<String, String, String> result = LinuxGraphicsCard.findDrmInfo("99:00.0", tempDir.toString());
        assertThat(result.getA(), is(""));
        assertThat(result.getB(), is(""));
        assertThat(result.getC(), is(""));
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

    // Fixture: lspci -vnnmm output with one VGA card
    private static final List<String> LSPCI_VNNMM = Arrays.asList("Slot:\t01:00.0",
            "Class:\tVGA compatible controller [0300]", "Vendor:\tNVIDIA Corporation [10de]",
            "Device:\tGA102 [GeForce RTX 3090] [2204]", "SVendor:\tASUS [1043]",
            "SDevice:\tGA102 [GeForce RTX 3090] [8687]", "Rev:\ta1", "");

    private static final Function<LinuxGraphicsCard.Attrs, GraphicsCard> STUB_FACTORY = LinuxGraphicsCardTest::stubGraphicsCard;

    private static GraphicsCard stubGraphicsCard(LinuxGraphicsCard.Attrs attrs) {
        return new StubGraphicsCard(attrs.getName(), attrs.getDeviceId(), attrs.getVendor(), attrs.getVersionInfo(),
                attrs.getVram(), attrs.getDrmDevicePath(), attrs.getDriverName(), attrs.getPciBusId());
    }

    // No-op lookups for pure parsing tests
    private static final ToLongFunction<String> NO_VRAM = LinuxGraphicsCardTest::noVram;
    private static final Function<@Nullable String, Triplet<String, String, String>> NO_DRM = LinuxGraphicsCardTest::noDrm;

    private static long noVram(String slot) {
        return 0L;
    }

    private static Triplet<String, String, String> noDrm(@Nullable String slot) {
        return new Triplet<>("", "", "");
    }

    @Test
    void testGetGraphicsCardsFromLspciSingleCard() {
        List<GraphicsCard> cards = LinuxGraphicsCard.getGraphicsCardsFromLspci(LSPCI_VNNMM, STUB_FACTORY, NO_VRAM,
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

    @Test
    void testGetGraphicsCardsFromLspciPassesEachSlotToLookups() {
        List<String> twoCards = Arrays.asList("Slot:\t00:02.0", "Class:\tVGA compatible controller [0300]",
                "Vendor:\tIntel Corporation [8086]", "Device:\tUHD Graphics 630 [3E92]", "Rev:\t00", "",
                "Slot:\t01:00.0", "Class:\tVGA compatible controller [0300]", "Vendor:\tNVIDIA Corporation [10de]",
                "Device:\tGA102 [GeForce RTX 3090] [2204]", "Rev:\ta1", "");

        List<String> drmSlots = new ArrayList<>();
        List<String> vramSlots = new ArrayList<>();
        List<GraphicsCard> cards = LinuxGraphicsCard.getGraphicsCardsFromLspci(twoCards, STUB_FACTORY, slot -> {
            vramSlots.add(slot);
            return 1024L;
        }, slot -> {
            drmSlots.add(slot);
            return new Triplet<>("/sys/class/drm/card0/device", "driver", slot);
        });

        // Each card must be correlated using its own PCI slot, not null and not a shared one
        assertThat(drmSlots, is(Arrays.asList("00:02.0", "01:00.0")));
        assertThat(vramSlots, is(Arrays.asList("00:02.0", "01:00.0")));
        assertThat(((LinuxGraphicsCard) cards.get(0)).getPciBusId(), is("00:02.0"));
        assertThat(((LinuxGraphicsCard) cards.get(1)).getPciBusId(), is("01:00.0"));
        assertThat(cards.get(0).getVRam(), is(1024L));
    }

    @Test
    void testGetGraphicsCardsFromLspciSkipsNonGraphicsSlots() {
        // PCI class 0x0000 renders as "Non-VGA unclassified device", which contains "VGA"; 0x1180 is a signal
        // processing controller. Neither is a graphics card and neither may reach the DRM lookup.
        List<String> mixed = Arrays.asList("Slot:\t00:13.0", "Class:\tNon-VGA unclassified device [0000]",
                "Vendor:\tIntel Corporation [8086]",
                "Device:\t100 Series/C230 Series Chipset Family Integrated Sensor Hub [a135]", "Rev:\t31", "",
                "Slot:\t00:11.0", "Class:\tSignal processing controller [1180]", "Vendor:\tIntel Corporation [8086]",
                "Device:\tIntegrated Sensor Hub [a135]", "Rev:\t31", "", "Slot:\t01:00.0",
                "Class:\tVGA compatible controller [0300]", "Vendor:\tNVIDIA Corporation [10de]",
                "Device:\tGP107GL [Quadro P400] [1cb3]", "Rev:\ta1", "");
        List<String> drmSlots = new ArrayList<>();
        List<GraphicsCard> cards = LinuxGraphicsCard.getGraphicsCardsFromLspci(mixed, STUB_FACTORY, NO_VRAM, slot -> {
            drmSlots.add(slot);
            return new Triplet<>("", "", "");
        });
        assertThat(cards.size(), is(1));
        assertThat(cards.get(0).getDeviceId(), is("0x1cb3"));
        assertThat(drmSlots, is(Collections.singletonList("01:00.0")));
    }

    @Test
    void testIsDisplayClass() {
        assertThat(LinuxGraphicsCard.isDisplayClass("VGA compatible controller [0300]"), is(true));
        assertThat(LinuxGraphicsCard.isDisplayClass("XGA compatible controller [0301]"), is(true));
        assertThat(LinuxGraphicsCard.isDisplayClass("3D controller [0302]"), is(true));
        assertThat(LinuxGraphicsCard.isDisplayClass("Display controller [0380]"), is(true));
        // Contains "VGA" but is PCI class 0x0000, not a graphics card
        assertThat(LinuxGraphicsCard.isDisplayClass("Non-VGA unclassified device [0000]"), is(false));
        assertThat(LinuxGraphicsCard.isDisplayClass("Signal processing controller [1180]"), is(false));
        assertThat(LinuxGraphicsCard.isDisplayClass("Host bridge [0600]"), is(false));
        // Without numeric class codes, fall back to the class name
        assertThat(LinuxGraphicsCard.isDisplayClass("VGA compatible controller"), is(true));
        assertThat(LinuxGraphicsCard.isDisplayClass("3D controller"), is(true));
        assertThat(LinuxGraphicsCard.isDisplayClass("Non-VGA unclassified device"), is(false));
        // A bracket group too short to hold a base class falls through to the name check
        assertThat(LinuxGraphicsCard.isDisplayClass("Bogus controller [3]"), is(false));
    }

    @Test
    void testGetGraphicsCardsFromLspciNewRecordClearsPreviousCardState() {
        // The second card omits Vendor and Rev, so any value it reports for them would be leaked from the first card
        List<String> lspci = Arrays.asList("Slot:\t01:00.0", "Class:\tVGA compatible controller [0300]",
                "Vendor:\tNVIDIA Corporation [10de]", "Device:\tGP107GL [Quadro P400] [1cb3]", "Rev:\ta1", "",
                "Slot:\t00:02.0", "Class:\tVGA compatible controller [0300]", "Device:\tASPEED Graphics Family [2000]",
                "");
        List<String> drmSlots = new ArrayList<>();
        List<GraphicsCard> cards = LinuxGraphicsCard.getGraphicsCardsFromLspci(lspci, STUB_FACTORY, NO_VRAM, slot -> {
            drmSlots.add(slot);
            return new Triplet<>("", "", slot);
        });
        assertThat(cards.size(), is(2));
        assertThat(cards.get(0).getName(), is("GP107GL [Quadro P400]"));
        assertThat(cards.get(0).getVendor(), is("NVIDIA Corporation (0x10de)"));
        assertThat(cards.get(0).getVersionInfo(), is("Rev:\ta1"));
        GraphicsCard second = cards.get(1);
        assertThat(second.getName(), is("ASPEED Graphics Family"));
        assertThat(second.getDeviceId(), is("0x2000"));
        assertThat(second.getVendor(), is(Constants.UNKNOWN));
        assertThat(second.getVersionInfo(), is(Constants.UNKNOWN));
        assertThat(drmSlots, is(Arrays.asList("01:00.0", "00:02.0")));
    }

    @Test
    void testGetGraphicsCardsFromLspciValuelessClassLine() {
        // A "Class:" line with no value must not be treated as a display controller
        List<String> lspci = Arrays.asList("Slot:\t01:00.0", "Class:", "Vendor:\tNVIDIA Corporation [10de]",
                "Device:\tGP107GL [Quadro P400] [1cb3]", "");
        assertThat(LinuxGraphicsCard.getGraphicsCardsFromLspci(lspci, STUB_FACTORY, NO_VRAM, NO_DRM), is(empty()));
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

    @Test
    void testGetGraphicsCardsFromLspciVendorWithoutBracket() {
        // A Vendor line whose value has no "[hex]" id does not parse as machine-readable; the raw text is used
        List<String> lspci = Arrays.asList("Slot:\t01:00.0", "Class:\tVGA compatible controller [0300]",
                "Vendor:\tRedHat, Inc.", "Device:\tVirtio GPU [1050]", "");
        List<GraphicsCard> cards = LinuxGraphicsCard.getGraphicsCardsFromLspci(lspci, STUB_FACTORY, NO_VRAM, NO_DRM);
        assertThat(cards.size(), is(1));
        assertThat(cards.get(0).getVendor(), is("RedHat, Inc."));
        assertThat(cards.get(0).getName(), is("Virtio GPU"));
    }

    @Test
    void testGetGraphicsCardsFromLspciNoTrailingBlankLine() {
        // Output that ends mid-card (no terminating blank line) still flushes the last card
        List<String> lspci = Arrays.asList("Slot:\t01:00.0", "Class:\tVGA compatible controller [0300]",
                "Vendor:\tNVIDIA Corporation [10de]", "Device:\tGA102 [GeForce RTX 3090] [2204]", "Rev:\ta1");
        List<GraphicsCard> cards = LinuxGraphicsCard.getGraphicsCardsFromLspci(lspci, STUB_FACTORY, NO_VRAM, NO_DRM);
        assertThat(cards.size(), is(1));
        assertThat(cards.get(0).getName(), is("GA102 [GeForce RTX 3090]"));
        assertThat(cards.get(0).getVersionInfo(), is("Rev:\ta1"));
    }

    // -------------------------------------------------------------------------
    // getGraphicsCardsFromLshw parsing
    // -------------------------------------------------------------------------

    // Fixture: `lshw -C display` output (real structure) with two display nodes. The second card has no bus info so
    // its DRM lookup is with a null slot; resources memory ranges drive the VRAM total.
    private static final List<String> LSHW = Arrays.asList("  *-display",
            "       description: VGA compatible controller", "       product: GA102 [GeForce RTX 3090]",
            "       vendor: NVIDIA Corporation", "       physical id: 0", "       bus info: pci@0000:01:00.0",
            "       version: a1", "       width: 64 bits", "       clock: 33MHz",
            "       capabilities: pm msi pciexpress vga_controller bus_master cap_list rom",
            "       configuration: driver=nvidia latency=0",
            "       resources: irq:178 memory:f6000000-f6ffffff memory:e0000000-efffffff", "  *-display",
            "       description: VGA compatible controller", "       product: UHD Graphics 630",
            "       vendor: Intel Corporation", "       physical id: 2",
            "       resources: irq:24 memory:db000000-dbffffff");

    @Test
    void testGetGraphicsCardsFromLshwTwoCards() {
        List<GraphicsCard> cards = LinuxGraphicsCard.getGraphicsCardsFromLshw(LSHW, STUB_FACTORY, NO_DRM);
        assertThat(cards.size(), is(2));

        GraphicsCard nvidia = cards.get(0);
        assertThat(nvidia.getName(), is("GA102 [GeForce RTX 3090]"));
        assertThat(nvidia.getVendor(), is("NVIDIA Corporation"));
        assertThat(nvidia.getVersionInfo(), is("version: a1"));
        // 16 MiB (f6000000-f6ffffff) + 256 MiB (e0000000-efffffff)
        assertThat(nvidia.getVRam(), is(16L * 1024 * 1024 + 256L * 1024 * 1024));

        GraphicsCard intel = cards.get(1);
        assertThat(intel.getName(), is("UHD Graphics 630"));
        assertThat(intel.getVendor(), is("Intel Corporation"));
        // No version line -> UNKNOWN version info; single 16 MiB memory range
        assertThat(intel.getVersionInfo(), is(Constants.UNKNOWN));
        assertThat(intel.getVRam(), is(16L * 1024 * 1024));
    }

    @Test
    void testGetGraphicsCardsFromLshwEmpty() {
        assertThat(LinuxGraphicsCard.getGraphicsCardsFromLshw(Collections.emptyList(), STUB_FACTORY, NO_DRM),
                is(empty()));
    }
}
