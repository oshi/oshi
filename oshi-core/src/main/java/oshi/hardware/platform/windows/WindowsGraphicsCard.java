/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.HashMap;
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
import oshi.util.gpu.AdlUtil;
import oshi.util.gpu.NvmlUtil;
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
    public static final String LOCATION_INFORMATION = "LocationInformation";
    public static final String DISPLAY_DEVICES_REGISTRY_PATH = "SYSTEM\\CurrentControlSet\\Control\\Class\\{4d36e968-e325-11ce-bfc1-08002be10318}\\";

    // PDH instance prefix for this adapter's LUID, e.g. "luid_0x00000000_0x0001234_phys_0"
    // Used to filter GPU Engine and GPU Adapter Memory counter instances.
    private final String luidPrefix;

    // LHM hardware identifier for this GPU, e.g. "/gpu-nvidia/0". Empty if LHM is not available.
    private final String lhmParent;

    // PCI bus number from DXGI, used to correlate with ADL. -1 if unknown.
    private final int pciBusNumber;

    // PCI bus ID string for NVML correlation, e.g. "0000:01:00.0". Empty if unknown.
    private final String pciBusId;

    /**
     * Constructor for WindowsGraphicsCard
     *
     * @param name         The name
     * @param deviceId     The device ID
     * @param vendor       The vendor
     * @param versionInfo  The version info
     * @param vram         The VRAM
     * @param luidPrefix   PDH LUID instance prefix for this adapter, or empty string if unknown
     * @param lhmParent    LHM hardware identifier for this GPU, or empty string if unavailable
     * @param pciBusNumber PCI bus number for ADL correlation, or -1 if unknown
     * @param pciBusId     PCI bus ID string for NVML correlation, or empty string if unknown
     */
    WindowsGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram, String luidPrefix,
            String lhmParent, int pciBusNumber, String pciBusId) {
        super(name, deviceId, vendor, versionInfo, vram);
        this.luidPrefix = luidPrefix;
        this.lhmParent = lhmParent;
        this.pciBusNumber = pciBusNumber;
        this.pciBusId = pciBusId;
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
                int pciBusNumber = -1;
                String pciBusId = "";
                String locationInfo = RegistryUtil.getStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey,
                        LOCATION_INFORMATION);
                DxgiAdapterInfo dxgiMatch = WindowsDxgi.findMatch(remainingDxgi, pciVendorId, pciDeviceId, name);
                if (dxgiMatch != null) {
                    vram = dxgiMatch.getDedicatedVideoMemory();
                    dxgiIndex = dxgiAdapters.indexOf(dxgiMatch);
                    luidPrefix = buildLuidPrefix(dxgiMatch);
                    pciBusNumber = parsePciBusNumber(locationInfo);
                    pciBusId = buildPciBusId(locationInfo);
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
                        Util.isBlank(versionInfo) ? Constants.UNKNOWN : versionInfo, vram, luidPrefix, lhmParent,
                        pciBusNumber, pciBusId);
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
                int pciBusNumber = -1;
                String pciBusId = "";
                if (dxgiMatch != null) {
                    vram = dxgiMatch.getDedicatedVideoMemory();
                    dxgiIndex = dxgiAdapters.indexOf(dxgiMatch);
                    luidPrefix = buildLuidPrefix(dxgiMatch);
                    remainingDxgi.remove(dxgiMatch);
                } else {
                    vram = WmiUtil.getUint32asLong(cards, VideoControllerProperty.ADAPTERRAM, index);
                }
                String lhmParent = lhmParentMap.getOrDefault(WindowsDxgi.normalizeName(Util.isBlank(name) ? "" : name),
                        "");
                GraphicsCard card = new WindowsGraphicsCard(Util.isBlank(name) ? Constants.UNKNOWN : name, deviceId,
                        Util.isBlank(vendor) ? Constants.UNKNOWN : vendor, versionInfo, vram, luidPrefix, lhmParent,
                        pciBusNumber, pciBusId);
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
     * Returns an empty map if LHM is not running. If two GPU entries normalize to the same name the key is mapped to an
     * empty string so callers skip LHM for that ambiguous name rather than returning the wrong adapter's metrics.
     *
     * @return map of normalized GPU name to LHM hardware identifier
     */
    private static Map<String, String> buildLhmParentMap() {
        Map<String, String> map = new HashMap<>();
        try {
            WmiResult<LhmHardwareProperty> hw = LhmSensor.queryGpuHardware();
            for (int i = 0; i < hw.getResultCount(); i++) {
                String identifier = WmiUtil.getString(hw, LhmHardwareProperty.IDENTIFIER, i);
                String hwName = WmiUtil.getString(hw, LhmHardwareProperty.NAME, i);
                if (!identifier.isEmpty() && !hwName.isEmpty()) {
                    String norm = WindowsDxgi.normalizeName(hwName);
                    if (map.containsKey(norm)) {
                        // Two adapters with the same normalized name: mark ambiguous so neither
                        // is used (empty string is the "skip LHM" sentinel for callers).
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

    /**
     * Parses the PCI bus number from a Windows registry {@code LocationInformation} string of the form
     * {@code "PCI bus N, device N, function N"}.
     *
     * @param locationInfo the LocationInformation registry value
     * @return PCI bus number, or -1 if not parseable
     */
    static int parsePciBusNumber(String locationInfo) {
        if (locationInfo == null || locationInfo.isEmpty()) {
            return -1;
        }
        // Format: "PCI bus N, device N, function N" (case-insensitive)
        String lower = locationInfo.toLowerCase(Locale.ROOT);
        int busIdx = lower.indexOf("pci bus ");
        if (busIdx < 0) {
            return -1;
        }
        int start = busIdx + 8;
        int end = lower.indexOf(',', start);
        String numStr = end > start ? locationInfo.substring(start, end).trim() : locationInfo.substring(start).trim();
        return ParseUtil.parseIntOrDefault(numStr, -1);
    }

    /**
     * Parses the PCI device number from a Windows registry {@code LocationInformation} string.
     *
     * @param locationInfo the LocationInformation registry value
     * @return PCI device number, or -1 if not parseable
     */
    static int parsePciDevice(String locationInfo) {
        if (locationInfo == null || locationInfo.isEmpty()) {
            return -1;
        }
        String lower = locationInfo.toLowerCase(Locale.ROOT);
        int devIdx = lower.indexOf("device ");
        if (devIdx < 0) {
            return -1;
        }
        int start = devIdx + 7;
        int end = lower.indexOf(',', start);
        String numStr = end > start ? locationInfo.substring(start, end).trim() : locationInfo.substring(start).trim();
        return ParseUtil.parseIntOrDefault(numStr, -1);
    }

    /**
     * Parses the PCI function number from a Windows registry {@code LocationInformation} string.
     *
     * @param locationInfo the LocationInformation registry value
     * @return PCI function number, or -1 if not parseable
     */
    static int parsePciFunction(String locationInfo) {
        if (locationInfo == null || locationInfo.isEmpty()) {
            return -1;
        }
        String lower = locationInfo.toLowerCase(Locale.ROOT);
        int fnIdx = lower.indexOf("function ");
        if (fnIdx < 0) {
            return -1;
        }
        int start = fnIdx + 9;
        int end = lower.indexOf(',', start);
        String numStr = end > start ? locationInfo.substring(start, end).trim() : locationInfo.substring(start).trim();
        return ParseUtil.parseIntOrDefault(numStr, -1);
    }

    /**
     * Builds a PCI bus ID string in {@code "0000:BB:DD.F"} format from a Windows registry {@code LocationInformation}
     * string. Returns an empty string if any component cannot be parsed.
     *
     * @param locationInfo the LocationInformation registry value
     * @return PCI bus ID string, or empty string if not parseable
     */
    static String buildPciBusId(String locationInfo) {
        int bus = parsePciBusNumber(locationInfo);
        int device = parsePciDevice(locationInfo);
        int function = parsePciFunction(locationInfo);
        if (bus < 0 || device < 0 || function < 0) {
            return "";
        }
        return String.format(Locale.ROOT, "0000:%02x:%02x.%x", bus, device, function);
    }

    /**
     * Builds the PDH LUID instance prefix for the given DXGI adapter. The prefix has the form
     * {@code luid_0xHHHHHHHH_0xLLLLLLLL_phys_0} matching the Windows PDH GPU Engine and GPU Adapter Memory counter
     * instance names.
     *
     * <p>
     * The LUID is read directly from {@code DXGI_ADAPTER_DESC.AdapterLuid}, so this method works correctly on multi-GPU
     * systems. A zero LUID (both parts zero) indicates the adapter did not supply a valid LUID; in that case an empty
     * string is returned and PDH metrics will report {@code -1}.
     *
     * @param adapter the DXGI adapter info containing the LUID
     * @return PDH LUID instance prefix string, or empty string if the LUID is zero
     */
    private static String buildLuidPrefix(DxgiAdapterInfo adapter) {
        int low = adapter.getLuidLowPart();
        int high = adapter.getLuidHighPart();
        if (low == 0 && high == 0) {
            // Zero LUID is invalid; fall back to PDH enumeration for single-GPU case.
            return buildLuidPrefixFromPdh();
        }
        return String.format(Locale.ROOT, "luid_0x%08x_0x%08x_phys_0", high, low);
    }

    /**
     * Fallback LUID prefix discovery by enumerating PDH GPU Adapter Memory instances. Used when the DXGI adapter
     * reports a zero LUID.
     *
     * <p>
     * Only reliable on single-GPU systems: when multiple GPU Adapter Memory instances are present, the correct
     * per-adapter mapping cannot be determined without a valid LUID, so an empty string is returned and PDH metrics
     * will report {@code -1}.
     *
     * @return PDH LUID instance prefix string, or empty string if not determinable
     */
    private static String buildLuidPrefixFromPdh() {
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
        // Monotonic timestamp in 100ns units via System.nanoTime(); matches GpuTicks.getTimestamp() contract.
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
        // First sum per-PID rows into per-engine-type totals, then sum across all engine types.
        // Summing across types gives the total cumulative active ticks for the adapter; deltas
        // between two snapshots correctly reflect overall GPU work regardless of which engine
        // type is busiest at any given moment.
        Map<String, Long> engineTypeSums = new HashMap<>();
        String luidLower = luidPrefix.toLowerCase(Locale.ROOT);
        for (int i = 0; i < Math.min(instances.size(), runningTimes.size()); i++) {
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

        long totalTicks = engineTypeSums.values().stream().mapToLong(Long::longValue).sum();
        return new DefaultGpuTicks(timestamp, totalTicks);
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
                int limit = Math.min(instances.size(), dedicated.size());
                for (int i = 0; i < limit; i++) {
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
            int limit = Math.min(instances.size(), shared.size());
            for (int i = 0; i < limit; i++) {
                if (instances.get(i).toLowerCase(Locale.ROOT).contains(luidLower)) {
                    return shared.get(i);
                }
            }
        }
        return -1L;
    }

    @Override
    public double getTemperature() {
        // Priority 1: NVML
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            double val = NvmlUtil.getTemperature(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        // Priority 2: ADL
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            double val = AdlUtil.getTemperature(adlIndex);
            if (val >= 0) {
                return val;
            }
        }
        // Priority 3: LHM
        return lhmFloatSensor("Temperature", "GPU Core");
    }

    @Override
    public double getPowerDraw() {
        // Priority 1: NVML
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            double val = NvmlUtil.getPowerDraw(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        // Priority 2: ADL
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            double val = AdlUtil.getPowerDraw(adlIndex);
            if (val >= 0) {
                return val;
            }
        }
        // Priority 3: LHM
        double lhm = lhmFloatSensor("Power", "GPU Package");
        if (lhm >= 0) {
            return lhm;
        }
        return lhmFloatSensor("Power", "GPU Power");
    }

    @Override
    public long getCoreClockMhz() {
        // Priority 1: NVML
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            long val = NvmlUtil.getCoreClockMhz(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        // Priority 2: ADL
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            long val = AdlUtil.getCoreClockMhz(adlIndex);
            if (val >= 0) {
                return val;
            }
        }
        // Priority 3: LHM
        double lhm = lhmFloatSensor("Clock", "GPU Core");
        return lhm >= 0 ? (long) lhm : -1L;
    }

    @Override
    public long getMemoryClockMhz() {
        // Priority 1: NVML
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            long val = NvmlUtil.getMemoryClockMhz(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        // Priority 2: ADL
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            long val = AdlUtil.getMemoryClockMhz(adlIndex);
            if (val >= 0) {
                return val;
            }
        }
        // Priority 3: LHM
        double lhm = lhmFloatSensor("Clock", "GPU Memory");
        return lhm >= 0 ? (long) lhm : -1L;
    }

    @Override
    public double getFanSpeedPercent() {
        // Priority 1: NVML
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            double val = NvmlUtil.getFanSpeedPercent(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        // Priority 2: ADL
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            double val = AdlUtil.getFanSpeedPercent(adlIndex);
            if (val >= 0) {
                return val;
            }
        }
        // Priority 3: LHM Control sensor (percentage)
        double lhm = lhmFloatSensor("Control", "GPU Fan");
        if (lhm >= 0) {
            return lhm;
        }
        return lhmFloatSensor("Control", "GPU Fan 1");
    }

    /**
     * Finds the stable NVML device identifier for this card. Tries PCI bus ID first, then falls back to name matching.
     *
     * @return stable device identifier string, or null if NVML unavailable or no match
     */
    private String findNvmlDevice() {
        if (!NvmlUtil.isAvailable()) {
            return null;
        }
        if (!pciBusId.isEmpty()) {
            String id = NvmlUtil.findDevice(pciBusId);
            if (id != null) {
                return id;
            }
        }
        return NvmlUtil.findDeviceByName(getName());
    }

    /**
     * Finds the ADL adapter index for this card by PCI bus number.
     *
     * @return ADL adapter index, or -1 if ADL unavailable or no match
     */
    private int findAdlIndex() {
        if (!AdlUtil.isAvailable() || pciBusNumber < 0) {
            return -1;
        }
        return AdlUtil.findAdapterIndex(pciBusNumber);
    }

    /**
     * Queries a single float sensor value from LHM.
     *
     * @param sensorType LHM sensor type string
     * @param sensorName LHM sensor name string
     * @return sensor value, or -1 if LHM unavailable, parent empty, or sensor not found
     */
    private double lhmFloatSensor(String sensorType, String sensorName) {
        if (lhmParent.isEmpty()) {
            return -1d;
        }
        try {
            WmiResult<LhmSensorProperty> sensors = LhmSensor.querySensors(lhmParent, sensorType);
            for (int i = 0; i < sensors.getResultCount(); i++) {
                if (sensorName.equals(WmiUtil.getString(sensors, LhmSensorProperty.NAME, i))) {
                    return WmiUtil.getFloat(sensors, LhmSensorProperty.VALUE, i);
                }
            }
        } catch (Exception e) {
            LOG.debug("LHM {} {} query failed: {}", sensorType, sensorName, e.getMessage());
        }
        return -1d;
    }
}
