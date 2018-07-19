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
package oshi.hardware.common;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.NetworkIF;
import oshi.hardware.Networks;

/**
 * Network interfaces implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public abstract class AbstractNetworks implements Networks {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNetworks.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkIF[] getNetworks() {
        List<NetworkIF> result = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces;

        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            LOG.error("Socket exception when retrieving interfaces: {}", ex);
            return new NetworkIF[0];
        }

        for (NetworkInterface netint : Collections.list(interfaces)) {
            try {
                if (!netint.isLoopback() && netint.getHardwareAddress() != null) {
                    NetworkIF netIF = new NetworkIF();
                    netIF.setNetworkInterface(netint);
                    netIF.updateNetworkStats();
                    result.add(netIF);
                }
            } catch (SocketException ex) {
                LOG.error("Socket exception when retrieving interface \"{}\": {}", netint.getName(), ex);
            }
        }

        return result.toArray(new NetworkIF[result.size()]);
    }
}
