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
 * enrico[dot]bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.common;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.hardware.Networks;
import oshi.hardware.stores.HWNetworkStore;
import oshi.json.NullAwareJsonObjectBuilder;

/**
 * Network interfaces implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public abstract class AbstractNetworks implements Networks {

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    /**
     * Set network parameters in store
     *
     * @param netstore Store which set network parameters
     * @param netint Network interface examined
     * @throws SocketException
     */
    protected void setNetworkParameters(HWNetworkStore netstore, NetworkInterface netint) throws SocketException {
        Enumeration<InetAddress> addresses;
        StringBuilder sb;
        byte[] mac;

        mac = netint.getHardwareAddress();
        netstore.setName(netint.getName());
        netstore.setDescription(netint.getDisplayName());

        sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
        }
        netstore.setMacaddr(sb.toString());

        netstore.setIpaddr("Unknown");
        netstore.setIpv6addr("unknown");
        addresses = netint.getInetAddresses();
        for (InetAddress address : Collections.list(addresses)) {
            if (address.getHostAddress().contains(":")) {
                netstore.setIpv6addr(address.getHostAddress().split("%")[0]);
            } else {
                netstore.setIpaddr(address.getHostAddress());
            }
        }
    }

    @Override
    public abstract HWNetworkStore[] getNetworks();

    @Override
    public JsonObject toJSON() {
        JsonArrayBuilder netArray = jsonFactory.createArrayBuilder();
        for (HWNetworkStore store : getNetworks()) {
            netArray.add(store.toJSON());
        }
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("networks", netArray).build();
    }
}
