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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.common;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.NetworkIF;
import oshi.hardware.Networks;
import oshi.json.NullAwareJsonObjectBuilder;

/**
 * Network interfaces implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public abstract class AbstractNetworks implements Networks {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNetworks.class);

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkIF[] getNetworks() {
        List<NetworkIF> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(interfaces)) {
                if (!netint.isLoopback()) {
                    NetworkIF netIF = new NetworkIF();
                    netIF.setNetworkInterface(netint);
                    this.updateNetworkStats(netIF);
                    result.add(netIF);
                }
            }
        } catch (SocketException ex) {
            LOG.error("Socket exception when retrieving network interfaces: " + ex.getMessage());
        }
        return result.toArray(new NetworkIF[result.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON() {
        JsonArrayBuilder netArray = jsonFactory.createArrayBuilder();
        for (NetworkIF store : getNetworks()) {
            netArray.add(store.toJSON());
        }
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("networks", netArray).build();
    }
}
