/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.windows.Advapi32FFM;
import oshi.ffm.windows.SetupApiFFM;
import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;

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

    WindowsDisplayFFM(byte[] edid) {
        super(edid);
        LOG.debug("Initialized WindowsDisplayFFM");
    }

    public static List<Display> getDisplays() {
        List<Display> displays = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment guidSeg = arena.allocate(16);
            guidSeg.copyFrom(MemorySegment.ofArray(GUID_DEVINTERFACE_MONITOR));

            Optional<MemorySegment> hDevInfoOpt = SetupApiFFM.SetupDiGetClassDevs(guidSeg,
                    SetupApiFFM.DIGCF_PRESENT | SetupApiFFM.DIGCF_DEVICEINTERFACE);
            if (hDevInfoOpt.isEmpty()) {
                return displays;
            }
            MemorySegment hDevInfo = hDevInfoOpt.get();
            try {
                MemorySegment devInfoData = arena.allocate(SetupApiFFM.SP_DEVINFO_DATA_SIZE);
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
                    try {
                        // First call to get size
                        MemorySegment pType = arena.allocate(JAVA_INT);
                        MemorySegment lpcbData = arena.allocate(JAVA_INT);
                        MemorySegment dummyBuf = arena.allocate(1);
                        lpcbData.set(JAVA_INT, 0, 1);

                        int rc = Advapi32FFM.RegQueryValueEx(key, edidName, 0, pType, dummyBuf, lpcbData);
                        if (rc != ERROR_MORE_DATA) {
                            LOG.debug("Sizing call for EDID data for monitor {}: rc={}", i, rc);
                        } else {
                            int size = lpcbData.get(JAVA_INT, 0);
                            MemorySegment edidBuf = arena.allocate(size);
                            lpcbData.set(JAVA_INT, 0, size);
                            rc = Advapi32FFM.RegQueryValueEx(key, edidName, 0, pType, edidBuf, lpcbData);
                            if (rc == ERROR_SUCCESS) {
                                byte[] edid = edidBuf.asSlice(0, size).toArray(JAVA_BYTE);
                                displays.add(new WindowsDisplayFFM(edid));
                            } else {
                                LOG.debug("Failed to read EDID data for monitor {}: rc={}", i, rc);
                            }
                        }
                    } catch (Throwable t) {
                        LOG.debug("Failed to read EDID from registry", t);
                    } finally {
                        try {
                            Advapi32FFM.RegCloseKey(key);
                        } catch (Throwable t) {
                            LOG.debug("RegCloseKey failed", t);
                        }
                    }
                }
            } finally {
                SetupApiFFM.SetupDiDestroyDeviceInfoList(hDevInfo);
            }
        }
        return displays;
    }
}
