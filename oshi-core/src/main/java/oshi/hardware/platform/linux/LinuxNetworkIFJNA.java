/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystemJNA.HAS_UDEV;

import java.net.NetworkInterface;
import java.util.List;

import com.sun.jna.platform.linux.Udev;
import com.sun.jna.platform.linux.Udev.UdevContext;
import com.sun.jna.platform.linux.Udev.UdevDevice;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.platform.linux.LinuxNetworkIF;
import oshi.util.linux.SysPath;

/**
 * JNA-based Linux network interface implementation.
 */
@ThreadSafe
public final class LinuxNetworkIFJNA extends LinuxNetworkIF {

    LinuxNetworkIFJNA(NetworkInterface netint) throws InstantiationException {
        super(netint, queryIfModel(netint));
    }

    private static String queryIfModel(NetworkInterface netint) {
        String name = netint.getName();
        if (!HAS_UDEV) {
            return queryIfModelFromSysfs(name);
        }
        UdevContext udev = Udev.INSTANCE.udev_new();
        if (udev != null) {
            try {
                UdevDevice device = udev.deviceNewFromSyspath(SysPath.NET + name);
                if (device != null) {
                    try {
                        String devVendor = device.getPropertyValue("ID_VENDOR_FROM_DATABASE");
                        String devModel = device.getPropertyValue("ID_MODEL_FROM_DATABASE");
                        return formatModel(devVendor, devModel, name);
                    } finally {
                        device.unref();
                    }
                }
            } finally {
                udev.unref();
            }
        }
        return name;
    }

    /**
     * Gets network interfaces on this machine
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @return A list of {@link NetworkIF} objects representing the interfaces
     */
    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        return getNetworks(includeLocalInterfaces, LinuxNetworkIFJNA::new);
    }
}
