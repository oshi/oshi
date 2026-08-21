/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

import oshi.driver.common.windows.DisplayConnector;

/**
 * Binding to the {@code user32.dll} Connecting and Configuring Displays (CCD) functions, which are not mapped by JNA's
 * platform {@code User32}. These functions have no {@code A}/{@code W} variants, so the library is loaded without a
 * name-mangling function mapper. Buffers are passed as raw pointers and their fields read by offset (see
 * {@link DisplayConnector}), rather than mapping the {@code DISPLAYCONFIG_*} structs.
 * <p>
 * This class should be considered non-API as it may be removed if/when its code is incorporated into the JNA project.
 */
public interface User32Ext extends StdCallLibrary {

    User32Ext INSTANCE = Native.load("user32", User32Ext.class);

    /**
     * Retrieves the size of the buffers required to call {@code QueryDisplayConfig}.
     *
     * @param flags                    a combination of {@code QDC_*} flags
     * @param numPathArrayElements     receives the number of path elements
     * @param numModeInfoArrayElements receives the number of mode info elements
     * @return {@code ERROR_SUCCESS} (0) on success, otherwise a Win32 error code
     */
    int GetDisplayConfigBufferSizes(int flags, IntByReference numPathArrayElements,
            IntByReference numModeInfoArrayElements);

    /**
     * Retrieves the active display paths and their modes.
     *
     * @param flags                    a combination of {@code QDC_*} flags
     * @param numPathArrayElements     in/out count of path array elements
     * @param pathArray                buffer receiving {@code DISPLAYCONFIG_PATH_INFO} elements
     * @param numModeInfoArrayElements in/out count of mode info array elements
     * @param modeInfoArray            buffer receiving {@code DISPLAYCONFIG_MODE_INFO} elements
     * @param currentTopologyId        optional topology id output, may be {@code null}
     * @return {@code ERROR_SUCCESS} (0) on success, otherwise a Win32 error code
     */
    int QueryDisplayConfig(int flags, IntByReference numPathArrayElements, Pointer pathArray,
            IntByReference numModeInfoArrayElements, Pointer modeInfoArray, Pointer currentTopologyId);

    /**
     * Retrieves display configuration information about a device, given a request packet whose header identifies the
     * target.
     *
     * @param requestPacket a {@code DISPLAYCONFIG_DEVICE_INFO_HEADER} followed by the type-specific payload
     * @return {@code ERROR_SUCCESS} (0) on success, otherwise a Win32 error code
     */
    int DisplayConfigGetDeviceInfo(Pointer requestPacket);
}
