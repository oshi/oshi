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

import java.net.NetworkInterface;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * A network interface in the machine, including statistics.
 * <p>
 * Thread safe for the designed use of retrieving the most recent data. Users
 * should be aware that the {@link #updateAttributes()} method may update
 * attributes, including the time stamp, and should externally synchronize such
 * usage to ensure consistent calculations.
 */
@ThreadSafe
public interface NetworkIF {

    /**
     * Gets the {@link java.net.NetworkInterface} object.
     *
     * @return the network interface, an instance of
     *         {@link java.net.NetworkInterface}.
     */
    NetworkInterface queryNetworkInterface();

    /**
     * Interface name.
     *
     * @return The interface name.
     */
    String getName();

    /**
     * Interface description.
     *
     * @return The description of the network interface. On some platforms, this is
     *         identical to the name.
     */
    String getDisplayName();

    /**
     * The interface Maximum Transmission Unit (MTU).
     *
     * @return The MTU of the network interface.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date.
     */
    int getMTU();

    /**
     * The Media Access Control (MAC) address.
     *
     * @return The MAC Address.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date.
     */
    String getMacaddr();

    /**
     * The Internet Protocol (IP) v4 address.
     *
     * @return An array of IPv4 Addresses.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date.
     */
    String[] getIPv4addr();

    /**
     * The Internet Protocol (IP) v4 subnet masks.
     *
     * @return An array of IPv4 subnet mask lengths, corresponding to the IPv4
     *         addresses from {@link #getIPv4addr()}. Ranges between 0-32.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date.
     *
     */
    Short[] getSubnetMasks();

    /**
     * The Internet Protocol (IP) v6 address.
     *
     * @return An array of IPv6 Addresses.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date.
     */
    String[] getIPv6addr();

    /**
     * The Internet Protocol (IP) v6 address.
     *
     * @return The IPv6 address prefix lengths, corresponding to the IPv6 addresses
     *         from {@link #getIPv6addr()}. Ranges between 0-128.
     *         <p>
     *         This value is set when the {@link oshi.hardware.NetworkIF} is
     *         instantiated and may not be up to date.
     */
    Short[] getPrefixLengths();

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
    int getIfType();

    /**
     * (Windows Vista and higher only) The NDIS physical medium type. This member
     * can be one of the values from the {@code NDIS_PHYSICAL_MEDIUM} enumeration
     * type defined in the {@code Ntddndis.h} header file.
     *
     * @return the ndisPhysicalMediumType
     */
    int getNdisPhysicalMediumType();

    /**
     * (Windows Vista and higher) Set if a connector is present on the network
     * interface.
     * <p>
     * (Linux) Indicates the current physical link state of the interface.
     *
     * @return {@code true} if there is a physical network adapter (Windows) or a
     *         connected cable (Linux), false otherwise
     */
    boolean isConnectorPresent();

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
    long getBytesRecv();

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
    long getBytesSent();

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
    long getPacketsRecv();

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
    long getPacketsSent();

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
    long getInErrors();

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
    long getOutErrors();

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
    long getInDrops();

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
    long getCollisions();

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
    long getSpeed();

    /**
     * <p>
     * Getter for the field <code>timeStamp</code>.
     * </p>
     *
     * @return Returns the timeStamp.
     */
    long getTimeStamp();

    /**
     * Determines if the MAC address on this interface corresponds to a known
     * Virtual Machine.
     *
     * @return {@code true} if the MAC address corresponds to a known virtual
     *         machine.
     */
    boolean isKnownVmMacAddr();

    /**
     * Updates interface network statistics on this interface. Statistics include
     * packets and bytes sent and received, and interface speed.
     *
     * @return {@code true} if the update was successful, {@code false} otherwise.
     */
    boolean updateAttributes();
}
