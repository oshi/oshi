/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static org.slf4j.event.Level.WARN;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.software.os.linux.LinuxOperatingSystemFFM.HAS_UDEV;

import java.lang.foreign.MemorySegment;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.NativeHandle;
import oshi.ffm.platform.linux.UdevFunctions;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.platform.linux.LinuxNetworkIF;
import oshi.util.Util;
import oshi.util.linux.SysPath;

/**
 * FFM-based Linux network interface implementation.
 */
@ThreadSafe
public final class LinuxNetworkIFFFM extends LinuxNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxNetworkIFFFM.class);

    LinuxNetworkIFFFM(NetworkInterface netint) throws InstantiationException {
        super(netint, queryIfModel(netint));
    }

    private static String queryIfModel(NetworkInterface netint) {
        String name = netint.getName();
        if (!HAS_UDEV) {
            return queryIfModelFromSysfs(name);
        }
        return callInArenaOrDefault(arena -> {
            MemorySegment udev = UdevFunctions.udev_new();
            if (MemorySegment.NULL.equals(udev)) {
                return queryIfModelFromSysfs(name);
            }
            try (NativeHandle udevHandle = NativeHandle.of(udev, UdevFunctions::udev_unref)) {
                MemorySegment device = UdevFunctions.deviceNewFromSyspath(udev, SysPath.NET + name, arena);
                if (!MemorySegment.NULL.equals(device)) {
                    try (NativeHandle devHandle = NativeHandle.of(device, UdevFunctions::udev_device_unref)) {
                        String devVendor = UdevFunctions.getPropertyValue(device, "ID_VENDOR_FROM_DATABASE", arena);
                        String devModel = UdevFunctions.getPropertyValue(device, "ID_MODEL_FROM_DATABASE", arena);
                        if (!Util.isBlank(devModel)) {
                            if (!Util.isBlank(devVendor)) {
                                return devVendor + " " + devModel;
                            }
                            return devModel;
                        }
                    }
                }
            }
            return name;
        }, LOG, WARN, "Error querying network interface model for " + name, name);
    }

    /**
     * Gets network interfaces on this machine
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @return A list of {@link NetworkIF} objects representing the interfaces
     */
    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(new LinuxNetworkIFFFM(ni));
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.toString());
            }
        }
        return ifList;
    }
}
