/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.net.NetStat;
import oshi.driver.mac.net.NetStat.IFdata;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.platform.mac.MacNetworkIF;
import oshi.jna.platform.mac.SystemConfiguration;
import oshi.jna.platform.mac.SystemConfiguration.SCNetworkInterfaceRef;

/**
 * MacNetworks JNA implementation.
 */
@ThreadSafe
public final class MacNetworkIfJNA extends MacNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(MacNetworkIfJNA.class);

    public MacNetworkIfJNA(NetworkInterface netint, Map<Integer, IFdata> data) throws InstantiationException {
        super(netint, queryIfDisplayName(netint));
        updateNetworkStats(data);
    }

    private static String queryIfDisplayName(NetworkInterface netint) {
        String name = netint.getName();
        CFArrayRef ifArray = SystemConfiguration.INSTANCE.SCNetworkInterfaceCopyAll();
        if (ifArray != null) {
            try {
                int count = ifArray.getCount();
                for (int i = 0; i < count; i++) {
                    Pointer pNetIf = ifArray.getValueAtIndex(i);
                    SCNetworkInterfaceRef scNetIf = new SCNetworkInterfaceRef(pNetIf);
                    CFStringRef cfName = SystemConfiguration.INSTANCE.SCNetworkInterfaceGetBSDName(scNetIf);
                    if (cfName != null && name.equals(cfName.stringValue())) {
                        CFStringRef cfDisplayName = SystemConfiguration.INSTANCE
                                .SCNetworkInterfaceGetLocalizedDisplayName(scNetIf);
                        return cfDisplayName.stringValue();
                    }
                }
            } finally {
                ifArray.release();
            }
        }
        return name;
    }

    /**
     * Gets all network interfaces on this machine
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @return A list of {@link NetworkIF} objects representing the interfaces
     */
    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        final Map<Integer, IFdata> data = NetStat.queryIFdata(-1);
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(new MacNetworkIfJNA(ni, data));
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.getMessage());
            }
        }
        return ifList;
    }

    @Override
    public boolean updateAttributes() {
        int index = queryNetworkInterface().getIndex();
        return updateNetworkStats(NetStat.queryIFdata(index));
    }

    private boolean updateNetworkStats(Map<Integer, IFdata> data) {
        int index = queryNetworkInterface().getIndex();
        if (data.containsKey(index)) {
            IFdata ifData = data.get(index);
            setIfType(ifData.getIfType());
            setBytesSent(ifData.getOBytes());
            setBytesRecv(ifData.getIBytes());
            setPacketsSent(ifData.getOPackets());
            setPacketsRecv(ifData.getIPackets());
            setOutErrors(ifData.getOErrors());
            setInErrors(ifData.getIErrors());
            setCollisions(ifData.getCollisions());
            setInDrops(ifData.getIDrops());
            setSpeed(ifData.getSpeed());
            setTimeStamp(ifData.getTimeStamp());
            return true;
        }
        return false;
    }
}
