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
package oshi.hardware.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.IPHlpAPI; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.IPHlpAPI.MIB_IFROW;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_IF_ROW2;

import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworks;
import oshi.jna.platform.windows.VersionHelpers;
import oshi.util.ParseUtil;

/**
 * @author widdis[at]gmail[dot]com
 */
public class WindowsNetworks extends AbstractNetworks {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsNetworks.class);

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

    /**
     * Updates interface network statistics on the given interface. Statistics
     * include packets and bytes sent and received, and interface speed.
     *
     * @param netIF
     *            The interface on which to update statistics
     */
    public static void updateNetworkStats(NetworkIF netIF) {
        // MIB_IFROW2 requires Vista (6.0) or later.
        if (IS_VISTA_OR_GREATER) {
            // Create new MIB_IFROW2 and set index to this interface index
            MIB_IF_ROW2 ifRow = new MIB_IF_ROW2();
            ifRow.InterfaceIndex = netIF.getNetworkInterface().getIndex();
            if (0 != IPHlpAPI.INSTANCE.GetIfEntry2(ifRow)) {
                // Error, abort
                LOG.error("Failed to retrieve data for interface {}, {}", netIF.getNetworkInterface().getIndex(),
                        netIF.getName());
                return;
            }
            // These are unsigned longs. netIF setter will mask sign bit.
            netIF.setBytesSent(ifRow.OutOctets);
            netIF.setBytesRecv(ifRow.InOctets);
            netIF.setPacketsSent(ifRow.OutUcastPkts);
            netIF.setPacketsRecv(ifRow.InUcastPkts);
            netIF.setOutErrors(ifRow.OutErrors);
            netIF.setInErrors(ifRow.InErrors);
            netIF.setSpeed(ifRow.ReceiveLinkSpeed);
        } else {
            // Create new MIB_IFROW and set index to this interface index
            MIB_IFROW ifRow = new MIB_IFROW();
            ifRow.dwIndex = netIF.getNetworkInterface().getIndex();
            if (0 != IPHlpAPI.INSTANCE.GetIfEntry(ifRow)) {
                // Error, abort
                LOG.error("Failed to retrieve data for interface {}, {}", netIF.getNetworkInterface().getIndex(),
                        netIF.getName());
                return;
            }
            // These are unsigned ints. Widen them to longs.
            netIF.setBytesSent(ParseUtil.unsignedIntToLong(ifRow.dwOutOctets));
            netIF.setBytesRecv(ParseUtil.unsignedIntToLong(ifRow.dwInOctets));
            netIF.setPacketsSent(ParseUtil.unsignedIntToLong(ifRow.dwOutUcastPkts));
            netIF.setPacketsRecv(ParseUtil.unsignedIntToLong(ifRow.dwInUcastPkts));
            netIF.setOutErrors(ParseUtil.unsignedIntToLong(ifRow.dwOutErrors));
            netIF.setInErrors(ParseUtil.unsignedIntToLong(ifRow.dwInErrors));
            netIF.setSpeed(ParseUtil.unsignedIntToLong(ifRow.dwSpeed));
        }
        netIF.setTimeStamp(System.currentTimeMillis());
    }
}
