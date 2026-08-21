/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinReg.HKEY;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.windows.DisplayConnector;
import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.Struct.CloseableSpDeviceInterfaceData;
import oshi.jna.Struct.CloseableSpDevinfoData;
import oshi.jna.platform.windows.User32Ext;
import oshi.util.Constants;

/**
 * A Display
 */
@Immutable
final class WindowsDisplayJNA extends AbstractDisplay {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsDisplayJNA.class);

    private static final SetupApi SU = SetupApi.INSTANCE;
    private static final Advapi32 ADV = Advapi32.INSTANCE;

    private static final Guid.GUID GUID_DEVINTERFACE_MONITOR = new Guid.GUID("E6F07B5F-EE97-4a90-B076-33F57BF4EAA7");

    // SP_DEVICE_INTERFACE_DETAIL_DATA.cbSize must be 8 on 64-bit and 6 on 32-bit; the DevicePath follows the 4-byte
    // cbSize field.
    private static final int DETAIL_CBSIZE = Native.POINTER_SIZE == 8 ? 8 : 6;
    private static final int DETAIL_PATH_OFFSET = 4;

    private final String devicePort;

    /**
     * Constructor for WindowsDisplay.
     *
     * @param edid a byte array representing a display EDID
     */
    WindowsDisplayJNA(byte[] edid) {
        this(edid, Constants.UNKNOWN);
    }

    /**
     * Constructor for WindowsDisplay with a device port.
     *
     * @param edid       a byte array representing a display EDID
     * @param devicePort the connector this display is attached to
     */
    WindowsDisplayJNA(byte[] edid, String devicePort) {
        super(edid);
        this.devicePort = devicePort;
        LOG.debug("Initialized WindowsDisplay");
    }

    @Override
    public String getDevicePort() {
        return this.devicePort;
    }

    /**
     * Gets Display Information
     *
     * @return An array of Display objects representing monitors, etc.
     */
    public static List<Display> getDisplays() {
        List<Display> displays = new ArrayList<>();

        // Map every active connector's device interface path to its connector name (e.g. "HDMI", "DisplayPort-1").
        Map<String, String> portByPath = queryConnectorPorts();

        HANDLE hDevInfo = SU.SetupDiGetClassDevs(GUID_DEVINTERFACE_MONITOR, null, null,
                SetupApi.DIGCF_PRESENT | SetupApi.DIGCF_DEVICEINTERFACE);
        if (!hDevInfo.equals(WinBase.INVALID_HANDLE_VALUE)) {
            try (CloseableSpDeviceInterfaceData deviceInterfaceData = new CloseableSpDeviceInterfaceData();
                    CloseableSpDevinfoData info = new CloseableSpDevinfoData()) {
                deviceInterfaceData.cbSize = deviceInterfaceData.size();

                for (int memberIndex = 0; SU.SetupDiEnumDeviceInfo(hDevInfo, memberIndex, info); memberIndex++) {
                    HKEY key = SU.SetupDiOpenDevRegKey(hDevInfo, info, SetupApi.DICS_FLAG_GLOBAL, 0, SetupApi.DIREG_DEV,
                            WinNT.KEY_QUERY_VALUE);
                    try {
                        byte[] edid = new byte[1];

                        try (CloseableIntByReference pType = new CloseableIntByReference();
                                CloseableIntByReference lpcbData = new CloseableIntByReference()) {
                            if (ADV.RegQueryValueEx(key, "EDID", 0, pType, edid,
                                    lpcbData) == WinError.ERROR_MORE_DATA) {
                                edid = new byte[lpcbData.getValue()];
                                if (ADV.RegQueryValueEx(key, "EDID", 0, pType, edid,
                                        lpcbData) == WinError.ERROR_SUCCESS) {
                                    String port = lookupPort(hDevInfo, info, deviceInterfaceData, portByPath);
                                    displays.add(new WindowsDisplayJNA(edid, port));
                                }
                            }
                        }
                    } finally {
                        Advapi32.INSTANCE.RegCloseKey(key);
                    }
                }
            } finally {
                SU.SetupDiDestroyDeviceInfoList(hDevInfo);
            }
        }
        return displays;
    }

    // Resolves the connector name for the current device by fetching its device interface path and looking it up in the
    // CCD-derived map. Returns the sentinel if the interface or path cannot be obtained.
    private static String lookupPort(HANDLE hDevInfo, CloseableSpDevinfoData info,
            CloseableSpDeviceInterfaceData deviceInterfaceData, Map<String, String> portByPath) {
        if (!SU.SetupDiEnumDeviceInterfaces(hDevInfo, info.getPointer(), GUID_DEVINTERFACE_MONITOR, 0,
                deviceInterfaceData)) {
            return Constants.UNKNOWN;
        }
        String path = getDeviceInterfacePath(hDevInfo, deviceInterfaceData);
        if (path == null) {
            return Constants.UNKNOWN;
        }
        return portByPath.getOrDefault(DisplayConnector.normalizePath(path), Constants.UNKNOWN);
    }

    // Two-call SetupDiGetDeviceInterfaceDetail: first for the required size, then to read the device path.
    private static @Nullable String getDeviceInterfacePath(HANDLE hDevInfo,
            CloseableSpDeviceInterfaceData deviceInterfaceData) {
        try (CloseableIntByReference requiredSize = new CloseableIntByReference()) {
            SU.SetupDiGetDeviceInterfaceDetail(hDevInfo, deviceInterfaceData, null, 0, requiredSize, null);
            int size = requiredSize.getValue();
            if (size <= DETAIL_PATH_OFFSET) {
                return null;
            }
            try (Memory detail = new Memory(size)) {
                detail.clear();
                detail.setInt(0, DETAIL_CBSIZE);
                if (SU.SetupDiGetDeviceInterfaceDetail(hDevInfo, deviceInterfaceData, detail, size, requiredSize,
                        null)) {
                    return detail.getWideString(DETAIL_PATH_OFFSET);
                }
            }
        }
        return null;
    }

    // Builds a map from normalized monitor device interface path to connector name, from the CCD active paths.
    private static Map<String, String> queryConnectorPorts() {
        Map<String, String> map = new HashMap<>();
        User32Ext u32 = User32Ext.INSTANCE;
        try (CloseableIntByReference numPaths = new CloseableIntByReference();
                CloseableIntByReference numModes = new CloseableIntByReference()) {
            if (u32.GetDisplayConfigBufferSizes(DisplayConnector.QDC_ONLY_ACTIVE_PATHS, numPaths,
                    numModes) != WinError.ERROR_SUCCESS) {
                return map;
            }
            int pathCount = numPaths.getValue();
            int modeCount = numModes.getValue();
            if (pathCount <= 0) {
                return map;
            }
            try (Memory paths = new Memory((long) pathCount * DisplayConnector.PATH_INFO_SIZE);
                    Memory modes = new Memory(Math.max(1L, (long) modeCount * DisplayConnector.MODE_INFO_SIZE))) {
                paths.clear();
                modes.clear();
                if (u32.QueryDisplayConfig(DisplayConnector.QDC_ONLY_ACTIVE_PATHS, numPaths, paths, numModes, modes,
                        null) != WinError.ERROR_SUCCESS) {
                    return map;
                }
                int actualPaths = numPaths.getValue();
                for (int i = 0; i < actualPaths; i++) {
                    long base = (long) i * DisplayConnector.PATH_INFO_SIZE;
                    int flags = paths.getInt(base + DisplayConnector.PATH_FLAGS_OFFSET);
                    if ((flags & DisplayConnector.PATH_ACTIVE) == 0) {
                        continue;
                    }
                    long adapterId = paths.getLong(base + DisplayConnector.PATH_TARGET_ADAPTER_ID_OFFSET);
                    int targetId = paths.getInt(base + DisplayConnector.PATH_TARGET_ID_OFFSET);
                    addConnector(map, u32, adapterId, targetId);
                }
            }
        }
        return map;
    }

    // Fetches one target's DISPLAYCONFIG_TARGET_DEVICE_NAME and records its device path -> connector name.
    private static void addConnector(Map<String, String> map, User32Ext u32, long adapterId, int targetId) {
        try (Memory tdn = new Memory(DisplayConnector.TARGET_DEVICE_NAME_SIZE)) {
            tdn.clear();
            tdn.setInt(0, DisplayConnector.DEVICE_INFO_GET_TARGET_NAME);
            tdn.setInt(DisplayConnector.TDN_HEADER_SIZE_OFFSET, DisplayConnector.TARGET_DEVICE_NAME_SIZE);
            tdn.setLong(DisplayConnector.TDN_HEADER_ADAPTER_ID_OFFSET, adapterId);
            tdn.setInt(DisplayConnector.TDN_HEADER_ID_OFFSET, targetId);
            if (u32.DisplayConfigGetDeviceInfo(tdn) != WinError.ERROR_SUCCESS) {
                return;
            }
            int outputTechnology = tdn.getInt(DisplayConnector.TDN_OUTPUT_TECHNOLOGY_OFFSET);
            int connectorInstance = tdn.getInt(DisplayConnector.TDN_CONNECTOR_INSTANCE_OFFSET);
            String key = DisplayConnector
                    .normalizePath(tdn.getWideString(DisplayConnector.TDN_MONITOR_DEVICE_PATH_OFFSET));
            if (!Constants.UNKNOWN.equals(key)) {
                map.put(key, DisplayConnector.connectorName(outputTechnology, connectorInstance));
            }
        }
    }
}
