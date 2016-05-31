/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.mac;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworks;
import oshi.jna.platform.mac.SystemB;
import oshi.jna.platform.mac.SystemB.IFmsgHdr;
import oshi.jna.platform.mac.SystemB.IFmsgHdr2;

/**
 * @author widdis[at]gmail[dot]com
 */
public class MacNetworks extends AbstractNetworks {

    private static final Logger LOG = LoggerFactory.getLogger(MacNetworks.class);

    private static int CTL_NET = 4;
    private static int PF_ROUTE = 17;
    private static int NET_RT_IFLIST2 = 6;
    private static int RTM_IFINFO2 = 0x12;

    /**
     * Class to store a subset of IF data in the ifMap
     */
    private static class IFdata {
        IFdata(long oPackets, long iPackets, long oBytes, long iBytes, long speed) {
            this.oPackets = oPackets;
            this.iPackets = iPackets;
            this.oBytes = oBytes;
            this.iBytes = iBytes;
            this.speed = speed;
        }

        long oPackets;
        long iPackets;
        long oBytes;
        long iBytes;
        long speed;
    }

    /**
     * Update map no more frequently than 200ms.
     * 
     * Key is the index of the IF.
     */
    private Map<Integer, IFdata> ifMap = new HashMap<>();
    private long lastIFmapTime = 0L;

    /**
     * Map all network interfaces. Ported from source code of "netstat -ir". See
     * http://opensource.apple.com/source/network_cmds/network_cmds-457/
     * netstat.tproj/if.c
     */
    private void mapIFs() {
        long now = System.currentTimeMillis();
        if (now - lastIFmapTime < 200L) {
            // Polled too recently; do nothing
            return;
        }
        lastIFmapTime = now;

        // Get buffer of all interface information
        int mib[] = { CTL_NET, PF_ROUTE, 0, 0, NET_RT_IFLIST2, 0 };
        IntByReference len = new IntByReference();
        if (0 != SystemB.INSTANCE.sysctl(mib, 6, null, len, null, 0)) {
            LOG.error("Didn't get buffer length for IFLIST2");
            return;
        }
        Pointer buf = new Memory(len.getValue());
        if (0 != SystemB.INSTANCE.sysctl(mib, 6, buf, len, null, 0)) {
            LOG.error("Didn't get buffer for IFLIST2");
            return;
        }

        // CLear out old hashmap
        ifMap.clear();

        // Iterate offset from buf's pointer up to limit of buf
        int lim = len.getValue();
        for (int next = 0; next < lim;) {
            // Get pointer to current native part of buf
            Pointer p = new Pointer(Pointer.nativeValue(buf) + next);
            // Cast pointer to if_msghdr
            IFmsgHdr ifm = new IFmsgHdr(p);
            ifm.read();
            // Advance next
            next += ifm.ifm_msglen;
            // Skip messages which are not the right format
            if (ifm.ifm_type != RTM_IFINFO2) {
                continue;
            }
            // Cast pointer to if_msghdr2
            IFmsgHdr2 if2m = new IFmsgHdr2(p);
            if2m.read();

            // Grab data to place in map using index as the key
            ifMap.put(Integer.valueOf(if2m.ifm_index),
                    new IFdata(if2m.ifm_data.ifi_opackets, if2m.ifm_data.ifi_ipackets, if2m.ifm_data.ifi_obytes,
                            if2m.ifm_data.ifi_ibytes, if2m.ifm_data.ifi_baudrate));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNetworkStats(NetworkIF netIF) {
        // Update data
        mapIFs();

        Integer index = Integer.valueOf(netIF.getNetworkInterface().getIndex());
        if (ifMap.containsKey(index)) {
            // Retrieve values from map and store
            IFdata ifData = ifMap.get(index);
            netIF.setBytesSent(ifData.oBytes);
            netIF.setBytesRecv(ifData.iBytes);
            netIF.setPacketsSent(ifData.oPackets);
            netIF.setPacketsRecv(ifData.iPackets);
            netIF.setSpeed(ifData.speed);
        }
    }
}
