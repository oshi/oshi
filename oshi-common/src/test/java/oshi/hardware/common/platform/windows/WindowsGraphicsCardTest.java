/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_STRING;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_UINT32;
import static oshi.driver.common.windows.wmi.WmiConstants.VT_BSTR;
import static oshi.driver.common.windows.wmi.WmiConstants.VT_I4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.gpu.DxgiAdapterInfo;
import oshi.driver.common.windows.wmi.Win32VideoController.VideoControllerProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.hardware.GraphicsCard;
import oshi.util.Constants;

/**
 * Tests the shared Windows graphics card enumeration without Windows, by stubbing the WMI result. This path is the
 * fallback used only when the display device registry keys yield nothing, so it is otherwise hard to reach on real
 * hardware.
 */
class WindowsGraphicsCardTest {

    /** A concrete card so the factory has something to build. */
    private static final class TestCard extends WindowsGraphicsCard {
        private TestCard(String name, String deviceId, String vendor, String versionInfo, long vram, String luidPrefix,
                String lhmParent, int pciBusNumber, String pciBusId) {
            super(name, deviceId, vendor, versionInfo, vram, luidPrefix, lhmParent, pciBusNumber, pciBusId);
        }

        @Override
        public oshi.hardware.GpuStats createStatsSession() {
            throw new UnsupportedOperationException("not exercised by this test");
        }
    }

    /** One Win32_VideoController row. */
    private static final class Row {
        private final Map<VideoControllerProperty, Object> values = new EnumMap<>(VideoControllerProperty.class);

        private Row set(VideoControllerProperty property, Object value) {
            values.put(property, value);
            return this;
        }
    }

    private static Row row(String name, String pnpDeviceId, String vendor, String driverVersion, int errorCode,
            int adapterRam) {
        return new Row().set(VideoControllerProperty.NAME, name).set(VideoControllerProperty.PNPDEVICEID, pnpDeviceId)
                .set(VideoControllerProperty.ADAPTERCOMPATIBILITY, vendor)
                .set(VideoControllerProperty.DRIVERVERSION, driverVersion)
                .set(VideoControllerProperty.CONFIGMANAGERERRORCODE, errorCode)
                .set(VideoControllerProperty.ADAPTERRAM, adapterRam);
    }

    private static WmiResult<VideoControllerProperty> result(Row... rows) {
        List<Row> list = Arrays.asList(rows);
        return new WmiResult<VideoControllerProperty>() {
            @Override
            public int getResultCount() {
                return list.size();
            }

            @Override
            public @Nullable Object getValue(VideoControllerProperty property, int index) {
                return list.get(index).values.get(property);
            }

            @Override
            public int getVtType(VideoControllerProperty property) {
                return isNumeric(property) ? VT_I4 : VT_BSTR;
            }

            @Override
            public int getCIMType(VideoControllerProperty property) {
                return isNumeric(property) ? CIM_UINT32 : CIM_STRING;
            }

            private boolean isNumeric(VideoControllerProperty property) {
                return property == VideoControllerProperty.CONFIGMANAGERERRORCODE
                        || property == VideoControllerProperty.ADAPTERRAM;
            }
        };
    }

    private static List<GraphicsCard> build(List<DxgiAdapterInfo> adapters, WmiResult<VideoControllerProperty> cards) {
        return WindowsGraphicsCard.buildFromWmi(adapters, Collections.emptyMap(), cards, adapter -> "luid_test",
                TestCard::new);
    }

    private static List<String> names(List<GraphicsCard> cards) {
        return cards.stream().map(GraphicsCard::getName).collect(Collectors.toList());
    }

    @Test
    void testWithoutDxgiEveryRowIsKeptInOrder() {
        // With no DXGI adapters there is nothing to match against, so all rows are kept for maximum compatibility,
        // including ones reporting a non-zero ConfigManagerErrorCode.
        List<GraphicsCard> cards = build(Collections.emptyList(),
                result(row("Card A", "PCI\\VEN_10DE&DEV_1C03", "NVIDIA", "1.2.3", 0, 4096),
                        row("Card B", "PCI\\VEN_8086&DEV_56A0", "Intel", "4.5.6", 22, 2048)));
        assertThat("both rows are kept", names(cards), contains("Card A", "Card B"));
        assertThat("VRAM falls back to AdapterRAM", cards.get(0).getVRam(), is(4096L));
    }

    @Test
    void testGhostDevicesAreSkippedWhenDxgiIsAvailable() {
        // A non-zero ConfigManagerErrorCode marks a disabled or errored device once DXGI can confirm what is present.
        List<DxgiAdapterInfo> adapters = Collections
                .singletonList(new DxgiAdapterInfo("NVIDIA GeForce", 0x10DE, 0x1C03, 8_000_000_000L, 1, 0));
        List<GraphicsCard> cards = build(adapters,
                result(row("Ghost", "PCI\\VEN_8086&DEV_56A0", "Intel", "4.5.6", 22, 2048),
                        row("NVIDIA GeForce", "PCI\\VEN_10DE&DEV_1C03", "NVIDIA", "1.2.3", 0, 2048)));
        assertThat("the ghost row is dropped", names(cards), contains("NVIDIA GeForce"));
        assertThat("DXGI VRAM beats the 32-bit AdapterRAM", cards.get(0).getVRam(), is(8_000_000_000L));
        assertThat("the LUID prefix comes from the supplied builder", ((TestCard) cards.get(0)).getLuidPrefix(),
                is("luid_test"));
    }

    @Test
    void testMatchedCardsAreOrderedByDxgiIndex() {
        // DXGI enumerates the primary desktop adapter first, so the result follows DXGI order, not WMI row order.
        List<DxgiAdapterInfo> adapters = Arrays.asList(new DxgiAdapterInfo("Primary", 0x10DE, 0x1C03, 100L, 1, 0),
                new DxgiAdapterInfo("Secondary", 0x8086, 0x56A0, 200L, 2, 0));
        List<GraphicsCard> cards = build(adapters,
                result(row("Secondary", "PCI\\VEN_8086&DEV_56A0", "Intel", "4.5.6", 0, 1),
                        row("Primary", "PCI\\VEN_10DE&DEV_1C03", "NVIDIA", "1.2.3", 0, 1)));
        assertThat("DXGI order wins over row order", names(cards), contains("Primary", "Secondary"));
    }

    @Test
    void testAnAdapterIsConsumedByTheFirstMatchingRow() {
        // Two rows for the same PCI IDs must not both claim the single DXGI adapter, or the second would inherit the
        // first one's VRAM.
        List<DxgiAdapterInfo> adapters = Collections
                .singletonList(new DxgiAdapterInfo("Shared", 0x10DE, 0x1C03, 9999L, 1, 0));
        List<GraphicsCard> cards = build(adapters,
                result(row("Shared", "PCI\\VEN_10DE&DEV_1C03", "NVIDIA", "1.2.3", 0, 111),
                        row("Shared", "PCI\\VEN_10DE&DEV_1C03", "NVIDIA", "1.2.3", 0, 222)));
        assertThat("both rows are returned", cards.size(), is(2));
        List<Long> vram = cards.stream().map(GraphicsCard::getVRam).collect(Collectors.toList());
        assertThat("only one row got the DXGI VRAM", vram, contains(9999L, 222L));
    }

    @Test
    void testVendorAndDeviceIdDerivation() {
        List<GraphicsCard> cards = build(Collections.emptyList(),
                result(row("Card", "PCI\\VEN_10DE&DEV_1C03&SUBSYS_0&REV_A1\\4&SERIAL&0", "NVIDIA", "1.2.3", 0, 1)));
        GraphicsCard card = cards.get(0);
        assertThat("the vendor keeps its PCI id in parentheses", card.getVendor(), is("NVIDIA (0x10de)"));
        assertThat("the version is prefixed", card.getVersionInfo(), is("DriverVersion=1.2.3"));
    }

    @Test
    void testBlankFieldsFallBackToUnknown() {
        List<GraphicsCard> cards = build(Collections.emptyList(), result(row("", "", "", "", 0, 1)));
        GraphicsCard card = cards.get(0);
        assertThat("a blank name is unknown", card.getName(), is(Constants.UNKNOWN));
        assertThat("a blank vendor is unknown", card.getVendor(), is(Constants.UNKNOWN));
        assertThat("a blank version is unknown", card.getVersionInfo(), is(Constants.UNKNOWN));
    }

    @Test
    void testNoRowsYieldsAnEmptyList() {
        assertThat("no rows means no cards", build(Collections.emptyList(), result()), is(empty()));
    }

    @Test
    void testPciCorrelationFieldsAreUnsetOnThisPath() {
        // The bus number and bus ID come from the registry LocationInformation value, which WMI does not expose, so
        // ADL and NVML correlation are deliberately skipped for cards found this way.
        List<GraphicsCard> cards = build(Collections.emptyList(),
                result(row("Card", "PCI\\VEN_10DE&DEV_1C03", "NVIDIA", "1.2.3", 0, 1)));
        assertThat("bus number is unknown", ((TestCard) cards.get(0)).getPciBusNumber(), is(-1));
        assertThat("bus id is empty", ((TestCard) cards.get(0)).getPciBusId(), is(""));
    }

    @Test
    void testLhmParentIsMatchedOnNormalizedName() {
        Map<String, String> lhm = new HashMap<>();
        lhm.put("nvidia geforce rtx 3080", "/gpu-nvidia/0");
        List<GraphicsCard> cards = new ArrayList<>(WindowsGraphicsCard.buildFromWmi(Collections.emptyList(), lhm,
                result(row("NVIDIA GeForce RTX 3080", "PCI\\VEN_10DE&DEV_1C03", "NVIDIA", "1.2.3", 0, 1)),
                adapter -> "", TestCard::new));
        assertThat("the LHM identifier is attached", ((TestCard) cards.get(0)).getLhmParent(), is("/gpu-nvidia/0"));
    }
}
