/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

/**
 * Bluetooth API functions from bthprops.cpl. This class should be considered non-API as it may be removed if/when its
 * code is incorporated into the JNA project.
 */
public interface BluetoothApis extends com.sun.jna.win32.StdCallLibrary {

    /** Instance of BluetoothApis. */
    BluetoothApis INSTANCE = Native.load("BluetoothApis", BluetoothApis.class, W32APIOptions.DEFAULT_OPTIONS);

    /** Maximum Bluetooth device name length. */
    int BLUETOOTH_MAX_NAME_SIZE = 248;

    /**
     * BLUETOOTH_DEVICE_INFO structure.
     */
    @FieldOrder({ "dwSize", "Address", "ulClassofDevice", "fConnected", "fRemembered", "fAuthenticated", "stLastSeen",
            "stLastUsed", "szName" })
    class BLUETOOTH_DEVICE_INFO extends Structure {
        public int dwSize;
        public long Address;
        public int ulClassofDevice;
        public boolean fConnected;
        public boolean fRemembered;
        public boolean fAuthenticated;
        public SYSTEMTIME stLastSeen;
        public SYSTEMTIME stLastUsed;
        public char[] szName = new char[BLUETOOTH_MAX_NAME_SIZE];

        public BLUETOOTH_DEVICE_INFO() {
            dwSize = size();
        }
    }

    /**
     * SYSTEMTIME structure (subset for Bluetooth).
     */
    @FieldOrder({ "wYear", "wMonth", "wDayOfWeek", "wDay", "wHour", "wMinute", "wSecond", "wMilliseconds" })
    class SYSTEMTIME extends Structure {
        public short wYear;
        public short wMonth;
        public short wDayOfWeek;
        public short wDay;
        public short wHour;
        public short wMinute;
        public short wSecond;
        public short wMilliseconds;
    }

    /**
     * BLUETOOTH_DEVICE_SEARCH_PARAMS structure.
     */
    @FieldOrder({ "dwSize", "fReturnAuthenticated", "fReturnRemembered", "fReturnUnknown", "fReturnConnected",
            "fIssueInquiry", "cTimeoutMultiplier", "hRadio" })
    class BLUETOOTH_DEVICE_SEARCH_PARAMS extends Structure {
        public int dwSize;
        public boolean fReturnAuthenticated;
        public boolean fReturnRemembered;
        public boolean fReturnUnknown;
        public boolean fReturnConnected;
        public boolean fIssueInquiry;
        public byte cTimeoutMultiplier;
        public HANDLE hRadio;

        public BLUETOOTH_DEVICE_SEARCH_PARAMS() {
            dwSize = size();
        }
    }

    /**
     * BLUETOOTH_FIND_RADIO_PARAMS structure.
     */
    @FieldOrder({ "dwSize" })
    class BLUETOOTH_FIND_RADIO_PARAMS extends Structure {
        public int dwSize;

        public BLUETOOTH_FIND_RADIO_PARAMS() {
            dwSize = size();
        }
    }

    /**
     * BLUETOOTH_RADIO_INFO structure.
     */
    @FieldOrder({ "dwSize", "address", "szName", "ulClassofDevice", "lmpSubversion", "manufacturer" })
    class BLUETOOTH_RADIO_INFO extends Structure {
        public int dwSize;
        public long address;
        public char[] szName = new char[BLUETOOTH_MAX_NAME_SIZE];
        public int ulClassofDevice;
        public short lmpSubversion;
        public short manufacturer;

        public BLUETOOTH_RADIO_INFO() {
            dwSize = size();
        }
    }

    /**
     * Finds the first Bluetooth radio.
     *
     * @param pbtfrp  search parameters
     * @param phRadio receives the radio handle
     * @return a search handle, or null on failure
     */
    HANDLE BluetoothFindFirstRadio(BLUETOOTH_FIND_RADIO_PARAMS pbtfrp, PointerByReference phRadio);

    /**
     * Finds the next Bluetooth radio.
     *
     * @param hFind   the search handle from BluetoothFindFirstRadio
     * @param phRadio receives the radio handle
     * @return true if another radio was found
     */
    boolean BluetoothFindNextRadio(HANDLE hFind, PointerByReference phRadio);

    /**
     * Closes a radio search handle.
     *
     * @param hFind the search handle
     * @return true on success
     */
    boolean BluetoothFindRadioClose(HANDLE hFind);

    /**
     * Gets radio information.
     *
     * @param hRadio     the radio handle
     * @param pRadioInfo receives the radio info
     * @return 0 on success
     */
    int BluetoothGetRadioInfo(HANDLE hRadio, BLUETOOTH_RADIO_INFO pRadioInfo);

    /**
     * Finds the first Bluetooth device.
     *
     * @param pbtsp search parameters
     * @param pbtdi receives device info
     * @return a search handle, or null on failure
     */
    HANDLE BluetoothFindFirstDevice(BLUETOOTH_DEVICE_SEARCH_PARAMS pbtsp, BLUETOOTH_DEVICE_INFO pbtdi);

    /**
     * Finds the next Bluetooth device.
     *
     * @param hFind the search handle
     * @param pbtdi receives device info
     * @return true if another device was found
     */
    boolean BluetoothFindNextDevice(HANDLE hFind, BLUETOOTH_DEVICE_INFO pbtdi);

    /**
     * Closes a device search handle.
     *
     * @param hFind the search handle
     * @return true on success
     */
    boolean BluetoothFindDeviceClose(HANDLE hFind);
}
