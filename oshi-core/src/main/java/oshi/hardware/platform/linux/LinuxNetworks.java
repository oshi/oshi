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
package oshi.hardware.platform.linux;

import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworks;
import oshi.util.FileUtil;

/**
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class LinuxNetworks extends AbstractNetworks {

    private static final long serialVersionUID = 1L;

    /**
     * Updates interface network statistics on the given interface. Statistics
     * include packets and bytes sent and received, and interface speed.
     *
     * @param netIF
     *            The interface on which to update statistics
     */
    public static void updateNetworkStats(NetworkIF netIF) {
        String txBytesPath = String.format("/sys/class/net/%s/statistics/tx_bytes", netIF.getName());
        String rxBytesPath = String.format("/sys/class/net/%s/statistics/rx_bytes", netIF.getName());
        String txPacketsPath = String.format("/sys/class/net/%s/statistics/tx_packets", netIF.getName());
        String rxPacketsPath = String.format("/sys/class/net/%s/statistics/rx_packets", netIF.getName());
        String speed = String.format("/sys/class/net/%s/speed", netIF.getName());

        netIF.setTimeStamp(System.currentTimeMillis());
        netIF.setBytesSent(FileUtil.getLongFromFile(txBytesPath));
        netIF.setBytesRecv(FileUtil.getLongFromFile(rxBytesPath));
        netIF.setPacketsSent(FileUtil.getLongFromFile(txPacketsPath));
        netIF.setPacketsRecv(FileUtil.getLongFromFile(rxPacketsPath));
        netIF.setSpeed(FileUtil.getLongFromFile(speed));
    }
}
