/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux.nativefree;

import java.net.NetworkInterface;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.platform.linux.LinuxNetworkIF;

/**
 * Native-free Linux network interface implementation. Uses sysfs for model identification.
 */
@ThreadSafe
public final class LinuxNetworkIfNF extends LinuxNetworkIF {

    LinuxNetworkIfNF(NetworkInterface netint) throws InstantiationException {
        super(netint, queryIfModelFromSysfs(netint.getName()));
    }

    /**
     * Gets network interfaces on this machine.
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @return a list of {@link NetworkIF} objects
     */
    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        return getNetworks(includeLocalInterfaces, LinuxNetworkIfNF::new);
    }
}
