/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.gpu;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * FFM-based optional runtime binding to the AMD Display Library (ADL) on Windows. All methods return sentinel values
 * ({@code -1} or {@code -1d}) when ADL is unavailable or a specific query fails.
 *
 * <p>
 * Only Overdrive N (Radeon RX 400 series and newer) is supported.
 */
@ThreadSafe
public final class AdlUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(AdlUtilFFM.class);

    private static final int ADL_OK = 0;
    private static final int ADL_OVERDRIVE_VERSION_N = 8;
    private static final int ADL_FAN_SPEED_MODE_PERCENT = 1;
    private static final int ADL_OVERDRIVE_TEMPERATURE_EDGE = 1;

    // AdapterInfo struct: iSize(4) + iAdapterIndex(4) + strUDID(256) + iBusNumber(4) + iDeviceNumber(4)
    // + iFunctionNumber(4) + iVendorID(4) + strAdapterName(256) + strDisplayName(256) + iPresent(4)
    // + iExist(4) + strDriverPath(256) + strDriverPathExt(256) + strPNPString(256) + iOSDisplayIndex(4)
    private static final int ADAPTER_INFO_SIZE = 1572;
    private static final long AI_ADAPTER_INDEX_OFFSET = 4;
    private static final long AI_BUS_NUMBER_OFFSET = 264;
    private static final long AI_PRESENT_OFFSET = 792;

    // ADLODNPerformanceStatus: 18 ints = 72 bytes
    private static final int PERF_STATUS_SIZE = 72;
    private static final long PERF_CORE_CLOCK_OFFSET = 0;
    private static final long PERF_MEMORY_CLOCK_OFFSET = 4;
    private static final long PERF_GPU_ACTIVITY_OFFSET = 24;

    // ADLODNFanControl: 8 ints = 32 bytes
    private static final int FAN_CONTROL_SIZE = 32;
    private static final long FAN_CONTROL_MODE_OFFSET = 4;
    private static final long FAN_CURRENT_SPEED_OFFSET = 12;

    private static final boolean AVAILABLE;
    private static final MethodHandle ADL2_MAIN_CONTROL_CREATE;
    private static final MethodHandle ADL2_MAIN_CONTROL_DESTROY;
    private static final MethodHandle ADL2_ADAPTER_NUMBER_OF_ADAPTERS_GET;
    private static final MethodHandle ADL2_ADAPTER_ADAPTER_INFO_GET;
    private static final MethodHandle ADL2_OVERDRIVE_CAPS;
    private static final MethodHandle ADL2_OVERDRIVEN_TEMPERATURE_GET;
    private static final MethodHandle ADL2_OVERDRIVEN_PERFORMANCE_STATUS_GET;
    private static final MethodHandle ADL2_OVERDRIVEN_FAN_CONTROL_GET;
    private static final MethodHandle ADL2_OVERDRIVE6_CURRENT_POWER_GET;
    // Upcall stub for malloc callback
    private static final MemorySegment MALLOC_STUB;

    static {
        boolean available = false;
        MethodHandle hCreate = null;
        MethodHandle hDestroy = null;
        MethodHandle hNumAdapters = null;
        MethodHandle hAdapterInfo = null;
        MethodHandle hCaps = null;
        MethodHandle hTemp = null;
        MethodHandle hPerf = null;
        MethodHandle hFan = null;
        MethodHandle hPower = null;
        MemorySegment mallocStub = MemorySegment.NULL;
        try {
            SymbolLookup adl;
            try {
                adl = SymbolLookup.libraryLookup("atiadlxx", Arena.global());
            } catch (Throwable t) {
                adl = SymbolLookup.libraryLookup("atiadlxy", Arena.global());
            }
            Linker linker = Linker.nativeLinker();

            // Create malloc upcall: int (*)(int size) -> Pointer
            // ADL expects __stdcall on Windows but FFM uses default calling convention which works for x64
            FunctionDescriptor mallocDesc = FunctionDescriptor.of(ADDRESS, JAVA_INT);
            MethodHandle mallocTarget = java.lang.invoke.MethodHandles.lookup().findStatic(AdlUtilFFM.class,
                    "adlMalloc", java.lang.invoke.MethodType.methodType(MemorySegment.class, int.class));
            mallocStub = linker.upcallStub(mallocTarget, mallocDesc, Arena.global());

            hCreate = linker.downcallHandle(adl.findOrThrow("ADL2_Main_Control_Create"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
            hDestroy = linker.downcallHandle(adl.findOrThrow("ADL2_Main_Control_Destroy"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS));
            hNumAdapters = linker.downcallHandle(adl.findOrThrow("ADL2_Adapter_NumberOfAdapters_Get"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            hAdapterInfo = linker.downcallHandle(adl.findOrThrow("ADL2_Adapter_AdapterInfo_Get"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
            hCaps = linker.downcallHandle(adl.findOrThrow("ADL2_Overdrive_Caps"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
            hTemp = linker.downcallHandle(adl.findOrThrow("ADL2_OverdriveN_Temperature_Get"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));
            hPerf = linker.downcallHandle(adl.findOrThrow("ADL2_OverdriveN_PerformanceStatus_Get"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
            hFan = linker.downcallHandle(adl.findOrThrow("ADL2_OverdriveN_FanControl_Get"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
            hPower = linker.downcallHandle(adl.findOrThrow("ADL2_Overdrive6_CurrentPower_Get"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));
            available = true;
            LOG.debug("ADL library loaded via FFM");
        } catch (Throwable t) {
            LOG.debug("ADL library not available via FFM: {}", t.getMessage());
        }
        AVAILABLE = available;
        ADL2_MAIN_CONTROL_CREATE = hCreate;
        ADL2_MAIN_CONTROL_DESTROY = hDestroy;
        ADL2_ADAPTER_NUMBER_OF_ADAPTERS_GET = hNumAdapters;
        ADL2_ADAPTER_ADAPTER_INFO_GET = hAdapterInfo;
        ADL2_OVERDRIVE_CAPS = hCaps;
        ADL2_OVERDRIVEN_TEMPERATURE_GET = hTemp;
        ADL2_OVERDRIVEN_PERFORMANCE_STATUS_GET = hPerf;
        ADL2_OVERDRIVEN_FAN_CONTROL_GET = hFan;
        ADL2_OVERDRIVE6_CURRENT_POWER_GET = hPower;
        MALLOC_STUB = mallocStub;
    }

    private AdlUtilFFM() {
    }

    // Lazy adapter enumeration
    private static volatile boolean adaptersEnumerated = false;
    private static final AtomicReference<Map<Integer, Integer>> BUS_TO_INDEX = new AtomicReference<>(
            Collections.emptyMap());

    /**
     * ADL malloc callback invoked by the native library.
     *
     * @param size the number of bytes to allocate
     * @return a memory segment of the requested size, or NULL if size is non-positive
     */
    @SuppressWarnings("unused")
    private static MemorySegment adlMalloc(int size) {
        if (size <= 0) {
            return MemorySegment.NULL;
        }
        return Arena.global().allocate(size);
    }

    private static MemorySegment adlInit(Arena arena) {
        if (!AVAILABLE) {
            return null;
        }
        try {
            MemorySegment ctxRef = arena.allocate(ADDRESS);
            int ret = (int) ADL2_MAIN_CONTROL_CREATE.invokeExact(MALLOC_STUB, 1, ctxRef);
            if (ret == ADL_OK) {
                return ctxRef.get(ADDRESS, 0);
            }
            LOG.debug("ADL2_Main_Control_Create failed with code {}", ret);
        } catch (Throwable t) {
            LOG.debug("ADL init failed: {}", t.getMessage());
        }
        return null;
    }

    private static void adlUninit(MemorySegment context) {
        try {
            ADL2_MAIN_CONTROL_DESTROY.invokeExact(context);
        } catch (Throwable t) {
            LOG.debug("ADL uninit failed: {}", t.getMessage());
        }
    }

    private static void ensureAdaptersEnumerated(MemorySegment context, Arena arena) {
        if (adaptersEnumerated) {
            return;
        }
        Map<Integer, Integer> result = enumerateAdapters(context, arena);
        if (result != null) {
            BUS_TO_INDEX.set(result);
            adaptersEnumerated = true;
            LOG.debug("ADL (FFM) enumerated {} adapter(s)", BUS_TO_INDEX.get().size());
        }
    }

    private static Map<Integer, Integer> enumerateAdapters(MemorySegment ctx, Arena arena) {
        try {
            MemorySegment numRef = arena.allocate(JAVA_INT);
            if ((int) ADL2_ADAPTER_NUMBER_OF_ADAPTERS_GET.invokeExact(ctx, numRef) != ADL_OK) {
                return null;
            }
            int num = numRef.get(JAVA_INT, 0);
            if (num <= 0) {
                return Collections.emptyMap();
            }
            MemorySegment infos = arena.allocate((long) ADAPTER_INFO_SIZE * num);
            // Set iSize for each entry
            for (int i = 0; i < num; i++) {
                infos.set(JAVA_INT, (long) i * ADAPTER_INFO_SIZE, ADAPTER_INFO_SIZE);
            }
            if ((int) ADL2_ADAPTER_ADAPTER_INFO_GET.invokeExact(ctx, infos, ADAPTER_INFO_SIZE * num) != ADL_OK) {
                return null;
            }
            Map<Integer, Integer> map = new HashMap<>();
            for (int i = 0; i < num; i++) {
                long base = (long) i * ADAPTER_INFO_SIZE;
                int present = infos.get(JAVA_INT, base + AI_PRESENT_OFFSET);
                if (present != 0) {
                    int busNumber = infos.get(JAVA_INT, base + AI_BUS_NUMBER_OFFSET);
                    int adapterIndex = infos.get(JAVA_INT, base + AI_ADAPTER_INDEX_OFFSET);
                    map.put(busNumber, adapterIndex);
                }
            }
            return Collections.unmodifiableMap(map);
        } catch (Throwable t) {
            LOG.debug("ADL adapter enumeration failed: {}", t.getMessage());
            return null;
        }
    }

    private static boolean supportsOverdriveN(MemorySegment context, int adapterIndex, Arena arena) {
        try {
            MemorySegment supported = arena.allocate(JAVA_INT);
            MemorySegment enabled = arena.allocate(JAVA_INT);
            MemorySegment version = arena.allocate(JAVA_INT);
            if ((int) ADL2_OVERDRIVE_CAPS.invokeExact(context, adapterIndex, supported, enabled, version) == ADL_OK) {
                return version.get(JAVA_INT, 0) >= ADL_OVERDRIVE_VERSION_N;
            }
        } catch (Throwable t) {
            LOG.debug("ADL Overdrive_Caps failed: {}", t.getMessage());
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static int findAdapterIndex(int pciBusNumber) {
        if (!AVAILABLE) {
            return -1;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ctx = adlInit(arena);
            if (ctx == null) {
                return -1;
            }
            try {
                ensureAdaptersEnumerated(ctx, arena);
                return BUS_TO_INDEX.get().getOrDefault(pciBusNumber, -1);
            } finally {
                adlUninit(ctx);
            }
        }
    }

    public static double getTemperature(int adapterIndex) {
        if (adapterIndex < 0) {
            return -1d;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ctx = adlInit(arena);
            if (ctx == null) {
                return -1d;
            }
            try {
                if (!supportsOverdriveN(ctx, adapterIndex, arena)) {
                    return -1d;
                }
                MemorySegment temp = arena.allocate(JAVA_INT);
                if ((int) ADL2_OVERDRIVEN_TEMPERATURE_GET.invokeExact(ctx, adapterIndex, ADL_OVERDRIVE_TEMPERATURE_EDGE,
                        temp) == ADL_OK) {
                    return temp.get(JAVA_INT, 0) / 1000.0;
                }
            } finally {
                adlUninit(ctx);
            }
        } catch (Throwable t) {
            LOG.debug("ADL getTemperature failed: {}", t.getMessage());
        }
        return -1d;
    }

    public static long getCoreClockMhz(int adapterIndex) {
        if (adapterIndex < 0) {
            return -1L;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ctx = adlInit(arena);
            if (ctx == null) {
                return -1L;
            }
            try {
                if (!supportsOverdriveN(ctx, adapterIndex, arena)) {
                    return -1L;
                }
                MemorySegment perf = arena.allocate(PERF_STATUS_SIZE);
                if ((int) ADL2_OVERDRIVEN_PERFORMANCE_STATUS_GET.invokeExact(ctx, adapterIndex, perf) == ADL_OK) {
                    return perf.get(JAVA_INT, PERF_CORE_CLOCK_OFFSET) / 100L;
                }
            } finally {
                adlUninit(ctx);
            }
        } catch (Throwable t) {
            LOG.debug("ADL getCoreClockMhz failed: {}", t.getMessage());
        }
        return -1L;
    }

    public static long getMemoryClockMhz(int adapterIndex) {
        if (adapterIndex < 0) {
            return -1L;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ctx = adlInit(arena);
            if (ctx == null) {
                return -1L;
            }
            try {
                if (!supportsOverdriveN(ctx, adapterIndex, arena)) {
                    return -1L;
                }
                MemorySegment perf = arena.allocate(PERF_STATUS_SIZE);
                if ((int) ADL2_OVERDRIVEN_PERFORMANCE_STATUS_GET.invokeExact(ctx, adapterIndex, perf) == ADL_OK) {
                    return perf.get(JAVA_INT, PERF_MEMORY_CLOCK_OFFSET) / 100L;
                }
            } finally {
                adlUninit(ctx);
            }
        } catch (Throwable t) {
            LOG.debug("ADL getMemoryClockMhz failed: {}", t.getMessage());
        }
        return -1L;
    }

    public static double getPowerDraw(int adapterIndex) {
        if (adapterIndex < 0) {
            return -1d;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ctx = adlInit(arena);
            if (ctx == null) {
                return -1d;
            }
            try {
                if (!supportsOverdriveN(ctx, adapterIndex, arena)) {
                    return -1d;
                }
                MemorySegment power = arena.allocate(JAVA_INT);
                if ((int) ADL2_OVERDRIVE6_CURRENT_POWER_GET.invokeExact(ctx, adapterIndex, 0, power) == ADL_OK) {
                    return power.get(JAVA_INT, 0) / 256.0;
                }
            } finally {
                adlUninit(ctx);
            }
        } catch (Throwable t) {
            LOG.debug("ADL getPowerDraw failed: {}", t.getMessage());
        }
        return -1d;
    }

    public static double getFanSpeedPercent(int adapterIndex) {
        if (adapterIndex < 0) {
            return -1d;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ctx = adlInit(arena);
            if (ctx == null) {
                return -1d;
            }
            try {
                if (!supportsOverdriveN(ctx, adapterIndex, arena)) {
                    return -1d;
                }
                MemorySegment fan = arena.allocate(FAN_CONTROL_SIZE);
                if ((int) ADL2_OVERDRIVEN_FAN_CONTROL_GET.invokeExact(ctx, adapterIndex, fan) == ADL_OK) {
                    int mode = fan.get(JAVA_INT, FAN_CONTROL_MODE_OFFSET);
                    if (mode == ADL_FAN_SPEED_MODE_PERCENT) {
                        return fan.get(JAVA_INT, FAN_CURRENT_SPEED_OFFSET);
                    }
                }
            } finally {
                adlUninit(ctx);
            }
        } catch (Throwable t) {
            LOG.debug("ADL getFanSpeedPercent failed: {}", t.getMessage());
        }
        return -1d;
    }
}
