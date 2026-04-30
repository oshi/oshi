/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.gpu.DxgiAdapterInfo;
import oshi.driver.common.windows.gpu.DxgiUtil;
import oshi.driver.common.windows.perfmon.GpuInformation.GpuAdapterMemoryProperty;
import oshi.driver.common.windows.wmi.LhmSensor.LhmHardwareProperty;
import oshi.driver.common.windows.wmi.Win32VideoController.VideoControllerProperty;
import oshi.driver.windows.perfmon.GpuInformationFFM;
import oshi.driver.windows.wmi.LhmSensorFFM;
import oshi.driver.windows.wmi.Win32VideoControllerFFM;
import oshi.ffm.util.platform.windows.Advapi32UtilFFM;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiUtilFFM;
import oshi.ffm.windows.DxgiFFM;
import oshi.ffm.windows.WinRegFFM;
import oshi.hardware.GpuStats;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Graphics Card obtained from the Windows registry using FFM, with VRAM sourced from DXGI.
 */
@ThreadSafe
final class WindowsGraphicsCardFFM extends AbstractGraphicsCard {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsGraphicsCardFFM.class);

    private static final MemorySegment HKLM = MemorySegment.ofAddress(WinRegFFM.HKEY_LOCAL_MACHINE);

    private static final String ADAPTER_STRING = "HardwareInformation.AdapterString";
    private static final String DRIVER_DESC = "DriverDesc";
    private static final String DRIVER_VERSION = "DriverVersion";
    private static final String VENDOR = "ProviderName";
    private static final String QW_MEMORY_SIZE = "HardwareInformation.qwMemorySize";
    private static final String MATCHING_DEVICE_ID = "MatchingDeviceId";
    private static final String LOCATION_INFORMATION = "LocationInformation";
    private static final String DISPLAY_DEVICES_REGISTRY_PATH = "SYSTEM\\CurrentControlSet\\Control\\Class\\{4d36e968-e325-11ce-bfc1-08002be10318}\\";

    private final String luidPrefix;
    private final String lhmParent;
    private final int pciBusNumber;
    private final String pciBusId;

    WindowsGraphicsCardFFM(String name, String deviceId, String vendor, String versionInfo, long vram,
            String luidPrefix, String lhmParent, int pciBusNumber, String pciBusId) {
        super(name, deviceId, vendor, versionInfo, vram);
        this.luidPrefix = luidPrefix;
        this.lhmParent = lhmParent;
        this.pciBusNumber = pciBusNumber;
        this.pciBusId = pciBusId;
    }

    @Override
    public GpuStats createStatsSession() {
        return new WindowsGpuStatsFFM(luidPrefix, lhmParent, pciBusNumber, pciBusId, getName());
    }

    public static List<GraphicsCard> getGraphicsCards() {
        List<DxgiAdapterInfo> dxgiAdapters = DxgiFFM.queryAdapters();
        boolean dxgiAvailable = !dxgiAdapters.isEmpty();
        List<DxgiAdapterInfo> remainingDxgi = new ArrayList<>(dxgiAdapters);

        Map<String, String> lhmParentMap = buildLhmParentMap();

        TreeMap<Integer, GraphicsCard> dxgiOrdered = new TreeMap<>();
        List<GraphicsCard> cardList = new ArrayList<>();

        try {
            String[] keys = Advapi32UtilFFM.registryGetKeys(HKLM, DISPLAY_DEVICES_REGISTRY_PATH, 0);
            int index = 1;
            for (String key : keys) {
                if (!key.startsWith("0")) {
                    continue;
                }
                try {
                    String fullKey = DISPLAY_DEVICES_REGISTRY_PATH + key;
                    if (!Advapi32UtilFFM.registryValueExists(HKLM, fullKey, ADAPTER_STRING)) {
                        continue;
                    }

                    String name = registryString(fullKey, DRIVER_DESC);
                    String deviceId = "VideoController" + index++;
                    String vendor = registryString(fullKey, VENDOR);
                    String versionInfo = registryString(fullKey, DRIVER_VERSION);

                    String matchingDeviceId = registryString(fullKey, MATCHING_DEVICE_ID);
                    Pair<Integer, Integer> pciIds = ParseUtil.parseDeviceIdToVendorProductIds(matchingDeviceId);
                    int pciVendorId = pciIds == null ? 0 : pciIds.getA();
                    int pciDeviceId = pciIds == null ? 0 : pciIds.getB();

                    long vram = -1L;
                    int dxgiIndex = -1;
                    String luidPrefix = "";
                    int pciBusNumber = -1;
                    String pciBusId = "";
                    String locationInfo = registryString(fullKey, LOCATION_INFORMATION);
                    DxgiAdapterInfo dxgiMatch = DxgiUtil.findMatch(remainingDxgi, pciVendorId, pciDeviceId, name);
                    if (dxgiMatch != null) {
                        vram = dxgiMatch.getDedicatedVideoMemory();
                        dxgiIndex = dxgiAdapters.indexOf(dxgiMatch);
                        luidPrefix = buildLuidPrefix(dxgiMatch);
                        pciBusNumber = DxgiUtil.parsePciBusNumber(locationInfo);
                        pciBusId = DxgiUtil.buildPciBusId(locationInfo);
                    } else if (dxgiAvailable) {
                        continue; // ghost device
                    }

                    if (vram < 0 && Advapi32UtilFFM.registryValueExists(HKLM, fullKey, QW_MEMORY_SIZE)) {
                        Object regValue = Advapi32UtilFFM.registryGetValue(HKLM, fullKey, QW_MEMORY_SIZE);
                        vram = DxgiUtil.registryValueToVram(regValue);
                    }
                    if (vram < 0) {
                        vram = 0L;
                    }

                    String lhmParent = lhmParentMap.getOrDefault(DxgiUtil.normalizeName(Util.isBlank(name) ? "" : name),
                            "");

                    GraphicsCard card = new WindowsGraphicsCardFFM(Util.isBlank(name) ? Constants.UNKNOWN : name,
                            Util.isBlank(deviceId) ? Constants.UNKNOWN : deviceId,
                            Util.isBlank(vendor) ? Constants.UNKNOWN : vendor,
                            Util.isBlank(versionInfo) ? Constants.UNKNOWN : versionInfo, vram, luidPrefix, lhmParent,
                            pciBusNumber, pciBusId);
                    if (dxgiMatch != null) {
                        remainingDxgi.remove(dxgiMatch);
                    }
                    if (dxgiIndex >= 0) {
                        dxgiOrdered.put(dxgiIndex, card);
                    } else {
                        cardList.add(card);
                    }
                } catch (Throwable t) {
                    LOG.debug("Error reading graphics card registry key {}: {}", key, t.getMessage());
                }
            }
        } catch (Throwable t) {
            LOG.debug("Failed to enumerate display device registry keys: {}", t.getMessage());
        }

        List<GraphicsCard> result = new ArrayList<>(dxgiOrdered.values());
        result.addAll(cardList);

        if (result.isEmpty()) {
            return getGraphicsCardsFromWmi(dxgiAdapters, remainingDxgi, lhmParentMap);
        }
        return result;
    }

    private static List<GraphicsCard> getGraphicsCardsFromWmi(List<DxgiAdapterInfo> dxgiAdapters,
            List<DxgiAdapterInfo> remainingDxgi, Map<String, String> lhmParentMap) {
        boolean dxgiAvailable = !dxgiAdapters.isEmpty();
        List<DxgiAdapterInfo> working = new ArrayList<>(remainingDxgi);
        TreeMap<Integer, GraphicsCard> dxgiOrdered = new TreeMap<>();
        List<GraphicsCard> cardList = new ArrayList<>();

        WmiResult<VideoControllerProperty> cards = Win32VideoControllerFFM.queryVideoController();
        for (int index = 0; index < cards.getResultCount(); index++) {
            if (dxgiAvailable
                    && WmiUtilFFM.getUint32(cards, VideoControllerProperty.CONFIGMANAGERERRORCODE, index) != 0) {
                continue;
            }
            String name = WmiUtilFFM.getString(cards, VideoControllerProperty.NAME, index);
            Triplet<String, String, String> idPair = ParseUtil.parseDeviceIdToVendorProductSerial(
                    WmiUtilFFM.getString(cards, VideoControllerProperty.PNPDEVICEID, index));
            String deviceId = idPair == null ? Constants.UNKNOWN : idPair.getB();
            String vendor = WmiUtilFFM.getString(cards, VideoControllerProperty.ADAPTERCOMPATIBILITY, index);
            if (idPair != null) {
                if (Util.isBlank(vendor)) {
                    deviceId = idPair.getA();
                } else {
                    vendor = vendor + " (" + idPair.getA() + ")";
                }
            }
            String versionInfo = WmiUtilFFM.getString(cards, VideoControllerProperty.DRIVERVERSION, index);
            if (!Util.isBlank(versionInfo)) {
                versionInfo = "DriverVersion=" + versionInfo;
            } else {
                versionInfo = Constants.UNKNOWN;
            }

            Pair<Integer, Integer> pciIds = ParseUtil.parseDeviceIdToVendorProductIds(
                    WmiUtilFFM.getString(cards, VideoControllerProperty.PNPDEVICEID, index));
            int pciVendorId = pciIds == null ? 0 : pciIds.getA();
            int pciDeviceId = pciIds == null ? 0 : pciIds.getB();
            DxgiAdapterInfo dxgiMatch = DxgiUtil.findMatch(working, pciVendorId, pciDeviceId, name);
            long vram;
            int dxgiIndex = -1;
            String luidPrefix = "";
            int pciBusNumber = -1;
            String pciBusId = "";
            if (dxgiMatch != null) {
                vram = dxgiMatch.getDedicatedVideoMemory();
                dxgiIndex = dxgiAdapters.indexOf(dxgiMatch);
                luidPrefix = DxgiUtil.buildLuidPrefix(dxgiMatch);
            } else {
                vram = WmiUtilFFM.getUint32asLong(cards, VideoControllerProperty.ADAPTERRAM, index);
            }
            String lhmParent = lhmParentMap.getOrDefault(DxgiUtil.normalizeName(Util.isBlank(name) ? "" : name), "");
            GraphicsCard card = new WindowsGraphicsCardFFM(Util.isBlank(name) ? Constants.UNKNOWN : name, deviceId,
                    Util.isBlank(vendor) ? Constants.UNKNOWN : vendor, versionInfo, vram, luidPrefix, lhmParent,
                    pciBusNumber, pciBusId);
            if (dxgiMatch != null) {
                working.remove(dxgiMatch);
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

    private static Map<String, String> buildLhmParentMap() {
        Map<String, String> map = new HashMap<>();
        try {
            WmiResult<LhmHardwareProperty> hw = LhmSensorFFM.queryGpuHardware();
            for (int i = 0; i < hw.getResultCount(); i++) {
                String identifier = WmiUtilFFM.getString(hw, LhmHardwareProperty.IDENTIFIER, i);
                String hwName = WmiUtilFFM.getString(hw, LhmHardwareProperty.NAME, i);
                if (!identifier.isEmpty() && !hwName.isEmpty()) {
                    String norm = DxgiUtil.normalizeName(hwName);
                    if (map.containsKey(norm)) {
                        map.put(norm, "");
                    } else {
                        map.put(norm, identifier);
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("LHM GPU hardware query failed (LHM may not be running): {}", e.getMessage());
        }
        return map;
    }

    private static String buildLuidPrefix(DxgiAdapterInfo adapter) {
        String prefix = DxgiUtil.buildLuidPrefix(adapter);
        if (!prefix.isEmpty()) {
            return prefix;
        }
        return buildLuidPrefixFromPdh();
    }

    private static String buildLuidPrefixFromPdh() {
        Pair<List<String>, Map<GpuAdapterMemoryProperty, List<Long>>> adapterData = GpuInformationFFM
                .queryGpuAdapterMemoryCounters();
        List<String> instances = adapterData.getA();
        if (instances.isEmpty()) {
            return "";
        }
        if (instances.size() == 1) {
            return instances.get(0);
        }
        LOG.debug("Multiple GPU Adapter Memory instances found ({}); LUID matching not yet implemented for multi-GPU",
                instances.size());
        return "";
    }

    private static String registryString(String keyPath, String valueName) {
        Object val = Advapi32UtilFFM.registryGetValue(HKLM, keyPath, valueName);
        if (val instanceof String) {
            return (String) val;
        }
        return "";
    }
}
