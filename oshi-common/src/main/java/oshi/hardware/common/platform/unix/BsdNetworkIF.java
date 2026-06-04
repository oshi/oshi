/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * BsdNetworkIF applicable to FreeBSD and OpenBSD.
 */
@ThreadSafe
public final class BsdNetworkIF extends AbstractNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(BsdNetworkIF.class);

    public BsdNetworkIF(NetworkInterface netint) throws InstantiationException {
        super(netint);
        updateAttributes();
    }

    /**
     * Gets all network interfaces on this machine
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @return A list of {@link NetworkIF} objects representing the interfaces
     */
    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(new BsdNetworkIF(ni));
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.getMessage());
            }
        }
        return ifList;
    }

    @Override
    public boolean updateAttributes() {
        String stats = ExecutingCommand.getAnswerAt("netstat -bI " + getName(), 1);
        setTimeStamp(System.currentTimeMillis());
        String[] split = ParseUtil.whitespaces.split(stats);
        if (split.length < 12) {
            // No update
            return false;
        }
        setBytesSent(ParseUtil.parseUnsignedLongOrDefault(split[10], 0L));
        setBytesRecv(ParseUtil.parseUnsignedLongOrDefault(split[7], 0L));
        setPacketsSent(ParseUtil.parseUnsignedLongOrDefault(split[8], 0L));
        setPacketsRecv(ParseUtil.parseUnsignedLongOrDefault(split[4], 0L));
        setOutErrors(ParseUtil.parseUnsignedLongOrDefault(split[9], 0L));
        setInErrors(ParseUtil.parseUnsignedLongOrDefault(split[5], 0L));
        setCollisions(ParseUtil.parseUnsignedLongOrDefault(split[11], 0L));
        setInDrops(ParseUtil.parseUnsignedLongOrDefault(split[6], 0L));
        return true;
    }
}
