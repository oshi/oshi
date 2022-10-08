/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinReg.HKEY;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.Struct.CloseableSpDeviceInterfaceData;
import oshi.jna.Struct.CloseableSpDevinfoData;

/**
 * A Display
 */
@Immutable
final class WindowsDisplay extends AbstractDisplay {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsDisplay.class);

    private static final SetupApi SU = SetupApi.INSTANCE;
    private static final Advapi32 ADV = Advapi32.INSTANCE;

    private static final Guid.GUID GUID_DEVINTERFACE_MONITOR = new Guid.GUID("E6F07B5F-EE97-4a90-B076-33F57BF4EAA7");

    /**
     * Constructor for WindowsDisplay.
     *
     * @param edid a byte array representing a display EDID
     */
    WindowsDisplay(byte[] edid) {
        super(edid);
        LOG.debug("Initialized WindowsDisplay");
    }

    /**
     * Gets Display Information
     *
     * @return An array of Display objects representing monitors, etc.
     */
    public static List<Display> getDisplays() {
        List<Display> displays = new ArrayList<>();

        HANDLE hDevInfo = SU.SetupDiGetClassDevs(GUID_DEVINTERFACE_MONITOR, null, null,
                SetupApi.DIGCF_PRESENT | SetupApi.DIGCF_DEVICEINTERFACE);
        if (!hDevInfo.equals(WinBase.INVALID_HANDLE_VALUE)) {
            try (CloseableSpDeviceInterfaceData deviceInterfaceData = new CloseableSpDeviceInterfaceData();
                    CloseableSpDevinfoData info = new CloseableSpDevinfoData()) {
                deviceInterfaceData.cbSize = deviceInterfaceData.size();

                for (int memberIndex = 0; SU.SetupDiEnumDeviceInfo(hDevInfo, memberIndex, info); memberIndex++) {
                    HKEY key = SU.SetupDiOpenDevRegKey(hDevInfo, info, SetupApi.DICS_FLAG_GLOBAL, 0, SetupApi.DIREG_DEV,
                            WinNT.KEY_QUERY_VALUE);

                    byte[] edid = new byte[1];

                    try (CloseableIntByReference pType = new CloseableIntByReference();
                            CloseableIntByReference lpcbData = new CloseableIntByReference()) {
                        if (ADV.RegQueryValueEx(key, "EDID", 0, pType, edid, lpcbData) == WinError.ERROR_MORE_DATA) {
                            edid = new byte[lpcbData.getValue()];
                            if (ADV.RegQueryValueEx(key, "EDID", 0, pType, edid, lpcbData) == WinError.ERROR_SUCCESS) {
                                Display display = new WindowsDisplay(edid);
                                displays.add(display);
                            }
                        }
                    }
                    Advapi32.INSTANCE.RegCloseKey(key);
                }
            }
            SU.SetupDiDestroyDeviceInfoList(hDevInfo);
        }
        return displays;
    }
}
