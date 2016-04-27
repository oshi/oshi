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
package oshi.hardware.stores;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.json.OshiJsonObject;

/**
 * Store object of network interfaces attributes
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class HWNetworkStore implements OshiJsonObject {

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private String name;
    private String macaddr;
    private String ipaddr;
    private String ipv6addr;
    // TODO: add more data

    /**
     * @return the interface name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Set the interface name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the MAC Address
     */
    public String getMacaddr() {
        return macaddr;
    }

    /**
     * @param macaddr Set the MAC Address
     */
    public void setMacaddr(String macaddr) {
        this.macaddr = macaddr;
    }

    /**
     * @return The IP Address
     */
    public String getIpaddr() {
        return ipaddr;
    }

    /**
     * @param ipaddr Set The IP Address
     */
    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }

    /**
     * @return Set The IPv6 Address
     */
    public String getIpv6addr() {
        return ipv6addr;
    }

    /**
     * @param ipv6addr Set The IPv6 Address
     */
    public void setIpv6addr(String ipv6addr) {
        this.ipv6addr = ipv6addr;
    }

    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder())
                .add("name", this.getName())
                .add("mac", this.getMacaddr())
                .add("ip", this.getIpaddr())
                .add("ipv6", this.getIpv6addr())
                .build();
        
    }

}
