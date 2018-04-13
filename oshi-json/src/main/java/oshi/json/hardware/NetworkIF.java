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
package oshi.json.hardware;

import java.net.NetworkInterface;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.SystemInfo;
import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.util.PropertiesUtil;

/**
 * A network interface in the machine, including statistics
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class NetworkIF extends AbstractOshiJsonObject {

    private static final long serialVersionUID = 2L;

    private static final Logger LOG = LoggerFactory.getLogger(NetworkIF.class);

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.hardware.NetworkIF networkIf;

    /**
     * Creates a new NetworkIF object.
     */
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
     *         execute the {@link #updateNetworkStats()} method
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
     *         execute the {@link #updateNetworkStats()} method
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
     *         update this value, execute the {@link #updateNetworkStats()}
     *         method
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
     *         execute the {@link #updateNetworkStats()} method
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
     * @return Input Errors. This value is set when the {@link NetworkIF} is
     *         instantiated and may not be up to date. To update this value,
     *         execute the {@link #updateNetworkStats()} method
     */
    public long getInErrors() {
        return this.networkIf.getInErrors();
    }

    /**
     * @param inErrors
     *            The Input Errors to set.
     */
    public void setInErrors(long inErrors) {
        this.networkIf.setInErrors(inErrors);
    }

    /**
     * @return The Output Errors. This value is set when the {@link NetworkIF}
     *         is instantiated and may not be up to date. To update this value,
     *         execute the {@link #updateNetworkStats()} method
     */
    public long getOutErrors() {
        return this.networkIf.getOutErrors();
    }

    /**
     * @param outErrors
     *            The Output Errors to set.
     */
    public void setOutErrors(long outErrors) {
        this.networkIf.setOutErrors(outErrors);
    }

    /**
     * @return The speed of the network interface in bits per second. This value
     *         is set when the {@link NetworkIF} is instantiated and may not be
     *         up to date. To update this value, execute the
     *         {@link #updateNetworkStats()} method
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
     * @return Returns the timeStamp.
     */
    public long getTimeStamp() {
        return this.networkIf.getTimeStamp();
    }

    /**
     * @param timeStamp
     *            The timeStamp to set.
     */
    public void setTimeStamp(long timeStamp) {
        this.networkIf.setTimeStamp(timeStamp);
    }

    /**
     * Updates interface network statistics on this interface. Statistics
     * include packets and bytes sent and received, and interface speed.
     */
    public void updateNetworkStats() {
        switch (SystemInfo.getCurrentPlatformEnum()) {
        case WINDOWS:
            oshi.hardware.platform.windows.WindowsNetworks.updateNetworkStats(this.networkIf);
            break;
        case LINUX:
            oshi.hardware.platform.linux.LinuxNetworks.updateNetworkStats(this.networkIf);
            break;
        case MACOSX:
            oshi.hardware.platform.mac.MacNetworks.updateNetworkStats(this.networkIf);
            break;
        case SOLARIS:
            oshi.hardware.platform.unix.solaris.SolarisNetworks.updateNetworkStats(this.networkIf);
            break;
        case FREEBSD:
            oshi.hardware.platform.unix.freebsd.FreeBsdNetworks.updateNetworkStats(this.networkIf);
            break;
        default:
            LOG.error("Unsupported platform. No update performed.");
            break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "hardware.networks.name")) {
            json.add("name", getName());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.networks.displayName")) {
            json.add("displayName", getDisplayName());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.networks.mac")) {
            json.add("mac", getMacaddr());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.networks.ipv4")) {
            JsonArrayBuilder ipv4ArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (String ipv4 : getIPv4addr()) {
                ipv4ArrayBuilder.add(ipv4);
            }
            json.add("ipv4", ipv4ArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.networks.ipv6")) {
            JsonArrayBuilder ipv6ArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (String ipv6 : getIPv6addr()) {
                ipv6ArrayBuilder.add(ipv6);
            }
            json.add("ipv6", ipv6ArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.networks.mtu")) {
            json.add("mtu", getMTU());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.networks.bytesRecv")) {
            json.add("bytesRecv", getBytesRecv());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.networks.bytesSent")) {
            json.add("bytesSent", getBytesSent());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.networks.packetsRecv")) {
            json.add("packetsRecv", getPacketsRecv());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.networks.packetsSent")) {
            json.add("packetsSent", getPacketsSent());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.networks.inErrors")) {
            json.add("inErrors", getInErrors());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.networks.outErrors")) {
            json.add("outErrors", getOutErrors());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.networks.speed")) {
            json.add("speed", getSpeed());
        }
        if (PropertiesUtil.getBoolean(properties, "hardware.networks.timeStamp")) {
            json.add("timeStamp", getSpeed());
        }
        return json.build();
    }
}
