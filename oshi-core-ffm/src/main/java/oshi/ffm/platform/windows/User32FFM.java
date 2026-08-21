/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.platform.windows;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.util.ExceptionUtil.getIntOrDefault;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Binding to the {@code user32.dll} Connecting and Configuring Displays (CCD) functions. Buffers are passed as raw
 * segments and their fields read by offset (see {@link oshi.driver.common.windows.DisplayConnector}), rather than
 * mapping the {@code DISPLAYCONFIG_*} structs.
 */
public final class User32FFM extends WindowsForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(User32FFM.class);

    private static final SymbolLookup USER32 = lib("User32");

    private User32FFM() {
    }

    private static final MethodHandle GetDisplayConfigBufferSizes = downcall(USER32, "GetDisplayConfigBufferSizes",
            JAVA_INT, JAVA_INT, ADDRESS, ADDRESS);

    /**
     * Retrieves the buffer sizes required to call {@code QueryDisplayConfig}.
     *
     * @param flags    a combination of {@code QDC_*} flags
     * @param numPaths receives the number of path elements
     * @param numModes receives the number of mode info elements
     * @return {@code ERROR_SUCCESS} (0) on success, otherwise a Win32 error code, or -1 on invocation failure
     */
    public static int GetDisplayConfigBufferSizes(int flags, MemorySegment numPaths, MemorySegment numModes) {
        return getIntOrDefault(() -> (int) GetDisplayConfigBufferSizes.invokeExact(flags, numPaths, numModes), -1, LOG,
                "User32FFM.GetDisplayConfigBufferSizes failed");
    }

    private static final MethodHandle QueryDisplayConfig = downcall(USER32, "QueryDisplayConfig", JAVA_INT, JAVA_INT,
            ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS);

    /**
     * Retrieves the active display paths and their modes.
     *
     * @param flags      a combination of {@code QDC_*} flags
     * @param numPaths   in/out count of path array elements
     * @param pathArray  buffer receiving {@code DISPLAYCONFIG_PATH_INFO} elements
     * @param numModes   in/out count of mode info array elements
     * @param modeArray  buffer receiving {@code DISPLAYCONFIG_MODE_INFO} elements
     * @param topologyId optional topology id output, may be {@code MemorySegment.NULL}
     * @return {@code ERROR_SUCCESS} (0) on success, otherwise a Win32 error code, or -1 on invocation failure
     */
    public static int QueryDisplayConfig(int flags, MemorySegment numPaths, MemorySegment pathArray,
            MemorySegment numModes, MemorySegment modeArray, MemorySegment topologyId) {
        return getIntOrDefault(
                () -> (int) QueryDisplayConfig.invokeExact(flags, numPaths, pathArray, numModes, modeArray, topologyId),
                -1, LOG, "User32FFM.QueryDisplayConfig failed");
    }

    private static final MethodHandle DisplayConfigGetDeviceInfo = downcall(USER32, "DisplayConfigGetDeviceInfo",
            JAVA_INT, ADDRESS);

    /**
     * Retrieves display configuration information about a device, given a request packet whose header identifies the
     * target.
     *
     * @param requestPacket a {@code DISPLAYCONFIG_DEVICE_INFO_HEADER} followed by the type-specific payload
     * @return {@code ERROR_SUCCESS} (0) on success, otherwise a Win32 error code, or -1 on invocation failure
     */
    public static int DisplayConfigGetDeviceInfo(MemorySegment requestPacket) {
        return getIntOrDefault(() -> (int) DisplayConfigGetDeviceInfo.invokeExact(requestPacket), -1, LOG,
                "User32FFM.DisplayConfigGetDeviceInfo failed");
    }
}
