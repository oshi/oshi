/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.windows.WinErrorFFM.ERROR_INSUFFICIENT_BUFFER;
import static oshi.ffm.windows.WinErrorFFM.ERROR_NO_MORE_ITEMS;
import static oshi.util.Util.is64Bit;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.Optional;
import java.util.OptionalInt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SetupApiFFM extends WindowsForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(SetupApiFFM.class);

    private static final SymbolLookup SETUPAPI = lib("SetupApi");

    /**
     * SP_DEVICE_INTERFACE_DATA: cbSize (DWORD) + InterfaceClassGuid (GUID=16 bytes) + Flags (DWORD) + Reserved
     * (ULONG_PTR=8 bytes on 64-bit)
     */
    public static final StructLayout SP_DEVICE_INTERFACE_DATA = structLayout(JAVA_INT.withName("cbSize"),
            MemoryLayout.sequenceLayout(16, JAVA_BYTE).withName("InterfaceClassGuid"), JAVA_INT.withName("Flags"),
            ADDRESS.withName("Reserved"));

    public static final int DIGCF_PRESENT = 0x00000002;
    public static final int DIGCF_DEVICEINTERFACE = 0x00000010;

    private static final MethodHandle SetupDiGetClassDevs = downcall(SETUPAPI, "SetupDiGetClassDevsW", ADDRESS, ADDRESS,
            ADDRESS, ADDRESS, JAVA_INT);

    public static Optional<MemorySegment> SetupDiGetClassDevs(MemorySegment classGuid, int flags) {
        try {
            MemorySegment handle = (MemorySegment) SetupDiGetClassDevs.invokeExact(classGuid, MemorySegment.NULL,
                    MemorySegment.NULL, flags);
            if (Kernel32FFM.isInvalidHandle(handle)) {
                return Optional.empty();
            }
            return Optional.of(handle);
        } catch (Throwable t) {
            LOG.debug("SetupApiFFM.SetupDiGetClassDevs failed", t);
            return Optional.empty();
        }
    }

    private static final MethodHandle SetupDiEnumDeviceInterfaces = downcall(SETUPAPI, "SetupDiEnumDeviceInterfaces",
            JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS);

    /**
     * @param hDevInfo            the device info set handle
     * @param classGuid           the interface class GUID
     * @param memberIndex         zero-based index of the interface to retrieve
     * @param deviceInterfaceData receives information about the device interface
     * @return 0 if no more items, 1 if success, -1 on other error
     */
    public static int SetupDiEnumDeviceInterfaces(MemorySegment hDevInfo, MemorySegment classGuid, int memberIndex,
            MemorySegment deviceInterfaceData) {
        try {
            int result = (int) SetupDiEnumDeviceInterfaces.invokeExact(hDevInfo, MemorySegment.NULL, classGuid,
                    memberIndex, deviceInterfaceData);
            if (isSuccess(result)) {
                return 1;
            }
            int err = Kernel32FFM.GetLastError().orElse(0);
            return err == ERROR_NO_MORE_ITEMS ? 0 : -1;
        } catch (Throwable t) {
            LOG.debug("SetupApiFFM.SetupDiEnumDeviceInterfaces failed", t);
            return -1;
        }
    }

    private static final MethodHandle SetupDiGetDeviceInterfaceDetail = downcall(SETUPAPI,
            "SetupDiGetDeviceInterfaceDetailW", JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    /**
     * First call with null detail buffer to get required size.
     *
     * @param hDevInfo            the device info set handle
     * @param deviceInterfaceData the device interface data
     * @param arena               the arena to allocate the size buffer from
     * @return required buffer size, or 0 on unexpected error
     */
    public static int SetupDiGetDeviceInterfaceDetailSize(MemorySegment hDevInfo, MemorySegment deviceInterfaceData,
            Arena arena) {
        try {
            MemorySegment requiredSize = arena.allocate(JAVA_INT);
            SetupDiGetDeviceInterfaceDetail.invokeExact(hDevInfo, deviceInterfaceData, MemorySegment.NULL, 0,
                    requiredSize, MemorySegment.NULL);
            int err = Kernel32FFM.GetLastError().orElse(0);
            if (err == ERROR_INSUFFICIENT_BUFFER) {
                return requiredSize.get(JAVA_INT, 0);
            }
            return 0;
        } catch (Throwable t) {
            LOG.debug("SetupApiFFM.SetupDiGetDeviceInterfaceDetailSize failed", t);
            return 0;
        }
    }

    /**
     * Second call with allocated buffer to get device path. The detail buffer layout is: cbSize (DWORD=4) + DevicePath
     * (WCHAR[]). On 64-bit, cbSize field value must be 8; on 32-bit it is 5 or 6.
     *
     * @param hDevInfo            the device info set handle
     * @param deviceInterfaceData the device interface data
     * @param requiredSize        the buffer size returned by {@link #SetupDiGetDeviceInterfaceDetailSize}
     * @param arena               the arena to allocate buffers from
     * @return the device path string, or empty if failed
     */
    public static Optional<String> SetupDiGetDeviceInterfaceDetail(MemorySegment hDevInfo,
            MemorySegment deviceInterfaceData, int requiredSize, Arena arena) {
        try {
            MemorySegment detail = arena.allocate(requiredSize);
            detail.set(JAVA_INT, 0, is64Bit() ? 8 : 6);
            MemorySegment reqSize = arena.allocate(JAVA_INT);
            int result = (int) SetupDiGetDeviceInterfaceDetail.invokeExact(hDevInfo, deviceInterfaceData, detail,
                    requiredSize, reqSize, MemorySegment.NULL);
            if (!isSuccess(result)) {
                return Optional.empty();
            }
            return Optional.of(readWideString(detail.asSlice(4)));
        } catch (Throwable t) {
            LOG.debug("SetupApiFFM.SetupDiGetDeviceInterfaceDetail failed", t);
            return Optional.empty();
        }
    }

    private static final MethodHandle SetupDiDestroyDeviceInfoList = downcall(SETUPAPI, "SetupDiDestroyDeviceInfoList",
            JAVA_INT, ADDRESS);

    public static OptionalInt SetupDiDestroyDeviceInfoList(MemorySegment hDevInfo) {
        try {
            return OptionalInt.of((int) SetupDiDestroyDeviceInfoList.invokeExact(hDevInfo));
        } catch (Throwable t) {
            LOG.debug("SetupApiFFM.SetupDiDestroyDeviceInfoList failed", t);
            return OptionalInt.empty();
        }
    }
}
