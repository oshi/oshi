/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import static oshi.software.os.unix.solaris.SolarisOperatingSystem.HAS_KSTAT2;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;

/**
 * SolarisNetworks class.
 */
@ThreadSafe
public final class SolarisNetworkIF extends AbstractNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(SolarisNetworkIF.class);

    public SolarisNetworkIF(NetworkInterface netint) throws InstantiationException {
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
                ifList.add(new SolarisNetworkIF(ni));
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.getMessage());
            }
        }
        return ifList;
    }

    @Override
    public boolean updateAttributes() {
        // Initialize to a sane default value
        setTimeStamp(System.currentTimeMillis());
        if (HAS_KSTAT2) {
            // Use Kstat2 implementation
            return updateAttributes2();
        }
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = kc.lookup("link", -1, getName());
            if (ksp == null) { // Solaris 10 compatibility
                ksp = kc.lookup(null, -1, getName());
            }
            if (ksp != null && kc.read(ksp)) {
                setBytesSent(KstatUtil.dataLookupLong(ksp, "obytes64"));
                setBytesRecv(KstatUtil.dataLookupLong(ksp, "rbytes64"));
                setPacketsSent(KstatUtil.dataLookupLong(ksp, "opackets64"));
                setPacketsRecv(KstatUtil.dataLookupLong(ksp, "ipackets64"));
                setOutErrors(KstatUtil.dataLookupLong(ksp, "oerrors"));
                setInErrors(KstatUtil.dataLookupLong(ksp, "ierrors"));
                setCollisions(KstatUtil.dataLookupLong(ksp, "collisions"));
                setInDrops(KstatUtil.dataLookupLong(ksp, "dl_idrops"));
                setSpeed(KstatUtil.dataLookupLong(ksp, "ifspeed"));
                // Snap time in ns; convert to ms
                setTimeStamp(ksp.ks_snaptime / 1_000_000L);
                return true;
            }
        }
        return false;
    }

    private boolean updateAttributes2() {
        Object[] results = KstatUtil.queryKstat2("kstat:/net/link/" + getName() + "/0", "obytes64", "rbytes64",
                "opackets64", "ipackets64", "oerrors", "ierrors", "collisions", "dl_idrops", "ifspeed", "snaptime");
        if (results[results.length - 1] == null) {
            return false;
        }
        setBytesSent(results[0] == null ? 0L : (long) results[0]);
        setBytesRecv(results[1] == null ? 0L : (long) results[1]);
        setPacketsSent(results[2] == null ? 0L : (long) results[2]);
        setPacketsRecv(results[3] == null ? 0L : (long) results[3]);
        setOutErrors(results[4] == null ? 0L : (long) results[4]);
        setInErrors(results[5] == null ? 0L : (long) results[5]);
        setCollisions(results[6] == null ? 0L : (long) results[6]);
        setInDrops(results[7] == null ? 0L : (long) results[7]);
        setSpeed(results[8] == null ? 0L : (long) results[8]);
        // Snap time in ns; convert to ms
        setTimeStamp((long) results[9] / 1_000_000L);
        return true;
    }
}
