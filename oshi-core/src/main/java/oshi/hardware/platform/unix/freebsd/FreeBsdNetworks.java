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
package oshi.hardware.platform.unix.freebsd;

import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworks;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * @author widdis[at]gmail[dot]com
 */
public class FreeBsdNetworks extends AbstractNetworks {

    private static final long serialVersionUID = 1L;

    /**
     * Updates interface network statistics on the given interface. Statistics
     * include packets and bytes sent and received, and interface speed.
     *
     * @param netIF
     *            The interface on which to update statistics
     */
    public static void updateNetworkStats(NetworkIF netIF) {
        String stats = ExecutingCommand.getAnswerAt("netstat -bI " + netIF.getName(), 1);
        netIF.setTimeStamp(System.currentTimeMillis());
        String[] split = ParseUtil.whitespaces.split(stats);
        if (split.length < 12) {
            // No update
            return;
        }
        netIF.setBytesSent(ParseUtil.parseUnsignedLongOrDefault(split[10], 0L));
        netIF.setBytesRecv(ParseUtil.parseUnsignedLongOrDefault(split[7], 0L));
        netIF.setPacketsSent(ParseUtil.parseUnsignedLongOrDefault(split[8], 0L));
        netIF.setPacketsRecv(ParseUtil.parseUnsignedLongOrDefault(split[4], 0L));
        netIF.setOutErrors(ParseUtil.parseUnsignedLongOrDefault(split[9], 0L));
        netIF.setInErrors(ParseUtil.parseUnsignedLongOrDefault(split[5], 0L));
    }
}
