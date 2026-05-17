/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for the Windows Bluetooth API (bthprops.cpl).
 */
public class BluetoothApisFFM extends WindowsForeignFunctions {

    /** Maximum Bluetooth device name length. */
    public static final int BLUETOOTH_MAX_NAME_SIZE = 248;

    private static final SymbolLookup BTHPROPS = lib("bthprops.cpl");

    /** SYSTEMTIME layout: 8 shorts = 16 bytes. */
    public static final StructLayout SYSTEMTIME_LAYOUT = structLayout(JAVA_SHORT.withName("wYear"),
            JAVA_SHORT.withName("wMonth"), JAVA_SHORT.withName("wDayOfWeek"), JAVA_SHORT.withName("wDay"),
            JAVA_SHORT.withName("wHour"), JAVA_SHORT.withName("wMinute"), JAVA_SHORT.withName("wSecond"),
            JAVA_SHORT.withName("wMilliseconds"));

    /** BLUETOOTH_DEVICE_INFO layout. */
    public static final StructLayout BLUETOOTH_DEVICE_INFO_LAYOUT = structLayout(JAVA_INT.withName("dwSize"),
            JAVA_INT.withName("padding0"), JAVA_LONG.withName("Address"), JAVA_INT.withName("ulClassofDevice"),
            JAVA_INT.withName("fConnected"), JAVA_INT.withName("fRemembered"), JAVA_INT.withName("fAuthenticated"),
            SYSTEMTIME_LAYOUT.withName("stLastSeen"), SYSTEMTIME_LAYOUT.withName("stLastUsed"),
            sequenceLayout(BLUETOOTH_MAX_NAME_SIZE, JAVA_SHORT).withName("szName"));

    /** BLUETOOTH_DEVICE_SEARCH_PARAMS layout. */
    public static final StructLayout BLUETOOTH_DEVICE_SEARCH_PARAMS_LAYOUT = structLayout(JAVA_INT.withName("dwSize"),
            JAVA_INT.withName("fReturnAuthenticated"), JAVA_INT.withName("fReturnRemembered"),
            JAVA_INT.withName("fReturnUnknown"), JAVA_INT.withName("fReturnConnected"),
            JAVA_INT.withName("fIssueInquiry"), JAVA_BYTE.withName("cTimeoutMultiplier"),
            sequenceLayout(7, JAVA_BYTE).withName("padding"), ADDRESS.withName("hRadio"));

    /** BLUETOOTH_FIND_RADIO_PARAMS layout. */
    public static final StructLayout BLUETOOTH_FIND_RADIO_PARAMS_LAYOUT = structLayout(JAVA_INT.withName("dwSize"));

    /** BLUETOOTH_RADIO_INFO layout. */
    public static final StructLayout BLUETOOTH_RADIO_INFO_LAYOUT = structLayout(JAVA_INT.withName("dwSize"),
            JAVA_INT.withName("padding0"), JAVA_LONG.withName("address"),
            sequenceLayout(BLUETOOTH_MAX_NAME_SIZE, JAVA_SHORT).withName("szName"),
            JAVA_INT.withName("ulClassofDevice"), JAVA_SHORT.withName("lmpSubversion"),
            JAVA_SHORT.withName("manufacturer"));

    private static final MethodHandle BluetoothFindFirstRadio = downcall(BTHPROPS, "BluetoothFindFirstRadio", ADDRESS,
            ADDRESS, ADDRESS);
    private static final MethodHandle BluetoothFindNextRadio = downcall(BTHPROPS, "BluetoothFindNextRadio", JAVA_INT,
            ADDRESS, ADDRESS);
    private static final MethodHandle BluetoothFindRadioClose = downcall(BTHPROPS, "BluetoothFindRadioClose", JAVA_INT,
            ADDRESS);
    private static final MethodHandle BluetoothGetRadioInfo = downcall(BTHPROPS, "BluetoothGetRadioInfo", JAVA_INT,
            ADDRESS, ADDRESS);
    private static final MethodHandle BluetoothFindFirstDevice = downcall(BTHPROPS, "BluetoothFindFirstDevice", ADDRESS,
            ADDRESS, ADDRESS);
    private static final MethodHandle BluetoothFindNextDevice = downcall(BTHPROPS, "BluetoothFindNextDevice", JAVA_INT,
            ADDRESS, ADDRESS);
    private static final MethodHandle BluetoothFindDeviceClose = downcall(BTHPROPS, "BluetoothFindDeviceClose",
            JAVA_INT, ADDRESS);

    public static MemorySegment BluetoothFindFirstRadio(MemorySegment params, MemorySegment phRadio) throws Throwable {
        return (MemorySegment) BluetoothFindFirstRadio.invokeExact(params, phRadio);
    }

    public static int BluetoothFindNextRadio(MemorySegment hFind, MemorySegment phRadio) throws Throwable {
        return (int) BluetoothFindNextRadio.invokeExact(hFind, phRadio);
    }

    public static int BluetoothFindRadioClose(MemorySegment hFind) throws Throwable {
        return (int) BluetoothFindRadioClose.invokeExact(hFind);
    }

    public static int BluetoothGetRadioInfo(MemorySegment hRadio, MemorySegment pRadioInfo) throws Throwable {
        return (int) BluetoothGetRadioInfo.invokeExact(hRadio, pRadioInfo);
    }

    public static MemorySegment BluetoothFindFirstDevice(MemorySegment params, MemorySegment pDeviceInfo)
            throws Throwable {
        return (MemorySegment) BluetoothFindFirstDevice.invokeExact(params, pDeviceInfo);
    }

    public static int BluetoothFindNextDevice(MemorySegment hFind, MemorySegment pDeviceInfo) throws Throwable {
        return (int) BluetoothFindNextDevice.invokeExact(hFind, pDeviceInfo);
    }

    public static int BluetoothFindDeviceClose(MemorySegment hFind) throws Throwable {
        return (int) BluetoothFindDeviceClose.invokeExact(hFind);
    }
}
