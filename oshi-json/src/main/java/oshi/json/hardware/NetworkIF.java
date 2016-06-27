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

import java.net.NetworkInterface;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.hardware.Networks;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;

/**
 * A network interface in the machine, including statistics
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class NetworkIF extends AbstractOshiJsonObject {

    private static final long serialVersionUID = 1L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.NetworkIF networkIf;

    public NetworkIF() {
        this.networkIf = new oshi.hardware.NetworkIF();
    }

    /**
     * @return the network interface
     */
    public NetworkInterface getNetworkInterface() {
        return this.networkIf.getNetworkInterface();
    }

    /**
     * Sets the network interface and calculates other information derived from
     * it
     * 
     * @param networkInterface
     *            The network interface to set
     */
    public void setNetworkInterface(NetworkInterface networkInterface) {
        this.networkIf.setNetworkInterface(networkInterface);
    }

    /**
     * @return The interface name.
     */
    public String getName() {
        return this.networkIf.getName();
    }

    /**
     * @return The description of the network interface. On some platforms, this
     *         is identical to the name.
     */
    public String getDisplayName() {
        return this.networkIf.getDisplayName();
    }

    /**
     * @return The MTU of the network interface. This value is set when the
     *         {@link NetworkIF} is instantiated and may not be up to date. To
     *         update this value, execute the
     *         {@link #setNetworkInterface(NetworkInterface)} method
     */
    public int getMTU() {
        return this.networkIf.getMTU();
    }

    /**
     * @return The MAC Address. This value is set when the {@link NetworkIF} is
     *         instantiated and may not be up to date. To update this value,
     *         execute the {@link #setNetworkInterface(NetworkInterface)} method
     */
    public String getMacaddr() {
        return this.networkIf.getMacaddr();
    }

    /**
     * @return The IPv4 Addresses. This value is set when the {@link NetworkIF}
     *         is instantiated and may not be up to date. To update this value,
     *         execute the {@link #setNetworkInterface(NetworkInterface)} method
     */
    public String[] getIPv4addr() {
        return this.networkIf.getIPv4addr();
    }

    /**
     * @return The IPv6 Addresses. This value is set when the {@link NetworkIF}
     *         is instantiated and may not be up to date. To update this value,
     *         execute the {@link #setNetworkInterface(NetworkInterface)} method
     */
    public String[] getIPv6addr() {
        return this.networkIf.getIPv6addr();
    }

    /**
     * @return The Bytes Received. This value is set when the {@link NetworkIF}
     *         is instantiated and may not be up to date. To update this value,
     *         execute the {@link Networks#updateNetworkStats(NetworkIF)} method
     */
    public long getBytesRecv() {
        return this.networkIf.getBytesRecv();
    }

    /**
     * @param bytesRecv
     *            Set Bytes Received
     */
    public void setBytesRecv(long bytesRecv) {
        this.networkIf.setBytesRecv(bytesRecv);
    }

    /**
     * @return The Bytes Sent. This value is set when the {@link NetworkIF} is
     *         instantiated and may not be up to date. To update this value,
     *         execute the {@link Networks#updateNetworkStats(NetworkIF)} method
     */
    public long getBytesSent() {
        return this.networkIf.getBytesSent();
    }

    /**
     * @param bytesSent
     *            Set the Bytes Sent
     */
    public void setBytesSent(long bytesSent) {
        this.networkIf.setBytesSent(bytesSent);
    }

    /**
     * @return The Packets Received. This value is set when the
     *         {@link NetworkIF} is instantiated and may not be up to date. To
     *         update this value, execute the
     *         {@link Networks#updateNetworkStats(NetworkIF)} method
     */
    public long getPacketsRecv() {
        return this.networkIf.getPacketsRecv();
    }

    /**
     * @param packetsRecv
     *            Set The Packets Received
     */
    public void setPacketsRecv(long packetsRecv) {
        this.networkIf.setPacketsRecv(packetsRecv);
    }

    /**
     * @return The Packets Sent. This value is set when the {@link NetworkIF} is
     *         instantiated and may not be up to date. To update this value,
     *         execute the {@link Networks#updateNetworkStats(NetworkIF)} method
     */
    public long getPacketsSent() {
        return this.networkIf.getPacketsSent();
    }

    /**
     * @param packetsSent
     *            Set The Packets Sent
     */
    public void setPacketsSent(long packetsSent) {
        this.networkIf.setPacketsSent(packetsSent);
    }

    /**
     * @return The speed of the network interface in bits per second. This value
     *         is set when the {@link NetworkIF} is instantiated and may not be
     *         up to date. To update this value, execute the
     *         {@link Networks#updateNetworkStats(NetworkIF)} method
     */
    public long getSpeed() {
        return this.networkIf.getSpeed();
    }

    /**
     * @param speed
     *            Set the speed of the network interface
     */
    public void setSpeed(long speed) {
        this.networkIf.setSpeed(speed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
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
