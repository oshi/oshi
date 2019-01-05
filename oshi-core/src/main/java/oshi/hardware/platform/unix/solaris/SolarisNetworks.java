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
package oshi.hardware.platform.unix.solaris;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; // NOSONAR

import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworks;
import oshi.util.platform.unix.solaris.KstatUtil;

/**
 * @author widdis[at]gmail[dot]com
 */
public class SolarisNetworks extends AbstractNetworks {

    private static final long serialVersionUID = 1L;

    /**
     * Updates interface network statistics on the given interface. Statistics
     * include packets and bytes sent and received, and interface speed.
     *
     * @param netIF
     *            The interface on which to update statistics
     */
    public static void updateNetworkStats(NetworkIF netIF) {
        Kstat ksp = KstatUtil.kstatLookup("link", -1, netIF.getName());
        if (ksp == null) { // Solaris 10 compatibility
            ksp = KstatUtil.kstatLookup(null, -1, netIF.getName());
        }
        if (ksp != null && KstatUtil.kstatRead(ksp)) {
            netIF.setBytesSent(KstatUtil.kstatDataLookupLong(ksp, "obytes64"));
            netIF.setBytesRecv(KstatUtil.kstatDataLookupLong(ksp, "rbytes64"));
            netIF.setPacketsSent(KstatUtil.kstatDataLookupLong(ksp, "opackets64"));
            netIF.setPacketsRecv(KstatUtil.kstatDataLookupLong(ksp, "ipackets64"));
            netIF.setOutErrors(KstatUtil.kstatDataLookupLong(ksp, "oerrors"));
            netIF.setInErrors(KstatUtil.kstatDataLookupLong(ksp, "ierrors"));
            netIF.setSpeed(KstatUtil.kstatDataLookupLong(ksp, "ifspeed"));
            // Snap time in ns; convert to ms
            netIF.setTimeStamp(ksp.ks_snaptime / 1_000_000L);
        }
    }
}
