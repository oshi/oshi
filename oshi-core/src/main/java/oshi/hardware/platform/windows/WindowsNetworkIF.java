/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.hardware.platform.windows;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.IPHlpAPI; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.IPHlpAPI.MIB_IFROW;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_IF_ROW2;
import com.sun.jna.platform.win32.VersionHelpers;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.util.ParseUtil;

/**
 * WindowsNetworks class.
 */
@ThreadSafe
public final class WindowsNetworkIF extends AbstractNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsNetworkIF.class);

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();
    private static final byte CONNECTOR_PRESENT_BIT = 0b00000100;
    private static final int MIB_IF_TYPE_LOOPBACK = 24;

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
    private String ifAlias;
    private IfOperStatus ifOperStatus;

    public WindowsNetworkIF(NetworkInterface netint, MIB_IF_ROW2 ifRow) throws InstantiationException {
        super(netint);
        updateAttributes(ifRow);
    }

    /**
     * Gets all network interfaces on this machine
     *
     * @param includeLocalInterfaces
     *            include local interfaces in the result
     * @return A list of {@link NetworkIF} objects representing the interfaces
     */
    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(true)) {
            try {
                MIB_IF_ROW2 ifRow = queryIfEntry(ni.getIndex());
                if (includeLocalInterfaces || ifRow != null && ifRow.Type != MIB_IF_TYPE_LOOPBACK) {
                    ifList.add(new WindowsNetworkIF(ni, ifRow));
                }
            } catch (InstantiationException e) {
                LOG.debug("Network Interface Instantiation failed: {}", e.getMessage());
            }
        }
        return ifList;
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
    public String getIfAlias() {
        return ifAlias;
    }

    @Override
    public IfOperStatus getIfOperStatus() {
        return ifOperStatus;
    }

    @Override
    public boolean updateAttributes() {
        MIB_IF_ROW2 ifRow = queryIfEntry(queryNetworkInterface().getIndex());
        return updateAttributes(ifRow);
    }

    private boolean updateAttributes(MIB_IF_ROW2 ifRow) {
        if (ifRow == null) {
            LOG.error("Failed to retrieve data for interface {}, {}", queryNetworkInterface().getIndex(), getName());
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
        this.collisions = ifRow.OutDiscards; // closest proxy
        this.inDrops = ifRow.InDiscards; // closest proxy
        this.speed = ifRow.ReceiveLinkSpeed;
        this.ifAlias = Native.toString(ifRow.Alias);
        this.ifOperStatus = IfOperStatus.byValue(ifRow.OperStatus);
        this.timeStamp = System.currentTimeMillis();
        return true;
    }

    private static MIB_IF_ROW2 queryIfEntry(int index) {
        // Create new MIB_IFROW2 and set index to this interface index
        MIB_IF_ROW2 ifRow = new MIB_IF_ROW2();
        if (IS_VISTA_OR_GREATER) {
            // Set index to this interface index
            ifRow.InterfaceIndex = index;
            if (0 == IPHlpAPI.INSTANCE.GetIfEntry2(ifRow)) {
                return ifRow;
            }
        } else {
            // MIB_IFROW2 requires Vista (6.0) or later. Use MIB_IFROW and copy over
            MIB_IFROW ifRowXP = new MIB_IFROW();
            // Set index to this interface index
            ifRowXP.dwIndex = index;
            if (0 == IPHlpAPI.INSTANCE.GetIfEntry(ifRowXP)) {
                ifRow.Type = ifRowXP.dwType;
                ifRow.OutOctets = ParseUtil.unsignedIntToLong(ifRowXP.dwOutOctets);
                ifRow.InOctets = ParseUtil.unsignedIntToLong(ifRowXP.dwInOctets);
                ifRow.OutUcastPkts = ParseUtil.unsignedIntToLong(ifRowXP.dwOutUcastPkts);
                ifRow.InUcastPkts = ParseUtil.unsignedIntToLong(ifRowXP.dwInUcastPkts);
                ifRow.OutErrors = ParseUtil.unsignedIntToLong(ifRowXP.dwOutErrors);
                ifRow.InErrors = ParseUtil.unsignedIntToLong(ifRowXP.dwInErrors);
                ifRow.OutDiscards = ParseUtil.unsignedIntToLong(ifRowXP.dwOutDiscards); // closest proxy
                ifRow.InDiscards = ParseUtil.unsignedIntToLong(ifRowXP.dwInDiscards); // closest proxy
                ifRow.ReceiveLinkSpeed = ParseUtil.unsignedIntToLong(ifRowXP.dwSpeed);
                ifRow.Alias = new char[0];
                switch (ifRowXP.dwOperStatus) {
                case 4: // IF_OPER_STATUS_CONNECTED
                case 5: // IF_OPER_STATUS_OPERATIONAL
                    ifRow.OperStatus = 1; // Up
                    ifRow.InterfaceAndOperStatusFlags = CONNECTOR_PRESENT_BIT;
                    break;
                case 0: // IF_OPER_STATUS_NON_OPERATIONAL
                case 3: // IF_OPER_STATUS_CONNECTING
                    ifRow.InterfaceAndOperStatusFlags = CONNECTOR_PRESENT_BIT;
                    ifRow.OperStatus = 2; // Down
                    break;
                case 1: // IF_OPER_STATUS_UNREACHABLE
                case 2: // IF_OPER_STATUS_DISCONNECTED
                    ifRow.OperStatus = 2; // Down
                    break;
                default:
                    ifRow.OperStatus = 4; // Unknown
                }
                // PhysicalMediumType = 0, Unspecified
            }
        }
        return null;
    }
}
