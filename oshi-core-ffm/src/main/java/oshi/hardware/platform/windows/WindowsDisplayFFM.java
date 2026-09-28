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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.windows.DisplayConnector;
import oshi.driver.common.windows.DisplayConnector.Connector;
import oshi.ffm.NativeHandle;
import oshi.ffm.platform.windows.Advapi32FFM;
import oshi.ffm.platform.windows.SetupApiFFM;
import oshi.ffm.platform.windows.User32FFM;
import oshi.hardware.Display;
import oshi.hardware.DisplayMode;
import oshi.hardware.common.platform.windows.WindowsDisplay;
import oshi.util.Constants;

/**
 * A Display using FFM for native access.
 */
@Immutable
final class WindowsDisplayFFM extends WindowsDisplay {

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

    WindowsDisplayFFM(byte[] edid, @Nullable Connector connector) {
        super(edid, connector);
        LOG.debug("Initialized WindowsDisplayFFM");
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

            // Map every active connector's device interface path to its connector and mode.
            Map<String, Connector> connectorByPath = queryConnectors(arena);

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
                        byte @Nullable [] edid = queryEdidFromKey(key, edidName, arena);
                        if (edid != null) {
                            Connector connector = lookupConnector(hDevInfo, devInfoData, guidSeg, did, connectorByPath,
                                    arena);
                            displays.add(new WindowsDisplayFFM(edid, connector));
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

    // Resolves the connector for the current device by fetching its device interface path and looking it up in the
    // CCD-derived map. Returns null if the interface or path cannot be obtained or is not an active connector.
    private static @Nullable Connector lookupConnector(MemorySegment hDevInfo, MemorySegment devInfoData,
            MemorySegment guidSeg, MemorySegment did, Map<String, Connector> connectorByPath, Arena arena) {
        did.fill((byte) 0);
        did.set(JAVA_INT, 0, (int) SP_DEVICE_INTERFACE_DATA.byteSize());
        if (SetupApiFFM.SetupDiEnumDeviceInterfaces(hDevInfo, devInfoData, guidSeg, 0, did) != 1) {
            return null;
        }
        int size = SetupApiFFM.SetupDiGetDeviceInterfaceDetailSize(hDevInfo, did, arena);
        if (size <= 0) {
            return null;
        }
        Optional<String> path = SetupApiFFM.SetupDiGetDeviceInterfaceDetail(hDevInfo, did, size, arena);
        if (!path.isPresent()) {
            return null;
        }
        return connectorByPath.get(DisplayConnector.normalizePath(path.get()));
    }

    // Builds a map from normalized monitor device interface path to connector and mode, from the CCD active paths. A
    // topology change between sizing and querying the buffers makes QueryDisplayConfig fail with
    // ERROR_INSUFFICIENT_BUFFER, which is retryable by re-sizing.
    private static Map<String, Connector> queryConnectors(Arena arena) {
        for (int attempt = 0; attempt < QDC_ATTEMPTS; attempt++) {
            Map<String, Connector> map = queryConnectorsOnce(arena);
            if (map != null) {
                return map;
            }
        }
        LOG.debug("Display configuration kept changing; unable to map connectors.");
        return new HashMap<>();
    }

    // Returns null if the buffers were too small and the caller should re-size and retry.
    private static @Nullable Map<String, Connector> queryConnectorsOnce(Arena arena) {
        Map<String, Connector> map = new HashMap<>();
        MemorySegment numPaths = arena.allocate(JAVA_INT);
        MemorySegment numModes = arena.allocate(JAVA_INT);
        if (User32FFM.GetDisplayConfigBufferSizes(DisplayConnector.QDC_ONLY_ACTIVE_PATHS, numPaths,
                numModes) != ERROR_SUCCESS) {
            return map;
        }
        int pathCount = numPaths.get(JAVA_INT, 0);
        int modeCount = numModes.get(JAVA_INT, 0);
        if (pathCount <= 0) {
            return map;
        }
        MemorySegment paths = arena.allocate((long) pathCount * DisplayConnector.PATH_INFO_SIZE);
        MemorySegment modes = arena.allocate(Math.max(1L, (long) modeCount * DisplayConnector.MODE_INFO_SIZE));
        int rc = User32FFM.QueryDisplayConfig(DisplayConnector.QDC_ONLY_ACTIVE_PATHS, numPaths, paths, numModes, modes,
                MemorySegment.NULL);
        if (rc == ERROR_INSUFFICIENT_BUFFER) {
            return null;
        }
        if (rc != ERROR_SUCCESS) {
            return map;
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
            DisplayMode mode = DisplayConnector.readMode(off -> paths.get(JAVA_INT, base + off),
                    off -> modes.get(JAVA_INT, off), actualModes);
            addConnector(map, arena, adapterId, targetId, mode);
        }
        return map;
    }

    // Fetches one target's DISPLAYCONFIG_TARGET_DEVICE_NAME and records its device path -> connector.
    private static void addConnector(Map<String, Connector> map, Arena arena, long adapterId, int targetId,
            @Nullable DisplayMode mode) {
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
            map.put(key, new Connector(outputTechnology, connectorInstance, mode));
        }
    }
}
