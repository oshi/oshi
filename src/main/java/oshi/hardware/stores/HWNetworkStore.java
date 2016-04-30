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
    private String description;
    private String macaddr;
    private String ipaddr;
    private String ipv6addr;
    private long bytesRecv;
    private long bytesSent;
    private long packetsRecv;
    private long packetsSent;
    private int mtu;
    private long speed;

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

    /**
     * @return The Bytes Received
     */
    public long getBytesRecv() {
        return bytesRecv;
    }

    /**
     * @param bytesRecv Set Bytes Received
     */
    public void setBytesRecv(long bytesRecv) {
        this.bytesRecv = bytesRecv;
    }

    /**
     * @return The Bytes Sent
     */
    public long getBytesSent() {
        return bytesSent;
    }

    /**
     * @param bytesSent Set the Bytes Sent
     */
    public void setBytesSent(long bytesSent) {
        this.bytesSent = bytesSent;
    }

    /**
     * @return The Packets Received
     */
    public long getPacketsRecv() {
        return packetsRecv;
    }

    /**
     * @param packetsRecv Set The Packets Received
     */
    public void setPacketsRecv(long packetsRecv) {
        this.packetsRecv = packetsRecv;
    }

    /**
     * @return The Packets Sent
     */
    public long getPacketsSent() {
        return packetsSent;
    }

    /**
     * @param packetsSent Set The Packets Sent
     */
    public void setPacketsSent(long packetsSent) {
        this.packetsSent = packetsSent;
    }

    /**
     * @return the jsonFactory
     */
    public JsonBuilderFactory getJsonFactory() {
        return jsonFactory;
    }

    /**
     * @return The description of the network interface
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description Set the description of the network interface
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return The MTU of the network interface
     */
    public int getMTU() {
        return mtu;
    }

    /**
     * @param mtu Set the MTU of the network interface
     */
    public void setMTU(int mtu) {
        this.mtu = mtu;
    }

    /**
     * @return The speed of the network interface
     */
    public long getSpeed() {
        return speed;
    }

    /**
     * @param speed Set the speed of the network interface
     */
    public void setSpeed(long speed) {
        this.speed = speed;
    }

    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder())
                .add("name", this.getName())
                .add("mac", this.getMacaddr())
                .add("ip", this.getIpaddr())
                .add("ipv6", this.getIpv6addr())
                .add("bytesrecv", this.getBytesRecv())
                .add("bytessent", this.getBytesSent())
                .add("packetsrecv", this.getPacketsRecv())
                .add("packetssent", this.getPacketsSent())
                .build();
    }
}
