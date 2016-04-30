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
package oshi.hardware;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.json.NullAwareJsonObjectBuilder;
import oshi.json.OshiJsonObject;

/**
 * Store object of network interfaces attributes
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class NetworkIF implements OshiJsonObject {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkIF.class);

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private NetworkInterface networkInterface;
    private int mtu;
    private String mac;
    private String[] ipv4;
    private String[] ipv6;
    private long bytesRecv;
    private long bytesSent;
    private long packetsRecv;
    private long packetsSent;
    private long speed;

    /**
     * @return the network interface
     */
    public NetworkInterface getNetworkInterface() {
        return this.networkInterface;
    }

    /**
     * Sets the network interface and calculates other information derived from
     * it
     * 
     * @param networkInterface
     *            The network interface to set
     */
    public void setNetworkInterface(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
        try {
            // Set MTU
            this.mtu = networkInterface.getMTU();
            // Set MAC
            StringBuilder sb = new StringBuilder(18);
            byte[] mac = networkInterface.getHardwareAddress();
            if (mac != null) {
                for (byte b : mac) {
                    if (sb.length() > 0)
                        sb.append(':');
                    sb.append(String.format("%02x", b));
                }
            } else {
                this.mac = "Unknown";
            }
            this.mac = sb.toString();
            // Set IP arrays
            ArrayList<String> ipv4list = new ArrayList<>();
            ArrayList<String> ipv6list = new ArrayList<>();
            for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                if (address.getHostAddress().length() == 0) {
                    continue;
                } else if (address.getHostAddress().contains(":")) {
                    ipv6list.add(address.getHostAddress().split("%")[0]);
                } else {
                    ipv4list.add(address.getHostAddress());
                }
            }
            this.ipv4 = ipv4list.toArray(new String[ipv4list.size()]);
            this.ipv6 = ipv6list.toArray(new String[ipv6list.size()]);
        } catch (SocketException e) {
            LOG.error("Socket exception: {}", e);
            return;
        }
    }

    /**
     * @return The interface name.
     */
    public String getName() {
        return networkInterface.getName();
    }

    /**
     * @return The description of the network interface. On some platforms, this
     *         is identical to the name.
     */
    public String getDisplayName() {
        return networkInterface.getDisplayName();
    }

    /**
     * @return The MTU of the network interface. This value is set when the
     *         {@link NetworkIF} is instantiated and may not be up to date. To
     *         update this value, execute the
     *         {@link #setNetworkInterface(NetworkInterface)} method
     */
    public int getMTU() {
        return mtu;
    }

    /**
     * @return The MAC Address. This value is set when the {@link NetworkIF} is
     *         instantiated and may not be up to date. To update this value,
     *         execute the {@link #setNetworkInterface(NetworkInterface)} method
     */
    public String getMacaddr() {
        return mac;
    }

    /**
     * @return The IPv4 Addresses. This value is set when the {@link NetworkIF}
     *         is instantiated and may not be up to date. To update this value,
     *         execute the {@link #setNetworkInterface(NetworkInterface)} method
     */
    public String[] getIPv4addr() {
        return Arrays.copyOf(this.ipv4, ipv4.length);
    }

    /**
     * @return The IPv6 Addresses. This value is set when the {@link NetworkIF}
     *         is instantiated and may not be up to date. To update this value,
     *         execute the {@link #setNetworkInterface(NetworkInterface)} method
     */
    public String[] getIPv6addr() {
        return Arrays.copyOf(this.ipv6, ipv6.length);
    }

    /**
     * @return The Bytes Received. This value is set when the {@link NetworkIF}
     *         is instantiated and may not be up to date. To update this value,
     *         execute the {@link Networks#updateNetworkStats(NetworkIF)} method
     */
    public long getBytesRecv() {
        return bytesRecv;
    }

    /**
     * @param bytesRecv
     *            Set Bytes Received
     */
    public void setBytesRecv(long bytesRecv) {
        this.bytesRecv = bytesRecv;
    }

    /**
     * @return The Bytes Sent. This value is set when the {@link NetworkIF} is
     *         instantiated and may not be up to date. To update this value,
     *         execute the {@link Networks#updateNetworkStats(NetworkIF)} method
     */
    public long getBytesSent() {
        return bytesSent;
    }

    /**
     * @param bytesSent
     *            Set the Bytes Sent
     */
    public void setBytesSent(long bytesSent) {
        this.bytesSent = bytesSent;
    }

    /**
     * @return The Packets Received. This value is set when the
     *         {@link NetworkIF} is instantiated and may not be up to date. To
     *         update this value, execute the
     *         {@link Networks#updateNetworkStats(NetworkIF)} method
     */
    public long getPacketsRecv() {
        return packetsRecv;
    }

    /**
     * @param packetsRecv
     *            Set The Packets Received
     */
    public void setPacketsRecv(long packetsRecv) {
        this.packetsRecv = packetsRecv;
    }

    /**
     * @return The Packets Sent. This value is set when the {@link NetworkIF} is
     *         instantiated and may not be up to date. To update this value,
     *         execute the {@link Networks#updateNetworkStats(NetworkIF)} method
     */
    public long getPacketsSent() {
        return packetsSent;
    }

    /**
     * @param packetsSent
     *            Set The Packets Sent
     */
    public void setPacketsSent(long packetsSent) {
        this.packetsSent = packetsSent;
    }

    /**
     * @return The speed of the network interface in bits per second. This value
     *         is set when the {@link NetworkIF} is instantiated and may not be
     *         up to date. To update this value, execute the
     *         {@link Networks#updateNetworkStats(NetworkIF)} method
     */
    public long getSpeed() {
        return speed;
    }

    /**
     * @param speed
     *            Set the speed of the network interface
     */
    public void setSpeed(long speed) {
        this.speed = speed;
    }

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
