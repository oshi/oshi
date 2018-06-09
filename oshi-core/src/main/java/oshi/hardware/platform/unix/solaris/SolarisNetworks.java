/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
 * https://github.com/oshi/oshi/graphs/contributors
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
        if (ksp != null && KstatUtil.kstatRead(ksp)) {
            netIF.setBytesSent(KstatUtil.kstatDataLookupLong(ksp, "obytes64"));
            netIF.setBytesRecv(KstatUtil.kstatDataLookupLong(ksp, "rbytes64"));
            netIF.setPacketsSent(KstatUtil.kstatDataLookupLong(ksp, "opackets64"));
            netIF.setPacketsRecv(KstatUtil.kstatDataLookupLong(ksp, "ipackets64"));
            netIF.setOutErrors(KstatUtil.kstatDataLookupLong(ksp, "oerrors"));
            netIF.setInErrors(KstatUtil.kstatDataLookupLong(ksp, "ierrors"));
            netIF.setSpeed(KstatUtil.kstatDataLookupLong(ksp, "ifspeed"));
            // Snap time in ns; convert to ms
            netIF.setTimeStamp(ksp.ks_snaptime / 1000000L);
        }
    }
}
