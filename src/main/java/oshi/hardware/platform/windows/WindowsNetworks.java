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
package oshi.hardware.platform.windows;

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

/**
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class WindowsNetworks extends AbstractNetworks {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsNetworks.class);

    private void setNetworkStats(HWNetworkStore netstore) {
        // TODO: set network stats on Windows
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
