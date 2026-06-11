/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import java.lang.foreign.MemorySegment;
import java.net.NetworkInterface;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.solaris.LibKstatFunctions;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM.KstatChain;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.platform.unix.solaris.SolarisNetworkIF;

/**
 * FFM-backed Solaris NetworkIF. Uses the legacy {@code kstat} chain only; Kstat2 exists only on the JDK 17-capped
 * latest Solaris, so FFM (JDK 25) never needs it.
 */
@ThreadSafe
public final class SolarisNetworkIFFFM extends SolarisNetworkIF {

    public SolarisNetworkIFFFM(NetworkInterface netint) throws InstantiationException {
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
        return getNetworks(includeLocalInterfaces, SolarisNetworkIFFFM::new);
    }

    @Override
    protected IfStats queryStats() {
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            MemorySegment ksp = kc.lookup("link", -1, getName());
            if (ksp.address() == 0L) {
                ksp = kc.lookup(null, -1, getName());
            }
            if (ksp.address() != 0L && kc.read(ksp)) {
                IfStats stats = new IfStats();
                stats.bytesSent = KstatUtilFFM.dataLookupLong(ksp, "obytes64");
                stats.bytesRecv = KstatUtilFFM.dataLookupLong(ksp, "rbytes64");
                stats.packetsSent = KstatUtilFFM.dataLookupLong(ksp, "opackets64");
                stats.packetsRecv = KstatUtilFFM.dataLookupLong(ksp, "ipackets64");
                stats.outErrors = KstatUtilFFM.dataLookupLong(ksp, "oerrors");
                stats.inErrors = KstatUtilFFM.dataLookupLong(ksp, "ierrors");
                stats.collisions = KstatUtilFFM.dataLookupLong(ksp, "collisions");
                stats.inDrops = KstatUtilFFM.dataLookupLong(ksp, "dl_idrops");
                stats.speed = KstatUtilFFM.dataLookupLong(ksp, "ifspeed");
                stats.timeStamp = LibKstatFunctions.kstatSnaptime(ksp) / 1_000_000L;
                return stats;
            }
        }
        return null;
    }
}
