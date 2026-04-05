/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystem.HAS_UDEV;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.linux.Udev;
import com.sun.jna.platform.linux.Udev.UdevContext;
import com.sun.jna.platform.linux.Udev.UdevDevice;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.util.Util;
import oshi.util.platform.linux.SysPath;

/**
 * JNA-based Linux network interface implementation.
 */
@ThreadSafe
public final class LinuxNetworkIFJNA extends LinuxNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxNetworkIFJNA.class);

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
                        if (!Util.isBlank(devModel)) {
                            if (!Util.isBlank(devVendor)) {
                                return devVendor + " " + devModel;
                            }
                            return devModel;
                        }
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
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(new LinuxNetworkIFJNA(ni));
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.getMessage());
            }
        }
        return ifList;
    }
}
