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
import oshi.hardware.common.platform.windows.WindowsNetworkIF;
import oshi.jna.Struct.CloseableMibIfRow;
import oshi.jna.Struct.CloseableMibIfRow2;
import oshi.util.ParseUtil;

/**
 * WindowsNetworks class.
 * <p>
 * Not {@code final} so that tests can subclass it to force the {@link #isVistaOrGreater()} version gate and exercise
 * the pre-Vista {@code MIB_IFROW} query path against the real system.
 */
@ThreadSafe
public class WindowsNetworkIfJNA extends WindowsNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsNetworkIfJNA.class);

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

    public WindowsNetworkIfJNA(NetworkInterface netint) throws InstantiationException {
        super(netint);
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

    /**
     * Whether {@code MIB_IFROW2} (Vista+) is available. Overridable so tests can force the pre-Vista {@code MIB_IFROW}
     * path.
     *
     * @return {@code true} on Windows Vista or later
     */
    protected boolean isVistaOrGreater() {
        return IS_VISTA_OR_GREATER;
    }

    @Override
    protected IfStats queryStats() {
        return isVistaOrGreater() ? queryStatsVista() : queryStatsLegacy();
    }

    private IfStats queryStatsVista() {
        // MIB_IFROW2 requires Vista (6.0) or later.
        try (CloseableMibIfRow2 ifRow = new CloseableMibIfRow2()) {
            ifRow.InterfaceIndex = queryNetworkInterface().getIndex();
            if (0 != IPHlpAPI.INSTANCE.GetIfEntry2(ifRow)) {
                LOG.error("Failed to retrieve data for interface {}, {}", queryNetworkInterface().getIndex(),
                        getName());
                return null;
            }
            IfStats stats = new IfStats();
            stats.ifType = ifRow.Type;
            stats.ndisPhysicalMediumType = ifRow.PhysicalMediumType;
            stats.connectorPresent = (ifRow.InterfaceAndOperStatusFlags & CONNECTOR_PRESENT_BIT) > 0;
            stats.bytesSent = ifRow.OutOctets;
            stats.bytesRecv = ifRow.InOctets;
            stats.packetsSent = ifRow.OutUcastPkts;
            stats.packetsRecv = ifRow.InUcastPkts;
            stats.outErrors = ifRow.OutErrors;
            stats.inErrors = ifRow.InErrors;
            stats.collisions = ifRow.OutDiscards;
            stats.inDrops = ifRow.InDiscards;
            stats.speed = ifRow.ReceiveLinkSpeed;
            stats.ifAlias = Native.toString(ifRow.Alias);
            stats.ifOperStatus = IfOperStatus.byValue(ifRow.OperStatus);
            return stats;
        }
    }

    private IfStats queryStatsLegacy() {
        // Pre-Vista MIB_IFROW: a narrower field set with unsigned 32-bit counters widened to long. Physical medium
        // type, connector-present, alias, and oper-status are not available and keep their IfStats defaults.
        try (CloseableMibIfRow ifRow = new CloseableMibIfRow()) {
            ifRow.dwIndex = queryNetworkInterface().getIndex();
            if (0 != IPHlpAPI.INSTANCE.GetIfEntry(ifRow)) {
                LOG.error("Failed to retrieve data for interface {}, {}", queryNetworkInterface().getIndex(),
                        getName());
                return null;
            }
            IfStats stats = new IfStats();
            stats.ifType = ifRow.dwType;
            stats.bytesSent = ParseUtil.unsignedIntToLong(ifRow.dwOutOctets);
            stats.bytesRecv = ParseUtil.unsignedIntToLong(ifRow.dwInOctets);
            stats.packetsSent = ParseUtil.unsignedIntToLong(ifRow.dwOutUcastPkts);
            stats.packetsRecv = ParseUtil.unsignedIntToLong(ifRow.dwInUcastPkts);
            stats.outErrors = ParseUtil.unsignedIntToLong(ifRow.dwOutErrors);
            stats.inErrors = ParseUtil.unsignedIntToLong(ifRow.dwInErrors);
            stats.collisions = ParseUtil.unsignedIntToLong(ifRow.dwOutDiscards);
            stats.inDrops = ParseUtil.unsignedIntToLong(ifRow.dwInDiscards);
            stats.speed = ParseUtil.unsignedIntToLong(ifRow.dwSpeed);
            return stats;
        }
    }
}
