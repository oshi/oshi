/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.gpu;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.windows.Adl;
import oshi.jna.platform.windows.Adl.ADLODNFanControl;
import oshi.jna.platform.windows.Adl.ADLODNPerformanceStatus;
import oshi.jna.platform.windows.Adl.AdapterInfo;
import oshi.jna.platform.windows.Adl.AdlLibrary;
import oshi.jna.platform.windows.Adl.AdlMallocCallback;

/**
 * Optional runtime binding to the AMD Display Library (ADL) on Windows. All methods return sentinel values ({@code -1}
 * or {@code -1L}) when ADL is unavailable or a specific query fails.
 *
 * <p>
 * Only Overdrive N (Radeon RX 400 series and newer) is supported. Older Overdrive versions return -1.
 *
 * <p>
 * The native library is loaded once at class initialization. Each metric call pairs {@code ADL2_Main_Control_Create}
 * with {@code ADL2_Main_Control_Destroy} to correctly manage ADL's internal reference count, ensuring OSHI does not
 * interfere with other code in the same process that may also be managing the ADL lifecycle.
 *
 * <p>
 * Adapter bus-number-to-index mappings are enumerated once on first successful init and cached thereafter.
 */
@ThreadSafe
public final class AdlUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AdlUtil.class);

    // -------------------------------------------------------------------------
    // Library loading (holder pattern — loads the .dll once)
    // -------------------------------------------------------------------------

    // Retains Memory objects allocated by the ADL malloc callback until ADL frees them via the C runtime.
    // ADL calls our callback to allocate and then calls C free() directly — we cannot intercept the free,
    // so we keep all allocations alive for the process lifetime to prevent GC from collecting them early.
    private static final Set<Memory> ADL_ALLOCATIONS = ConcurrentHashMap.newKeySet();

    private static final class Holder {
        static final AdlLibrary LIB;
        static final boolean LIBRARY_LOADED;
        // Keep a reference to the malloc callback to prevent GC while ADL holds a native pointer to it.
        @SuppressWarnings("unused")
        static final AdlMallocCallback MALLOC_CB;

        static {
            AdlLibrary lib = null;
            boolean loaded = false;
            AdlMallocCallback cb = null;
            try {
                try {
                    lib = Native.load("atiadlxx", AdlLibrary.class);
                } catch (UnsatisfiedLinkError e) {
                    lib = Native.load("atiadlxy", AdlLibrary.class);
                }
                cb = size -> {
                    Memory mem = new Memory((long) size);
                    ADL_ALLOCATIONS.add(mem);
                    return mem;
                };
                loaded = true;
                LOG.debug("ADL library loaded");
            } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
                LOG.debug("ADL library not available: {}", e.getMessage());
            }
            LIB = lib;
            LIBRARY_LOADED = loaded;
            MALLOC_CB = cb;
        }
    }

    private AdlUtil() {
    }

    // Lazy adapter enumeration state — written once, read-only thereafter
    private static volatile boolean adaptersEnumerated = false;
    private static volatile Map<Integer, Integer> busToIndex = Collections.emptyMap();

    // -------------------------------------------------------------------------
    // Init/uninit helpers (COM pattern)
    // -------------------------------------------------------------------------

    /**
     * Calls {@code ADL2_Main_Control_Create}, incrementing ADL's internal reference count. Every successful call must
     * be paired with exactly one call to {@link #adlUninit(Pointer)}, which decrements the same counter. This ensures
     * OSHI does not permanently hold a reference that would interfere with other code in the process managing the ADL
     * lifecycle.
     *
     * @return the ADL context pointer if initialization succeeded, or {@code null} if it failed
     */
    private static Pointer adlInit() {
        if (!Holder.LIBRARY_LOADED) {
            return null;
        }
        PointerByReference ctxRef = new PointerByReference();
        int ret = Holder.LIB.ADL2_Main_Control_Create(Holder.MALLOC_CB, 1, ctxRef);
        if (ret == Adl.ADL_OK) {
            return ctxRef.getValue();
        }
        LOG.debug("ADL2_Main_Control_Create failed with code {}", ret);
        return null;
    }

    /**
     * Calls {@code ADL2_Main_Control_Destroy}, decrementing the same internal reference count that {@link #adlInit()}
     * incremented. Must be called exactly once for each successful call to {@link #adlInit()}.
     *
     * @param context the ADL context pointer returned by {@link #adlInit()}
     */
    private static void adlUninit(Pointer context) {
        Holder.LIB.ADL2_Main_Control_Destroy(context);
    }

    /**
     * Enumerates adapter mappings on first call after a successful init. Subsequent calls are no-ops. Must be called
     * while ADL is initialized (i.e. between adlInit and adlUninit).
     *
     * @param context the ADL context pointer
     */
    private static void ensureAdaptersEnumerated(Pointer context) {
        if (adaptersEnumerated) {
            return;
        }
        Map<Integer, Integer> result = enumerateAdapters(context);
        if (result != null) {
            busToIndex = result;
            adaptersEnumerated = true;
            LOG.debug("ADL enumerated {} adapter(s)", busToIndex.size());
        } else {
            LOG.debug("ADL adapter enumeration failed; will retry on next call");
        }
    }

    private static Map<Integer, Integer> enumerateAdapters(Pointer ctx) {
        IntByReference numRef = new IntByReference();
        if (Holder.LIB.ADL2_Adapter_NumberOfAdapters_Get(ctx, numRef) != Adl.ADL_OK) {
            return null;
        }
        int num = numRef.getValue();
        if (num <= 0) {
            return Collections.emptyMap();
        }
        AdapterInfo[] infos = (AdapterInfo[]) new AdapterInfo().toArray(num);
        for (AdapterInfo info : infos) {
            info.iSize = info.size();
        }
        if (Holder.LIB.ADL2_Adapter_AdapterInfo_Get(ctx, infos, infos[0].size() * num) != Adl.ADL_OK) {
            return null;
        }
        Map<Integer, Integer> map = new HashMap<>();
        for (AdapterInfo info : infos) {
            if (info.iPresent != 0) {
                map.put(info.iBusNumber, info.iAdapterIndex);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static boolean supportsOverdriveN(Pointer context, int adapterIndex) {
        IntByReference supported = new IntByReference();
        IntByReference enabled = new IntByReference();
        IntByReference version = new IntByReference();
        if (Holder.LIB.ADL2_Overdrive_Caps(context, adapterIndex, supported, enabled, version) == Adl.ADL_OK) {
            return version.getValue() >= Adl.ADL_OVERDRIVE_VERSION_N;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns whether the ADL native library was successfully loaded. Does not indicate whether any AMD GPU is present
     * or whether {@code ADL2_Main_Control_Create} will succeed.
     *
     * @return true if the ADL library is available
     */
    public static boolean isAvailable() {
        return Holder.LIBRARY_LOADED;
    }

    /**
     * Finds the ADL adapter index for the given PCI bus number.
     *
     * @param pciBusNumber PCI bus number
     * @return adapter index, or -1 if not found or ADL unavailable
     */
    public static int findAdapterIndex(int pciBusNumber) {
        if (!Holder.LIBRARY_LOADED) {
            return -1;
        }
        Pointer ctx = adlInit();
        if (ctx == null) {
            return -1;
        }
        try {
            ensureAdaptersEnumerated(ctx);
            return busToIndex.getOrDefault(pciBusNumber, -1);
        } finally {
            adlUninit(ctx);
        }
    }

    /**
     * Returns GPU temperature in degrees Celsius, or -1 if unavailable.
     *
     * @param adapterIndex ADL adapter index from {@link #findAdapterIndex(int)}
     * @return temperature in °C or -1
     */
    public static double getTemperature(int adapterIndex) {
        if (adapterIndex < 0) {
            return -1d;
        }
        Pointer ctx = adlInit();
        if (ctx == null) {
            return -1d;
        }
        try {
            if (!supportsOverdriveN(ctx, adapterIndex)) {
                return -1d;
            }
            IntByReference temp = new IntByReference();
            if (Holder.LIB.ADL2_OverdriveN_Temperature_Get(ctx, adapterIndex, Adl.ADL_OVERDRIVE_TEMPERATURE_EDGE,
                    temp) == Adl.ADL_OK) {
                return temp.getValue() / 1000.0;
            }
            return -1d;
        } finally {
            adlUninit(ctx);
        }
    }

    /**
     * Returns GPU core utilization percentage (0–100), or -1 if unavailable.
     *
     * @param adapterIndex ADL adapter index
     * @return utilization percentage or -1
     */
    public static double getGpuUtilization(int adapterIndex) {
        if (adapterIndex < 0) {
            return -1d;
        }
        Pointer ctx = adlInit();
        if (ctx == null) {
            return -1d;
        }
        try {
            if (!supportsOverdriveN(ctx, adapterIndex)) {
                return -1d;
            }
            ADLODNPerformanceStatus perf = new ADLODNPerformanceStatus();
            if (Holder.LIB.ADL2_OverdriveN_PerformanceStatus_Get(ctx, adapterIndex, perf) == Adl.ADL_OK) {
                perf.read();
                return perf.iGPUActivityPercent;
            }
            return -1d;
        } finally {
            adlUninit(ctx);
        }
    }

    /**
     * Returns GPU core clock speed in MHz, or -1 if unavailable. ADL reports clocks in 10 kHz units.
     *
     * @param adapterIndex ADL adapter index
     * @return core clock in MHz or -1
     */
    public static long getCoreClockMhz(int adapterIndex) {
        if (adapterIndex < 0) {
            return -1L;
        }
        Pointer ctx = adlInit();
        if (ctx == null) {
            return -1L;
        }
        try {
            if (!supportsOverdriveN(ctx, adapterIndex)) {
                return -1L;
            }
            ADLODNPerformanceStatus perf = new ADLODNPerformanceStatus();
            if (Holder.LIB.ADL2_OverdriveN_PerformanceStatus_Get(ctx, adapterIndex, perf) == Adl.ADL_OK) {
                perf.read();
                // iCoreClock is in 10 kHz units; divide by 100 to get MHz
                return perf.iCoreClock / 100L;
            }
            return -1L;
        } finally {
            adlUninit(ctx);
        }
    }

    /**
     * Returns GPU memory clock speed in MHz, or -1 if unavailable.
     *
     * @param adapterIndex ADL adapter index
     * @return memory clock in MHz or -1
     */
    public static long getMemoryClockMhz(int adapterIndex) {
        if (adapterIndex < 0) {
            return -1L;
        }
        Pointer ctx = adlInit();
        if (ctx == null) {
            return -1L;
        }
        try {
            if (!supportsOverdriveN(ctx, adapterIndex)) {
                return -1L;
            }
            ADLODNPerformanceStatus perf = new ADLODNPerformanceStatus();
            if (Holder.LIB.ADL2_OverdriveN_PerformanceStatus_Get(ctx, adapterIndex, perf) == Adl.ADL_OK) {
                perf.read();
                return perf.iMemoryClock / 100L;
            }
            return -1L;
        } finally {
            adlUninit(ctx);
        }
    }

    /**
     * Returns GPU power draw in watts, or -1 if unavailable. Uses Overdrive 6 power API which is available on Overdrive
     * N adapters. Power is reported in units of 1/256 watts.
     *
     * @param adapterIndex ADL adapter index
     * @return power in watts or -1
     */
    public static double getPowerDraw(int adapterIndex) {
        if (adapterIndex < 0) {
            return -1d;
        }
        Pointer ctx = adlInit();
        if (ctx == null) {
            return -1d;
        }
        try {
            if (!supportsOverdriveN(ctx, adapterIndex)) {
                return -1d;
            }
            IntByReference power = new IntByReference();
            if (Holder.LIB.ADL2_Overdrive6_CurrentPower_Get(ctx, adapterIndex, 0, power) == Adl.ADL_OK) {
                return power.getValue() / 256.0;
            }
            return -1d;
        } finally {
            adlUninit(ctx);
        }
    }

    /**
     * Returns GPU fan speed as a percentage (0–100), or -1 if unavailable.
     *
     * @param adapterIndex ADL adapter index
     * @return fan speed percentage or -1
     */
    public static double getFanSpeedPercent(int adapterIndex) {
        if (adapterIndex < 0) {
            return -1d;
        }
        Pointer ctx = adlInit();
        if (ctx == null) {
            return -1d;
        }
        try {
            if (!supportsOverdriveN(ctx, adapterIndex)) {
                return -1d;
            }
            ADLODNFanControl fan = new ADLODNFanControl();
            if (Holder.LIB.ADL2_OverdriveN_FanControl_Get(ctx, adapterIndex, fan) == Adl.ADL_OK) {
                fan.read();
                if (fan.iFanControlMode == Adl.ADL_FAN_SPEED_MODE_PERCENT) {
                    return fan.iCurrentFanSpeed;
                }
                // RPM mode: no max available here, return -1
            }
            return -1d;
        } finally {
            adlUninit(ctx);
        }
    }
}
