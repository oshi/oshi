/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

    public WindowsNetworkIF(NetworkInterface netint) throws InstantiationException {
        super(netint);
        updateAttributes();
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
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(new WindowsNetworkIF(ni));
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
        // MIB_IFROW2 requires Vista (6.0) or later.
        if (IS_VISTA_OR_GREATER) {
            // Create new MIB_IFROW2 and set index to this interface index
            MIB_IF_ROW2 ifRow = new MIB_IF_ROW2();
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
            this.collisions = ifRow.OutDiscards; // closest proxy
            this.inDrops = ifRow.InDiscards; // closest proxy
            this.speed = ifRow.ReceiveLinkSpeed;
            this.ifAlias = Native.toString(ifRow.Alias);
            this.ifOperStatus = IfOperStatus.byValue(ifRow.OperStatus);
        } else {
            // Create new MIB_IFROW and set index to this interface index
            MIB_IFROW ifRow = new MIB_IFROW();
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
            this.collisions = ParseUtil.unsignedIntToLong(ifRow.dwOutDiscards); // closest proxy
            this.inDrops = ParseUtil.unsignedIntToLong(ifRow.dwInDiscards); // closest proxy
            this.speed = ParseUtil.unsignedIntToLong(ifRow.dwSpeed);
            this.ifAlias = ""; // not supported by MIB_IFROW
            this.ifOperStatus = IfOperStatus.UNKNOWN; // not supported
        }
        this.timeStamp = System.currentTimeMillis();
        return true;
    }
}
