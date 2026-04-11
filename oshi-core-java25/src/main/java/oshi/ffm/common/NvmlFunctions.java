/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.common;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.ForeignFunctions;

/**
 * FFM bindings for the NVIDIA Management Library (NVML).
 */
public final class NvmlFunctions extends ForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(NvmlFunctions.class);

    private NvmlFunctions() {
    }

    // Constants matching NVML C header
    /** Successful return code. */
    public static final int NVML_SUCCESS = 0;
    /** GPU temperature sensor type. */
    public static final int NVML_TEMPERATURE_GPU = 0;
    /** Graphics clock domain. */
    public static final int NVML_CLOCK_GRAPHICS = 0;
    /** Memory clock domain. */
    public static final int NVML_CLOCK_MEM = 2;
    /** Buffer size for device name queries. */
    public static final int NVML_DEVICE_NAME_BUFFER_SIZE = 96;

    // nvmlUtilization_t: { unsigned int gpu; unsigned int memory; }
    /** Layout for nvmlUtilization_t struct. */
    public static final StructLayout UTILIZATION_LAYOUT = MemoryLayout.structLayout(JAVA_INT.withName("gpu"),
            JAVA_INT.withName("memory"));

    // nvmlMemory_t: { unsigned long long total; unsigned long long free; unsigned long long used; }
    /** Layout for nvmlMemory_t struct. */
    public static final StructLayout MEMORY_LAYOUT = MemoryLayout.structLayout(JAVA_LONG.withName("total"),
            JAVA_LONG.withName("free"), JAVA_LONG.withName("used"));

    // nvmlPciInfo_t: { char busIdLegacy[16]; unsigned int domain, bus, device, pciDeviceId, pciSubSystemId;
    // char busId[32]; }
    /** Layout for nvmlPciInfo_t struct. */
    public static final StructLayout PCI_INFO_LAYOUT = MemoryLayout.structLayout(
            MemoryLayout.sequenceLayout(16, JAVA_BYTE).withName("busIdLegacy"), JAVA_INT.withName("domain"),
            JAVA_INT.withName("bus"), JAVA_INT.withName("device"), JAVA_INT.withName("pciDeviceId"),
            JAVA_INT.withName("pciSubSystemId"), MemoryLayout.sequenceLayout(32, JAVA_BYTE).withName("busId"));

    // Method handles
    private static final MethodHandle nvmlInit_v2;
    private static final MethodHandle nvmlShutdown;
    private static final MethodHandle nvmlDeviceGetCount_v2;
    private static final MethodHandle nvmlDeviceGetHandleByIndex_v2;
    private static final MethodHandle nvmlDeviceGetName;
    private static final MethodHandle nvmlDeviceGetPciInfo_v3;
    private static final MethodHandle nvmlDeviceGetUtilizationRates;
    private static final MethodHandle nvmlDeviceGetMemoryInfo;
    private static final MethodHandle nvmlDeviceGetTemperature;
    private static final MethodHandle nvmlDeviceGetPowerUsage;
    private static final MethodHandle nvmlDeviceGetClockInfo;
    private static final MethodHandle nvmlDeviceGetFanSpeed;

    private static final boolean AVAILABLE;

    static {
        boolean available = false;
        MethodHandle hInit = null;
        MethodHandle hShutdown = null;
        MethodHandle hGetCount = null;
        MethodHandle hGetHandle = null;
        MethodHandle hGetName = null;
        MethodHandle hGetPci = null;
        MethodHandle hGetUtil = null;
        MethodHandle hGetMem = null;
        MethodHandle hGetTemp = null;
        MethodHandle hGetPower = null;
        MethodHandle hGetClock = null;
        MethodHandle hGetFan = null;
        try {
            String libName = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")
                    ? "nvml"
                    : "nvidia-ml";
            SymbolLookup nvml = libraryLookup(libName);

            hInit = LINKER.downcallHandle(nvml.findOrThrow("nvmlInit_v2"), FunctionDescriptor.of(JAVA_INT));
            hShutdown = LINKER.downcallHandle(nvml.findOrThrow("nvmlShutdown"), FunctionDescriptor.of(JAVA_INT));
            hGetCount = LINKER.downcallHandle(nvml.findOrThrow("nvmlDeviceGetCount_v2"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS));
            hGetHandle = LINKER.downcallHandle(nvml.findOrThrow("nvmlDeviceGetHandleByIndex_v2"),
                    FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS));
            hGetName = LINKER.downcallHandle(nvml.findOrThrow("nvmlDeviceGetName"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
            hGetPci = LINKER.downcallHandle(nvml.findOrThrow("nvmlDeviceGetPciInfo_v3"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            hGetUtil = LINKER.downcallHandle(nvml.findOrThrow("nvmlDeviceGetUtilizationRates"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            hGetMem = LINKER.downcallHandle(nvml.findOrThrow("nvmlDeviceGetMemoryInfo"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            hGetTemp = LINKER.downcallHandle(nvml.findOrThrow("nvmlDeviceGetTemperature"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
            hGetPower = LINKER.downcallHandle(nvml.findOrThrow("nvmlDeviceGetPowerUsage"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            hGetClock = LINKER.downcallHandle(nvml.findOrThrow("nvmlDeviceGetClockInfo"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
            hGetFan = LINKER.downcallHandle(nvml.findOrThrow("nvmlDeviceGetFanSpeed"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            available = true;
            LOG.debug("NVML library loaded via FFM");
        } catch (Throwable t) {
            LOG.debug("NVML library not available via FFM: {}", t.getMessage());
        }
        nvmlInit_v2 = hInit;
        nvmlShutdown = hShutdown;
        nvmlDeviceGetCount_v2 = hGetCount;
        nvmlDeviceGetHandleByIndex_v2 = hGetHandle;
        nvmlDeviceGetName = hGetName;
        nvmlDeviceGetPciInfo_v3 = hGetPci;
        nvmlDeviceGetUtilizationRates = hGetUtil;
        nvmlDeviceGetMemoryInfo = hGetMem;
        nvmlDeviceGetTemperature = hGetTemp;
        nvmlDeviceGetPowerUsage = hGetPower;
        nvmlDeviceGetClockInfo = hGetClock;
        nvmlDeviceGetFanSpeed = hGetFan;
        AVAILABLE = available;
    }

    /**
     * Returns whether the NVML native library was successfully loaded.
     *
     * @return true if the NVML library is available
     */
    public static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * Calls nvmlInit_v2.
     *
     * @return NVML return code
     */
    public static int init() {
        try {
            return (int) nvmlInit_v2.invokeExact();
        } catch (Throwable t) {
            LOG.debug("nvmlInit_v2 failed: {}", t.getMessage());
            return -1;
        }
    }

    /**
     * Calls nvmlShutdown.
     *
     * @return NVML return code
     */
    public static int shutdown() {
        try {
            return (int) nvmlShutdown.invokeExact();
        } catch (Throwable t) {
            LOG.debug("nvmlShutdown failed: {}", t.getMessage());
            return -1;
        }
    }

    /**
     * Gets the device count.
     *
     * @param countSeg pointer to int to receive count
     * @return NVML return code
     */
    public static int deviceGetCount(MemorySegment countSeg) {
        try {
            return (int) nvmlDeviceGetCount_v2.invokeExact(countSeg);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Gets a device handle by index.
     *
     * @param index     device index
     * @param handleSeg pointer to receive device handle
     * @return NVML return code
     */
    public static int deviceGetHandleByIndex(int index, MemorySegment handleSeg) {
        try {
            return (int) nvmlDeviceGetHandleByIndex_v2.invokeExact(index, handleSeg);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Gets the device name.
     *
     * @param device  device handle
     * @param nameSeg buffer to receive name
     * @param length  buffer length
     * @return NVML return code
     */
    public static int deviceGetName(MemorySegment device, MemorySegment nameSeg, int length) {
        try {
            return (int) nvmlDeviceGetName.invokeExact(device, nameSeg, length);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Gets PCI info for a device.
     *
     * @param device device handle
     * @param pciSeg nvmlPciInfo_t struct to fill
     * @return NVML return code
     */
    public static int deviceGetPciInfo(MemorySegment device, MemorySegment pciSeg) {
        try {
            return (int) nvmlDeviceGetPciInfo_v3.invokeExact(device, pciSeg);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Gets utilization rates for a device.
     *
     * @param device  device handle
     * @param utilSeg nvmlUtilization_t struct to fill
     * @return NVML return code
     */
    public static int deviceGetUtilizationRates(MemorySegment device, MemorySegment utilSeg) {
        try {
            return (int) nvmlDeviceGetUtilizationRates.invokeExact(device, utilSeg);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Gets memory info for a device.
     *
     * @param device device handle
     * @param memSeg nvmlMemory_t struct to fill
     * @return NVML return code
     */
    public static int deviceGetMemoryInfo(MemorySegment device, MemorySegment memSeg) {
        try {
            return (int) nvmlDeviceGetMemoryInfo.invokeExact(device, memSeg);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Gets temperature for a device.
     *
     * @param device     device handle
     * @param sensorType sensor type constant
     * @param tempSeg    pointer to int to receive temperature
     * @return NVML return code
     */
    public static int deviceGetTemperature(MemorySegment device, int sensorType, MemorySegment tempSeg) {
        try {
            return (int) nvmlDeviceGetTemperature.invokeExact(device, sensorType, tempSeg);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Gets power usage for a device.
     *
     * @param device   device handle
     * @param powerSeg pointer to int to receive power in milliwatts
     * @return NVML return code
     */
    public static int deviceGetPowerUsage(MemorySegment device, MemorySegment powerSeg) {
        try {
            return (int) nvmlDeviceGetPowerUsage.invokeExact(device, powerSeg);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Gets clock info for a device.
     *
     * @param device    device handle
     * @param clockType clock type constant
     * @param clockSeg  pointer to int to receive clock in MHz
     * @return NVML return code
     */
    public static int deviceGetClockInfo(MemorySegment device, int clockType, MemorySegment clockSeg) {
        try {
            return (int) nvmlDeviceGetClockInfo.invokeExact(device, clockType, clockSeg);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Gets fan speed for a device.
     *
     * @param device   device handle
     * @param speedSeg pointer to int to receive fan speed percentage
     * @return NVML return code
     */
    public static int deviceGetFanSpeed(MemorySegment device, MemorySegment speedSeg) {
        try {
            return (int) nvmlDeviceGetFanSpeed.invokeExact(device, speedSeg);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Reads a null-terminated string from a byte sequence within a struct segment.
     *
     * @param seg    the struct segment
     * @param offset byte offset of the char array field
     * @param maxLen maximum length of the char array
     * @return the string, or empty if null/empty
     */
    public static String readString(MemorySegment seg, long offset, int maxLen) {
        for (int i = 0; i < maxLen; i++) {
            if (seg.get(JAVA_BYTE, offset + i) == 0) {
                if (i == 0) {
                    return "";
                }
                return seg.getString(offset);
            }
        }
        byte[] bytes = new byte[maxLen];
        MemorySegment.copy(seg, JAVA_BYTE, offset, bytes, 0, maxLen);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8).trim();
    }
}
