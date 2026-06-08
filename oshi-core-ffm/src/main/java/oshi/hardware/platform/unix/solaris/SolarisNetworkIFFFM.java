/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import java.lang.foreign.MemorySegment;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.solaris.LibKstatFunctions;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM.KstatChain;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;

@ThreadSafe
public final class SolarisNetworkIFFFM extends AbstractNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(SolarisNetworkIFFFM.class);

    public SolarisNetworkIFFFM(NetworkInterface netint) throws InstantiationException {
        super(netint);
        updateAttributes();
    }

    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(new SolarisNetworkIFFFM(ni));
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.getMessage());
            }
        }
        return ifList;
    }

    @Override
    public boolean updateAttributes() {
        setTimeStamp(System.currentTimeMillis());
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            MemorySegment ksp = kc.lookup("link", -1, getName());
            if (ksp.address() == 0L) {
                ksp = kc.lookup(null, -1, getName());
            }
            if (ksp.address() != 0L && kc.read(ksp)) {
                setBytesSent(KstatUtilFFM.dataLookupLong(ksp, "obytes64"));
                setBytesRecv(KstatUtilFFM.dataLookupLong(ksp, "rbytes64"));
                setPacketsSent(KstatUtilFFM.dataLookupLong(ksp, "opackets64"));
                setPacketsRecv(KstatUtilFFM.dataLookupLong(ksp, "ipackets64"));
                setOutErrors(KstatUtilFFM.dataLookupLong(ksp, "oerrors"));
                setInErrors(KstatUtilFFM.dataLookupLong(ksp, "ierrors"));
                setCollisions(KstatUtilFFM.dataLookupLong(ksp, "collisions"));
                setInDrops(KstatUtilFFM.dataLookupLong(ksp, "dl_idrops"));
                setSpeed(KstatUtilFFM.dataLookupLong(ksp, "ifspeed"));
                setTimeStamp(LibKstatFunctions.kstatSnaptime(ksp) / 1_000_000L);
                return true;
            }
        }
        return false;
    }
}
