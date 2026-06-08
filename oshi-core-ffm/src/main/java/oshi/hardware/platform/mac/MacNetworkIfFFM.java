/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static oshi.ffm.platform.mac.SystemConfigurationFunctions.SCNetworkInterfaceCopyAll;
import static oshi.ffm.platform.mac.SystemConfigurationFunctions.SCNetworkInterfaceGetBSDName;
import static oshi.ffm.platform.mac.SystemConfigurationFunctions.SCNetworkInterfaceGetLocalizedDisplayName;

import java.lang.foreign.MemorySegment;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.net.NetStatFFM;
import oshi.driver.mac.net.NetStatFFM.IFdata;
import oshi.ffm.platform.mac.CoreFoundation.CFArrayRef;
import oshi.ffm.platform.mac.CoreFoundation.CFStringRef;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.platform.mac.MacNetworkIF;

/**
 * MacNetworks FFM implementation.
 */
@ThreadSafe
public final class MacNetworkIfFFM extends MacNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(MacNetworkIfFFM.class);

    public MacNetworkIfFFM(NetworkInterface netint, Map<Integer, IFdata> data) throws InstantiationException {
        super(netint, queryIfDisplayName(netint));
        updateNetworkStats(data);
    }

    private static String queryIfDisplayName(NetworkInterface netint) {
        String name = netint.getName();
        try {
            MemorySegment ifArray = SCNetworkInterfaceCopyAll();
            if (ifArray.equals(MemorySegment.NULL)) {
                return name;
            }
            CFArrayRef cfArray = new CFArrayRef(ifArray);
            try (cfArray) {
                int count = cfArray.getCount();
                for (int i = 0; i < count; i++) {
                    MemorySegment pNetIf = cfArray.getValueAtIndex(i);
                    MemorySegment cfNameSeg = SCNetworkInterfaceGetBSDName(pNetIf);
                    if (!cfNameSeg.equals(MemorySegment.NULL)) {
                        if (name.equals(CFStringRef.stringValue(cfNameSeg))) {
                            MemorySegment cfDisplayNameSeg = SCNetworkInterfaceGetLocalizedDisplayName(pNetIf);
                            if (!cfDisplayNameSeg.equals(MemorySegment.NULL)) {
                                return CFStringRef.stringValue(cfDisplayNameSeg);
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LOG.debug("Failed to query SC network interface display name for {}", name);
        }
        return name;
    }

    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        final Map<Integer, IFdata> data = NetStatFFM.queryIFdata(-1);
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(new MacNetworkIfFFM(ni, data));
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.getMessage());
            }
        }
        return ifList;
    }

    @Override
    public boolean updateAttributes() {
        int index = queryNetworkInterface().getIndex();
        return updateNetworkStats(NetStatFFM.queryIFdata(index));
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
