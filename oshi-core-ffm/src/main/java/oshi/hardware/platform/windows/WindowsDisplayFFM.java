/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;
import static oshi.ffm.platform.windows.SetupApiFFM.SP_DEVICE_INTERFACE_DATA;
import static oshi.ffm.platform.windows.WindowsForeignFunctions.readWideString;
import static oshi.util.ExceptionUtil.getOrDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.windows.DisplayConnector;
import oshi.ffm.NativeHandle;
import oshi.ffm.platform.windows.Advapi32FFM;
import oshi.ffm.platform.windows.SetupApiFFM;
import oshi.ffm.platform.windows.User32FFM;
import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;
import oshi.util.Constants;

/**
 * A Display using FFM for native access.
 */
@Immutable
final class WindowsDisplayFFM extends AbstractDisplay {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsDisplayFFM.class);

    // GUID_DEVINTERFACE_MONITOR {E6F07B5F-EE97-4a90-B076-33F57BF4EAA7}
    private static final byte[] GUID_DEVINTERFACE_MONITOR = { 0x5F, 0x7B, (byte) 0xF0, (byte) 0xE6, (byte) 0x97,
            (byte) 0xEE, (byte) 0x90, 0x4A, (byte) 0xB0, 0x76, 0x33, (byte) 0xF5, 0x7B, (byte) 0xF4, (byte) 0xEA,
            (byte) 0xA7 };

    private static final int KEY_QUERY_VALUE = 0x0001;
    private static final int ERROR_MORE_DATA = 234;
    private static final int ERROR_SUCCESS = 0;
    private static final int ERROR_INSUFFICIENT_BUFFER = 122;

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

    WindowsDisplayFFM(byte[] edid) {
        this(edid, Constants.UNKNOWN, false);
    }

    WindowsDisplayFFM(byte[] edid, String devicePort) {
        this(edid, devicePort, false);
    }

    WindowsDisplayFFM(byte[] edid, String devicePort, boolean primary) {
        super(edid);
        this.devicePort = devicePort;
        this.primary = primary;
        LOG.debug("Initialized WindowsDisplayFFM");
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
     * @return A list of Display objects representing monitors, etc. Displays whose connector Windows cannot resolve
     *         report {@link Constants#UNKNOWN} as their device port.
     */
    public static List<Display> getDisplays() {
        List<Display> displays = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment guidSeg = arena.allocate(16);
            guidSeg.copyFrom(MemorySegment.ofArray(GUID_DEVINTERFACE_MONITOR));

            // Query the CCD display configuration for connector names and primary display identity.
            DisplayConfig config = queryDisplayConfig(arena);

            Optional<MemorySegment> hDevInfoOpt = SetupApiFFM.SetupDiGetClassDevs(guidSeg,
                    SetupApiFFM.DIGCF_PRESENT | SetupApiFFM.DIGCF_DEVICEINTERFACE);
            if (hDevInfoOpt.isEmpty()) {
                return displays;
            }
            MemorySegment hDevInfo = hDevInfoOpt.get();
            // wrapped only to release the native handle on close
            try (var _ = NativeHandle.of(hDevInfo, SetupApiFFM::SetupDiDestroyDeviceInfoList)) {
                MemorySegment devInfoData = arena.allocate(SetupApiFFM.SP_DEVINFO_DATA_SIZE);
                MemorySegment did = arena.allocate(SP_DEVICE_INTERFACE_DATA);
                MemorySegment edidName = arena.allocateFrom("EDID", java.nio.charset.StandardCharsets.UTF_16LE);

                for (int i = 0;; i++) {
                    devInfoData.fill((byte) 0);
                    devInfoData.set(JAVA_INT, 0, SetupApiFFM.SP_DEVINFO_DATA_SIZE);
                    if (!SetupApiFFM.SetupDiEnumDeviceInfo(hDevInfo, i, devInfoData)) {
                        break;
                    }

                    MemorySegment key = SetupApiFFM.SetupDiOpenDevRegKey(hDevInfo, devInfoData,
                            SetupApiFFM.DICS_FLAG_GLOBAL, 0, SetupApiFFM.DIREG_DEV, KEY_QUERY_VALUE);
                    if (key == null) {
                        continue;
                    }
                    // wrapped only to release the native handle on close
                    try (var _ = NativeHandle.of(key, Advapi32FFM::RegCloseKey)) {
                        byte[] edid = queryEdidFromKey(key, edidName, arena);
                        if (edid != null) {
                            String path = getDeviceInterfacePath(hDevInfo, devInfoData, guidSeg, did, arena);
                            String normalizedPath = path != null ? DisplayConnector.normalizePath(path)
                                    : Constants.UNKNOWN;
                            String port = config.portByPath.getOrDefault(normalizedPath, Constants.UNKNOWN);
                            boolean primary = config.primaryPaths.contains(normalizedPath);
                            displays.add(new WindowsDisplayFFM(edid, port, primary));
                        }
                    }
                }
            }
        }
        return displays;
    }

    private static byte @Nullable [] queryEdidFromKey(MemorySegment key, MemorySegment edidName, Arena arena) {
        return getOrDefault(() -> {
            MemorySegment pType = arena.allocate(JAVA_INT);
            MemorySegment lpcbData = arena.allocate(JAVA_INT);
            MemorySegment dummyBuf = arena.allocate(1);
            lpcbData.set(JAVA_INT, 0, 1);

            int rc = Advapi32FFM.RegQueryValueEx(key, edidName, pType, dummyBuf, lpcbData);
            if (rc != ERROR_MORE_DATA) {
                return null;
            }
            int size = lpcbData.get(JAVA_INT, 0);
            MemorySegment edidBuf = arena.allocate(size);
            lpcbData.set(JAVA_INT, 0, size);
            rc = Advapi32FFM.RegQueryValueEx(key, edidName, pType, edidBuf, lpcbData);
            if (rc == ERROR_SUCCESS) {
                return edidBuf.asSlice(0, size).toArray(JAVA_BYTE);
            }
            return null;
        }, null, LOG, "Failed to read EDID from registry");
    }

    // Obtains the device interface path for the current device: enumerates the monitor interface, then reads the path.
    // Returns null if the interface or path cannot be obtained.
    private static @Nullable String getDeviceInterfacePath(MemorySegment hDevInfo, MemorySegment devInfoData,
            MemorySegment guidSeg, MemorySegment did, Arena arena) {
        did.fill((byte) 0);
        did.set(JAVA_INT, 0, (int) SP_DEVICE_INTERFACE_DATA.byteSize());
        if (SetupApiFFM.SetupDiEnumDeviceInterfaces(hDevInfo, devInfoData, guidSeg, 0, did) != 1) {
            return null;
        }
        int size = SetupApiFFM.SetupDiGetDeviceInterfaceDetailSize(hDevInfo, did, arena);
        if (size <= 0) {
            return null;
        }
        return SetupApiFFM.SetupDiGetDeviceInterfaceDetail(hDevInfo, did, size, arena).orElse(null);
    }

    // Builds a DisplayConfig from the CCD active paths, containing both the connector-name map and the set of primary
    // device paths. A topology change between sizing and querying the buffers makes QueryDisplayConfig fail with
    // ERROR_INSUFFICIENT_BUFFER, which is retryable by re-sizing.
    private static DisplayConfig queryDisplayConfig(Arena arena) {
        for (int attempt = 0; attempt < QDC_ATTEMPTS; attempt++) {
            DisplayConfig config = queryDisplayConfigOnce(arena);
            if (config != null) {
                return config;
            }
        }
        LOG.debug("Display configuration kept changing; unable to map connectors.");
        return new DisplayConfig(new HashMap<>(), new HashSet<>());
    }

    // Returns null if the buffers were too small and the caller should re-size and retry.
    private static @Nullable DisplayConfig queryDisplayConfigOnce(Arena arena) {
        Map<String, String> portMap = new HashMap<>();
        Set<String> primaryPaths = new HashSet<>();
        MemorySegment numPaths = arena.allocate(JAVA_INT);
        MemorySegment numModes = arena.allocate(JAVA_INT);
        if (User32FFM.GetDisplayConfigBufferSizes(DisplayConnector.QDC_ONLY_ACTIVE_PATHS, numPaths,
                numModes) != ERROR_SUCCESS) {
            return new DisplayConfig(portMap, primaryPaths);
        }
        int pathCount = numPaths.get(JAVA_INT, 0);
        int modeCount = numModes.get(JAVA_INT, 0);
        if (pathCount <= 0) {
            return new DisplayConfig(portMap, primaryPaths);
        }
        MemorySegment paths = arena.allocate((long) pathCount * DisplayConnector.PATH_INFO_SIZE);
        MemorySegment modes = arena.allocate(Math.max(1L, (long) modeCount * DisplayConnector.MODE_INFO_SIZE));
        int rc = User32FFM.QueryDisplayConfig(DisplayConnector.QDC_ONLY_ACTIVE_PATHS, numPaths, paths, numModes, modes,
                MemorySegment.NULL);
        if (rc == ERROR_INSUFFICIENT_BUFFER) {
            return null;
        }
        if (rc != ERROR_SUCCESS) {
            return new DisplayConfig(portMap, primaryPaths);
        }
        int actualPaths = numPaths.get(JAVA_INT, 0);
        int actualModes = numModes.get(JAVA_INT, 0);
        for (int i = 0; i < actualPaths; i++) {
            long base = (long) i * DisplayConnector.PATH_INFO_SIZE;
            int flags = paths.get(JAVA_INT, base + DisplayConnector.PATH_FLAGS_OFFSET);
            if ((flags & DisplayConnector.PATH_ACTIVE) == 0) {
                continue;
            }
            long adapterId = paths.get(JAVA_LONG_UNALIGNED, base + DisplayConnector.PATH_TARGET_ADAPTER_ID_OFFSET);
            int targetId = paths.get(JAVA_INT, base + DisplayConnector.PATH_TARGET_ID_OFFSET);
            // Check whether this path's source mode is at desktop position (0, 0), which Windows
            // defines as the primary display.
            boolean primaryPath = isSourceAtOrigin(paths, base, modes, actualModes);
            addConnector(portMap, primaryPaths, arena, adapterId, targetId, primaryPath);
        }
        return new DisplayConfig(portMap, primaryPaths);
    }

    // Returns true if the source mode for the given path has desktop position (0, 0).
    private static boolean isSourceAtOrigin(MemorySegment paths, long pathBase, MemorySegment modes, int modeCount) {
        int modeIdx = paths.get(JAVA_INT, pathBase + DisplayConnector.PATH_SOURCE_MODE_IDX_OFFSET);
        if (modeIdx < 0 || modeIdx >= modeCount) {
            return false;
        }
        long modeBase = (long) modeIdx * DisplayConnector.MODE_INFO_SIZE;
        int infoType = modes.get(JAVA_INT, modeBase + DisplayConnector.MODE_INFO_TYPE_OFFSET);
        if (infoType != DisplayConnector.MODE_INFO_TYPE_SOURCE) {
            return false;
        }
        int posX = modes.get(JAVA_INT, modeBase + DisplayConnector.SOURCE_MODE_POSITION_X_OFFSET);
        int posY = modes.get(JAVA_INT, modeBase + DisplayConnector.SOURCE_MODE_POSITION_Y_OFFSET);
        return posX == 0 && posY == 0;
    }

    // Fetches one target's DISPLAYCONFIG_TARGET_DEVICE_NAME and records its device path -> connector name. If
    // primaryPath is true, the normalized device path is also added to the primary set.
    private static void addConnector(Map<String, String> portMap, Set<String> primaryPaths, Arena arena, long adapterId,
            int targetId, boolean primaryPath) {
        MemorySegment tdn = arena.allocate(DisplayConnector.TARGET_DEVICE_NAME_SIZE);
        tdn.set(JAVA_INT, 0, DisplayConnector.DEVICE_INFO_GET_TARGET_NAME);
        tdn.set(JAVA_INT, DisplayConnector.TDN_HEADER_SIZE_OFFSET, DisplayConnector.TARGET_DEVICE_NAME_SIZE);
        tdn.set(JAVA_LONG_UNALIGNED, DisplayConnector.TDN_HEADER_ADAPTER_ID_OFFSET, adapterId);
        tdn.set(JAVA_INT, DisplayConnector.TDN_HEADER_ID_OFFSET, targetId);
        if (User32FFM.DisplayConfigGetDeviceInfo(tdn) != ERROR_SUCCESS) {
            return;
        }
        int outputTechnology = tdn.get(JAVA_INT, DisplayConnector.TDN_OUTPUT_TECHNOLOGY_OFFSET);
        int connectorInstance = tdn.get(JAVA_INT, DisplayConnector.TDN_CONNECTOR_INSTANCE_OFFSET);
        String path = readWideString(tdn.asSlice(DisplayConnector.TDN_MONITOR_DEVICE_PATH_OFFSET));
        String key = DisplayConnector.normalizePath(path);
        if (!Constants.UNKNOWN.equals(key)) {
            portMap.put(key, DisplayConnector.connectorName(outputTechnology, connectorInstance));
            if (primaryPath) {
                primaryPaths.add(key);
            }
        }
    }
}
