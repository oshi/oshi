/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.gpu;

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
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.common.NvmlFunctions;
import oshi.util.common.gpu.NvmlDeviceCache;
import oshi.util.common.gpu.NvmlQuery;
import oshi.util.common.gpu.NvmlQuery.NvmlScope;
import oshi.util.tuples.Pair;

/**
 * FFM-based optional runtime binding to the NVIDIA Management Library (NVML). All methods return sentinel values
 * ({@code -1} or {@code -1L}) when NVML is unavailable or a specific query fails.
 * <p>
 * The query skeleton shared with the JNA binding lives in {@link NvmlQuery}; only the native reads below differ.
 */
@ThreadSafe
public final class NvmlUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(NvmlUtilFFM.class);

    private static final long BUS_ID_LEGACY_OFFSET = NvmlFunctions.PCI_INFO_LAYOUT
            .byteOffset(MemoryLayout.PathElement.groupElement("busIdLegacy"));
    private static final long BUS_ID_OFFSET = NvmlFunctions.PCI_INFO_LAYOUT
            .byteOffset(MemoryLayout.PathElement.groupElement("busId"));

    // Derived from the layout rather than repeated as literals, so the reads cannot drift from the struct definition.
    private static final int BUS_ID_LEGACY_LENGTH = (int) NvmlFunctions.PCI_INFO_LAYOUT
            .select(MemoryLayout.PathElement.groupElement("busIdLegacy")).byteSize();
    private static final int BUS_ID_LENGTH = (int) NvmlFunctions.PCI_INFO_LAYOUT
            .select(MemoryLayout.PathElement.groupElement("busId")).byteSize();

    private static final NvmlDeviceCache DEVICE_CACHE = new NvmlDeviceCache("FFM");

    private static final NvmlScope<Device> SCOPE = new NvmlScope<Device>() {
        @Override
        public boolean init() {
            return nvmlInit();
        }

        @Override
        public void uninit() {
            nvmlUninit();
        }

        @Override
        public <R> R withDevice(String deviceId, Function<Device, R> body, R sentinel) {
            // The arena is opened and closed here so no MemorySegment outlives the query or escapes this package.
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment handle = acquireHandleByBusId(deviceId, arena);
                return handle == null ? sentinel : body.apply(new Device(handle, arena));
            }
        }
    };

    private NvmlUtilFFM() {
    }

    /**
     * An acquired device handle together with the arena its out-parameters are allocated from. Both are valid only for
     * the duration of one {@link NvmlScope#withDevice} call.
     */
    private static final class Device {
        private final MemorySegment handle;
        private final Arena arena;

        Device(MemorySegment handle, Arena arena) {
            this.handle = handle;
            this.arena = arena;
        }
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

    // -------------------------------------------------------------------------
    // Device enumeration
    // -------------------------------------------------------------------------

    /**
     * Applies {@code visitor} to each NVML device handle in index order, stopping early if it returns true. Devices
     * whose handle cannot be acquired are skipped. Must be called while NVML is initialized.
     *
     * @param arena   allocates the out-parameters for the walk
     * @param visitor applied to each handle, returning true to stop the walk
     * @return false if the device count could not be read, letting callers distinguish an NVML failure from a machine
     *         with no devices
     */
    private static boolean forEachDevice(Arena arena, Predicate<MemorySegment> visitor) {
        MemorySegment countSeg = arena.allocate(JAVA_INT);
        if (NvmlFunctions.deviceGetCount(countSeg) != NvmlFunctions.NVML_SUCCESS) {
            return false;
        }
        int count = countSeg.get(JAVA_INT, 0);
        for (int i = 0; i < count; i++) {
            MemorySegment handleSeg = arena.allocate(ADDRESS);
            if (NvmlFunctions.deviceGetHandleByIndex(i, handleSeg) != NvmlFunctions.NVML_SUCCESS) {
                continue;
            }
            if (visitor.test(handleSeg.get(ADDRESS, 0))) {
                return true;
            }
        }
        return true;
    }

    /**
     * Reads a device's two PCI bus ID forms, lowercased.
     *
     * @param handle the device handle
     * @param arena  allocates the PCI info struct
     * @return the modern and legacy bus IDs, or {@code null} if the PCI info could not be read
     */
    private static @Nullable Pair<String, String> readBusIds(MemorySegment handle, Arena arena) {
        MemorySegment pciSeg = arena.allocate(NvmlFunctions.PCI_INFO_LAYOUT);
        if (NvmlFunctions.deviceGetPciInfo(handle, pciSeg) != NvmlFunctions.NVML_SUCCESS) {
            return null;
        }
        return new Pair<>(NvmlFunctions.readString(pciSeg, BUS_ID_OFFSET, BUS_ID_LENGTH).toLowerCase(Locale.ROOT),
                NvmlFunctions.readString(pciSeg, BUS_ID_LEGACY_OFFSET, BUS_ID_LEGACY_LENGTH).toLowerCase(Locale.ROOT));
    }

    /**
     * Reads a device's name, lowercased.
     *
     * @param handle the device handle
     * @param arena  allocates the name buffer
     * @return the name, or {@code null} if it could not be read
     */
    private static @Nullable String readName(MemorySegment handle, Arena arena) {
        MemorySegment nameSeg = arena.allocate(NvmlFunctions.NVML_DEVICE_NAME_BUFFER_SIZE);
        if (NvmlFunctions.deviceGetName(handle, nameSeg,
                NvmlFunctions.NVML_DEVICE_NAME_BUFFER_SIZE) != NvmlFunctions.NVML_SUCCESS) {
            return null;
        }
        return nameSeg.getString(0).toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a set of PCI bus ID strings for all NVML devices, or {@code null} on NVML error (so the caller can
     * distinguish a real failure from a legitimate empty result).
     *
     * @return set of PCI bus ID strings, or {@code null} on NVML error
     */
    private static @Nullable Set<String> enumerateDeviceBusIds() {
        try (Arena arena = Arena.ofConfined()) {
            Set<String> ids = new HashSet<>();
            boolean enumerated = forEachDevice(arena, handle -> {
                Pair<String, String> busIds = readBusIds(handle, arena);
                if (busIds != null) {
                    addIfNotEmpty(ids, busIds.getA());
                    addIfNotEmpty(ids, busIds.getB());
                }
                return false;
            });
            return enumerated ? Collections.unmodifiableSet(ids) : null;
        }
    }

    private static void addIfNotEmpty(Set<String> ids, String id) {
        if (!id.isEmpty()) {
            ids.add(id);
        }
    }

    private static @Nullable MemorySegment acquireHandleByBusId(String pciBusId, Arena arena) {
        String needle = pciBusId.toLowerCase(Locale.ROOT);
        MemorySegment[] found = new MemorySegment[1];
        forEachDevice(arena, handle -> {
            Pair<String, String> busIds = readBusIds(handle, arena);
            if (busIds != null
                    && (NvmlQuery.matches(busIds.getA(), needle) || NvmlQuery.matches(busIds.getB(), needle))) {
                found[0] = handle;
                return true;
            }
            return false;
        });
        return found[0];
    }

    private static @Nullable MemorySegment acquireHandleByName(String gpuName, Arena arena) {
        String needle = gpuName.toLowerCase(Locale.ROOT);
        MemorySegment[] found = new MemorySegment[1];
        forEachDevice(arena, handle -> {
            String name = readName(handle, arena);
            if (name != null && NvmlQuery.matches(name, needle)) {
                found[0] = handle;
                return true;
            }
            return false;
        });
        return found[0];
    }

    /**
     * Counts the devices whose name matches, so an ambiguous match can be rejected rather than resolved arbitrarily.
     *
     * @param gpuName GPU name to match
     * @param arena   allocates the out-parameters for the walk
     * @return the number of matching devices, or {@code -1} if the devices could not be enumerated
     */
    private static int countMatchesByName(String gpuName, Arena arena) {
        String needle = gpuName.toLowerCase(Locale.ROOT);
        int[] matches = new int[1];
        boolean enumerated = forEachDevice(arena, handle -> {
            String name = readName(handle, arena);
            if (name != null && NvmlQuery.matches(name, needle)) {
                matches[0]++;
            }
            return false;
        });
        return enumerated ? matches[0] : -1;
    }

    // -------------------------------------------------------------------------
    // Metric readers — the only part that differs from the JNA binding
    // -------------------------------------------------------------------------

    private static double readUtilization(Device device) {
        MemorySegment utilSeg = device.arena.allocate(NvmlFunctions.UTILIZATION_LAYOUT);
        if (NvmlFunctions.deviceGetUtilizationRates(device.handle, utilSeg) == NvmlFunctions.NVML_SUCCESS) {
            return utilSeg.get(JAVA_INT, 0);
        }
        return -1d;
    }

    private static long readVramTotal(Device device) {
        MemorySegment memSeg = device.arena.allocate(NvmlFunctions.MEMORY_LAYOUT);
        if (NvmlFunctions.deviceGetMemoryInfo(device.handle, memSeg) == NvmlFunctions.NVML_SUCCESS) {
            return memSeg.get(JAVA_LONG,
                    NvmlFunctions.MEMORY_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("total")));
        }
        return -1L;
    }

    private static long readVramUsed(Device device) {
        MemorySegment memSeg = device.arena.allocate(NvmlFunctions.MEMORY_LAYOUT);
        if (NvmlFunctions.deviceGetMemoryInfo(device.handle, memSeg) == NvmlFunctions.NVML_SUCCESS) {
            return memSeg.get(JAVA_LONG,
                    NvmlFunctions.MEMORY_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("used")));
        }
        return -1L;
    }

    private static double readTemperature(Device device) {
        MemorySegment tempSeg = device.arena.allocate(JAVA_INT);
        if (NvmlFunctions.deviceGetTemperature(device.handle, NvmlFunctions.NVML_TEMPERATURE_GPU,
                tempSeg) == NvmlFunctions.NVML_SUCCESS) {
            return tempSeg.get(JAVA_INT, 0);
        }
        return -1d;
    }

    private static double readPowerDraw(Device device) {
        MemorySegment powerSeg = device.arena.allocate(JAVA_INT);
        if (NvmlFunctions.deviceGetPowerUsage(device.handle, powerSeg) == NvmlFunctions.NVML_SUCCESS) {
            return powerSeg.get(JAVA_INT, 0) / 1000.0;
        }
        return -1d;
    }

    private static long readClock(Device device, int clockType) {
        MemorySegment clockSeg = device.arena.allocate(JAVA_INT);
        if (NvmlFunctions.deviceGetClockInfo(device.handle, clockType, clockSeg) == NvmlFunctions.NVML_SUCCESS) {
            return clockSeg.get(JAVA_INT, 0);
        }
        return -1L;
    }

    private static double readFanSpeed(Device device) {
        MemorySegment speedSeg = device.arena.allocate(JAVA_INT);
        if (NvmlFunctions.deviceGetFanSpeed(device.handle, speedSeg) == NvmlFunctions.NVML_SUCCESS) {
            return speedSeg.get(JAVA_INT, 0);
        }
        return -1d;
    }

    // -------------------------------------------------------------------------
    // Public API — mirrors NvmlUtilJNA exactly
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
    public static @Nullable String findDevice(@Nullable String pciBusId) {
        if (!NvmlFunctions.isAvailable() || pciBusId == null || pciBusId.isEmpty()) {
            return null;
        }
        if (!nvmlInit()) {
            return null;
        }
        try {
            return NvmlQuery.matchBusId(DEVICE_CACHE.get(NvmlUtilFFM::enumerateDeviceBusIds), pciBusId);
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
    public static @Nullable String findDeviceByName(@Nullable String gpuName) {
        if (!NvmlFunctions.isAvailable() || gpuName == null || gpuName.isEmpty()) {
            return null;
        }
        if (!nvmlInit()) {
            return null;
        }
        try (Arena arena = Arena.ofConfined()) {
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
            Pair<String, String> busIds = readBusIds(handle, arena);
            if (busIds == null) {
                return null;
            }
            // Prefer the modern form, matching the order enumerateDeviceBusIds records them in.
            return busIds.getA().isEmpty() ? emptyToNull(busIds.getB()) : busIds.getA();
        } finally {
            nvmlUninit();
        }
    }

    private static @Nullable String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    /**
     * Returns GPU core utilization percentage (0–100), or -1 if unavailable.
     *
     * @param deviceId stable device identifier
     * @return utilization percentage or -1
     */
    public static double getGpuUtilization(@Nullable String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, NvmlUtilFFM::readUtilization, -1d);
    }

    /**
     * Returns total VRAM in bytes, or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}, or
     *                 {@code null} if neither matched a device
     * @return total bytes or -1
     */
    public static long getVramTotal(@Nullable String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, NvmlUtilFFM::readVramTotal, -1L);
    }

    /**
     * Returns VRAM used in bytes, or -1 if unavailable.
     *
     * @param deviceId stable device identifier
     * @return bytes used or -1
     */
    public static long getVramUsed(@Nullable String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, NvmlUtilFFM::readVramUsed, -1L);
    }

    /**
     * Returns GPU temperature in degrees Celsius, or -1 if unavailable.
     *
     * @param deviceId stable device identifier
     * @return temperature in °C or -1
     */
    public static double getTemperature(@Nullable String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, NvmlUtilFFM::readTemperature, -1d);
    }

    /**
     * Returns GPU power draw in watts, or -1 if unavailable.
     *
     * @param deviceId stable device identifier
     * @return power in watts or -1
     */
    public static double getPowerDraw(@Nullable String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, NvmlUtilFFM::readPowerDraw, -1d);
    }

    /**
     * Returns GPU core clock speed in MHz, or -1 if unavailable.
     *
     * @param deviceId stable device identifier
     * @return core clock in MHz or -1
     */
    public static long getCoreClockMhz(@Nullable String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, device -> readClock(device, NvmlFunctions.NVML_CLOCK_GRAPHICS), -1L);
    }

    /**
     * Returns GPU memory clock speed in MHz, or -1 if unavailable.
     *
     * @param deviceId stable device identifier
     * @return memory clock in MHz or -1
     */
    public static long getMemoryClockMhz(@Nullable String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, device -> readClock(device, NvmlFunctions.NVML_CLOCK_MEM), -1L);
    }

    /**
     * Returns GPU fan speed as a percentage (0–100), or -1 if unavailable.
     *
     * @param deviceId stable device identifier
     * @return fan speed percentage or -1
     */
    public static double getFanSpeedPercent(@Nullable String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, NvmlUtilFFM::readFanSpeed, -1d);
    }
}
