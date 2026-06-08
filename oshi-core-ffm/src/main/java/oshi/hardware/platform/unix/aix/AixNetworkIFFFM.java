/*
 * Copyright 2026 The OSHI Project Contributors
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

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.perfstat.PerfstatNetInterfaceFFM;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.platform.unix.aix.AixNetworkIF;

/**
 * FFM-backed AIX NetworkIF.
 */
@ThreadSafe
public final class AixNetworkIFFFM extends AixNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(AixNetworkIFFFM.class);

    private final Supplier<PerfstatNetInterfaceFFM.NetInterface[]> netstats;

    private AixNetworkIFFFM(NetworkInterface netint, Supplier<PerfstatNetInterfaceFFM.NetInterface[]> netstats)
            throws InstantiationException {
        super(netint);
        this.netstats = netstats;
        updateAttributes();
    }

    /**
     * Gets all network interfaces on this machine.
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @return list of {@link NetworkIF} objects
     */
    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        Supplier<PerfstatNetInterfaceFFM.NetInterface[]> netstats = memoize(PerfstatNetInterfaceFFM::queryNetInterfaces,
                defaultExpiration());
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(new AixNetworkIFFFM(ni, netstats));
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.getMessage());
            }
        }
        return ifList;
    }

    @Override
    protected IfStats queryStats() {
        for (PerfstatNetInterfaceFFM.NetInterface stat : netstats.get()) {
            if (stat.name.equals(this.getName())) {
                IfStats out = new IfStats();
                out.bytesSent = stat.obytes;
                out.bytesRecv = stat.ibytes;
                out.packetsSent = stat.opackets;
                out.packetsRecv = stat.ipackets;
                out.outErrors = stat.oerrors;
                out.inErrors = stat.ierrors;
                out.collisions = stat.collisions;
                out.inDrops = stat.if_iqdrops;
                out.speed = stat.bitrate;
                return out;
            }
        }
        return null;
    }
}
