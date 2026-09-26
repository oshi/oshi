/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import oshi.jna.platform.windows.User32;
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

    // Attempts allowed for the QueryDisplayConfig size-then-query pair, in case the topology changes between them.
    private static final int QDC_ATTEMPTS = 3;

    private final String devicePort;
    private final boolean primary;

    /**
     * Value object holding the results of a CCD display configuration query: the connector-name map and the set of
     * normalized monitor device paths that belong to the Windows primary display (source mode position 0,0).
     */
    private static final class DisplayConfig {
        private final Map<String, String> portByPath;
        private final Set<String> primaryPaths;

        DisplayConfig(Map<String, String> portByPath, Set<String> primaryPaths) {
            this.portByPath = portByPath;
            this.primaryPaths = primaryPaths;
        }
    }

    /**
     * Constructor for WindowsDisplay.
     *
     * @param edid a byte array representing a display EDID
     */
    WindowsDisplayJNA(byte[] edid) {
        this(edid, Constants.UNKNOWN, false);
    }

    /**
     * Constructor for WindowsDisplay with a device port.
     *
     * @param edid       a byte array representing a display EDID
     * @param devicePort the connector this display is attached to
     */
    WindowsDisplayJNA(byte[] edid, String devicePort) {
        this(edid, devicePort, false);
    }

    /**
     * Constructor for WindowsDisplay with a device port and primary status.
     *
     * @param edid       a byte array representing a display EDID
     * @param devicePort the connector this display is attached to
     * @param primary    whether this display is the primary display
     */
    WindowsDisplayJNA(byte[] edid, String devicePort, boolean primary) {
        super(edid);
        this.devicePort = devicePort;
        this.primary = primary;
        LOG.debug("Initialized WindowsDisplay");
    }

    @Override
    public String getDevicePort() {
        return this.devicePort;
    }

    @Override
    public boolean isPrimary() {
        return this.primary;
    }

    /**
     * Gets Display Information
     *
     * @return A list of Display objects representing monitors, etc.
     */
    public static List<Display> getDisplays() {
        List<Display> displays = new ArrayList<>();

        // Query the CCD display configuration for connector names and primary display identity.
        DisplayConfig config = queryDisplayConfig();

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
                                    String path = getDeviceInterfacePath(hDevInfo, deviceInterfaceData, info);
                                    String normalizedPath = path != null ? DisplayConnector.normalizePath(path)
                                            : Constants.UNKNOWN;
                                    String port = config.portByPath.getOrDefault(normalizedPath, Constants.UNKNOWN);
                                    boolean primary = config.primaryPaths.contains(normalizedPath);
                                    displays.add(new WindowsDisplayJNA(edid, port, primary));
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

    // Obtains the device interface path for the current device: enumerates the monitor interface, then reads the path.
    // Returns null if the interface or path cannot be obtained.
    private static @Nullable String getDeviceInterfacePath(HANDLE hDevInfo,
            CloseableSpDeviceInterfaceData deviceInterfaceData, CloseableSpDevinfoData info) {
        if (!SU.SetupDiEnumDeviceInterfaces(hDevInfo, info.getPointer(), GUID_DEVINTERFACE_MONITOR, 0,
                deviceInterfaceData)) {
            return null;
        }
        return getDeviceInterfaceDetail(hDevInfo, deviceInterfaceData);
    }

    // Two-call SetupDiGetDeviceInterfaceDetail: first for the required size, then to read the device path.
    private static @Nullable String getDeviceInterfaceDetail(HANDLE hDevInfo,
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

    // Builds a DisplayConfig from the CCD active paths, containing both the connector-name map and the set of primary
    // device paths. A topology change between sizing and querying the buffers makes QueryDisplayConfig fail with
    // ERROR_INSUFFICIENT_BUFFER, which is retryable by re-sizing.
    private static DisplayConfig queryDisplayConfig() {
        User32 u32 = User32.INSTANCE;
        for (int attempt = 0; attempt < QDC_ATTEMPTS; attempt++) {
            DisplayConfig config = queryDisplayConfigOnce(u32);
            if (config != null) {
                return config;
            }
        }
        LOG.debug("Display configuration kept changing; unable to map connectors.");
        return new DisplayConfig(new HashMap<>(), new HashSet<>());
    }

    // Returns null if the buffers were too small and the caller should re-size and retry.
    private static @Nullable DisplayConfig queryDisplayConfigOnce(User32 u32) {
        Map<String, String> portMap = new HashMap<>();
        Set<String> primaryPaths = new HashSet<>();
        try (CloseableIntByReference numPaths = new CloseableIntByReference();
                CloseableIntByReference numModes = new CloseableIntByReference()) {
            if (u32.GetDisplayConfigBufferSizes(DisplayConnector.QDC_ONLY_ACTIVE_PATHS, numPaths,
                    numModes) != WinError.ERROR_SUCCESS) {
                return new DisplayConfig(portMap, primaryPaths);
            }
            int pathCount = numPaths.getValue();
            int modeCount = numModes.getValue();
            if (pathCount <= 0) {
                return new DisplayConfig(portMap, primaryPaths);
            }
            try (Memory paths = new Memory((long) pathCount * DisplayConnector.PATH_INFO_SIZE);
                    Memory modes = new Memory(Math.max(1L, (long) modeCount * DisplayConnector.MODE_INFO_SIZE))) {
                paths.clear();
                modes.clear();
                int rc = u32.QueryDisplayConfig(DisplayConnector.QDC_ONLY_ACTIVE_PATHS, numPaths, paths, numModes,
                        modes, null);
                if (rc == WinError.ERROR_INSUFFICIENT_BUFFER) {
                    return null;
                }
                if (rc != WinError.ERROR_SUCCESS) {
                    return new DisplayConfig(portMap, primaryPaths);
                }
                int actualPaths = numPaths.getValue();
                int actualModes = numModes.getValue();
                for (int i = 0; i < actualPaths; i++) {
                    long base = (long) i * DisplayConnector.PATH_INFO_SIZE;
                    int flags = paths.getInt(base + DisplayConnector.PATH_FLAGS_OFFSET);
                    if ((flags & DisplayConnector.PATH_ACTIVE) == 0) {
                        continue;
                    }
                    long adapterId = paths.getLong(base + DisplayConnector.PATH_TARGET_ADAPTER_ID_OFFSET);
                    int targetId = paths.getInt(base + DisplayConnector.PATH_TARGET_ID_OFFSET);
                    // Check whether this path's source mode is at desktop position (0, 0), which Windows
                    // defines as the primary display.
                    boolean primaryPath = isSourceAtOrigin(paths, base, modes, actualModes);
                    addConnector(portMap, primaryPaths, u32, adapterId, targetId, primaryPath);
                }
            }
        }
        return new DisplayConfig(portMap, primaryPaths);
    }

    // Returns true if the source mode for the given path has desktop position (0, 0).
    private static boolean isSourceAtOrigin(Memory paths, long pathBase, Memory modes, int modeCount) {
        int modeIdx = paths.getInt(pathBase + DisplayConnector.PATH_SOURCE_MODE_IDX_OFFSET);
        if (modeIdx < 0 || modeIdx >= modeCount) {
            return false;
        }
        long modeBase = (long) modeIdx * DisplayConnector.MODE_INFO_SIZE;
        int infoType = modes.getInt(modeBase + DisplayConnector.MODE_INFO_TYPE_OFFSET);
        if (infoType != DisplayConnector.MODE_INFO_TYPE_SOURCE) {
            return false;
        }
        int posX = modes.getInt(modeBase + DisplayConnector.SOURCE_MODE_POSITION_X_OFFSET);
        int posY = modes.getInt(modeBase + DisplayConnector.SOURCE_MODE_POSITION_Y_OFFSET);
        return posX == 0 && posY == 0;
    }

    // Fetches one target's DISPLAYCONFIG_TARGET_DEVICE_NAME and records its device path -> connector name. If
    // primaryPath is true, the normalized device path is also added to the primary set.
    private static void addConnector(Map<String, String> portMap, Set<String> primaryPaths, User32 u32, long adapterId,
            int targetId, boolean primaryPath) {
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
                portMap.put(key, DisplayConnector.connectorName(outputTechnology, connectorInstance));
                if (primaryPath) {
                    primaryPaths.add(key);
                }
            }
        }
    }
}
