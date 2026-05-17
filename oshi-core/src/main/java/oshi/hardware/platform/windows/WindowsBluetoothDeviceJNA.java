/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.PointerByReference;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.BluetoothDevice;
import oshi.hardware.common.AbstractBluetoothDevice;
import oshi.jna.platform.windows.BluetoothApis;
import oshi.jna.platform.windows.BluetoothApis.BLUETOOTH_DEVICE_INFO;
import oshi.jna.platform.windows.BluetoothApis.BLUETOOTH_DEVICE_SEARCH_PARAMS;
import oshi.jna.platform.windows.BluetoothApis.BLUETOOTH_FIND_RADIO_PARAMS;
import oshi.jna.platform.windows.BluetoothApis.BLUETOOTH_RADIO_INFO;

/**
 * Windows Bluetooth device enumeration via the Bluetooth API (bthprops.cpl).
 */
@Immutable
public final class WindowsBluetoothDeviceJNA extends AbstractBluetoothDevice {

    private WindowsBluetoothDeviceJNA(String name, String address, String majorDeviceClass, boolean connected,
            boolean paired, int batteryLevel, String adapterName) {
        super(name, address, majorDeviceClass, connected, paired, batteryLevel, adapterName);
    }

    /**
     * Gets Bluetooth devices known to the system.
     *
     * @return a list of {@link BluetoothDevice} objects
     */
    public static List<BluetoothDevice> getBluetoothDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        BLUETOOTH_FIND_RADIO_PARAMS radioParams = new BLUETOOTH_FIND_RADIO_PARAMS();
        PointerByReference phRadio = new PointerByReference();

        HANDLE hFindRadio = BluetoothApis.INSTANCE.BluetoothFindFirstRadio(radioParams, phRadio);
        if (hFindRadio == null) {
            return Collections.emptyList();
        }

        try {
            do {
                HANDLE hRadio = new HANDLE(phRadio.getValue());
                String adapterName = getRadioName(hRadio);
                queryDevicesForRadio(hRadio, adapterName, devices);
                Kernel32.INSTANCE.CloseHandle(hRadio);
            } while (BluetoothApis.INSTANCE.BluetoothFindNextRadio(hFindRadio, phRadio));
        } finally {
            BluetoothApis.INSTANCE.BluetoothFindRadioClose(hFindRadio);
        }

        return Collections.unmodifiableList(devices);
    }

    private static String getRadioName(HANDLE hRadio) {
        BLUETOOTH_RADIO_INFO radioInfo = new BLUETOOTH_RADIO_INFO();
        if (BluetoothApis.INSTANCE.BluetoothGetRadioInfo(hRadio, radioInfo) == 0) {
            return new String(radioInfo.szName).trim();
        }
        return "";
    }

    private static void queryDevicesForRadio(HANDLE hRadio, String adapterName, List<BluetoothDevice> devices) {
        BLUETOOTH_DEVICE_SEARCH_PARAMS searchParams = new BLUETOOTH_DEVICE_SEARCH_PARAMS();
        searchParams.fReturnAuthenticated = true;
        searchParams.fReturnRemembered = true;
        searchParams.fReturnConnected = true;
        searchParams.fReturnUnknown = false;
        searchParams.fIssueInquiry = false;
        searchParams.cTimeoutMultiplier = 0;
        searchParams.hRadio = hRadio;

        BLUETOOTH_DEVICE_INFO deviceInfo = new BLUETOOTH_DEVICE_INFO();
        HANDLE hFind = BluetoothApis.INSTANCE.BluetoothFindFirstDevice(searchParams, deviceInfo);
        if (hFind == null) {
            return;
        }

        try {
            do {
                devices.add(parseDeviceInfo(deviceInfo, adapterName));
                deviceInfo = new BLUETOOTH_DEVICE_INFO();
            } while (BluetoothApis.INSTANCE.BluetoothFindNextDevice(hFind, deviceInfo));
        } finally {
            BluetoothApis.INSTANCE.BluetoothFindDeviceClose(hFind);
        }
    }

    private static WindowsBluetoothDeviceJNA parseDeviceInfo(BLUETOOTH_DEVICE_INFO info, String adapterName) {
        String name = new String(info.szName).trim();
        String address = formatAddress(info.Address);
        String majorClass = AbstractBluetoothDevice.parseMajorDeviceClass(info.ulClassofDevice);
        boolean connected = info.fConnected;
        boolean paired = info.fRemembered;
        return new WindowsBluetoothDeviceJNA(name, address, majorClass, connected, paired, -1, adapterName);
    }

    private static String formatAddress(long addr) {
        return String.format(Locale.ROOT, "%02X:%02X:%02X:%02X:%02X:%02X", (addr >> 40) & 0xFF, (addr >> 32) & 0xFF,
                (addr >> 24) & 0xFF, (addr >> 16) & 0xFF, (addr >> 8) & 0xFF, addr & 0xFF);
    }
}
