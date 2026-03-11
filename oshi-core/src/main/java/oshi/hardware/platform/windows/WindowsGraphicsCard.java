/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinReg;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.DxgiAdapterInfo;
import oshi.driver.windows.perfmon.GpuInformation;
import oshi.driver.windows.perfmon.GpuInformation.GpuAdapterMemoryProperty;
import oshi.driver.windows.perfmon.GpuInformation.GpuEngineProperty;
import oshi.driver.windows.wmi.LhmSensor;
import oshi.driver.windows.wmi.LhmSensor.LhmHardwareProperty;
import oshi.driver.windows.wmi.LhmSensor.LhmSensorProperty;
import oshi.driver.windows.wmi.Win32VideoController;
import oshi.driver.windows.wmi.Win32VideoController.VideoControllerProperty;
import oshi.hardware.GraphicsCard;
import oshi.hardware.GpuTicks;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.hardware.common.DefaultGpuTicks;
import oshi.jna.platform.windows.WindowsDxgi;
import oshi.util.Constants;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.windows.RegistryUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Graphics Card obtained from the Windows registry, with VRAM sourced from DXGI.
 *
 * <p>
 * VRAM detection priority:
 * <ol>
 * <li>{@code DXGI_ADAPTER_DESC.DedicatedVideoMemory}: the authoritative Windows API value, not subject to the 2 GiB cap
 * that affects the 32-bit registry field.</li>
 * <li>{@code HardwareInformation.qwMemorySize} (64-bit registry value): used when DXGI enumeration is unavailable or no
 * adapter match is found.</li>
 * </ol>
 *
 * <p>
 * {@code HardwareInformation.MemorySize} (32-bit) is intentionally not used: Windows writes the sentinel value
 * {@code 0x7FFFF000} (~2 GiB) into this field for GPUs with more than 2 GiB of dedicated VRAM, making it unreliable for
 * modern discrete GPUs.
 *
 * <p>
 * When DXGI is available, ghost adapters (stale registry entries from hardware no longer present) are excluded because
 * {@code IDXGIFactory::EnumAdapters} only enumerates physically present adapters. The returned list is ordered to match
 * DXGI enumeration order, which places the primary desktop adapter first.
 *
 * <p>
 * Dynamic metrics (utilization, VRAM used, shared memory, ticks) are sourced from PDH GPU Engine / GPU Adapter Memory
 * counters (Windows 10 1709+) and optionally from LibreHardwareMonitor WMI sensors when LHM is running.
 */
@ThreadSafe
final class WindowsGraphicsCard extends AbstractGraphicsCard {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsGraphicsCard.class);

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

    // Conversion: LHM reports memory in MB; 1 MB = 1_048_576 bytes
    private static final long MB_TO_BYTES = 1_048_576L;

    public static final String ADAPTER_STRING = "HardwareInformation.AdapterString";
    public static final String DRIVER_DESC = "DriverDesc";
    public static final String DRIVER_VERSION = "DriverVersion";
    public static final String VENDOR = "ProviderName";
    public static final String QW_MEMORY_SIZE = "HardwareInformation.qwMemorySize";
    public static final String MATCHING_DEVICE_ID = "MatchingDeviceId";
    public static final String DISPLAY_DEVICES_REGISTRY_PATH = "SYSTEM\\CurrentControlSet\\Control\\Class\\{4d36e968-e325-11ce-bfc1-08002be10318}\\";

    // PDH instance prefix for this adapter's LUID, e.g. "luid_0x00000000_0x0001234_phys_0"
    // Used to filter GPU Engine and GPU Adapter Memory counter instances.
    private final String luidPrefix;

    // LHM hardware identifier for this GPU, e.g. "/gpu-nvidia/0". Empty if LHM is not available.
    private final String lhmParent;

    /**
     * Constructor for WindowsGraphicsCard
     *
     * @param name        The name
     * @param deviceId    The device ID
     * @param vendor      The vendor
     * @param versionInfo The version info
     * @param vram        The VRAM
     * @param luidPrefix  PDH LUID instance prefix for this adapter, or empty string if unknown
     * @param lhmParent   LHM hardware identifier for this GPU, or empty string if unavailable
     */
    WindowsGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram, String luidPrefix,
            String lhmParent) {
        super(name, deviceId, vendor, versionInfo, vram);
        this.luidPrefix = luidPrefix;
        this.lhmParent = lhmParent;
    }

    /**
     * public method used by {@link oshi.hardware.common.AbstractHardwareAbstractionLayer} to access the graphics cards.
     *
     * <p>
     * When DXGI is available, ghost adapters are excluded and the list is ordered with the primary desktop adapter
     * first. On systems without DXGI, all registry entries are returned in registry key order.
     *
     * @return List of {@link oshi.hardware.platform.windows.WindowsGraphicsCard} objects.
     */
    public static List<GraphicsCard> getGraphicsCards() {
        // Query DXGI once. Fails gracefully to empty list if unavailable.
        List<DxgiAdapterInfo> dxgiAdapters = WindowsDxgi.queryAdapters();
        boolean dxgiAvailable = !dxgiAdapters.isEmpty();
        // Mutable copy for match-and-consume (prevents same adapter matching two registry entries).
        List<DxgiAdapterInfo> remainingDxgi = new ArrayList<>(dxgiAdapters);

        // Build LHM parent map: GPU name (normalized) -> LHM identifier
        Map<String, String> lhmParentMap = buildLhmParentMap();

        // When DXGI is available, collect cards keyed by their DXGI enumeration index so the
        // final list is ordered primary-adapter-first (DXGI guarantees adapter 0 is the primary
        // desktop adapter). Registry entries with no DXGI match are ghost adapters and excluded.
        // When DXGI is unavailable, fall back to simple insertion order.
        TreeMap<Integer, GraphicsCard> dxgiOrdered = new TreeMap<>();
        List<GraphicsCard> cardList = new ArrayList<>();

        int index = 1;
        String[] keys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, DISPLAY_DEVICES_REGISTRY_PATH);
        for (String key : keys) {
            if (!key.startsWith("0")) {
                continue;
            }

            try {
                String fullKey = DISPLAY_DEVICES_REGISTRY_PATH + key;
                if (!Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, fullKey, ADAPTER_STRING)) {
                    continue;
                }

                String name = RegistryUtil.getStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, DRIVER_DESC);
                String deviceId = "VideoController" + index++;
                String vendor = RegistryUtil.getStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, VENDOR);
                String versionInfo = RegistryUtil.getStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, DRIVER_VERSION);

                // Parse PCI vendor/device IDs from MatchingDeviceId (e.g. "pci\ven_8086&dev_56a0&...")
                String matchingDeviceId = RegistryUtil.getStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey,
                        MATCHING_DEVICE_ID);
                Pair<Integer, Integer> pciIds = ParseUtil.parseDeviceIdToVendorProductIds(matchingDeviceId);
                int pciVendorId = pciIds == null ? 0 : pciIds.getA();
                int pciDeviceId = pciIds == null ? 0 : pciIds.getB();

                // Primary: DXGI DedicatedVideoMemory.
                // Track whether a DXGI match was found separately from the vram value, so that a
                // legitimate DedicatedVideoMemory == 0 (e.g. a software/render-only adapter) is
                // preserved and does not trigger the registry fallback.
                long vram = -1L;
                int dxgiIndex = -1;
                String luidPrefix = "";
                DxgiAdapterInfo dxgiMatch = WindowsDxgi.findMatch(remainingDxgi, pciVendorId, pciDeviceId, name);
                if (dxgiMatch != null) {
                    vram = dxgiMatch.getDedicatedVideoMemory();
                    dxgiIndex = dxgiAdapters.indexOf(dxgiMatch);
                    luidPrefix = buildLuidPrefix(dxgiMatch.getVendorId(), dxgiMatch.getDeviceId(), name);
                } else if (dxgiAvailable) {
                    // DXGI is available but this registry entry has no matching adapter:
                    // it is a ghost device (stale driver from hardware no longer present). Skip it.
                    continue;
                }

                // Fallback: 64-bit registry value qwMemorySize, only when DXGI had no match.
                if (vram < 0 && Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, fullKey, QW_MEMORY_SIZE)) {
                    Object regValue = Advapi32Util.registryGetValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, QW_MEMORY_SIZE);
                    vram = registryValueToVram(regValue);
                }

                // Normalise sentinel: if still unresolved report 0.
                if (vram < 0) {
                    vram = 0L;
                }

                // HardwareInformation.MemorySize (32-bit) is intentionally omitted: Windows caps
                // it at 0x7FFFF000 (~2 GiB) for GPUs with more VRAM, making it unreliable.

                String lhmParent = lhmParentMap.getOrDefault(WindowsDxgi.normalizeName(Util.isBlank(name) ? "" : name),
                        "");

                GraphicsCard card = new WindowsGraphicsCard(Util.isBlank(name) ? Constants.UNKNOWN : name,
                        Util.isBlank(deviceId) ? Constants.UNKNOWN : deviceId,
                        Util.isBlank(vendor) ? Constants.UNKNOWN : vendor,
                        Util.isBlank(versionInfo) ? Constants.UNKNOWN : versionInfo, vram, luidPrefix, lhmParent);
                // Remove dxgiMatch from remainingDxgi only after the card is successfully
                // constructed. This ensures that if earlier registry reads in this try block
                // throw a Win32Exception, dxgiMatch remains in remainingDxgi and is still
                // available for subsequent iterations or WMI fallback processing.
                if (dxgiMatch != null) {
                    remainingDxgi.remove(dxgiMatch);
                }
                if (dxgiIndex >= 0) {
                    dxgiOrdered.put(dxgiIndex, card);
                } else {
                    cardList.add(card);
                }
            } catch (Win32Exception e) {
                if (e.getErrorCode() != WinError.ERROR_ACCESS_DENIED) {
                    // Ignore access denied errors, re-throw others
                    throw e;
                }
            }
        }

        // Merge: DXGI-ordered cards first (primary adapter at index 0), then any non-DXGI cards.
        List<GraphicsCard> result = new ArrayList<>(dxgiOrdered.values());
        result.addAll(cardList);

        if (result.isEmpty()) {
            return getGraphicsCardsFromWmi(remainingDxgi, lhmParentMap);
        }
        return result;
    }

    /**
     * Converts a registry value (REG_QWORD as Long, REG_DWORD as Integer, or REG_BINARY as byte[]) to a VRAM size in
     * bytes. REG_BINARY is interpreted as little-endian.
     *
     * @param value the registry value object
     * @return the VRAM size in bytes, or 0 if the value type is unrecognised
     */
    static long registryValueToVram(Object value) {
        return WindowsDxgi.registryValueToVram(value);
    }

    // fall back if something went wrong
    private static List<GraphicsCard> getGraphicsCardsFromWmi(List<DxgiAdapterInfo> dxgiAdapters,
            Map<String, String> lhmParentMap) {
        List<GraphicsCard> cardList = new ArrayList<>();
        if (IS_VISTA_OR_GREATER) {
            boolean dxgiAvailable = !dxgiAdapters.isEmpty();
            // dxgiAdapters is not mutated; remainingDxgi is the working copy consumed during matching.
            // dxgiAdapters is retained as the stable reference for indexOf ordering lookups.
            List<DxgiAdapterInfo> remainingDxgi = new ArrayList<>(dxgiAdapters);
            TreeMap<Integer, GraphicsCard> dxgiOrdered = new TreeMap<>();

            WmiResult<VideoControllerProperty> cards = Win32VideoController.queryVideoController();
            for (int index = 0; index < cards.getResultCount(); index++) {
                // ConfigManagerErrorCode 0 = working properly; non-zero = disabled/error (ghost device).
                // When DXGI is unavailable, keep all entries for maximum compatibility.
                if (dxgiAvailable
                        && WmiUtil.getUint32(cards, VideoControllerProperty.CONFIGMANAGERERRORCODE, index) != 0) {
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
                if (!Util.isBlank(versionInfo)) {
                    versionInfo = "DriverVersion=" + versionInfo;
                } else {
                    versionInfo = Constants.UNKNOWN;
                }
                // Prefer DXGI DedicatedVideoMemory when a match can be found via the PCI IDs
                // extracted from PNPDEVICEID. Fall back to WMI AdapterRAM (32-bit capped) only
                // when no DXGI match is available.
                Pair<Integer, Integer> pciIds = ParseUtil.parseDeviceIdToVendorProductIds(
                        WmiUtil.getString(cards, VideoControllerProperty.PNPDEVICEID, index));
                int pciVendorId = pciIds == null ? 0 : pciIds.getA();
                int pciDeviceId = pciIds == null ? 0 : pciIds.getB();
                DxgiAdapterInfo dxgiMatch = WindowsDxgi.findMatch(remainingDxgi, pciVendorId, pciDeviceId, name);
                long vram;
                int dxgiIndex = -1;
                String luidPrefix = "";
                if (dxgiMatch != null) {
                    vram = dxgiMatch.getDedicatedVideoMemory();
                    dxgiIndex = dxgiAdapters.indexOf(dxgiMatch);
                    luidPrefix = buildLuidPrefix(dxgiMatch.getVendorId(), dxgiMatch.getDeviceId(), name);
                    remainingDxgi.remove(dxgiMatch);
                } else {
                    vram = WmiUtil.getUint32asLong(cards, VideoControllerProperty.ADAPTERRAM, index);
                }
                String lhmParent = lhmParentMap.getOrDefault(WindowsDxgi.normalizeName(Util.isBlank(name) ? "" : name),
                        "");
                GraphicsCard card = new WindowsGraphicsCard(Util.isBlank(name) ? Constants.UNKNOWN : name, deviceId,
                        Util.isBlank(vendor) ? Constants.UNKNOWN : vendor, versionInfo, vram, luidPrefix, lhmParent);
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
        return cardList;
    }

    /**
     * Queries LHM WMI for GPU hardware entries and returns a map from normalized GPU name to LHM parent identifier.
     * Returns an empty map if LHM is not running.
     *
     * @return map of normalized GPU name to LHM hardware identifier
     */
    private static Map<String, String> buildLhmParentMap() {
        Map<String, String> map = new java.util.HashMap<>();
        try {
            WmiResult<LhmHardwareProperty> hw = LhmSensor.queryGpuHardware();
            for (int i = 0; i < hw.getResultCount(); i++) {
                String identifier = WmiUtil.getString(hw, LhmHardwareProperty.IDENTIFIER, i);
                String hwName = WmiUtil.getString(hw, LhmHardwareProperty.NAME, i);
                if (!identifier.isEmpty() && !hwName.isEmpty()) {
                    map.put(WindowsDxgi.normalizeName(hwName), identifier);
                }
            }
        } catch (Exception e) {
            LOG.debug("LHM GPU hardware query failed (LHM may not be running): {}", e.getMessage());
        }
        return map;
    }

    /**
     * Builds the PDH LUID instance prefix used to match GPU Engine and GPU Adapter Memory counter instances to this
     * adapter. The prefix has the form {@code luid_0xHHHHHHHH_0xLLLLLLLL_phys_0} (case-insensitive in PDH).
     *
     * <p>
     * Since DXGI does not expose the LUID directly in {@code DXGI_ADAPTER_DESC}, we enumerate PDH GPU Adapter Memory
     * instances and match by adapter name to discover the LUID. Falls back to an empty string if no match is found.
     *
     * @param vendorId    PCI vendor ID of the adapter
     * @param deviceId    PCI device ID of the adapter
     * @param adapterName adapter name from the registry
     * @return PDH LUID instance prefix string, or empty string if not determinable
     */
    private static String buildLuidPrefix(int vendorId, int deviceId, String adapterName) {
        Pair<List<String>, Map<GpuAdapterMemoryProperty, List<Long>>> adapterData = GpuInformation
                .queryGpuAdapterMemoryCounters();
        List<String> instances = adapterData.getA();
        if (instances.isEmpty()) {
            return "";
        }
        // GPU Adapter Memory instance names have the form: luid_0xHHHH_0xLLLL_phys_0
        // There is one instance per physical adapter. We return the full instance name as the prefix.
        // If there is only one adapter, use it directly.
        if (instances.size() == 1) {
            return instances.get(0);
        }
        // Multiple adapters: we cannot reliably match by name here since GPU Adapter Memory
        // instances do not carry a name. Return empty; callers will get -1 for PDH metrics.
        // A future improvement could correlate via DXGI LUID enumeration.
        LOG.debug("Multiple GPU Adapter Memory instances found ({}); LUID matching not yet implemented for multi-GPU",
                instances.size());
        return "";
    }

    // -------------------------------------------------------------------------
    // Dynamic metric implementations
    // -------------------------------------------------------------------------

    @Override
    public GpuTicks getGpuTicks() {
        // Timestamp in 100ns units: System.nanoTime() / 100
        long timestamp = System.nanoTime() / 100L;

        if (luidPrefix.isEmpty()) {
            return new DefaultGpuTicks(timestamp, 0L);
        }

        Pair<List<String>, Map<GpuEngineProperty, List<Long>>> engineData = GpuInformation.queryGpuEngineCounters();
        List<String> instances = engineData.getA();
        Map<GpuEngineProperty, List<Long>> values = engineData.getB();
        List<Long> runningTimes = values.get(GpuEngineProperty.RUNNING_TIME);

        if (instances.isEmpty() || runningTimes == null) {
            return new DefaultGpuTicks(timestamp, 0L);
        }

        // GPU Engine instance names: pid_<PID>_luid_0xHHHH_0xLLLL_phys_0_eng_<N>_engtype_<TYPE>
        // Group by engine type, sum across all PIDs, then take the max across engine types.
        // GPU engines are parallel pipelines; max represents overall adapter utilization.
        Map<String, Long> engineTypeSums = new java.util.HashMap<>();
        String luidLower = luidPrefix.toLowerCase(Locale.ROOT);
        for (int i = 0; i < instances.size(); i++) {
            String inst = instances.get(i).toLowerCase(Locale.ROOT);
            if (!inst.contains(luidLower)) {
                continue;
            }
            // Extract engine type from instance name suffix "_engtype_<TYPE>"
            int engTypeIdx = inst.lastIndexOf("_engtype_");
            String engType = engTypeIdx >= 0 ? inst.substring(engTypeIdx) : inst;
            long ticks = runningTimes.get(i);
            engineTypeSums.merge(engType, ticks, Long::sum);
        }

        long maxTicks = engineTypeSums.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        return new DefaultGpuTicks(timestamp, maxTicks);
    }

    @Override
    public double getGpuUtilization() {
        if (!lhmParent.isEmpty()) {
            try {
                WmiResult<LhmSensorProperty> sensors = LhmSensor.querySensors(lhmParent, "Load");
                for (int i = 0; i < sensors.getResultCount(); i++) {
                    String sensorName = WmiUtil.getString(sensors, LhmSensorProperty.NAME, i);
                    if ("GPU Core".equals(sensorName)) {
                        return WmiUtil.getFloat(sensors, LhmSensorProperty.VALUE, i);
                    }
                }
            } catch (Exception e) {
                LOG.debug("LHM GPU utilization query failed: {}", e.getMessage());
            }
        }
        return -1d;
    }

    @Override
    public long getVramUsed() {
        // Priority 1: PDH Dedicated Usage counter
        if (!luidPrefix.isEmpty()) {
            Pair<List<String>, Map<GpuAdapterMemoryProperty, List<Long>>> adapterData = GpuInformation
                    .queryGpuAdapterMemoryCounters();
            List<String> instances = adapterData.getA();
            Map<GpuAdapterMemoryProperty, List<Long>> values = adapterData.getB();
            List<Long> dedicated = values.get(GpuAdapterMemoryProperty.DEDICATED_USAGE);
            if (dedicated != null) {
                String luidLower = luidPrefix.toLowerCase(Locale.ROOT);
                for (int i = 0; i < instances.size(); i++) {
                    if (instances.get(i).toLowerCase(Locale.ROOT).contains(luidLower)) {
                        return dedicated.get(i);
                    }
                }
            }
        }

        // Priority 2: LHM GPU Memory Used (reported in MB, convert to bytes)
        if (!lhmParent.isEmpty()) {
            try {
                WmiResult<LhmSensorProperty> sensors = LhmSensor.querySensors(lhmParent, "SmallData");
                for (int i = 0; i < sensors.getResultCount(); i++) {
                    String sensorName = WmiUtil.getString(sensors, LhmSensorProperty.NAME, i);
                    if ("GPU Memory Used".equals(sensorName)) {
                        float mb = WmiUtil.getFloat(sensors, LhmSensorProperty.VALUE, i);
                        return (long) (mb * MB_TO_BYTES);
                    }
                }
            } catch (Exception e) {
                LOG.debug("LHM GPU memory used query failed: {}", e.getMessage());
            }
        }

        return -1L;
    }

    @Override
    public long getSharedMemoryUsed() {
        if (luidPrefix.isEmpty()) {
            return -1L;
        }
        Pair<List<String>, Map<GpuAdapterMemoryProperty, List<Long>>> adapterData = GpuInformation
                .queryGpuAdapterMemoryCounters();
        List<String> instances = adapterData.getA();
        Map<GpuAdapterMemoryProperty, List<Long>> values = adapterData.getB();
        List<Long> shared = values.get(GpuAdapterMemoryProperty.SHARED_USAGE);
        if (shared != null) {
            String luidLower = luidPrefix.toLowerCase(Locale.ROOT);
            for (int i = 0; i < instances.size(); i++) {
                if (instances.get(i).toLowerCase(Locale.ROOT).contains(luidLower)) {
                    return shared.get(i);
                }
            }
        }
        return -1L;
    }
}
