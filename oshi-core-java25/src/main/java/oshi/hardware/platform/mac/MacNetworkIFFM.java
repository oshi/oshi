/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static oshi.ffm.mac.SystemConfigurationFunctions.SCNetworkInterfaceCopyAll;
import static oshi.ffm.mac.SystemConfigurationFunctions.SCNetworkInterfaceGetBSDName;
import static oshi.ffm.mac.SystemConfigurationFunctions.SCNetworkInterfaceGetLocalizedDisplayName;

import java.lang.foreign.MemorySegment;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.net.NetStat;
import oshi.driver.mac.net.NetStat.IFdata;
import oshi.ffm.mac.CoreFoundation.CFArrayRef;
import oshi.ffm.mac.CoreFoundation.CFStringRef;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;

/**
 * MacNetworks FFM implementation.
 */
@ThreadSafe
public final class MacNetworkIFFM extends AbstractNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(MacNetworkIFFM.class);

    private int ifType;
    private long bytesRecv;
    private long bytesSent;
    private long packetsRecv;
    private long packetsSent;
    private long inErrors;
    private long outErrors;
    private long inDrops;
    private long collisions;
    private long speed;
    private long timeStamp;

    public MacNetworkIFFM(NetworkInterface netint, Map<Integer, IFdata> data) throws InstantiationException {
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
            try {
                int count = cfArray.getCount();
                for (int i = 0; i < count; i++) {
                    MemorySegment pNetIf = cfArray.getValueAtIndex(i);
                    MemorySegment cfNameSeg = SCNetworkInterfaceGetBSDName(pNetIf);
                    if (!cfNameSeg.equals(MemorySegment.NULL)) {
                        CFStringRef cfName = new CFStringRef(cfNameSeg);
                        if (name.equals(cfName.stringValue())) {
                            MemorySegment cfDisplayNameSeg = SCNetworkInterfaceGetLocalizedDisplayName(pNetIf);
                            if (!cfDisplayNameSeg.equals(MemorySegment.NULL)) {
                                return new CFStringRef(cfDisplayNameSeg).stringValue();
                            }
                        }
                    }
                }
            } finally {
                cfArray.release();
            }
        } catch (Throwable e) {
            LOG.debug("Failed to query SC network interface display name for {}", name);
        }
        return name;
    }

    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        final Map<Integer, IFdata> data = NetStat.queryIFdata(-1);
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(new MacNetworkIFFM(ni, data));
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.getMessage());
            }
        }
        return ifList;
    }

    @Override
    public int getIfType() {
        return this.ifType;
    }

    @Override
    public long getBytesRecv() {
        return this.bytesRecv;
    }

    @Override
    public long getBytesSent() {
        return this.bytesSent;
    }

    @Override
    public long getPacketsRecv() {
        return this.packetsRecv;
    }

    @Override
    public long getPacketsSent() {
        return this.packetsSent;
    }

    @Override
    public long getInErrors() {
        return this.inErrors;
    }

    @Override
    public long getOutErrors() {
        return this.outErrors;
    }

    @Override
    public long getInDrops() {
        return this.inDrops;
    }

    @Override
    public long getCollisions() {
        return this.collisions;
    }

    @Override
    public long getSpeed() {
        return this.speed;
    }

    @Override
    public long getTimeStamp() {
        return this.timeStamp;
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
            this.ifType = ifData.getIfType();
            this.bytesSent = ifData.getOBytes();
            this.bytesRecv = ifData.getIBytes();
            this.packetsSent = ifData.getOPackets();
            this.packetsRecv = ifData.getIPackets();
            this.outErrors = ifData.getOErrors();
            this.inErrors = ifData.getIErrors();
            this.collisions = ifData.getCollisions();
            this.inDrops = ifData.getIDrops();
            this.speed = ifData.getSpeed();
            this.timeStamp = ifData.getTimeStamp();
            return true;
        }
        return false;
    }
}
