/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.gpu;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.common.Nvml;
import oshi.jna.common.Nvml.NvmlLibrary;
import oshi.jna.common.Nvml.NvmlMemory;
import oshi.jna.common.Nvml.NvmlPciInfo;
import oshi.jna.common.Nvml.NvmlUtilization;
import oshi.util.common.gpu.NvmlDeviceCache;
import oshi.util.common.gpu.NvmlQuery;
import oshi.util.common.gpu.NvmlQuery.NvmlScope;
import oshi.util.tuples.Pair;

/**
 * Optional runtime binding to the NVIDIA Management Library (NVML). All methods return sentinel values ({@code -1} or
 * {@code -1L}) when NVML is unavailable or a specific query fails.
 *
 * <p>
 * The native library is loaded once at class initialization. Each metric call pairs {@code nvmlInit_v2} with
 * {@code nvmlShutdown} to correctly manage NVML's internal reference count, ensuring OSHI does not interfere with other
 * code in the same process that may also be managing the NVML lifecycle.
 *
 * <p>
 * Device handles are enumerated once on first successful init and cached by PCI bus ID string for correlation with OSHI
 * GraphicsCard instances. The query skeleton shared with the FFM binding lives in {@link NvmlQuery}.
 */
@ThreadSafe
public final class NvmlUtilJNA {

    private static final Logger LOG = LoggerFactory.getLogger(NvmlUtilJNA.class);

    // -------------------------------------------------------------------------
    // Library loading (holder pattern — loads the .dll/.so once)
    // -------------------------------------------------------------------------

    private static final class Holder {
        static final NvmlLibrary LIB;
        static final boolean LIBRARY_LOADED;

        static {
            NvmlLibrary lib = null;
            boolean loaded = false;
            try {
                lib = Native.load(Platform.isWindows() ? "nvml" : "nvidia-ml", NvmlLibrary.class);
                loaded = true;
                LOG.debug("NVML library loaded");
            } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
                LOG.debug("NVML library not available", e);
            }
            LIB = lib;
            LIBRARY_LOADED = loaded;
        }
    }

    // Stores PCI bus ID strings (stable identifiers) rather than Pointer handles, which are only valid within a
    // single nvmlInit/nvmlShutdown scope.
    private static final NvmlDeviceCache DEVICE_CACHE = new NvmlDeviceCache("JNA");

    private static final NvmlScope<Pointer> SCOPE = new NvmlScope<Pointer>() {
        @Override
        public boolean init() {
            return nvmlInit();
        }

        @Override
        public void uninit() {
            nvmlUninit();
        }

        @Override
        public <R> R withDevice(String deviceId, Function<Pointer, R> body, R sentinel) {
            Pointer device = acquireHandleByBusId(deviceId);
            return device == null ? sentinel : body.apply(device);
        }
    };

    private NvmlUtilJNA() {
    }

    // -------------------------------------------------------------------------
    // Init/uninit helpers (COM pattern)
    // -------------------------------------------------------------------------

    /**
     * Calls {@code nvmlInit_v2}, incrementing NVML's internal reference count. Every successful call must be paired
     * with exactly one call to {@link #nvmlUninit()}, which decrements the same counter. This ensures OSHI does not
     * permanently hold a reference that would interfere with other code in the process managing the NVML lifecycle.
     *
     * @return true if this call successfully initialized NVML and must be paired with {@link #nvmlUninit()}
     */
    private static boolean nvmlInit() {
        if (!Holder.LIBRARY_LOADED) {
            return false;
        }
        int ret = Holder.LIB.nvmlInit_v2();
        if (ret == Nvml.NVML_SUCCESS) {
            return true;
        }
        LOG.debug("nvmlInit_v2 failed with code {}", ret);
        return false;
    }

    /**
     * Calls {@code nvmlShutdown}, decrementing the same internal reference count that {@link #nvmlInit()} incremented.
     * Must be called exactly once for each successful call to {@link #nvmlInit()}.
     */
    private static void nvmlUninit() {
        Holder.LIB.nvmlShutdown();
    }

    // -------------------------------------------------------------------------
    // Device enumeration
    // -------------------------------------------------------------------------

    /**
     * Applies {@code visitor} to each NVML device handle in index order, stopping early if it returns true. Devices
     * whose handle cannot be acquired are skipped. Must be called while NVML is initialized.
     *
     * @param visitor applied to each handle, returning true to stop the walk
     * @return false if the device count could not be read, letting callers distinguish an NVML failure from a machine
     *         with no devices
     */
    private static boolean forEachDevice(Predicate<Pointer> visitor) {
        IntByReference countRef = new IntByReference();
        if (Holder.LIB.nvmlDeviceGetCount_v2(countRef) != Nvml.NVML_SUCCESS) {
            return false;
        }
        int count = countRef.getValue();
        for (int i = 0; i < count; i++) {
            PointerByReference handleRef = new PointerByReference();
            if (Holder.LIB.nvmlDeviceGetHandleByIndex_v2(i, handleRef) != Nvml.NVML_SUCCESS) {
                continue;
            }
            if (visitor.test(handleRef.getValue())) {
                return true;
            }
        }
        return true;
    }

    /**
     * Reads a device's two PCI bus ID forms, lowercased.
     *
     * @param handle the device handle
     * @return the modern and legacy bus IDs, or {@code null} if the PCI info could not be read
     */
    private static Pair<String, String> readBusIds(Pointer handle) {
        NvmlPciInfo pci = new NvmlPciInfo();
        if (Holder.LIB.nvmlDeviceGetPciInfo_v3(handle, pci) != Nvml.NVML_SUCCESS) {
            return null;
        }
        pci.read();
        return new Pair<>(Native.toString(pci.busId).toLowerCase(Locale.ROOT),
                Native.toString(pci.busIdLegacy).toLowerCase(Locale.ROOT));
    }

    /**
     * Reads a device's name, lowercased.
     *
     * @param handle the device handle
     * @return the name, or {@code null} if it could not be read
     */
    private static String readName(Pointer handle) {
        byte[] nameBuf = new byte[Nvml.NVML_DEVICE_NAME_BUFFER_SIZE];
        if (Holder.LIB.nvmlDeviceGetName(handle, nameBuf, nameBuf.length) != Nvml.NVML_SUCCESS) {
            return null;
        }
        return Native.toString(nameBuf).toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a set of PCI bus ID strings for all NVML devices, or {@code null} on NVML error (so the caller can
     * distinguish a real failure from a legitimate empty result).
     *
     * @return set of PCI bus ID strings, or {@code null} on NVML error
     */
    private static Set<String> enumerateDeviceBusIds() {
        Set<String> ids = new HashSet<>();
        boolean enumerated = forEachDevice(handle -> {
            Pair<String, String> busIds = readBusIds(handle);
            if (busIds != null) {
                addIfNotEmpty(ids, busIds.getA());
                addIfNotEmpty(ids, busIds.getB());
            }
            return false;
        });
        return enumerated ? Collections.unmodifiableSet(ids) : null;
    }

    private static void addIfNotEmpty(Set<String> ids, String id) {
        if (!id.isEmpty()) {
            ids.add(id);
        }
    }

    /**
     * Acquires a fresh device handle within the current init scope by matching the given PCI bus ID fragment. Must be
     * called while NVML is initialized.
     *
     * @param pciBusId PCI bus ID fragment to match
     * @return device handle Pointer, or {@code null} if not found
     */
    private static Pointer acquireHandleByBusId(String pciBusId) {
        String needle = pciBusId.toLowerCase(Locale.ROOT);
        Pointer[] found = new Pointer[1];
        forEachDevice(handle -> {
            Pair<String, String> busIds = readBusIds(handle);
            if (busIds != null
                    && (NvmlQuery.matches(busIds.getA(), needle) || NvmlQuery.matches(busIds.getB(), needle))) {
                found[0] = handle;
                return true;
            }
            return false;
        });
        return found[0];
    }

    /**
     * Acquires a fresh device handle within the current init scope by matching the GPU name. Must be called while NVML
     * is initialized.
     *
     * @param gpuName GPU name to match (case-insensitive substring)
     * @return device handle Pointer, or {@code null} if not found
     */
    private static Pointer acquireHandleByName(String gpuName) {
        String needle = gpuName.toLowerCase(Locale.ROOT);
        Pointer[] found = new Pointer[1];
        forEachDevice(handle -> {
            String name = readName(handle);
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
     * @return the number of matching devices, or {@code -1} if the devices could not be enumerated
     */
    private static int countMatchesByName(String gpuName) {
        String needle = gpuName.toLowerCase(Locale.ROOT);
        int[] matches = new int[1];
        boolean enumerated = forEachDevice(handle -> {
            String name = readName(handle);
            if (name != null && NvmlQuery.matches(name, needle)) {
                matches[0]++;
            }
            return false;
        });
        return enumerated ? matches[0] : -1;
    }

    // -------------------------------------------------------------------------
    // Metric readers — the only part that differs from the FFM binding
    // -------------------------------------------------------------------------

    private static double readUtilization(Pointer device) {
        NvmlUtilization util = new NvmlUtilization();
        if (Holder.LIB.nvmlDeviceGetUtilizationRates(device, util) == Nvml.NVML_SUCCESS) {
            util.read();
            return util.gpu;
        }
        return -1d;
    }

    private static long readVramTotal(Pointer device) {
        NvmlMemory mem = new NvmlMemory();
        if (Holder.LIB.nvmlDeviceGetMemoryInfo(device, mem) == Nvml.NVML_SUCCESS) {
            mem.read();
            return mem.total;
        }
        return -1L;
    }

    private static long readVramUsed(Pointer device) {
        NvmlMemory mem = new NvmlMemory();
        if (Holder.LIB.nvmlDeviceGetMemoryInfo(device, mem) == Nvml.NVML_SUCCESS) {
            mem.read();
            return mem.used;
        }
        return -1L;
    }

    private static double readTemperature(Pointer device) {
        IntByReference temp = new IntByReference();
        if (Holder.LIB.nvmlDeviceGetTemperature(device, Nvml.NVML_TEMPERATURE_GPU, temp) == Nvml.NVML_SUCCESS) {
            return temp.getValue();
        }
        return -1d;
    }

    private static double readPowerDraw(Pointer device) {
        IntByReference power = new IntByReference();
        if (Holder.LIB.nvmlDeviceGetPowerUsage(device, power) == Nvml.NVML_SUCCESS) {
            return power.getValue() / 1000.0;
        }
        return -1d;
    }

    private static long readClock(Pointer device, int clockType) {
        IntByReference clock = new IntByReference();
        if (Holder.LIB.nvmlDeviceGetClockInfo(device, clockType, clock) == Nvml.NVML_SUCCESS) {
            return clock.getValue();
        }
        return -1L;
    }

    private static double readFanSpeed(Pointer device) {
        IntByReference speed = new IntByReference();
        if (Holder.LIB.nvmlDeviceGetFanSpeed(device, speed) == Nvml.NVML_SUCCESS) {
            return speed.getValue();
        }
        return -1d;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns whether the NVML native library was successfully loaded. Does not indicate whether any NVIDIA GPU is
     * present or whether {@code nvmlInit_v2} will succeed.
     *
     * @return true if the NVML library is available
     */
    public static boolean isAvailable() {
        return Holder.LIBRARY_LOADED;
    }

    /**
     * Finds the stable PCI bus ID string for the NVML device whose bus ID contains the given fragment. The match is
     * case-insensitive and uses substring matching to accommodate domain-qualified vs. non-qualified forms.
     *
     * <p>
     * Returns a stable string identifier rather than a device handle. Handles are only valid within a single
     * {@code nvmlInit}/{@code nvmlShutdown} scope; returning one across that boundary would leave the caller with a
     * stale pointer. Callers should pass the returned string to the metric methods, which re-acquire a fresh handle
     * internally.
     *
     * @param pciBusId PCI bus ID fragment (e.g. {@code "0000:01:00.0"} or {@code "01:00.0"})
     * @return matched PCI bus ID string, or {@code null} if not found or NVML unavailable
     */
    public static String findDevice(String pciBusId) {
        if (!Holder.LIBRARY_LOADED || pciBusId == null || pciBusId.isEmpty()) {
            return null;
        }
        if (!nvmlInit()) {
            return null;
        }
        try {
            // Return the canonical bus ID from the enumerated set that matches, not a handle.
            return NvmlQuery.matchBusId(DEVICE_CACHE.get(NvmlUtilJNA::enumerateDeviceBusIds), pciBusId);
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Finds the stable PCI bus ID string for the NVML device whose name matches the given GPU name. Used as a fallback
     * when PCI bus ID is unavailable.
     *
     * <p>
     * Returns a stable string identifier rather than a device handle for the same reason as {@link #findDevice}.
     *
     * @param gpuName GPU name string (case-insensitive substring match)
     * @return PCI bus ID string of the matched device, or {@code null} if not found or NVML unavailable
     */
    public static String findDeviceByName(String gpuName) {
        if (!Holder.LIBRARY_LOADED || gpuName == null || gpuName.isEmpty()) {
            return null;
        }
        if (!nvmlInit()) {
            return null;
        }
        try {
            // Check for ambiguous name matches before committing to the first hit.
            int matchCount = countMatchesByName(gpuName);
            if (matchCount <= 0) {
                // Zero matches, or nvmlDeviceGetCount_v2 failed so the devices could not be enumerated.
                return null;
            }
            if (matchCount > 1) {
                LOG.warn("NVML name match for '{}' is ambiguous ({} devices match); use PCI bus ID for reliable"
                        + " device identification", gpuName, matchCount);
                return null;
            }
            // Acquire a handle by name to confirm the device exists, then extract its bus ID.
            Pointer handle = acquireHandleByName(gpuName);
            if (handle == null) {
                return null;
            }
            Pair<String, String> busIds = readBusIds(handle);
            if (busIds == null) {
                return null;
            }
            // Prefer the modern form, matching the order enumerateDeviceBusIds records them in.
            return busIds.getA().isEmpty() ? emptyToNull(busIds.getB()) : busIds.getA();
        } finally {
            nvmlUninit();
        }
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    /**
     * Returns GPU core utilization percentage (0–100), or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
     * @return utilization percentage or -1
     */
    public static double getGpuUtilization(String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, NvmlUtilJNA::readUtilization, -1d);
    }

    /**
     * Returns total VRAM in bytes, or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
     * @return total bytes or -1
     */
    public static long getVramTotal(String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, NvmlUtilJNA::readVramTotal, -1L);
    }

    /**
     * Returns VRAM used in bytes, or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
     * @return bytes used or -1
     */
    public static long getVramUsed(String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, NvmlUtilJNA::readVramUsed, -1L);
    }

    /**
     * Returns GPU temperature in degrees Celsius, or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
     * @return temperature in °C or -1
     */
    public static double getTemperature(String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, NvmlUtilJNA::readTemperature, -1d);
    }

    /**
     * Returns GPU power draw in watts, or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
     * @return power in watts or -1
     */
    public static double getPowerDraw(String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, NvmlUtilJNA::readPowerDraw, -1d);
    }

    /**
     * Returns GPU core clock speed in MHz, or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
     * @return core clock in MHz or -1
     */
    public static long getCoreClockMhz(String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, device -> readClock(device, Nvml.NVML_CLOCK_GRAPHICS), -1L);
    }

    /**
     * Returns GPU memory clock speed in MHz, or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
     * @return memory clock in MHz or -1
     */
    public static long getMemoryClockMhz(String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, device -> readClock(device, Nvml.NVML_CLOCK_MEM), -1L);
    }

    /**
     * Returns GPU fan speed as a percentage (0–100), or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
     * @return fan speed percentage or -1
     */
    public static double getFanSpeedPercent(String deviceId) {
        return NvmlQuery.query(deviceId, SCOPE, NvmlUtilJNA::readFanSpeed, -1d);
    }
}
