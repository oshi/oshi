/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows;

import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.windows.Cfgmgr32FFM;
import oshi.ffm.windows.SetupApiFFM;
import oshi.util.tuples.Quintet;

/**
 * FFM-based utility to query device interfaces via Configuration Manager Device Tree functions.
 */
@ThreadSafe
public final class DeviceTreeFFM {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceTreeFFM.class);

    private static final int MAX_PATH = 260;

    private DeviceTreeFFM() {
    }

    /**
     * Queries devices matching the specified device interface GUID and returns maps representing device tree
     * relationships, name, device ID, and manufacturer.
     *
     * @param guidBytes 16-byte GUID of the device interface class
     * @return A {@link Quintet} of: top-level device set, parent map, name map, device ID map, manufacturer map
     */
    public static Quintet<Set<Integer>, Map<Integer, Integer>, Map<Integer, String>, Map<Integer, String>, Map<Integer, String>> queryDeviceTree(
            byte[] guidBytes) {
        if (guidBytes == null || guidBytes.length != 16) {
            throw new IllegalArgumentException("guidBytes must be exactly 16 bytes");
        }
        Map<Integer, Integer> parentMap = new HashMap<>();
        Map<Integer, String> nameMap = new HashMap<>();
        Map<Integer, String> deviceIdMap = new HashMap<>();
        Map<Integer, String> mfgMap = new HashMap<>();

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment guidSeg = arena.allocate(16);
            guidSeg.copyFrom(MemorySegment.ofArray(guidBytes));

            Optional<MemorySegment> hDevInfoOpt = SetupApiFFM.SetupDiGetClassDevs(guidSeg,
                    SetupApiFFM.DIGCF_PRESENT | SetupApiFFM.DIGCF_DEVICEINTERFACE);
            if (hDevInfoOpt.isPresent()) {
                MemorySegment hDevInfo = hDevInfoOpt.get();
                try {
                    MemorySegment devInfoData = arena.allocate(SetupApiFFM.SP_DEVINFO_DATA_SIZE);
                    MemorySegment childSeg = arena.allocate(JAVA_INT);
                    MemorySegment siblingSeg = arena.allocate(JAVA_INT);
                    MemorySegment buf = arena.allocate(MAX_PATH * 2L);
                    MemorySegment sizeSeg = arena.allocate(JAVA_INT);

                    Queue<Integer> deviceTree = new ArrayDeque<>();
                    for (int i = 0;; i++) {
                        devInfoData.fill((byte) 0);
                        devInfoData.set(JAVA_INT, 0, SetupApiFFM.SP_DEVINFO_DATA_SIZE);
                        if (!SetupApiFFM.SetupDiEnumDeviceInfo(hDevInfo, i, devInfoData)) {
                            break;
                        }
                        int devInst = devInfoData.get(JAVA_INT, SetupApiFFM.SP_DEVINFO_DATA_DEVINST_OFFSET);
                        deviceTree.add(devInst);

                        while (!deviceTree.isEmpty()) {
                            int node = deviceTree.poll();

                            deviceIdMap.put(node, Cfgmgr32FFM.getDeviceId(node, arena));

                            String name = Cfgmgr32FFM.getDevNodeProperty(node, Cfgmgr32FFM.CM_DRP_FRIENDLYNAME, buf,
                                    sizeSeg);
                            if (name.isEmpty()) {
                                name = Cfgmgr32FFM.getDevNodeProperty(node, Cfgmgr32FFM.CM_DRP_DEVICEDESC, buf,
                                        sizeSeg);
                            }
                            if (name.isEmpty()) {
                                name = Cfgmgr32FFM.getDevNodeProperty(node, Cfgmgr32FFM.CM_DRP_CLASS, buf, sizeSeg);
                                String svc = Cfgmgr32FFM.getDevNodeProperty(node, Cfgmgr32FFM.CM_DRP_SERVICE, buf,
                                        sizeSeg);
                                if (!svc.isEmpty()) {
                                    name = name + " (" + svc + ")";
                                }
                            }
                            nameMap.put(node, name);
                            mfgMap.put(node,
                                    Cfgmgr32FFM.getDevNodeProperty(node, Cfgmgr32FFM.CM_DRP_MFG, buf, sizeSeg));

                            // Traverse children
                            try {
                                if (Cfgmgr32FFM.CM_Get_Child(childSeg, node, 0) == Cfgmgr32FFM.CR_SUCCESS) {
                                    int child = childSeg.get(JAVA_INT, 0);
                                    parentMap.put(child, node);
                                    deviceTree.add(child);
                                    while (Cfgmgr32FFM.CM_Get_Sibling(siblingSeg, child, 0) == Cfgmgr32FFM.CR_SUCCESS) {
                                        int sibling = siblingSeg.get(JAVA_INT, 0);
                                        parentMap.put(sibling, node);
                                        deviceTree.add(sibling);
                                        child = sibling;
                                    }
                                }
                            } catch (Throwable t) {
                                LOG.debug("CM_Get_Child/Sibling threw for node {}", node, t);
                            }
                        }
                    }
                } finally {
                    SetupApiFFM.SetupDiDestroyDeviceInfoList(hDevInfo);
                }
            }
        }

        Set<Integer> controllerDevices = deviceIdMap.keySet().stream().filter(k -> !parentMap.containsKey(k))
                .collect(Collectors.toSet());
        return new Quintet<>(controllerDevices, parentMap, nameMap, deviceIdMap, mfgMap);
    }
}
