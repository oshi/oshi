/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac.net;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.SystemB.IFmsgHdr;
import com.sun.jna.platform.mac.SystemB.IFmsgHdr2;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.mac.IFdata;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.mac.SystemB;

/**
 * Utility to query NetStat.
 */
@ThreadSafe
public final class NetStat {

    private static final Logger LOG = LoggerFactory.getLogger(NetStat.class);

    private static final int CTL_NET = 4;
    private static final int PF_ROUTE = 17;
    private static final int NET_RT_IFLIST2 = 6;
    private static final int RTM_IFINFO2 = 0x12;

    // SIOCGIFMEDIA = _IOWR('i', 56, struct ifmediareq), which is packed to 44 bytes on macOS
    private static final long SIOCGIFMEDIA = 0xc02c6938L;
    private static final int IFMEDIAREQ_SIZE = 44;
    private static final int IFM_STATUS_OFFSET = 24;
    private static final int IFNAMSIZ = 16;
    private static final int AF_INET = 2;
    private static final int SOCK_DGRAM = 2;

    private NetStat() {
    }

    /**
     * Map data for network interfaces.
     *
     * @param index If positive, limit the map to only return data for this interface index. If negative, returns data
     *              for all indices.
     *
     * @return a map of {@link IFdata} object indexed by the interface index, encapsulating the stats
     */
    public static Map<Integer, IFdata> queryIFdata(int index) {
        // Ported from source code of "netstat -ir". See
        // https://opensource.apple.com/source/network_cmds/network_cmds-457/netstat.tproj/if.c
        Map<Integer, IFdata> data = new HashMap<>();
        // Get buffer of all interface information
        int[] mib = { CTL_NET, PF_ROUTE, 0, 0, NET_RT_IFLIST2, 0 };
        try (CloseableSizeTByReference len = new CloseableSizeTByReference()) {
            if (0 != SystemB.INSTANCE.sysctl(mib, 6, null, len, null, size_t.ZERO)) {
                LOG.error("Didn't get buffer length for IFLIST2");
                return data;
            }
            try (Memory buf = new Memory(len.longValue())) {
                if (0 != SystemB.INSTANCE.sysctl(mib, 6, buf, len, null, size_t.ZERO)) {
                    LOG.error("Didn't get buffer for IFLIST2");
                    return data;
                }
                final long now = System.currentTimeMillis();

                // Iterate offset from buf's pointer up to limit of buf
                int lim = (int) (buf.size() - new IFmsgHdr().size());
                int offset = 0;
                while (offset < lim) {
                    // Get pointer to current native part of buf
                    Pointer p = buf.share(offset);
                    // Cast pointer to if_msghdr
                    IFmsgHdr ifm = new IFmsgHdr(p);
                    ifm.read();
                    // Advance next
                    offset += ifm.ifm_msglen;
                    // Skip messages which are not the right format
                    if (ifm.ifm_type == RTM_IFINFO2) {
                        // Cast pointer to if_msghdr2
                        IFmsgHdr2 if2m = new IFmsgHdr2(p);
                        if2m.read();
                        if (index < 0 || index == if2m.ifm_index) {
                            data.put((int) if2m.ifm_index, new IFdata(0xff & if2m.ifm_data.ifi_type, if2m.ifm_flags,
                                    if2m.ifm_data.ifi_opackets, if2m.ifm_data.ifi_ipackets, if2m.ifm_data.ifi_obytes,
                                    if2m.ifm_data.ifi_ibytes, if2m.ifm_data.ifi_oerrors, if2m.ifm_data.ifi_ierrors,
                                    if2m.ifm_data.ifi_collisions, if2m.ifm_data.ifi_iqdrops, if2m.ifm_data.ifi_baudrate,
                                    now));
                            if (index >= 0) {
                                return data;
                            }
                        }
                    }
                }
            }
        }
        return data;
    }

    /**
     * Queries the media status of an interface, which reports whether its link is active.
     *
     * @param ifName the interface name
     * @return the {@code ifm_status} bits of {@code SIOCGIFMEDIA}, or -1 if the interface does not report media
     */
    public static int queryMediaStatus(String ifName) {
        int fd = SystemB.INSTANCE.socket(AF_INET, SOCK_DGRAM, 0);
        if (fd < 0) {
            return -1;
        }
        try (Memory req = new Memory(IFMEDIAREQ_SIZE)) {
            req.clear();
            byte[] name = ifName.getBytes(StandardCharsets.US_ASCII);
            req.write(0, name, 0, Math.min(name.length, IFNAMSIZ - 1));
            if (SystemB.INSTANCE.ioctl(fd, new NativeLong(SIOCGIFMEDIA), req) < 0) {
                return -1;
            }
            return req.getInt(IFM_STATUS_OFFSET);
        } finally {
            SystemB.INSTANCE.close(fd);
        }
    }
}
