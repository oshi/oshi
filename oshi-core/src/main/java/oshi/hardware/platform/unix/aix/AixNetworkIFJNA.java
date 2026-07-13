/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.net.NetworkInterface;
import java.util.List;
import java.util.function.Supplier;

import com.sun.jna.Native;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_netinterface_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.perfstat.PerfstatNetInterfaceJNA;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.platform.unix.aix.AixNetworkIF;

/**
 * JNA-backed AIX NetworkIF.
 */
@ThreadSafe
public final class AixNetworkIFJNA extends AixNetworkIF {

    private final Supplier<perfstat_netinterface_t[]> netstats;

    private AixNetworkIFJNA(NetworkInterface netint, Supplier<perfstat_netinterface_t[]> netstats)
            throws InstantiationException {
        super(netint);
        this.netstats = netstats;
        updateAttributes();
    }

    /**
     * Gets all network interfaces on this machine.
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @return a list of {@link NetworkIF} objects representing the interfaces
     */
    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        Supplier<perfstat_netinterface_t[]> netstats = memoize(PerfstatNetInterfaceJNA::queryNetInterfaces,
                defaultExpiration());
        return getNetworks(includeLocalInterfaces, ni -> new AixNetworkIFJNA(ni, netstats));
    }

    @Override
    protected IfStats queryStats() {
        for (perfstat_netinterface_t stat : netstats.get()) {
            if (Native.toString(stat.name).equals(this.getName())) {
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
