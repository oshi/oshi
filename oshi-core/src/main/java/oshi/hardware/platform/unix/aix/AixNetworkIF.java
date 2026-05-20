/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_netinterface_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.perfstat.PerfstatNetInterface;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;

/**
 * AIXNetworks class.
 */
@ThreadSafe
public final class AixNetworkIF extends AbstractNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(AixNetworkIF.class);

    private Supplier<perfstat_netinterface_t[]> netstats;

    public AixNetworkIF(NetworkInterface netint, Supplier<perfstat_netinterface_t[]> netstats)
            throws InstantiationException {
        super(netint);
        this.netstats = netstats;
        updateAttributes();
    }

    /**
     * Gets all network interfaces on this machine
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @return A list of {@link NetworkIF} objects representing the interfaces
     */
    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        Supplier<perfstat_netinterface_t[]> netstats = memoize(PerfstatNetInterface::queryNetInterfaces,
                defaultExpiration());
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(new AixNetworkIF(ni, netstats));
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.getMessage());
            }
        }
        return ifList;
    }

    @Override
    public boolean updateAttributes() {
        perfstat_netinterface_t[] stats = netstats.get();
        long now = System.currentTimeMillis();
        for (perfstat_netinterface_t stat : stats) {
            String name = Native.toString(stat.name);
            if (name.equals(this.getName())) {
                setBytesSent(stat.obytes);
                setBytesRecv(stat.ibytes);
                setPacketsSent(stat.opackets);
                setPacketsRecv(stat.ipackets);
                setOutErrors(stat.oerrors);
                setInErrors(stat.ierrors);
                setCollisions(stat.collisions);
                setInDrops(stat.if_iqdrops);
                setSpeed(stat.bitrate);
                setTimeStamp(now);
                return true;
            }
        }
        return false;
    }
}
