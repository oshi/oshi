/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.gpu;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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
 * GraphicsCard instances.
 */
@ThreadSafe
public final class NvmlUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NvmlUtil.class);

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
                LOG.debug("NVML library not available: {}", e.getMessage());
            }
            LIB = lib;
            LIBRARY_LOADED = loaded;
        }
    }

    // Lazy device enumeration state — written once on first successful enumeration, read-only thereafter.
    // Stores PCI bus ID strings (stable identifiers) rather than Pointer handles, which are only valid
    // within a single nvmlInit/nvmlShutdown scope.
    private static volatile boolean devicesEnumerated = false;
    private static volatile Set<String> deviceBusIds = Collections.emptySet();

    private NvmlUtil() {
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

    /**
     * Enumerates device PCI bus IDs on first call after a successful init. Only sets {@code devicesEnumerated} on
     * success so transient NVML failures allow a retry on the next call. Must be called while NVML is initialized.
     */
    private static void ensureDevicesEnumerated() {
        if (devicesEnumerated) {
            return;
        }
        Set<String> ids = enumerateDeviceBusIds();
        if (ids != null) {
            deviceBusIds = ids;
            devicesEnumerated = true;
            LOG.debug("NVML enumerated {} device(s)", deviceBusIds.size());
        }
    }

    /**
     * Returns a set of PCI bus ID strings for all NVML devices, or {@code null} on NVML error (so the caller can
     * distinguish a real failure from a legitimate empty result).
     *
     * @return set of PCI bus ID strings, or {@code null} on NVML error
     */
    private static Set<String> enumerateDeviceBusIds() {
        IntByReference countRef = new IntByReference();
        if (Holder.LIB.nvmlDeviceGetCount_v2(countRef) != Nvml.NVML_SUCCESS) {
            return null;
        }
        int count = countRef.getValue();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < count; i++) {
            PointerByReference handleRef = new PointerByReference();
            if (Holder.LIB.nvmlDeviceGetHandleByIndex_v2(i, handleRef) != Nvml.NVML_SUCCESS) {
                continue;
            }
            Pointer handle = handleRef.getValue();
            NvmlPciInfo pci = new NvmlPciInfo();
            if (Holder.LIB.nvmlDeviceGetPciInfo_v3(handle, pci) == Nvml.NVML_SUCCESS) {
                pci.read();
                String busId = Native.toString(pci.busId).toLowerCase(Locale.ROOT);
                if (!busId.isEmpty()) {
                    ids.add(busId);
                }
                String legacyId = Native.toString(pci.busIdLegacy).toLowerCase(Locale.ROOT);
                if (!legacyId.isEmpty()) {
                    ids.add(legacyId);
                }
            }
        }
        return Collections.unmodifiableSet(ids);
    }

    /**
     * Acquires a fresh device handle within the current init scope by matching the given PCI bus ID fragment. Must be
     * called while NVML is initialized.
     *
     * @param pciBusId PCI bus ID fragment to match
     * @return device handle Pointer, or {@code null} if not found
     */
    private static Pointer acquireHandleByBusId(String pciBusId) {
        IntByReference countRef = new IntByReference();
        if (Holder.LIB.nvmlDeviceGetCount_v2(countRef) != Nvml.NVML_SUCCESS) {
            return null;
        }
        String needle = pciBusId.toLowerCase(Locale.ROOT);
        int count = countRef.getValue();
        for (int i = 0; i < count; i++) {
            PointerByReference handleRef = new PointerByReference();
            if (Holder.LIB.nvmlDeviceGetHandleByIndex_v2(i, handleRef) != Nvml.NVML_SUCCESS) {
                continue;
            }
            Pointer handle = handleRef.getValue();
            NvmlPciInfo pci = new NvmlPciInfo();
            if (Holder.LIB.nvmlDeviceGetPciInfo_v3(handle, pci) == Nvml.NVML_SUCCESS) {
                pci.read();
                String busId = Native.toString(pci.busId).toLowerCase(Locale.ROOT);
                String legacyId = Native.toString(pci.busIdLegacy).toLowerCase(Locale.ROOT);
                if (busId.contains(needle) || needle.contains(busId) || legacyId.contains(needle)
                        || needle.contains(legacyId)) {
                    return handle;
                }
            }
        }
        return null;
    }

    /**
     * Acquires a fresh device handle within the current init scope by matching the GPU name. Must be called while NVML
     * is initialized.
     *
     * @param gpuName GPU name to match (case-insensitive substring)
     * @return device handle Pointer, or {@code null} if not found
     */
    private static Pointer acquireHandleByName(String gpuName) {
        IntByReference countRef = new IntByReference();
        if (Holder.LIB.nvmlDeviceGetCount_v2(countRef) != Nvml.NVML_SUCCESS) {
            return null;
        }
        String needle = gpuName.toLowerCase(Locale.ROOT);
        int count = countRef.getValue();
        for (int i = 0; i < count; i++) {
            PointerByReference handleRef = new PointerByReference();
            if (Holder.LIB.nvmlDeviceGetHandleByIndex_v2(i, handleRef) != Nvml.NVML_SUCCESS) {
                continue;
            }
            Pointer handle = handleRef.getValue();
            byte[] nameBuf = new byte[Nvml.NVML_DEVICE_NAME_BUFFER_SIZE];
            if (Holder.LIB.nvmlDeviceGetName(handle, nameBuf, nameBuf.length) == Nvml.NVML_SUCCESS) {
                String name = Native.toString(nameBuf).toLowerCase(Locale.ROOT);
                if (name.contains(needle) || needle.contains(name)) {
                    return handle;
                }
            }
        }
        return null;
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
        boolean init = nvmlInit();
        if (!init) {
            return null;
        }
        try {
            ensureDevicesEnumerated();
            // Verify a handle can be acquired (device exists), then return the canonical bus ID
            // from the enumerated set that matches — not the handle itself.
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
        boolean init = nvmlInit();
        if (!init) {
            return null;
        }
        try {
            ensureDevicesEnumerated();
            // Acquire a handle by name to confirm the device exists, then extract its bus ID.
            Pointer handle = acquireHandleByName(gpuName);
            if (handle == null) {
                return null;
            }
            NvmlPciInfo pci = new NvmlPciInfo();
            if (Holder.LIB.nvmlDeviceGetPciInfo_v3(handle, pci) == Nvml.NVML_SUCCESS) {
                pci.read();
                String busId = Native.toString(pci.busId).toLowerCase(Locale.ROOT);
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
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
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
        try {
            Pointer device = acquireHandleByBusId(deviceId);
            if (device == null) {
                return -1d;
            }
            NvmlUtilization util = new NvmlUtilization();
            if (Holder.LIB.nvmlDeviceGetUtilizationRates(device, util) == Nvml.NVML_SUCCESS) {
                util.read();
                return util.gpu;
            }
            return -1d;
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Returns VRAM used in bytes, or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
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
        try {
            Pointer device = acquireHandleByBusId(deviceId);
            if (device == null) {
                return -1L;
            }
            NvmlMemory mem = new NvmlMemory();
            if (Holder.LIB.nvmlDeviceGetMemoryInfo(device, mem) == Nvml.NVML_SUCCESS) {
                mem.read();
                return mem.used;
            }
            return -1L;
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Returns GPU temperature in degrees Celsius, or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
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
        try {
            Pointer device = acquireHandleByBusId(deviceId);
            if (device == null) {
                return -1d;
            }
            IntByReference temp = new IntByReference();
            if (Holder.LIB.nvmlDeviceGetTemperature(device, Nvml.NVML_TEMPERATURE_GPU, temp) == Nvml.NVML_SUCCESS) {
                return temp.getValue();
            }
            return -1d;
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Returns GPU power draw in watts, or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
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
        try {
            Pointer device = acquireHandleByBusId(deviceId);
            if (device == null) {
                return -1d;
            }
            IntByReference power = new IntByReference();
            if (Holder.LIB.nvmlDeviceGetPowerUsage(device, power) == Nvml.NVML_SUCCESS) {
                return power.getValue() / 1000.0;
            }
            return -1d;
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Returns GPU core clock speed in MHz, or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
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
        try {
            Pointer device = acquireHandleByBusId(deviceId);
            if (device == null) {
                return -1L;
            }
            IntByReference clock = new IntByReference();
            if (Holder.LIB.nvmlDeviceGetClockInfo(device, Nvml.NVML_CLOCK_GRAPHICS, clock) == Nvml.NVML_SUCCESS) {
                return clock.getValue();
            }
            return -1L;
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Returns GPU memory clock speed in MHz, or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
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
        try {
            Pointer device = acquireHandleByBusId(deviceId);
            if (device == null) {
                return -1L;
            }
            IntByReference clock = new IntByReference();
            if (Holder.LIB.nvmlDeviceGetClockInfo(device, Nvml.NVML_CLOCK_MEM, clock) == Nvml.NVML_SUCCESS) {
                return clock.getValue();
            }
            return -1L;
        } finally {
            nvmlUninit();
        }
    }

    /**
     * Returns GPU fan speed as a percentage (0–100), or -1 if unavailable.
     *
     * @param deviceId stable device identifier returned by {@link #findDevice} or {@link #findDeviceByName}
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
        try {
            Pointer device = acquireHandleByBusId(deviceId);
            if (device == null) {
                return -1d;
            }
            IntByReference speed = new IntByReference();
            if (Holder.LIB.nvmlDeviceGetFanSpeed(device, speed) == Nvml.NVML_SUCCESS) {
                return speed.getValue();
            }
            return -1d;
        } finally {
            nvmlUninit();
        }
    }
}
