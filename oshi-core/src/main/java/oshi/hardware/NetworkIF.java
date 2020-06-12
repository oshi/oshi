/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware;

import static oshi.util.Memoizer.memoize;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.SystemInfo;
import oshi.annotation.concurrent.NotThreadSafe;
import oshi.hardware.platform.linux.LinuxNetworks;
import oshi.hardware.platform.mac.MacNetworks;
import oshi.hardware.platform.unix.freebsd.FreeBsdNetworks;
import oshi.hardware.platform.unix.solaris.SolarisNetworks;
import oshi.hardware.platform.windows.WindowsNetworks;
import oshi.util.FileUtil;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;

/**
 * A network interface in the machine, including statistics
 * <p>
 * Thread safe if both threads only use getters, or if setter usage is
 * externally synchronized.
 */
@NotThreadSafe
public class NetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkIF.class);

    private NetworkInterface networkInterface;
    private int mtu;
    private String mac;
    private String[] ipv4;
    private Short[] subnetMasks;
    private String[] ipv6;
    private Short[] prefixLengths;
    private int ifType;
    private int ndisPhysicalMediumType;
    private boolean connectorPresent;
    private long bytesRecv;
    private long bytesSent;
    private long packetsRecv;
    private long packetsSent;
    private long inErrors;
    private long outErrors;
    private long inDrops;
    private long collisions;
    private long speed;
    private long timeStamp;

    private static final String OSHI_VM_MAC_ADDR_PROPERTIES = "oshi.vmmacaddr.properties";

    private final Supplier<Properties> vmMacAddrProps = memoize(NetworkIF::queryVmMacAddrProps);

    /**
     * Gets the core java {@link NetworkInterface} object.
     *
     * @return the network interface, an instance of
     *         {@link java.net.NetworkInterface}.
     */
    public NetworkInterface queryNetworkInterface() {
        return this.networkInterface;
    }

    /**
     * Sets the network interface and calculates other information derived from it
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
                List<String> octets = new ArrayList<>(6);
                for (byte b : hwmac) {
                    octets.add(String.format("%02x", b));
                }
                this.mac = String.join(":", octets);
            } else {
                this.mac = "Unknown";
            }
            // Set IP arrays
            ArrayList<String> ipv4list = new ArrayList<>();
            ArrayList<Short> subnetMaskList = new ArrayList<>();
            ArrayList<String> ipv6list = new ArrayList<>();
            ArrayList<Short> prefixLengthList = new ArrayList<>();

            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress address = interfaceAddress.getAddress();
                if (address.getHostAddress().length() > 0) {
                    if (address.getHostAddress().contains(":")) {
                        ipv6list.add(address.getHostAddress().split("%")[0]);
                        prefixLengthList.add(interfaceAddress.getNetworkPrefixLength());
                    } else {
                        ipv4list.add(address.getHostAddress());
                        subnetMaskList.add(interfaceAddress.getNetworkPrefixLength());
                    }
                }
            }

            this.ipv4 = ipv4list.toArray(new String[0]);
            this.subnetMasks = subnetMaskList.toArray(new Short[0]);
            this.ipv6 = ipv6list.toArray(new String[0]);
            this.prefixLengths = prefixLengthList.toArray(new Short[0]);
        } catch (SocketException e) {
            LOG.error("Socket exception: {}", e.getMessage());
        }
    }

    /**
     * <p>
     * Interface name.
     * </p>
     *
     * @return The interface name.
     */
    public String getName() {
        return this.networkInterface.getName();
    }

    /**
     * <p>
     * Interface description.
     * </p>
     *
     * @return The description of the network interface. On some platforms, this is
     *         identical to the name.
     */
    public String getDisplayName() {
        return this.networkInterface.getDisplayName();
    }

    /**
     * <p>
     * The interface Maximum Transmission Unit (MTU).
     * </p>
     *
     * @return The MTU of the network interface.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #setNetworkInterface(NetworkInterface)} method
     */
    public int getMTU() {
        return this.mtu;
    }

    /**
     * <p>
     * The Media Access Control (MAC) address.
     * </p>
     *
     * @return The MAC Address.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #setNetworkInterface(NetworkInterface)} method
     */
    public String getMacaddr() {
        return this.mac;
    }

    /**
     * <p>
     * The Internet Protocol (IP) v4 address.
     * </p>
     *
     * @return The IPv4 Addresses.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #setNetworkInterface(NetworkInterface)} method
     */
    public String[] getIPv4addr() {
        return Arrays.copyOf(this.ipv4, this.ipv4.length);
    }

    /**
     * <p>
     * The Internet Protocol (IP) v4 subnet masks.
     * </p>
     *
     * @return The IPv4 subnet mask length. Ranges between 0-32.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #setNetworkInterface(NetworkInterface)} method.
     *
     */
    public Short[] getSubnetMasks() {
        return Arrays.copyOf(this.subnetMasks, this.subnetMasks.length);
    }

    /**
     * <p>
     * The Internet Protocol (IP) v6 address.
     * </p>
     *
     * @return The IPv6 Addresses.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #setNetworkInterface(NetworkInterface)} method
     */
    public String[] getIPv6addr() {
        return Arrays.copyOf(this.ipv6, this.ipv6.length);
    }

    /**
     * <p>
     * The Internet Protocol (IP) v6 address.
     * </p>
     *
     * @return The IPv6 address prefix lengths. Ranges between 0-128.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #setNetworkInterface(NetworkInterface)} method
     */
    public Short[] getPrefixLengths() {
        return Arrays.copyOf(this.prefixLengths, this.prefixLengths.length);
    }

    /**
     * (Windows, macOS) The NDIS Interface Type. NDIS interface types are registered
     * with the Internet Assigned Numbers Authority (IANA), which publishes a list
     * of interface types periodically in the Assigned Numbers RFC, or in a
     * derivative of it that is specific to Internet network management number
     * assignments.
     * <p>
     * (Linux) ARP Protocol hardware identifiers defined in
     * {@code include/uapi/linux/if_arp.h}
     *
     * @return the ifType
     */
    public int getIfType() {
        return ifType;
    }

    /**
     * <p>
     * Setter for the field <code>ifType</code>.
     * </p>
     *
     * @param ifType
     *            the ifType to set
     */
    public void setIfType(int ifType) {
        this.ifType = ifType;
    }

    /**
     * <p>
     * (Windows Vista and higher only) The NDIS physical medium type. This member
     * can be one of the values from the {@code NDIS_PHYSICAL_MEDIUM} enumeration
     * type defined in the {@code Ntddndis.h} header file.
     * </p>
     *
     * @return the ndisPhysicalMediumType
     */
    public int getNdisPhysicalMediumType() {
        return ndisPhysicalMediumType;
    }

    /**
     * <p>
     * Setter for the field <code>ndisPhysicalMediumType</code>.
     * </p>
     *
     * @param ndisPhysicalMediumType
     *            the ndisPhysicalMediumType to set
     */
    public void setNdisPhysicalMediumType(int ndisPhysicalMediumType) {
        this.ndisPhysicalMediumType = ndisPhysicalMediumType;
    }

    /**
     * (Windows Vista and higher) Set if a connector is present on the network
     * interface.
     * <p>
     * (Linux) Indicates the current physical link state of the interface.
     *
     * @return {@code true} if there is a physical network adapter (Windows) or a
     *         connected cable (Linux), false otherwise
     */
    public boolean isConnectorPresent() {
        return connectorPresent;
    }

    /**
     * <p>
     * Setter for the field <code>connectorPresent</code>.
     * </p>
     *
     * @param connectorPresent
     *            the connectorPresent to set
     */
    public void setConnectorPresent(boolean connectorPresent) {
        this.connectorPresent = connectorPresent;
    }

    /**
     * <p>
     * Getter for the field <code>bytesRecv</code>.
     * </p>
     *
     * @return The Bytes Received.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #updateAttributes()} method
     */
    public long getBytesRecv() {
        return this.bytesRecv;
    }

    /**
     * <p>
     * Setter for the field <code>bytesRecv</code>.
     * </p>
     *
     * @param bytesRecv
     *            Set Bytes Received
     */
    public void setBytesRecv(long bytesRecv) {
        this.bytesRecv = ParseUtil.unsignedLongToSignedLong(bytesRecv);
    }

    /**
     * <p>
     * Getter for the field <code>bytesSent</code>.
     * </p>
     *
     * @return The Bytes Sent.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #updateAttributes()} method
     */
    public long getBytesSent() {
        return this.bytesSent;
    }

    /**
     * <p>
     * Setter for the field <code>bytesSent</code>.
     * </p>
     *
     * @param bytesSent
     *            Set the Bytes Sent
     */
    public void setBytesSent(long bytesSent) {
        this.bytesSent = ParseUtil.unsignedLongToSignedLong(bytesSent);
    }

    /**
     * <p>
     * Getter for the field <code>packetsRecv</code>.
     * </p>
     *
     * @return The Packets Received.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #updateAttributes()} method
     */
    public long getPacketsRecv() {
        return this.packetsRecv;
    }

    /**
     * <p>
     * Setter for the field <code>packetsRecv</code>.
     * </p>
     *
     * @param packetsRecv
     *            Set The Packets Received
     */
    public void setPacketsRecv(long packetsRecv) {
        this.packetsRecv = ParseUtil.unsignedLongToSignedLong(packetsRecv);
    }

    /**
     * <p>
     * Getter for the field <code>packetsSent</code>.
     * </p>
     *
     * @return The Packets Sent.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #updateAttributes()} method
     */
    public long getPacketsSent() {
        return this.packetsSent;
    }

    /**
     * <p>
     * Setter for the field <code>packetsSent</code>.
     * </p>
     *
     * @param packetsSent
     *            Set The Packets Sent
     */
    public void setPacketsSent(long packetsSent) {
        this.packetsSent = ParseUtil.unsignedLongToSignedLong(packetsSent);
    }

    /**
     * <p>
     * Getter for the field <code>inErrors</code>.
     * </p>
     *
     * @return Input Errors.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #updateAttributes()} method
     */
    public long getInErrors() {
        return this.inErrors;
    }

    /**
     * <p>
     * Setter for the field <code>inErrors</code>.
     * </p>
     *
     * @param inErrors
     *            The Input Errors to set.
     */
    public void setInErrors(long inErrors) {
        this.inErrors = ParseUtil.unsignedLongToSignedLong(inErrors);
    }

    /**
     * <p>
     * Getter for the field <code>outErrors</code>.
     * </p>
     *
     * @return The Output Errors.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #updateAttributes()} method
     */
    public long getOutErrors() {
        return this.outErrors;
    }

    /**
     * <p>
     * Setter for the field <code>outErrors</code>.
     * </p>
     *
     * @param outErrors
     *            The Output Errors to set.
     */
    public void setOutErrors(long outErrors) {
        this.outErrors = ParseUtil.unsignedLongToSignedLong(outErrors);
    }

    /**
     * <p>
     * Getter for the field <code>inDrops</code>.
     * </p>
     *
     * @return Incoming/Received dropped packets. On Windows, returns discarded
     *         incoming packets.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #updateAttributes()} method
     */
    public long getInDrops() {
        return inDrops;
    }

    /**
     * <p>
     * Setter for the field <code>inDrops</code>.
     * </p>
     *
     * @param inDrops
     *            The incoming (receive) dropped packets to set.
     */
    public void setInDrops(long inDrops) {
        this.inDrops = inDrops;
    }

    /**
     * <p>
     * Getter for the field <code>collisions</code>.
     * </p>
     *
     * @return Packet collisions. On Windows, returns discarded outgoing packets.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #updateAttributes()} method
     */
    public long getCollisions() {
        return collisions;
    }

    /**
     * <p>
     * Setter for the field <code>collisions</code>.
     * </p>
     *
     * @param collisions
     *            The collisions to set.
     */
    public void setCollisions(long collisions) {
        this.collisions = collisions;
    }

    /**
     * <p>
     * Getter for the field <code>speed</code>.
     * </p>
     *
     * @return The speed of the network interface in bits per second.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date. To update this value, execute
     *         the {@link #updateAttributes()} method
     */
    public long getSpeed() {
        return this.speed;
    }

    /**
     * <p>
     * Setter for the field <code>speed</code>.
     * </p>
     *
     * @param speed
     *            Set the speed of the network interface
     */
    public void setSpeed(long speed) {
        this.speed = ParseUtil.unsignedLongToSignedLong(speed);
    }

    /**
     * <p>
     * Getter for the field <code>timeStamp</code>.
     * </p>
     *
     * @return Returns the timeStamp.
     */
    public long getTimeStamp() {
        return this.timeStamp;
    }

    /**
     * <p>
     * Setter for the field <code>timeStamp</code>.
     * </p>
     *
     * @param timeStamp
     *            The timeStamp to set.
     */
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * Updates interface network statistics on this interface. Statistics include
     * packets and bytes sent and received, and interface speed.
     *
     * @return {@code true} if the update was successful, {@code false} otherwise.
     */
    public boolean updateAttributes() {
        switch (SystemInfo.getCurrentPlatformEnum()) {
        case WINDOWS:
            return WindowsNetworks.updateNetworkStats(this);
        case LINUX:
            return LinuxNetworks.updateNetworkStats(this);
        case MACOSX:
            return MacNetworks.updateNetworkStats(this);
        case SOLARIS:
            return SolarisNetworks.updateNetworkStats(this);
        case FREEBSD:
            return FreeBsdNetworks.updateNetworkStats(this);
        default:
            LOG.error("Unsupported platform. No update performed.");
            return false;
        }
    }

    /**
     * Determines if the MAC address on this interface corresponds to a known
     * Virtual Machine.
     *
     * @return {@code true} if the MAC address corresponds to a known virtual
     *         machine.
     */
    public boolean isKnownVmMacAddr() {
        String oui = getMacaddr().length() > 7 ? getMacaddr().substring(0, 8) : getMacaddr();
        return this.vmMacAddrProps.get().containsKey(oui.toUpperCase());
    }

    private static Properties queryVmMacAddrProps() {
        return FileUtil.readPropertiesFromFilename(OSHI_VM_MAC_ADDR_PROPERTIES);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(getName()).append(" ").append("(").append(getDisplayName()).append(")").append("\n");
        sb.append("  MAC Address: ").append(getMacaddr()).append("\n");
        sb.append("  MTU: ").append(getMTU()).append(", ").append("Speed: ").append(getSpeed()).append("\n");
        String[] ipv4withmask = getIPv4addr();
        if (this.ipv4.length == this.subnetMasks.length) {
            for (int i = 0; i < this.subnetMasks.length; i++) {
                ipv4withmask[i] += "/" + this.subnetMasks[i];
            }
        }
        sb.append("  IPv4: ").append(Arrays.toString(ipv4withmask)).append("\n");
        String[] ipv6withprefixlength = getIPv6addr();
        if (this.ipv6.length == this.prefixLengths.length) {
            for (int j = 0; j < this.prefixLengths.length; j++) {
                ipv6withprefixlength[j] += "/" + this.prefixLengths[j];
            }
        }
        sb.append("  IPv6: ").append(Arrays.toString(ipv6withprefixlength)).append("\n");
        sb.append("  Traffic: received ").append(getPacketsRecv()).append(" packets/")
                .append(FormatUtil.formatBytes(getBytesRecv())).append(" (" + getInErrors() + " err, ")
                .append(getInDrops() + " drop);");
        sb.append(" transmitted ").append(getPacketsSent()).append(" packets/")
                .append(FormatUtil.formatBytes(getBytesSent())).append(" (" + getOutErrors() + " err, ")
                .append(getCollisions() + " coll);");
        return sb.toString();
    }
}
