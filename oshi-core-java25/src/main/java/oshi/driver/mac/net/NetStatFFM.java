/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac.net;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.ValueLayout.OfLong;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.mac.MacSystemFunctions;

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

    // if_msghdr layout (just enough to read ifm_msglen and ifm_type)
    private static final long OFF_IFM_MSGLEN = 0;
    private static final long OFF_IFM_TYPE = 3; // byte at offset 3

    // if_msghdr2 layout offsets
    // short ifm_msglen (2), byte ifm_version (1), byte ifm_type (1),
    // int ifm_addrs (4), int ifm_flags (4), short ifm_index (2),
    // 2 bytes padding, int ifm_snd_len (4), int ifm_snd_maxlen (4),
    // int ifm_snd_drops (4), int ifm_timer (4)
    // then if_data64 follows
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
    private static final OfLong JAVA_LONG_UNALIGNED = ValueLayout.JAVA_LONG_UNALIGNED;

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
        try (Arena arena = Arena.ofConfined()) {
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
                        long oPackets = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_OPACKETS);
                        long iPackets = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_IPACKETS);
                        long oBytes = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_OBYTES);
                        long iBytes = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_IBYTES);
                        long oErrors = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_OERRORS);
                        long iErrors = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_IERRORS);
                        long collisions = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_COLLISIONS);
                        long iDrops = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_IQDROPS);
                        long speed = buf.get(JAVA_LONG_UNALIGNED, offset + OFF_IFI_BAUDRATE);
                        data.put(ifIndex, new IFdata(ifType, oPackets, iPackets, oBytes, iBytes, oErrors, iErrors,
                                collisions, iDrops, speed, now));
                        if (index >= 0) {
                            return data;
                        }
                    }
                }
                offset += msgLen;
            }
        } catch (Throwable t) {
            LOG.error("Error querying network interface data", t);
        }
        return data;
    }

    /**
     * Class to encapsulate IF data for method return.
     */
    @Immutable
    public static class IFdata {
        private final int ifType;
        private final long oPackets;
        private final long iPackets;
        private final long oBytes;
        private final long iBytes;
        private final long oErrors;
        private final long iErrors;
        private final long collisions;
        private final long iDrops;
        private final long speed;
        private final long timeStamp;

        IFdata(int ifType, long oPackets, long iPackets, long oBytes, long iBytes, long oErrors, long iErrors,
                long collisions, long iDrops, long speed, long timeStamp) {
            this.ifType = ifType;
            this.oPackets = oPackets & 0xffffffffL;
            this.iPackets = iPackets & 0xffffffffL;
            this.oBytes = oBytes & 0xffffffffL;
            this.iBytes = iBytes & 0xffffffffL;
            this.oErrors = oErrors & 0xffffffffL;
            this.iErrors = iErrors & 0xffffffffL;
            this.collisions = collisions & 0xffffffffL;
            this.iDrops = iDrops & 0xffffffffL;
            this.speed = speed & 0xffffffffL;
            this.timeStamp = timeStamp;
        }

        public int getIfType() {
            return ifType;
        }

        public long getOPackets() {
            return oPackets;
        }

        public long getIPackets() {
            return iPackets;
        }

        public long getOBytes() {
            return oBytes;
        }

        public long getIBytes() {
            return iBytes;
        }

        public long getOErrors() {
            return oErrors;
        }

        public long getIErrors() {
            return iErrors;
        }

        public long getCollisions() {
            return collisions;
        }

        public long getIDrops() {
            return iDrops;
        }

        public long getSpeed() {
            return speed;
        }

        public long getTimeStamp() {
            return timeStamp;
        }
    }
}
