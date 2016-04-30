/*
 * Copyright (c) 2016 com.github.dblock.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * enrico[dot]bianchi[at]gmail[dot]com
 *    com.github.dblock - initial API and implementation and/or initial documentation
 */
package oshi.hardware.platform.linux;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.hardware.common.AbstractNetworks;
import oshi.hardware.stores.HWNetworkStore;
import oshi.util.FileUtil;

/**
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class LinuxNetworks extends AbstractNetworks {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxNetworks.class);

    private long readFile(String file) {
        List<String> read;

        try {
            read = FileUtil.readFile(file);
            if (!read.isEmpty()) {
                return Long.parseLong(read.get(0));
            } else {
                return 0L;
            }
        } catch (NumberFormatException | IOException ex) {
            LOG.error("Error when retrieving network statistics for interface ");
            LOG.debug("Error message: " + ex.getMessage());
            return 0L;
        }
    }

    private void setNetworkStats(HWNetworkStore netstore) {
        String txBytesPath, rxBytesPath, txPacketsPath, rxPacketsPath;

        txBytesPath = String.format("/sys/class/net/%s/statistics/tx_bytes", netstore.getName());
        rxBytesPath = String.format("/sys/class/net/%s/statistics/rx_bytes", netstore.getName());
        txPacketsPath = String.format("/sys/class/net/%s/statistics/tx_packets", netstore.getName());
        rxPacketsPath = String.format("/sys/class/net/%s/statistics/rx_packets", netstore.getName());

        netstore.setBytesSent(this.readFile(txBytesPath));
        netstore.setBytesRecv(this.readFile(rxBytesPath));
        netstore.setPacketsSent(this.readFile(txPacketsPath));
        netstore.setPacketsRecv(this.readFile(rxPacketsPath));
    }

    @Override
    public HWNetworkStore[] getNetworks() {
        Enumeration<NetworkInterface> interfaces;
        HWNetworkStore netstore;
        List<HWNetworkStore> result;

        result = new ArrayList<>();

        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(interfaces)) {
                if (!netint.getDisplayName().equals("lo")) {
                    netstore = new HWNetworkStore();
                    this.setNetworkParameters(netstore, netint);
                    this.setNetworkStats(netstore);
                    result.add(netstore);
                }
            }
        } catch (SocketException ex) {
            LOG.debug("Socket exception when retrieving network interfaces: " + ex.getMessage());
        }

        return result.toArray(new HWNetworkStore[result.size()]);
    }
}
