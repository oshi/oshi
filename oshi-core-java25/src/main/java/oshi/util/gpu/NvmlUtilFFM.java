/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.gpu;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.common.NvmlFunctions;

/**
 * FFM-based optional runtime binding to the NVIDIA Management Library (NVML). All methods return sentinel values
 * ({@code -1} or {@code -1L}) when NVML is unavailable or a specific query fails.
 */
@ThreadSafe
public final class NvmlUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(NvmlUtilFFM.class);

    private static volatile boolean devicesEnumerated = false;
    private static volatile Set<String> deviceBusIds = Collections.emptySet();

    private NvmlUtilFFM() {
    }

    private static boolean nvmlInit() {
        if (!NvmlFunctions.isAvailable()) {
            return false;
        }
        int ret = NvmlFunctions.init();
        if (ret == NvmlFunctions.NVML_SUCCESS) {
            return true;
        }
        LOG.debug("nvmlInit_v2 failed with code {}", ret);
        return false;
    }

    private static void nvmlUninit() {
        NvmlFunctions.shutdown();
    }

    private static void ensureDevicesEnumerated() {
        if (devicesEnumerated) {
            return;
        }
        Set<String> ids = enumerateDeviceBusIds();
        if (ids != null) {
            deviceBusIds = ids;
            devicesEnumerated = true;
            LOG.debug("NVML (FFM) enumerated {} device(s)", deviceBusIds.size());
        }
    }

    private static Set<String> enumerateDeviceBusIds() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment countSeg = arena.allocate(JAVA_INT);
            if (NvmlFunctions.deviceGetCount(countSeg) != NvmlFunctions.NVML_SUCCESS) {
                return null;
            }
            int count = countSeg.get(JAVA_INT, 0);
            Set<String> ids = new HashSet<>();
            long busIdLegacyOffset = NvmlFunctions.PCI_INFO_LAYOUT
                    .byteOffset(MemoryLayout.PathElement.groupElement("busIdLegacy"));
            long busIdOffset = NvmlFunctions.PCI_INFO_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("busId"));
            for (int i = 0; i < count; i++) {
                MemorySegment handleSeg = arena.allocate(ADDRESS);
                if (NvmlFunctions.deviceGetHandleByIndex(i, handleSeg) != NvmlFunctions.NVML_SUCCESS) {
                    continue;
                }
                MemorySegment handle = handleSeg.get(ADDRESS, 0);
                MemorySegment pciSeg = arena.allocate(NvmlFunctions.PCI_INFO_LAYOUT);
                if (NvmlFunctions.deviceGetPciInfo(handle, pciSeg) == NvmlFunctions.NVML_SUCCESS) {
                    String legacyId = NvmlFunctions.readString(pciSeg, busIdLegacyOffset, 16).toLowerCase(Locale.ROOT);
                    if (!legacyId.isEmpty()) {
                        ids.add(legacyId);
                    }
                    String busId = NvmlFunctions.readString(pciSeg, busIdOffset, 32).toLowerCase(Locale.ROOT);
                    if (!busId.isEmpty()) {
                        ids.add(busId);
                    }
                }
            }
            return Collections.unmodifiableSet(ids);
        }
    }

    private static MemorySegment acquireHandleByBusId(String pciBusId, Arena arena) {
        MemorySegment countSeg = arena.allocate(JAVA_INT);
        if (NvmlFunctions.deviceGetCount(countSeg) != NvmlFunctions.NVML_SUCCESS) {
            return null;
        }
        String needle = pciBusId.toLowerCase(Locale.ROOT);
        int count = countSeg.get(JAVA_INT, 0);
        long busIdLegacyOffset = NvmlFunctions.PCI_INFO_LAYOUT
                .byteOffset(MemoryLayout.PathElement.groupElement("busIdLegacy"));
        long busIdOffset = NvmlFunctions.PCI_INFO_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("busId"));
        for (int i = 0; i < count; i++) {
            MemorySegment handleSeg = arena.allocate(ADDRESS);
            if (NvmlFunctions.deviceGetHandleByIndex(i, handleSeg) != NvmlFunctions.NVML_SUCCESS) {
                continue;
            }
            MemorySegment handle = handleSeg.get(ADDRESS, 0);
            MemorySegment pciSeg = arena.allocate(NvmlFunctions.PCI_INFO_LAYOUT);
            if (NvmlFunctions.deviceGetPciInfo(handle, pciSeg) == NvmlFunctions.NVML_SUCCESS) {
                String legacyId = NvmlFunctions.readString(pciSeg, busIdLegacyOffset, 16).toLowerCase(Locale.ROOT);
                String busId = NvmlFunctions.readString(pciSeg, busIdOffset, 32).toLowerCase(Locale.ROOT);
                if (busId.contains(needle) || needle.contains(busId) || legacyId.contains(needle)
                        || needle.contains(legacyId)) {
                    return handle;
                }
            }
        }
        return null;
    }

    private static MemorySegment acquireHandleByName(String gpuName, Arena arena) {
        MemorySegment countSeg = arena.allocate(JAVA_INT);
        if (NvmlFunctions.deviceGetCount(countSeg) != NvmlFunctions.NVML_SUCCESS) {
            return null;
        }
        String needle = gpuName.toLowerCase(Locale.ROOT);
        int count = countSeg.get(JAVA_INT, 0);
        for (int i = 0; i < count; i++) {
            MemorySegment handleSeg = arena.allocate(ADDRESS);
            if (NvmlFunctions.deviceGetHandleByIndex(i, handleSeg) != NvmlFunctions.NVML_SUCCESS) {
                continue;
            }
            MemorySegment handle = handleSeg.get(ADDRESS, 0);
            MemorySegment nameSeg = arena.allocate(NvmlFunctions.NVML_DEVICE_NAME_BUFFER_SIZE);
            if (NvmlFunctions.deviceGetName(handle, nameSeg,
                    NvmlFunctions.NVML_DEVICE_NAME_BUFFER_SIZE) == NvmlFunctions.NVML_SUCCESS) {
                String name = nameSeg.getString(0).toLowerCase(Locale.ROOT);
                if (name.contains(needle) || needle.contains(name)) {
                    return handle;
                }
            }
        }
        return null;
    }

    private static int countMatchesByName(String gpuName, Arena arena) {
        MemorySegment countSeg = arena.allocate(JAVA_INT);
        if (NvmlFunctions.deviceGetCount(countSeg) != NvmlFunctions.NVML_SUCCESS) {
            return -1;
        }
        String needle = gpuName.toLowerCase(Locale.ROOT);
        int total = countSeg.get(JAVA_INT, 0);
        int matches = 0;
        for (int i = 0; i < total; i++) {
            MemorySegment handleSeg = arena.allocate(ADDRESS);
            if (NvmlFunctions.deviceGetHandleByIndex(i, handleSeg) != NvmlFunctions.NVML_SUCCESS) {
                continue;
            }
            MemorySegment nameSeg = arena.allocate(NvmlFunctions.NVML_DEVICE_NAME_BUFFER_SIZE);
            if (NvmlFunctions.deviceGetName(handleSeg.get(ADDRESS, 0), nameSeg,
                    NvmlFunctions.NVML_DEVICE_NAME_BUFFER_SIZE) == NvmlFunctions.NVML_SUCCESS) {
                String name = nameSeg.getString(0).toLowerCase(Locale.ROOT);
                if (name.contains(needle) || needle.contains(name)) {
                    matches++;
                }
            }
        }
        return matches;
    }

    // -------------------------------------------------------------------------
    // Public API — mirrors NvmlUtil (JNA) exactly
    // -------------------------------------------------------------------------

    /**
     * Returns whether the NVML native library was successfully loaded.
     *
     * @return true if the NVML library is available
     */
    public static boolean isAvailable() {
        return NvmlFunctions.isAvailable();
    }

    /**
     * Finds the stable PCI bus ID string for the NVML device whose bus ID contains the given fragment.
     *
     * @param pciBusId PCI bus ID fragment
     * @return matched PCI bus ID string, or {@code null} if not found
     */
    public static String findDevice(String pciBusId) {
        if (!NvmlFunctions.isAvailable() || pciBusId == null || pciBusId.isEmpty()) {
            return null;
        }
        boolean init = nvmlInit();
        if (!init) {
            return null;
        }
        try {
            ensureDevicesEnumerated();
            String needle = pciBusId.toLowerCase(Locale.ROOT);
            for (String id : deviceBusIds) {
                if (id.contains(needle) || needle.contains(id)) {
                    return id;
                }
            }
            return null;
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Finds the stable PCI bus ID string for the NVML device whose name matches the given GPU name.
     *
     * @param gpuName GPU name string
     * @return PCI bus ID string, or {@code null} if not found
     */
    public static String findDeviceByName(String gpuName) {
        if (!NvmlFunctions.isAvailable() || gpuName == null || gpuName.isEmpty()) {
            return null;
        }
        boolean init = nvmlInit();
        if (!init) {
            return null;
        }
        try (Arena arena = Arena.ofConfined()) {
            ensureDevicesEnumerated();
            int matchCount = countMatchesByName(gpuName, arena);
            if (matchCount <= 0) {
                return null;
            }
            if (matchCount > 1) {
                LOG.warn("NVML name match for '{}' is ambiguous ({} devices match); use PCI bus ID for reliable"
                        + " device identification", gpuName, matchCount);
                return null;
            }
            MemorySegment handle = acquireHandleByName(gpuName, arena);
            if (handle == null) {
                return null;
            }
            long busIdLegacyOffset = NvmlFunctions.PCI_INFO_LAYOUT
                    .byteOffset(MemoryLayout.PathElement.groupElement("busIdLegacy"));
            MemorySegment pciSeg = arena.allocate(NvmlFunctions.PCI_INFO_LAYOUT);
            if (NvmlFunctions.deviceGetPciInfo(handle, pciSeg) == NvmlFunctions.NVML_SUCCESS) {
                String busId = NvmlFunctions.readString(pciSeg, busIdLegacyOffset, 16).toLowerCase(Locale.ROOT);
                if (!busId.isEmpty()) {
                    return busId;
                }
            }
            return null;
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Returns GPU core utilization percentage (0–100), or -1 if unavailable.
     *
     * @param deviceId stable device identifier
     * @return utilization percentage or -1
     */
    public static double getGpuUtilization(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return -1d;
        }
        boolean init = nvmlInit();
        if (!init) {
            return -1d;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment device = acquireHandleByBusId(deviceId, arena);
            if (device == null) {
                return -1d;
            }
            MemorySegment utilSeg = arena.allocate(NvmlFunctions.UTILIZATION_LAYOUT);
            if (NvmlFunctions.deviceGetUtilizationRates(device, utilSeg) == NvmlFunctions.NVML_SUCCESS) {
                return utilSeg.get(JAVA_INT, 0);
            }
            return -1d;
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Returns VRAM used in bytes, or -1 if unavailable.
     *
     * @param deviceId stable device identifier
     * @return bytes used or -1
     */
    public static long getVramUsed(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return -1L;
        }
        boolean init = nvmlInit();
        if (!init) {
            return -1L;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment device = acquireHandleByBusId(deviceId, arena);
            if (device == null) {
                return -1L;
            }
            MemorySegment memSeg = arena.allocate(NvmlFunctions.MEMORY_LAYOUT);
            if (NvmlFunctions.deviceGetMemoryInfo(device, memSeg) == NvmlFunctions.NVML_SUCCESS) {
                return memSeg.get(JAVA_LONG,
                        NvmlFunctions.MEMORY_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("used")));
            }
            return -1L;
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Returns GPU temperature in degrees Celsius, or -1 if unavailable.
     *
     * @param deviceId stable device identifier
     * @return temperature in °C or -1
     */
    public static double getTemperature(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return -1d;
        }
        boolean init = nvmlInit();
        if (!init) {
            return -1d;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment device = acquireHandleByBusId(deviceId, arena);
            if (device == null) {
                return -1d;
            }
            MemorySegment tempSeg = arena.allocate(JAVA_INT);
            if (NvmlFunctions.deviceGetTemperature(device, NvmlFunctions.NVML_TEMPERATURE_GPU,
                    tempSeg) == NvmlFunctions.NVML_SUCCESS) {
                return tempSeg.get(JAVA_INT, 0);
            }
            return -1d;
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Returns GPU power draw in watts, or -1 if unavailable.
     *
     * @param deviceId stable device identifier
     * @return power in watts or -1
     */
    public static double getPowerDraw(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return -1d;
        }
        boolean init = nvmlInit();
        if (!init) {
            return -1d;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment device = acquireHandleByBusId(deviceId, arena);
            if (device == null) {
                return -1d;
            }
            MemorySegment powerSeg = arena.allocate(JAVA_INT);
            if (NvmlFunctions.deviceGetPowerUsage(device, powerSeg) == NvmlFunctions.NVML_SUCCESS) {
                return powerSeg.get(JAVA_INT, 0) / 1000.0;
            }
            return -1d;
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Returns GPU core clock speed in MHz, or -1 if unavailable.
     *
     * @param deviceId stable device identifier
     * @return core clock in MHz or -1
     */
    public static long getCoreClockMhz(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return -1L;
        }
        boolean init = nvmlInit();
        if (!init) {
            return -1L;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment device = acquireHandleByBusId(deviceId, arena);
            if (device == null) {
                return -1L;
            }
            MemorySegment clockSeg = arena.allocate(JAVA_INT);
            if (NvmlFunctions.deviceGetClockInfo(device, NvmlFunctions.NVML_CLOCK_GRAPHICS,
                    clockSeg) == NvmlFunctions.NVML_SUCCESS) {
                return clockSeg.get(JAVA_INT, 0);
            }
            return -1L;
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Returns GPU memory clock speed in MHz, or -1 if unavailable.
     *
     * @param deviceId stable device identifier
     * @return memory clock in MHz or -1
     */
    public static long getMemoryClockMhz(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return -1L;
        }
        boolean init = nvmlInit();
        if (!init) {
            return -1L;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment device = acquireHandleByBusId(deviceId, arena);
            if (device == null) {
                return -1L;
            }
            MemorySegment clockSeg = arena.allocate(JAVA_INT);
            if (NvmlFunctions.deviceGetClockInfo(device, NvmlFunctions.NVML_CLOCK_MEM,
                    clockSeg) == NvmlFunctions.NVML_SUCCESS) {
                return clockSeg.get(JAVA_INT, 0);
            }
            return -1L;
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Returns GPU fan speed as a percentage (0–100), or -1 if unavailable.
     *
     * @param deviceId stable device identifier
     * @return fan speed percentage or -1
     */
    public static double getFanSpeedPercent(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return -1d;
        }
        boolean init = nvmlInit();
        if (!init) {
            return -1d;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment device = acquireHandleByBusId(deviceId, arena);
            if (device == null) {
                return -1d;
            }
            MemorySegment speedSeg = arena.allocate(JAVA_INT);
            if (NvmlFunctions.deviceGetFanSpeed(device, speedSeg) == NvmlFunctions.NVML_SUCCESS) {
                return speedSeg.get(JAVA_INT, 0);
            }
            return -1d;
        } finally {
            nvmlUninit();
        }
    }
}
