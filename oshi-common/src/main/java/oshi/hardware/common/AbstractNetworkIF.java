/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static oshi.util.Memoizer.memoize;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.util.Constants;
import oshi.util.ExceptionUtil;
import oshi.util.FileUtil;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;

/**
 * Network interfaces implementation.
 */
@ThreadSafe
public abstract class AbstractNetworkIF implements NetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNetworkIF.class);

    private static final String OSHI_VM_MAC_ADDR_PROPERTIES = "oshi.vmmacaddr.properties";

    private final NetworkInterface networkInterface;
    private final String name;
    private final String displayName;
    private final int index;
    private final long mtu;
    private final String mac;
    private final String[] ipv4;
    private final Short[] subnetMasks;
    private final String[] ipv6;
    private final Short[] prefixLengths;

    // Refreshed by each platform's updateNetworkStats(); protected so subclasses assign directly (see the
    // VisibilityModifier suppression for this file), matching the AbstractOSProcess model.
    protected volatile long bytesRecv;
    protected volatile long bytesSent;
    protected volatile long packetsRecv;
    protected volatile long packetsSent;
    protected volatile long inErrors;
    protected volatile long outErrors;
    protected volatile long inDrops;
    protected volatile long collisions;
    protected volatile long speed;
    protected volatile long timeStamp;

    private final Supplier<Properties> vmMacAddrProps = memoize(AbstractNetworkIF::queryVmMacAddrProps);

    /**
     * Construct a {@link NetworkIF} object backed by the specified {@link NetworkInterface}.
     *
     * @param netint The core java {@link NetworkInterface} backing this object.
     * @throws InstantiationException If a socket exception prevents access to the backing interface.
     */
    protected AbstractNetworkIF(NetworkInterface netint) throws InstantiationException {
        this(netint, netint.getDisplayName());
    }

    /**
     * Construct a {@link NetworkIF} object backed by the specified {@link NetworkInterface}.
     *
     * @param netint      The core java {@link NetworkInterface} backing this object.
     * @param displayName A string to use for the display name in preference to the {@link NetworkInterface} value.
     * @throws InstantiationException If a socket exception prevents access to the backing interface.
     */
    protected AbstractNetworkIF(NetworkInterface netint, String displayName) throws InstantiationException {
        this.networkInterface = netint;
        try {
            this.name = networkInterface.getName();
            this.displayName = displayName;
            this.index = networkInterface.getIndex();
            // Set MTU
            this.mtu = ParseUtil.unsignedIntToLong(networkInterface.getMTU());
            // Set MAC
            byte[] hwmac = networkInterface.getHardwareAddress();
            if (hwmac != null) {
                List<String> octets = new ArrayList<>(6);
                for (byte b : hwmac) {
                    octets.add(String.format(Locale.ROOT, "%02x", b));
                }
                this.mac = String.join(":", octets);
            } else {
                this.mac = Constants.UNKNOWN;
            }
            // Set IP arrays
            ArrayList<String> ipv4list = new ArrayList<>();
            ArrayList<Short> subnetMaskList = new ArrayList<>();
            ArrayList<String> ipv6list = new ArrayList<>();
            ArrayList<Short> prefixLengthList = new ArrayList<>();

            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress address = interfaceAddress.getAddress();
                if (!address.getHostAddress().isEmpty()) {
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
            throw new InstantiationException(e.getMessage());
        }
    }

    /**
     * Returns network interfaces on this machine.
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @return A list of network interfaces
     */
    protected static List<NetworkInterface> getNetworkInterfaces(boolean includeLocalInterfaces) {
        List<NetworkInterface> interfaces = getAllNetworkInterfaces();

        return includeLocalInterfaces ? interfaces
                : getAllNetworkInterfaces().stream().parallel().filter(ni -> !isLocalInterface(ni))
                        .collect(Collectors.toList());
    }

    /**
     * Creates a platform-specific {@link NetworkIF} from a {@link NetworkInterface}.
     */
    @FunctionalInterface
    protected interface NetworkIFFactory {
        /**
         * Creates the network interface.
         *
         * @param netint the underlying network interface
         * @return the created {@link NetworkIF}
         * @throws InstantiationException if the interface cannot be instantiated
         */
        NetworkIF create(NetworkInterface netint) throws InstantiationException;
    }

    /**
     * Builds the list of network interfaces, constructing each with the given platform-specific factory. Interfaces
     * that fail to instantiate are logged at debug and skipped.
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @param factory                creates the platform-specific interface instances
     * @return a list of {@link NetworkIF} objects representing the interfaces
     */
    protected static List<NetworkIF> getNetworks(boolean includeLocalInterfaces, NetworkIFFactory factory) {
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(factory.create(ni));
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.getMessage());
            }
        }
        return ifList;
    }

    /**
     * Returns all network interfaces.
     *
     * @return A list of network interfaces
     */
    private static List<NetworkInterface> getAllNetworkInterfaces() {
        return ExceptionUtil.getOrDefault(() -> {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            return interfaces == null ? Collections.emptyList() : Collections.list(interfaces);
        }, Collections.emptyList(), LOG, "Socket exception when retrieving interfaces: {}");
    }

    private static boolean isLocalInterface(NetworkInterface networkInterface) {
        return ExceptionUtil.getBooleanOrDefault(() -> networkInterface.getHardwareAddress() == null, false, LOG,
                "Socket exception when retrieving interface information: {}");
    }

    @Override
    public NetworkInterface queryNetworkInterface() {
        return this.networkInterface;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public long getMTU() {
        return this.mtu;
    }

    @Override
    public String getMacaddr() {
        return this.mac;
    }

    @Override
    public String[] getIPv4addr() {
        return Arrays.copyOf(this.ipv4, this.ipv4.length);
    }

    @Override
    public Short[] getSubnetMasks() {
        return Arrays.copyOf(this.subnetMasks, this.subnetMasks.length);
    }

    @Override
    public String[] getIPv6addr() {
        return Arrays.copyOf(this.ipv6, this.ipv6.length);
    }

    @Override
    public Short[] getPrefixLengths() {
        return Arrays.copyOf(this.prefixLengths, this.prefixLengths.length);
    }

    @Override
    public boolean isKnownVmMacAddr() {
        String oui = getMacaddr().length() > 7 ? getMacaddr().substring(0, 8) : getMacaddr();
        return this.vmMacAddrProps.get().containsKey(oui.toUpperCase(Locale.ROOT));
    }

    private static Properties queryVmMacAddrProps() {
        return FileUtil.readPropertiesFromFilename(OSHI_VM_MAC_ADDR_PROPERTIES);
    }

    @Override
    public long getBytesRecv() {
        return this.bytesRecv;
    }

    @Override
    public long getBytesSent() {
        return this.bytesSent;
    }

    @Override
    public long getPacketsRecv() {
        return this.packetsRecv;
    }

    @Override
    public long getPacketsSent() {
        return this.packetsSent;
    }

    @Override
    public long getInErrors() {
        return this.inErrors;
    }

    @Override
    public long getOutErrors() {
        return this.outErrors;
    }

    @Override
    public long getInDrops() {
        return this.inDrops;
    }

    @Override
    public long getCollisions() {
        return this.collisions;
    }

    @Override
    public long getSpeed() {
        return this.speed;
    }

    @Override
    public long getTimeStamp() {
        return this.timeStamp;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(getName());
        if (!getName().equals(getDisplayName())) {
            sb.append(" (").append(getDisplayName()).append(")");
        }
        if (!getIfAlias().isEmpty()) {
            sb.append(" [IfAlias=").append(getIfAlias()).append("]");
        }
        sb.append("\n");
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
