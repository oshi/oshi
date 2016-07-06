/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.unix.solaris;

import java.util.ArrayList;

import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworks;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

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
        ArrayList<String> stats = ExecutingCommand.runNative("kstat -p link::" + netIF.getName());
        if (stats != null) {
            for (String stat : stats) {
                String[] split = stat.split("\\s+");
                if (split[0].endsWith(":obytes") || split[0].endsWith(":obytes64")) {
                    netIF.setBytesSent(ParseUtil.parseLongOrDefault(split[1], 0L));
                } else if (split[0].endsWith(":rbytes") || split[0].endsWith(":rbytes64")) {
                    netIF.setBytesRecv(ParseUtil.parseLongOrDefault(split[1], 0L));
                } else if (split[0].endsWith(":opackets") || split[0].endsWith(":opackets64")) {
                    netIF.setPacketsSent(ParseUtil.parseLongOrDefault(split[1], 0L));
                } else if (split[0].endsWith(":ipackets") || split[0].endsWith(":ipackets64")) {
                    netIF.setPacketsRecv(ParseUtil.parseLongOrDefault(split[1], 0L));
                } else if (split[0].endsWith(":ifspeed")) {
                    netIF.setSpeed(ParseUtil.parseLongOrDefault(split[1], 0L));
                }
            }
        }
    }
}
