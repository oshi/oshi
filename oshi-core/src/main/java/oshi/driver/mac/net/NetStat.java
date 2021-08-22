/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.driver.mac.net;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; // NOSONAR squid:s1191
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.mac.SystemB.IFmsgHdr;
import com.sun.jna.platform.mac.SystemB.IFmsgHdr2;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;

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

    private NetStat() {
    }

    /**
     * Map data for network interfaces.
     *
     * @param index
     *            If positive, limit the map to only return data for this interface
     *            index. If negative, returns data for all indices.
     *
     * @return a map of {@link IFdata} object indexed by the interface index,
     *         encapsulating the stats
     */
    public static Map<Integer, IFdata> queryIFdata(int index) {
        // Ported from source code of "netstat -ir". See
        // https://opensource.apple.com/source/network_cmds/network_cmds-457/netstat.tproj/if.c
        Map<Integer, IFdata> data = new HashMap<>();
        // Get buffer of all interface information
        int[] mib = { CTL_NET, PF_ROUTE, 0, 0, NET_RT_IFLIST2, 0 };
        size_t.ByReference len = new size_t.ByReference();
        if (0 != SystemB.INSTANCE.sysctl(mib, 6, null, len, null, size_t.ZERO)) {
            LOG.error("Didn't get buffer length for IFLIST2");
            return data;
        }
        Memory buf = new Memory(len.longValue());
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
                    data.put((int) if2m.ifm_index,
                            new IFdata(0xff & if2m.ifm_data.ifi_type, if2m.ifm_data.ifi_opackets,
                                    if2m.ifm_data.ifi_ipackets, if2m.ifm_data.ifi_obytes, if2m.ifm_data.ifi_ibytes,
                                    if2m.ifm_data.ifi_oerrors, if2m.ifm_data.ifi_ierrors, if2m.ifm_data.ifi_collisions,
                                    if2m.ifm_data.ifi_iqdrops, if2m.ifm_data.ifi_baudrate, now));
                    if (index >= 0) {
                        return data;
                    }
                }
            }
        }
        return data;
    }

    /**
     * Class to encapsulate IF data for method return
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

        IFdata(int ifType, // NOSONAR squid:S00107
                long oPackets, long iPackets, long oBytes, long iBytes, long oErrors, long iErrors, long collisions,
                long iDrops, long speed, long timeStamp) {
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

        /**
         * @return the ifType
         */
        public int getIfType() {
            return ifType;
        }

        /**
         * @return the oPackets
         */
        public long getOPackets() {
            return oPackets;
        }

        /**
         * @return the iPackets
         */
        public long getIPackets() {
            return iPackets;
        }

        /**
         * @return the oBytes
         */
        public long getOBytes() {
            return oBytes;
        }

        /**
         * @return the iBytes
         */
        public long getIBytes() {
            return iBytes;
        }

        /**
         * @return the oErrors
         */
        public long getOErrors() {
            return oErrors;
        }

        /**
         * @return the iErrors
         */
        public long getIErrors() {
            return iErrors;
        }

        /**
         * @return the collisions
         */
        public long getCollisions() {
            return collisions;
        }

        /**
         * @return the iDrops
         */
        public long getIDrops() {
            return iDrops;
        }

        /**
         * @return the speed
         */
        public long getSpeed() {
            return speed;
        }

        /**
         * @return the timeStamp
         */
        public long getTimeStamp() {
            return timeStamp;
        }
    }
}
