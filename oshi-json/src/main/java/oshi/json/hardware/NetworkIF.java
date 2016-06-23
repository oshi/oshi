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
package oshi.json.hardware;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.json.OshiJsonObject;

/**
 * A network interface in the machine, including statistics
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class NetworkIF extends oshi.hardware.NetworkIF implements OshiJsonObject {

    private static final long serialVersionUID = 1L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON() {
        JsonArrayBuilder ipv4ArrayBuilder = jsonFactory.createArrayBuilder();
        for (String ipv4 : getIPv4addr()) {
            ipv4ArrayBuilder.add(ipv4);
        }
        JsonArrayBuilder ipv6ArrayBuilder = jsonFactory.createArrayBuilder();
        for (String ipv4 : getIPv6addr()) {
            ipv6ArrayBuilder.add(ipv4);
        }
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("name", this.getName())
                .add("displayName", this.getDisplayName()).add("mac", this.getMacaddr())
                .add("ipv4", ipv4ArrayBuilder.build()).add("ipv6", ipv6ArrayBuilder.build()).add("mtu", this.getMTU())
                .add("bytesRecv", this.getBytesRecv()).add("bytesSent", this.getBytesSent())
                .add("packetsRecv", this.getPacketsRecv()).add("packetsSent", this.getPacketsSent())
                .add("speed", this.getSpeed()).build();
    }
}
