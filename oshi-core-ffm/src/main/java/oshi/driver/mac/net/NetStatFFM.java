/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac.net;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.util.LogLevel.DEBUG;
import static oshi.util.LogLevel.ERROR;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.mac.IFdata;
import oshi.ffm.platform.mac.MacSystemFunctions;

/**
 * Utility to query NetStat using FFM.
 */
@ThreadSafe
public final class NetStatFFM {

    private static final Logger LOG = LoggerFactory.getLogger(NetStatFFM.class);

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

    // if_msghdr layout (just enough to read ifm_msglen and ifm_type)
    private static final long OFF_IFM_MSGLEN = 0;
    private static final long OFF_IFM_TYPE = 3; // byte at offset 3

    // if_msghdr2 layout offsets
    // short ifm_msglen (2), byte ifm_version (1), byte ifm_type (1),
    // int ifm_addrs (4), int ifm_flags (4), short ifm_index (2),
    // 2 bytes padding, int ifm_snd_len (4), int ifm_snd_maxlen (4),
    // int ifm_snd_drops (4), int ifm_timer (4)
    // then if_data64 follows
    private static final long OFF_IFM2_FLAGS = 8; // int at offset 8 (after msglen+version+type+addrs)
    private static final long OFF_IFM2_INDEX = 12; // short at offset 12 (after msglen+version+type+addrs+flags)

    // if_data64 starts after the if_msghdr2 header
    // Header: short(2) + byte(1) + byte(1) + int(4) + int(4) + short(2) + pad(2) + int(4) + int(4) + int(4) + int(4) =
    // 32
    private static final long OFF_IFDATA64 = 32;

    // if_data64 field offsets (relative to start of if_data64):
    // byte ifi_type(1), byte ifi_typelen(1), byte ifi_physical(1), byte ifi_addrlen(1),
    // byte ifi_hdrlen(1), byte ifi_recvquota(1), byte ifi_xmitquota(1), byte ifi_unused1(1) = 8
    // int ifi_mtu(4), int ifi_metric(4) = 8
    // long ifi_baudrate(8) = 8
    // long ifi_ipackets(8), long ifi_ierrors(8), long ifi_opackets(8), long ifi_oerrors(8)
    // long ifi_collisions(8), long ifi_ibytes(8), long ifi_obytes(8)
    // long ifi_imcasts(8), long ifi_omcasts(8), long ifi_iqdrops(8)
    // Unaligned long layout: messages in the sysctl buffer are packed at arbitrary offsets
    private static final ValueLayout.OfLong JAVA_LONG_UNALIGNED = ValueLayout.JAVA_LONG_UNALIGNED;

    private static final long OFF_IFI_TYPE = OFF_IFDATA64;
    private static final long OFF_IFI_BAUDRATE = OFF_IFDATA64 + 16;
    private static final long OFF_IFI_IPACKETS = OFF_IFI_BAUDRATE + 8;
    private static final long OFF_IFI_IERRORS = OFF_IFI_IPACKETS + 8;
    private static final long OFF_IFI_OPACKETS = OFF_IFI_IERRORS + 8;
    private static final long OFF_IFI_OERRORS = OFF_IFI_OPACKETS + 8;
    private static final long OFF_IFI_COLLISIONS = OFF_IFI_OERRORS + 8;
    private static final long OFF_IFI_IBYTES = OFF_IFI_COLLISIONS + 8;
    private static final long OFF_IFI_OBYTES = OFF_IFI_IBYTES + 8;
    private static final long OFF_IFI_IMCASTS = OFF_IFI_OBYTES + 8;
    private static final long OFF_IFI_OMCASTS = OFF_IFI_IMCASTS + 8;
    private static final long OFF_IFI_IQDROPS = OFF_IFI_OMCASTS + 8;

    private NetStatFFM() {
    }

    /**
     * Map data for network interfaces.
     *
     * @param index If positive, limit the map to only return data for this interface index. If negative, returns data
     *              for all indices.
     * @return a map of {@link IFdata} object indexed by the interface index
     */
    public static Map<Integer, IFdata> queryIFdata(int index) {
        Map<Integer, IFdata> data = new HashMap<>();
        return callInArenaOrDefault(arena -> {
            int[] mib = { CTL_NET, PF_ROUTE, 0, 0, NET_RT_IFLIST2, 0 };
            MemorySegment mibSeg = arena.allocate(ValueLayout.JAVA_INT, mib.length);
            for (int i = 0; i < mib.length; i++) {
                mibSeg.setAtIndex(ValueLayout.JAVA_INT, i, mib[i]);
            }

            // First call to get buffer size
            MemorySegment lenSeg = arena.allocate(ValueLayout.JAVA_LONG);
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);
            if (0 != MacSystemFunctions.sysctl(callState, mibSeg, mib.length, MemorySegment.NULL, lenSeg,
                    MemorySegment.NULL, 0)) {
                LOG.error("Didn't get buffer length for IFLIST2");
                return data;
            }
            long len = lenSeg.get(ValueLayout.JAVA_LONG, 0);
            if (len <= 0) {
                return data;
            }

            MemorySegment buf = arena.allocate(len);
            if (0 != MacSystemFunctions.sysctl(callState, mibSeg, mib.length, buf, lenSeg, MemorySegment.NULL, 0)) {
                LOG.error("Didn't get buffer for IFLIST2");
                return data;
            }

            final long now = System.currentTimeMillis();
            long offset = 0;
            long limit = len - 4; // minimum header size
            while (offset < limit) {
                int msgLen = Short.toUnsignedInt(buf.get(JAVA_SHORT, offset + OFF_IFM_MSGLEN));
                if (msgLen == 0) {
                    break;
                }
                byte msgType = buf.get(JAVA_BYTE, offset + OFF_IFM_TYPE);
                if (msgType == RTM_IFINFO2) {
                    int ifIndex = Short.toUnsignedInt(buf.get(JAVA_SHORT, offset + OFF_IFM2_INDEX));
                    if (index < 0 || index == ifIndex) {
                        int ifType = 0xff & buf.get(JAVA_BYTE, offset + OFF_IFI_TYPE);
                        int flags = buf.get(ValueLayout.JAVA_INT_UNALIGNED, offset + OFF_IFM2_FLAGS);
                        long oPackets = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_OPACKETS);
                        long iPackets = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_IPACKETS);
                        long oBytes = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_OBYTES);
                        long iBytes = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_IBYTES);
                        long oErrors = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_OERRORS);
                        long iErrors = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_IERRORS);
                        long collisions = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_COLLISIONS);
                        long iDrops = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_IQDROPS);
                        long speed = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_BAUDRATE);
                        data.put(ifIndex, new IFdata(ifType, flags, oPackets, iPackets, oBytes, iBytes, oErrors,
                                iErrors, collisions, iDrops, speed, now));
                        if (index >= 0) {
                            return data;
                        }
                    }
                }
                offset += msgLen;
            }
            return data;
        }, LOG, ERROR, "Error querying network interface data", data);
    }

    /**
     * Queries the media status of an interface, which reports whether its link is active.
     *
     * @param ifName the interface name
     * @return the {@code ifm_status} bits of {@code SIOCGIFMEDIA}, or -1 if the interface does not report media
     */
    public static int queryMediaStatus(String ifName) {
        return callInArenaOrDefault(arena -> {
            int fd = MacSystemFunctions.socket(AF_INET, SOCK_DGRAM, 0);
            if (fd < 0) {
                return -1;
            }
            try {
                MemorySegment req = arena.allocate(IFMEDIAREQ_SIZE);
                byte[] name = ifName.getBytes(StandardCharsets.US_ASCII);
                MemorySegment.copy(name, 0, req, JAVA_BYTE, 0, Math.min(name.length, IFNAMSIZ - 1));
                if (MacSystemFunctions.ioctl(fd, SIOCGIFMEDIA, req) < 0) {
                    return -1;
                }
                return req.get(ValueLayout.JAVA_INT, IFM_STATUS_OFFSET);
            } finally {
                MacSystemFunctions.close(fd);
            }
        }, -1, LOG, DEBUG, "Error querying media status for {}", ifName);
    }
}
