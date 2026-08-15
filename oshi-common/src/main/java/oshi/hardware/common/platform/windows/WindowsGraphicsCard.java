/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.gpu.DxgiAdapterInfo;
import oshi.driver.common.windows.gpu.DxgiUtil;
import oshi.driver.common.windows.wmi.Win32VideoController.VideoControllerProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Common logic for a Windows {@link GraphicsCard}, including the WMI fallback enumeration used when the display device
 * registry keys yield nothing.
 * <p>
 * Both bindings enumerate cards identically once their driver has returned the WMI rows; only which driver is called
 * differs, so {@link #buildFromWmi} takes the query result and a factory rather than performing the query itself.
 */
@ThreadSafe
public abstract class WindowsGraphicsCard extends AbstractGraphicsCard {

    /** PDH instance prefix for this adapter's LUID, e.g. {@code luid_0x00000000_0x0001234_phys_0}. */
    private final String luidPrefix;

    /** LHM hardware identifier for this GPU, e.g. {@code /gpu-nvidia/0}. Empty if LHM is not available. */
    private final String lhmParent;

    /** PCI bus number from DXGI, used to correlate with ADL. -1 if unknown. */
    private final int pciBusNumber;

    /** PCI bus ID string for NVML correlation, e.g. {@code 0000:01:00.0}. Empty if unknown. */
    private final String pciBusId;

    /**
     * Creates a Windows graphics card.
     *
     * @param name         the card name
     * @param deviceId     the device ID
     * @param vendor       the vendor
     * @param versionInfo  the version info
     * @param vram         the VRAM in bytes
     * @param luidPrefix   PDH LUID instance prefix for this adapter, or an empty string if unknown
     * @param lhmParent    LHM hardware identifier for this GPU, or an empty string if unavailable
     * @param pciBusNumber PCI bus number for ADL correlation, or -1 if unknown
     * @param pciBusId     PCI bus ID string for NVML correlation, or an empty string if unknown
     */
    protected WindowsGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram,
            String luidPrefix, String lhmParent, int pciBusNumber, String pciBusId) {
        super(name, deviceId, vendor, versionInfo, vram);
        this.luidPrefix = luidPrefix;
        this.lhmParent = lhmParent;
        this.pciBusNumber = pciBusNumber;
        this.pciBusId = pciBusId;
    }

    /**
     * Returns this adapter's PDH instance prefix, used to filter GPU Engine and GPU Adapter Memory counters.
     *
     * @return the PDH LUID instance prefix, or an empty string if unknown
     */
    protected String getLuidPrefix() {
        return luidPrefix;
    }

    /**
     * Returns this GPU's LibreHardwareMonitor hardware identifier, used to select its LHM sensors.
     *
     * @return the LHM hardware identifier, or an empty string if unavailable
     */
    protected String getLhmParent() {
        return lhmParent;
    }

    /**
     * Returns this adapter's PCI bus number, used to correlate with ADL.
     *
     * @return the PCI bus number, or -1 if unknown
     */
    protected int getPciBusNumber() {
        return pciBusNumber;
    }

    /**
     * Returns this adapter's PCI bus ID, used to correlate with NVML.
     *
     * @return the PCI bus ID string, or an empty string if unknown
     */
    protected String getPciBusId() {
        return pciBusId;
    }

    /**
     * Creates a backend's concrete card type.
     */
    @FunctionalInterface
    public interface CardFactory {
        /**
         * @param name         the card name
         * @param deviceId     the device ID
         * @param vendor       the vendor
         * @param versionInfo  the version info
         * @param vram         the VRAM in bytes
         * @param luidPrefix   PDH LUID instance prefix, or an empty string if unknown
         * @param lhmParent    LHM hardware identifier, or an empty string if unavailable
         * @param pciBusNumber PCI bus number, or -1 if unknown
         * @param pciBusId     PCI bus ID string, or an empty string if unknown
         * @return the card
         */
        GraphicsCard create(String name, String deviceId, String vendor, String versionInfo, long vram,
                String luidPrefix, String lhmParent, int pciBusNumber, String pciBusId);
    }

    /**
     * Builds the card list from {@code Win32_VideoController} rows, used as a fallback when the display device registry
     * keys yield nothing.
     * <p>
     * The returned list is ordered to match DXGI enumeration order, which places the primary desktop adapter first,
     * with any unmatched cards appended.
     *
     * @param dxgiAdapters      all DXGI adapters, retained unmutated as the stable reference for ordering lookups
     * @param lhmParentMap      normalized GPU name to LHM parent identifier
     * @param cards             the {@code Win32_VideoController} query result
     * @param luidPrefixBuilder derives the PDH LUID prefix for a matched adapter, falling back to a PDH instance lookup
     * @param factory           creates the backend's card type
     * @return the cards, never null
     */
    protected static List<GraphicsCard> buildFromWmi(List<DxgiAdapterInfo> dxgiAdapters,
            Map<String, String> lhmParentMap, WmiResult<VideoControllerProperty> cards,
            Function<DxgiAdapterInfo, String> luidPrefixBuilder, CardFactory factory) {
        boolean dxgiAvailable = !dxgiAdapters.isEmpty();
        // dxgiAdapters is not mutated; remainingDxgi is the working copy consumed during matching, and dxgiAdapters is
        // retained as the stable reference for indexOf ordering lookups.
        List<DxgiAdapterInfo> remainingDxgi = new ArrayList<>(dxgiAdapters);
        TreeMap<Integer, GraphicsCard> dxgiOrdered = new TreeMap<>();
        List<GraphicsCard> cardList = new ArrayList<>();

        for (int index = 0; index < cards.getResultCount(); index++) {
            // ConfigManagerErrorCode 0 = working properly; non-zero = disabled/error (ghost device).
            // When DXGI is unavailable, keep all entries for maximum compatibility.
            if (dxgiAvailable && WmiUtil.getUint32(cards, VideoControllerProperty.CONFIGMANAGERERRORCODE, index) != 0) {
                continue;
            }
            String name = WmiUtil.getString(cards, VideoControllerProperty.NAME, index);
            Triplet<String, String, String> idPair = ParseUtil.parseDeviceIdToVendorProductSerial(
                    WmiUtil.getString(cards, VideoControllerProperty.PNPDEVICEID, index));
            String deviceId = idPair == null ? Constants.UNKNOWN : idPair.getB();
            String vendor = WmiUtil.getString(cards, VideoControllerProperty.ADAPTERCOMPATIBILITY, index);
            if (idPair != null) {
                if (Util.isBlank(vendor)) {
                    deviceId = idPair.getA();
                } else {
                    vendor = vendor + " (" + idPair.getA() + ")";
                }
            }
            String versionInfo = WmiUtil.getString(cards, VideoControllerProperty.DRIVERVERSION, index);
            versionInfo = Util.isBlank(versionInfo) ? Constants.UNKNOWN : "DriverVersion=" + versionInfo;

            // Prefer DXGI DedicatedVideoMemory when a match can be found via the PCI IDs extracted from PNPDEVICEID.
            // Fall back to WMI AdapterRAM (32-bit capped) only when no DXGI match is available.
            Pair<Integer, Integer> pciIds = ParseUtil.parseDeviceIdToVendorProductIds(
                    WmiUtil.getString(cards, VideoControllerProperty.PNPDEVICEID, index));
            int pciVendorId = pciIds == null ? 0 : pciIds.getA();
            int pciDeviceId = pciIds == null ? 0 : pciIds.getB();
            DxgiAdapterInfo dxgiMatch = DxgiUtil.findMatch(remainingDxgi, pciVendorId, pciDeviceId, name);

            long vram;
            int dxgiIndex = -1;
            String luidPrefix = "";
            if (dxgiMatch == null) {
                vram = WmiUtil.getUint32asLong(cards, VideoControllerProperty.ADAPTERRAM, index);
            } else {
                vram = dxgiMatch.getDedicatedVideoMemory();
                dxgiIndex = dxgiAdapters.indexOf(dxgiMatch);
                luidPrefix = luidPrefixBuilder.apply(dxgiMatch);
            }
            String lhmParent = lhmParentMap.getOrDefault(DxgiUtil.normalizeName(Util.isBlank(name) ? "" : name), "");

            // pciBusNumber and pciBusId are not available via WMI; ADL and NVML correlation are skipped for cards
            // enumerated through this path.
            GraphicsCard card = factory.create(ParseUtil.getStringValueOrUnknown(name), deviceId,
                    ParseUtil.getStringValueOrUnknown(vendor), versionInfo, vram, luidPrefix, lhmParent, -1, "");

            // Remove dxgiMatch only after the card is successfully constructed, matching the registry path's defensive
            // pattern so that a failure during construction leaves the match available for subsequent entries.
            if (dxgiMatch != null) {
                remainingDxgi.remove(dxgiMatch);
            }
            if (dxgiIndex >= 0) {
                dxgiOrdered.put(dxgiIndex, card);
            } else {
                cardList.add(card);
            }
        }
        List<GraphicsCard> result = new ArrayList<>(dxgiOrdered.values());
        result.addAll(cardList);
        return result;
    }
}
