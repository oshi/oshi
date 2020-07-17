/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.hardware.platform.mac;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; // NOSONAR
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.mac.SystemB.IFmsgHdr;
import com.sun.jna.platform.mac.SystemB.IFmsgHdr2;
import com.sun.jna.ptr.IntByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.Networks;

/**
 * MacNetworks class.
 */
@ThreadSafe
public final class MacNetworks extends Networks {

    private static final Logger LOG = LoggerFactory.getLogger(MacNetworks.class);

    private static final int CTL_NET = 4;
    private static final int PF_ROUTE = 17;
    private static final int NET_RT_IFLIST2 = 6;
    private static final int RTM_IFINFO2 = 0x12;

    @Override
    public NetworkIF[] getNetworks() {
        List<NetworkIF> result = new ArrayList<>();
        Map<Integer, IFdata> ifDataMap = queryIFdata(-1);

        for (NetworkInterface netint : getNetworkInterfaces()) {
            NetworkIF netIF = new NetworkIF();
            netIF.setNetworkInterface(netint);
            updateNetworkStats(netIF, ifDataMap);
            result.add(netIF);
        }

        return result.toArray(new NetworkIF[0]);
    }

    /**
     * Map all network interfaces. Ported from source code of "netstat -ir". See
     * http://opensource.apple.com/source/network_cmds/network_cmds-457/
     * netstat.tproj/if.c
     *
     * @param index
     *            If nonnegative, returns results for only that index
     * @return a map of {@link IFData} object indexed by the interface index,
     *         encapsulating the stats, or {@code null} if the query failed.
     */
    public static Map<Integer, IFdata> queryIFdata(int index) {
        // Ported from source code of "netstat -ir". See
        // https://opensource.apple.com/source/network_cmds/network_cmds-457/netstat.tproj/if.c
        Map<Integer, IFdata> data = new HashMap<>();
        // Get buffer of all interface information
        int[] mib = { CTL_NET, PF_ROUTE, 0, 0, NET_RT_IFLIST2, 0 };
        IntByReference len = new IntByReference();
        if (0 != SystemB.INSTANCE.sysctl(mib, 6, null, len, null, 0)) {
            LOG.error("Didn't get buffer length for IFLIST2");
            return data;
        }
        Memory buf = new Memory(len.getValue());
        if (0 != SystemB.INSTANCE.sysctl(mib, 6, buf, len, null, 0)) {
            LOG.error("Didn't get buffer for IFLIST2");
            return data;
        }

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
                            new IFdata(if2m.ifm_data.ifi_type, if2m.ifm_data.ifi_opackets, if2m.ifm_data.ifi_ipackets,
                                    if2m.ifm_data.ifi_obytes, if2m.ifm_data.ifi_ibytes, if2m.ifm_data.ifi_oerrors,
                                    if2m.ifm_data.ifi_ierrors, if2m.ifm_data.ifi_collisions, if2m.ifm_data.ifi_iqdrops,
                                    if2m.ifm_data.ifi_baudrate, System.currentTimeMillis()));
                }
            }
        }
        return data;
    }

    /**
     * Updates interface network statistics on the given interface. Statistics
     * include packets and bytes sent and received, and interface speed.
     *
     * @param netIF
     *            The interface on which to update statistics
     * @return {@code true} if the update was successful, {@code false} otherwise.
     */
    public static boolean updateNetworkStats(NetworkIF netIF) {
        int index = netIF.queryNetworkInterface().getIndex();
        return updateNetworkStats(netIF, queryIFdata(index));
    }

    /**
     * Updates interface network statistics on the given interface. Statistics
     * include packets and bytes sent and received, and interface speed.
     *
     * @param netIF
     *            The interface on which to update statistics
     * @param data
     *            A map of network interface statistics with the index as the key
     * @return {@code true} if the update was successful, {@code false} otherwise.
     */
    private static boolean updateNetworkStats(NetworkIF netIF, Map<Integer, IFdata> data) {
        int index = netIF.queryNetworkInterface().getIndex();
        if (data.containsKey(index)) {
            IFdata ifData = data.get(index);
            // Update data
            netIF.setIfType(ifData.ifType);
            netIF.setBytesSent(ifData.oBytes);
            netIF.setBytesRecv(ifData.iBytes);
            netIF.setPacketsSent(ifData.oPackets);
            netIF.setPacketsRecv(ifData.iPackets);
            netIF.setOutErrors(ifData.oErrors);
            netIF.setInErrors(ifData.iErrors);
            netIF.setCollisions(ifData.collisions);
            netIF.setInDrops(ifData.iDrops);
            netIF.setSpeed(ifData.speed);
            netIF.setTimeStamp(ifData.timeStamp);
            return true;
        }
        return false;
    }

    /**
     * Class to encapsulate IF data for method return
     */
    private static class IFdata {
        private int ifType;
        private long oPackets;
        private long iPackets;
        private long oBytes;
        private long iBytes;
        private long oErrors;
        private long iErrors;
        private long collisions;
        private long iDrops;
        private long speed;
        private long timeStamp;

        IFdata(int ifType, // NOSONAR squid:S00107
                long oPackets, long iPackets, long oBytes, long iBytes, long oErrors, long iErrors, long collisions,
                long iDrops, long speed, long timeStamp) {
            this.ifType = ifType;
            this.oPackets = oPackets;
            this.iPackets = iPackets;
            this.oBytes = oBytes;
            this.iBytes = iBytes;
            this.oErrors = oErrors;
            this.iErrors = iErrors;
            this.collisions = collisions;
            this.iDrops = iDrops;
            this.speed = speed;
            this.timeStamp = timeStamp;
        }
    }
}
