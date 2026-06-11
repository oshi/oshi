/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import static oshi.software.os.unix.solaris.SolarisOperatingSystemJNA.HAS_KSTAT2;

import java.net.NetworkInterface;
import java.util.List;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.platform.unix.solaris.SolarisNetworkIF;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;

/**
 * JNA-backed Solaris NetworkIF. Uses Kstat2 where available, falling back to the legacy {@code kstat} chain.
 */
@ThreadSafe
public final class SolarisNetworkIFJNA extends SolarisNetworkIF {

    public SolarisNetworkIFJNA(NetworkInterface netint) throws InstantiationException {
        super(netint);
        updateAttributes();
    }

    /**
     * Gets all network interfaces on this machine.
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @return a list of {@link NetworkIF} objects representing the interfaces
     */
    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        return getNetworks(includeLocalInterfaces, SolarisNetworkIFJNA::new);
    }

    @Override
    protected IfStats queryStats() {
        if (HAS_KSTAT2) {
            return queryStats2();
        }
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = kc.lookup("link", -1, getName());
            if (ksp == null) { // Solaris 10 compatibility
                ksp = kc.lookup(null, -1, getName());
            }
            if (ksp != null && kc.read(ksp)) {
                IfStats stats = new IfStats();
                stats.bytesSent = KstatUtil.dataLookupLong(ksp, "obytes64");
                stats.bytesRecv = KstatUtil.dataLookupLong(ksp, "rbytes64");
                stats.packetsSent = KstatUtil.dataLookupLong(ksp, "opackets64");
                stats.packetsRecv = KstatUtil.dataLookupLong(ksp, "ipackets64");
                stats.outErrors = KstatUtil.dataLookupLong(ksp, "oerrors");
                stats.inErrors = KstatUtil.dataLookupLong(ksp, "ierrors");
                stats.collisions = KstatUtil.dataLookupLong(ksp, "collisions");
                stats.inDrops = KstatUtil.dataLookupLong(ksp, "dl_idrops");
                stats.speed = KstatUtil.dataLookupLong(ksp, "ifspeed");
                // Snap time in ns; convert to ms
                stats.timeStamp = ksp.ks_snaptime / 1_000_000L;
                return stats;
            }
        }
        return null;
    }

    private IfStats queryStats2() {
        Object[] results = KstatUtil.queryKstat2("kstat:/net/link/" + getName() + "/0", "obytes64", "rbytes64",
                "opackets64", "ipackets64", "oerrors", "ierrors", "collisions", "dl_idrops", "ifspeed", "snaptime");
        if (results[results.length - 1] == null) {
            return null;
        }
        IfStats stats = new IfStats();
        stats.bytesSent = results[0] == null ? 0L : (long) results[0];
        stats.bytesRecv = results[1] == null ? 0L : (long) results[1];
        stats.packetsSent = results[2] == null ? 0L : (long) results[2];
        stats.packetsRecv = results[3] == null ? 0L : (long) results[3];
        stats.outErrors = results[4] == null ? 0L : (long) results[4];
        stats.inErrors = results[5] == null ? 0L : (long) results[5];
        stats.collisions = results[6] == null ? 0L : (long) results[6];
        stats.inDrops = results[7] == null ? 0L : (long) results[7];
        stats.speed = results[8] == null ? 0L : (long) results[8];
        // Snap time in ns; convert to ms
        stats.timeStamp = (long) results[9] / 1_000_000L;
        return stats;
    }
}
