/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static oshi.ffm.platform.windows.BluetoothApisFFM.BLUETOOTH_DEVICE_INFO_LAYOUT;
import static oshi.ffm.platform.windows.BluetoothApisFFM.BLUETOOTH_DEVICE_SEARCH_PARAMS_LAYOUT;
import static oshi.ffm.platform.windows.BluetoothApisFFM.BLUETOOTH_FIND_RADIO_PARAMS_LAYOUT;
import static oshi.ffm.platform.windows.BluetoothApisFFM.BLUETOOTH_MAX_NAME_SIZE;
import static oshi.ffm.platform.windows.BluetoothApisFFM.BLUETOOTH_RADIO_INFO_LAYOUT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.NativeHandle;
import oshi.ffm.platform.windows.BluetoothApisFFM;
import oshi.ffm.platform.windows.Kernel32FFM;
import oshi.ffm.platform.windows.VersionHelpersFFM;
import oshi.ffm.platform.windows.WindowsForeignFunctions;
import oshi.hardware.BluetoothDevice;
import oshi.hardware.common.AbstractBluetoothDevice;
import oshi.util.FormatUtil;

/**
 * Windows Bluetooth device enumeration via FFM (bthprops.cpl).
 */
@Immutable
public final class WindowsBluetoothDeviceFFM extends AbstractBluetoothDevice {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsBluetoothDeviceFFM.class);

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpersFFM.IsWindowsVistaOrGreater();

    private WindowsBluetoothDeviceFFM(String name, String address, String majorDeviceClass, boolean connected,
            boolean paired, int batteryLevel, String adapterName) {
        super(name, address, majorDeviceClass, connected, paired, batteryLevel, adapterName);
    }

    /**
     * Gets Bluetooth devices known to the system.
     *
     * @return a list of {@link BluetoothDevice} objects
     */
    public static List<BluetoothDevice> getBluetoothDevices() {
        if (!IS_VISTA_OR_GREATER) {
            return Collections.emptyList();
        }
        List<BluetoothDevice> devices = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment radioParams = arena.allocate(BLUETOOTH_FIND_RADIO_PARAMS_LAYOUT);
            radioParams.set(JAVA_INT, 0, (int) BLUETOOTH_FIND_RADIO_PARAMS_LAYOUT.byteSize());
            MemorySegment phRadio = arena.allocate(ADDRESS);

            MemorySegment hFindRadio = BluetoothApisFFM.BluetoothFindFirstRadio(radioParams, phRadio);
            if (hFindRadio.equals(MemorySegment.NULL)) {
                return Collections.emptyList();
            }

            try (NativeHandle findRadioHandle = NativeHandle.of(hFindRadio,
                    BluetoothApisFFM::BluetoothFindRadioClose)) {
                do {
                    MemorySegment hRadio = phRadio.get(ADDRESS, 0);
                    try (NativeHandle radioHandle = NativeHandle.of(hRadio, Kernel32FFM::CloseHandle)) {
                        String adapterName = getRadioName(arena, hRadio);
                        queryDevicesForRadio(arena, hRadio, adapterName, devices);
                    }
                } while (WindowsForeignFunctions
                        .isSuccess(BluetoothApisFFM.BluetoothFindNextRadio(hFindRadio, phRadio)));
            }
        } catch (Throwable t) {
            LOG.warn("Error enumerating Bluetooth devices: {}", t.getMessage());
        }
        return Collections.unmodifiableList(devices);
    }

    private static String getRadioName(Arena arena, MemorySegment hRadio) throws Throwable {
        MemorySegment radioInfo = arena.allocate(BLUETOOTH_RADIO_INFO_LAYOUT);
        radioInfo.set(JAVA_INT, 0, (int) BLUETOOTH_RADIO_INFO_LAYOUT.byteSize());
        if (BluetoothApisFFM.BluetoothGetRadioInfo(hRadio, radioInfo) == 0) {
            return readDeviceName(radioInfo,
                    BLUETOOTH_RADIO_INFO_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("szName")));
        }
        return "";
    }

    private static void queryDevicesForRadio(Arena arena, MemorySegment hRadio, String adapterName,
            List<BluetoothDevice> devices) throws Throwable {
        MemorySegment searchParams = arena.allocate(BLUETOOTH_DEVICE_SEARCH_PARAMS_LAYOUT);
        searchParams.set(JAVA_INT, 0, (int) BLUETOOTH_DEVICE_SEARCH_PARAMS_LAYOUT.byteSize());
        long base = 4; // after dwSize
        searchParams.set(JAVA_INT, base, 1); // fReturnAuthenticated
        searchParams.set(JAVA_INT, base + 4, 1); // fReturnRemembered
        searchParams.set(JAVA_INT, base + 8, 0); // fReturnUnknown
        searchParams.set(JAVA_INT, base + 12, 1); // fReturnConnected
        searchParams.set(JAVA_INT, base + 16, 0); // fIssueInquiry
        searchParams.set(ADDRESS,
                BLUETOOTH_DEVICE_SEARCH_PARAMS_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("hRadio")),
                hRadio);

        MemorySegment deviceInfo = arena.allocate(BLUETOOTH_DEVICE_INFO_LAYOUT);
        deviceInfo.set(JAVA_INT, 0, (int) BLUETOOTH_DEVICE_INFO_LAYOUT.byteSize());

        MemorySegment hFind = BluetoothApisFFM.BluetoothFindFirstDevice(searchParams, deviceInfo);
        if (hFind.equals(MemorySegment.NULL)) {
            return;
        }

        try (NativeHandle findDevHandle = NativeHandle.of(hFind, BluetoothApisFFM::BluetoothFindDeviceClose)) {
            do {
                devices.add(parseDeviceInfo(deviceInfo, adapterName));
                deviceInfo = arena.allocate(BLUETOOTH_DEVICE_INFO_LAYOUT);
                deviceInfo.set(JAVA_INT, 0, (int) BLUETOOTH_DEVICE_INFO_LAYOUT.byteSize());
            } while (WindowsForeignFunctions.isSuccess(BluetoothApisFFM.BluetoothFindNextDevice(hFind, deviceInfo)));
        }
    }

    private static WindowsBluetoothDeviceFFM parseDeviceInfo(MemorySegment info, String adapterName) {
        long addr = info.get(JAVA_LONG,
                BLUETOOTH_DEVICE_INFO_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("Address")));
        int cod = info.get(JAVA_INT,
                BLUETOOTH_DEVICE_INFO_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("ulClassofDevice")));
        boolean connected = info.get(JAVA_INT,
                BLUETOOTH_DEVICE_INFO_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("fConnected"))) != 0;
        boolean paired = info.get(JAVA_INT,
                BLUETOOTH_DEVICE_INFO_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("fRemembered"))) != 0;
        String name = readDeviceName(info,
                BLUETOOTH_DEVICE_INFO_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("szName")));
        String majorClass = AbstractBluetoothDevice.parseMajorDeviceClass(cod);
        String address = FormatUtil.formatMacAddress(addr);
        return new WindowsBluetoothDeviceFFM(name, address, majorClass, connected, paired, -1, adapterName);
    }

    private static String readDeviceName(MemorySegment seg, long offset) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < BLUETOOTH_MAX_NAME_SIZE; i++) {
            char c = (char) seg.get(JAVA_SHORT, offset + (long) i * 2);
            if (c == '\0') {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
