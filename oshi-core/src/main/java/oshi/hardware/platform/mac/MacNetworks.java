/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; // NOSONAR
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.mac.SystemB.IFmsgHdr;
import com.sun.jna.platform.mac.SystemB.IFmsgHdr2;
import com.sun.jna.ptr.IntByReference;

import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworks;

/**
 * @author widdis[at]gmail[dot]com
 */
public class MacNetworks extends AbstractNetworks {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MacNetworks.class);

    private static final int CTL_NET = 4;
    private static final int PF_ROUTE = 17;
    private static final int NET_RT_IFLIST2 = 6;
    private static final int RTM_IFINFO2 = 0x12;

    /**
     * Class to encapsulate IF data for method return
     */
    private static class IFdata {
        private long oPackets;
        private long iPackets;
        private long oBytes;
        private long iBytes;
        private long oErrors;
        private long iErrors;
        private long speed;
        private long timeStamp;

        IFdata(long oPackets, long iPackets, // NOSONAR squid:S00107
                long oBytes, long iBytes, long oErrors, long iErrors, long speed, long timeStamp) {
            this.oPackets = oPackets;
            this.iPackets = iPackets;
            this.oBytes = oBytes;
            this.iBytes = iBytes;
            this.oErrors = oErrors;
            this.iErrors = iErrors;
            this.speed = speed;
            this.timeStamp = timeStamp;
        }
    }

    /**
     * Map all network interfaces. Ported from source code of "netstat -ir". See
     * http://opensource.apple.com/source/network_cmds/network_cmds-457/
     * netstat.tproj/if.c
     * 
     * @param index
     *            The network interface index to query
     * @return an {@link IFData} object encapsulating the stats if the index
     *         matches, or null if there was no match or an error reading the
     *         stats
     */
    private static synchronized IFdata queryIFdata(int index) {
        // Get buffer of all interface information
        int[] mib = { CTL_NET, PF_ROUTE, 0, 0, NET_RT_IFLIST2, 0 };
        IntByReference len = new IntByReference();
        if (0 != SystemB.INSTANCE.sysctl(mib, 6, null, len, null, 0)) {
            LOG.error("Didn't get buffer length for IFLIST2");
            return null;
        }
        Pointer buf = new Memory(len.getValue());
        if (0 != SystemB.INSTANCE.sysctl(mib, 6, buf, len, null, 0)) {
            LOG.error("Didn't get buffer for IFLIST2");
            return null;
        }

        // Iterate offset from buf's pointer up to limit of buf
        int lim = len.getValue();
        int next = 0;
        while (next < lim) {
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

            if (if2m.ifm_index == index) {
                return new IFdata(if2m.ifm_data.ifi_opackets, if2m.ifm_data.ifi_ipackets, if2m.ifm_data.ifi_obytes,
                        if2m.ifm_data.ifi_ibytes, if2m.ifm_data.ifi_oerrors, if2m.ifm_data.ifi_ierrors,
                        if2m.ifm_data.ifi_baudrate, System.currentTimeMillis());
            }
        }
        // If we get here we didn't find the index
        return null;
    }

    /**
     * Updates interface network statistics on the given interface. Statistics
     * include packets and bytes sent and received, and interface speed.
     *
     * @param netIF
     *            The interface on which to update statistics
     */
    public static synchronized void updateNetworkStats(NetworkIF netIF) {
        // Update data
        IFdata ifData = queryIFdata(netIF.queryNetworkInterface().getIndex());
        if (ifData != null) {
            netIF.setBytesSent(ifData.oBytes);
            netIF.setBytesRecv(ifData.iBytes);
            netIF.setPacketsSent(ifData.oPackets);
            netIF.setPacketsRecv(ifData.iPackets);
            netIF.setOutErrors(ifData.oErrors);
            netIF.setInErrors(ifData.iErrors);
            netIF.setSpeed(ifData.speed);
            netIF.setTimeStamp(ifData.timeStamp);
        }
    }
}
