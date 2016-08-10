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
package oshi.hardware;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.SystemInfo;
import oshi.hardware.platform.linux.LinuxNetworks;
import oshi.hardware.platform.mac.MacNetworks;
import oshi.hardware.platform.unix.freebsd.FreeBsdNetworks;
import oshi.hardware.platform.unix.solaris.SolarisNetworks;
import oshi.hardware.platform.windows.WindowsNetworks;

/**
 * A network interface in the machine, including statistics
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class NetworkIF implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(NetworkIF.class);

    private transient NetworkInterface networkInterface;
    private int mtu;
    private String mac;
    private String[] ipv4;
    private String[] ipv6;
    private long bytesRecv;
    private long bytesSent;
    private long packetsRecv;
    private long packetsSent;
    private long speed;
    private long timeStamp;

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
            byte[] hwmac = networkInterface.getHardwareAddress();
            if (hwmac != null) {
                StringJoiner sj = new StringJoiner(":");
                for (byte b : hwmac) {
                    sj.add(String.format("%02x", b));
                }
                this.mac = sj.toString();
            } else {
                this.mac = "Unknown";
            }
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
        return this.networkInterface.getName();
    }

    /**
     * @return The description of the network interface. On some platforms, this
     *         is identical to the name.
     */
    public String getDisplayName() {
        return this.networkInterface.getDisplayName();
    }

    /**
     * @return The MTU of the network interface. This value is set when the
     *         {@link NetworkIF} is instantiated and may not be up to date. To
     *         update this value, execute the
     *         {@link #setNetworkInterface(NetworkInterface)} method
     */
    public int getMTU() {
        return this.mtu;
    }

    /**
     * @return The MAC Address. This value is set when the {@link NetworkIF} is
     *         instantiated and may not be up to date. To update this value,
     *         execute the {@link #setNetworkInterface(NetworkInterface)} method
     */
    public String getMacaddr() {
        return this.mac;
    }

    /**
     * @return The IPv4 Addresses. This value is set when the {@link NetworkIF}
     *         is instantiated and may not be up to date. To update this value,
     *         execute the {@link #setNetworkInterface(NetworkInterface)} method
     */
    public String[] getIPv4addr() {
        return Arrays.copyOf(this.ipv4, this.ipv4.length);
    }

    /**
     * @return The IPv6 Addresses. This value is set when the {@link NetworkIF}
     *         is instantiated and may not be up to date. To update this value,
     *         execute the {@link #setNetworkInterface(NetworkInterface)} method
     */
    public String[] getIPv6addr() {
        return Arrays.copyOf(this.ipv6, this.ipv6.length);
    }

    /**
     * @return The Bytes Received. This value is set when the {@link NetworkIF}
     *         is instantiated and may not be up to date. To update this value,
     *         execute the {@link #updateNetworkStats()} method
     */
    public long getBytesRecv() {
        return this.bytesRecv;
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
     *         execute the {@link #updateNetworkStats()} method
     */
    public long getBytesSent() {
        return this.bytesSent;
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
     *         update this value, execute the {@link #updateNetworkStats()}
     *         method
     */
    public long getPacketsRecv() {
        return this.packetsRecv;
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
     *         execute the {@link #updateNetworkStats()} method
     */
    public long getPacketsSent() {
        return this.packetsSent;
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
     *         {@link #updateNetworkStats()} method
     */
    public long getSpeed() {
        return this.speed;
    }

    /**
     * @param speed
     *            Set the speed of the network interface
     */
    public void setSpeed(long speed) {
        this.speed = speed;
    }

    /**
     * @return Returns the timeStamp.
     */
    public long getTimeStamp() {
        return this.timeStamp;
    }

    /**
     * @param timeStamp
     *            The timeStamp to set.
     */
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * Updates interface network statistics on this interface. Statistics
     * include packets and bytes sent and received, and interface speed.
     */
    public void updateNetworkStats() {
        switch (SystemInfo.getCurrentPlatformEnum()) {
        case WINDOWS:
            WindowsNetworks.updateNetworkStats(this);
            break;
        case LINUX:
            LinuxNetworks.updateNetworkStats(this);
            break;
        case MACOSX:
            MacNetworks.updateNetworkStats(this);
            break;
        case SOLARIS:
            SolarisNetworks.updateNetworkStats(this);
            break;
        case FREEBSD:
            FreeBsdNetworks.updateNetworkStats(this);
            break;
        default:
            LOG.error("Unsupported platform. No update performed.");
            break;
        }
    }
}
