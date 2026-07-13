/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.net.NetworkInterface;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.IPHlpAPI;
import com.sun.jna.platform.win32.VersionHelpers;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.jna.Struct.CloseableMibIfRow;
import oshi.jna.Struct.CloseableMibIfRow2;
import oshi.util.ParseUtil;

/**
 * WindowsNetworks class.
 */
@ThreadSafe
public final class WindowsNetworkIfJNA extends AbstractNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsNetworkIfJNA.class);

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();
    private static final byte CONNECTOR_PRESENT_BIT = 0b00000100;

    private int ifType;
    private int ndisPhysicalMediumType;
    private boolean connectorPresent;
    private String ifAlias;
    private IfOperStatus ifOperStatus;

    public WindowsNetworkIfJNA(NetworkInterface netint) throws InstantiationException {
        super(netint);
        updateAttributes();
    }

    /**
     * Gets all network interfaces on this machine
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @return A list of {@link NetworkIF} objects representing the interfaces
     */
    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        return getNetworks(includeLocalInterfaces, WindowsNetworkIfJNA::new);
    }

    @Override
    public int getIfType() {
        return this.ifType;
    }

    @Override
    public int getNdisPhysicalMediumType() {
        return this.ndisPhysicalMediumType;
    }

    @Override
    public boolean isConnectorPresent() {
        return this.connectorPresent;
    }

    @Override
    public String getIfAlias() {
        return ifAlias;
    }

    @Override
    public IfOperStatus getIfOperStatus() {
        return ifOperStatus;
    }

    @Override
    public boolean updateAttributes() {
        // MIB_IFROW2 requires Vista (6.0) or later.
        if (IS_VISTA_OR_GREATER) {
            // Create new MIB_IFROW2 and set index to this interface index
            try (CloseableMibIfRow2 ifRow = new CloseableMibIfRow2()) {
                ifRow.InterfaceIndex = queryNetworkInterface().getIndex();
                if (0 != IPHlpAPI.INSTANCE.GetIfEntry2(ifRow)) {
                    // Error, abort
                    LOG.error("Failed to retrieve data for interface {}, {}", queryNetworkInterface().getIndex(),
                            getName());
                    return false;
                }
                this.ifType = ifRow.Type;
                this.ndisPhysicalMediumType = ifRow.PhysicalMediumType;
                this.connectorPresent = (ifRow.InterfaceAndOperStatusFlags & CONNECTOR_PRESENT_BIT) > 0;
                this.bytesSent = ifRow.OutOctets;
                this.bytesRecv = ifRow.InOctets;
                this.packetsSent = ifRow.OutUcastPkts;
                this.packetsRecv = ifRow.InUcastPkts;
                this.outErrors = ifRow.OutErrors;
                this.inErrors = ifRow.InErrors;
                this.collisions = ifRow.OutDiscards;
                this.inDrops = ifRow.InDiscards;
                this.speed = ifRow.ReceiveLinkSpeed;
                this.ifAlias = Native.toString(ifRow.Alias);
                this.ifOperStatus = IfOperStatus.byValue(ifRow.OperStatus);
            }
        } else {
            // Create new MIB_IFROW and set index to this interface index
            try (CloseableMibIfRow ifRow = new CloseableMibIfRow()) {
                ifRow.dwIndex = queryNetworkInterface().getIndex();
                if (0 != IPHlpAPI.INSTANCE.GetIfEntry(ifRow)) {
                    // Error, abort
                    LOG.error("Failed to retrieve data for interface {}, {}", queryNetworkInterface().getIndex(),
                            getName());
                    return false;
                }
                this.ifType = ifRow.dwType;
                // These are unsigned ints. Widen them to longs.
                this.bytesSent = ParseUtil.unsignedIntToLong(ifRow.dwOutOctets);
                this.bytesRecv = ParseUtil.unsignedIntToLong(ifRow.dwInOctets);
                this.packetsSent = ParseUtil.unsignedIntToLong(ifRow.dwOutUcastPkts);
                this.packetsRecv = ParseUtil.unsignedIntToLong(ifRow.dwInUcastPkts);
                this.outErrors = ParseUtil.unsignedIntToLong(ifRow.dwOutErrors);
                this.inErrors = ParseUtil.unsignedIntToLong(ifRow.dwInErrors);
                this.collisions = ParseUtil.unsignedIntToLong(ifRow.dwOutDiscards);
                this.inDrops = ParseUtil.unsignedIntToLong(ifRow.dwInDiscards);
                this.speed = ParseUtil.unsignedIntToLong(ifRow.dwSpeed);
                this.ifAlias = ""; // not supported by MIB_IFROW
                this.ifOperStatus = IfOperStatus.UNKNOWN; // not supported
            }
        }
        this.timeStamp = System.currentTimeMillis();
        return true;
    }
}
