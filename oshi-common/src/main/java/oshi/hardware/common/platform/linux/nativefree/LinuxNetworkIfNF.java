/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux.nativefree;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.platform.linux.LinuxNetworkIF;

/**
 * Native-free Linux network interface implementation. Uses sysfs for model identification.
 */
@ThreadSafe
public final class LinuxNetworkIfNF extends LinuxNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxNetworkIfNF.class);

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
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(new LinuxNetworkIfNF(ni));
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.toString());
            }
        }
        return ifList;
    }
}
