/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static org.slf4j.event.Level.ERROR;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.windows.IPHlpAPIFFM;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.platform.windows.WindowsNetworkIF;

/**
 * WindowsNetworks class using FFM.
 */
@ThreadSafe
public final class WindowsNetworkIfFFM extends WindowsNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsNetworkIfFFM.class);

    private static final byte CONNECTOR_PRESENT_BIT = 0b00000100;

    public WindowsNetworkIfFFM(NetworkInterface netint) throws InstantiationException {
        super(netint);
    }

    /**
     * Gets all network interfaces on this machine
     *
     * @param includeLocalInterfaces include local interfaces in the result
     * @return A list of {@link NetworkIF} objects representing the interfaces
     */
    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        return getNetworks(includeLocalInterfaces, WindowsNetworkIfFFM::new);
    }

    @Override
    protected IfStats queryStats() {
        // FFM targets JDK 25+ which requires Vista+; use GetIfEntry2 directly
        return callInArenaOrDefault(arena -> {
            MemorySegment ifRow = arena.allocate(IPHlpAPIFFM.MIB_IF_ROW2_SIZE);
            ifRow.fill((byte) 0);
            // Set InterfaceIndex at offset 8
            ifRow.set(ValueLayout.JAVA_INT, IPHlpAPIFFM.OFFSET_INTERFACE_INDEX, queryNetworkInterface().getIndex());
            if (0 != IPHlpAPIFFM.GetIfEntry2(ifRow)) {
                LOG.error("Failed to retrieve data for interface {}, {}", queryNetworkInterface().getIndex(),
                        getName());
                return null;
            }
            IfStats stats = new IfStats();
            stats.ifType = ifRow.get(ValueLayout.JAVA_INT, IPHlpAPIFFM.OFFSET_TYPE);
            stats.ndisPhysicalMediumType = ifRow.get(ValueLayout.JAVA_INT, IPHlpAPIFFM.OFFSET_PHYSICAL_MEDIUM_TYPE);
            byte flags = ifRow.get(ValueLayout.JAVA_BYTE, IPHlpAPIFFM.OFFSET_FLAGS);
            stats.connectorPresent = (flags & CONNECTOR_PRESENT_BIT) > 0;
            stats.speed = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_RECEIVE_LINK_SPEED);
            stats.bytesRecv = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_IN_OCTETS);
            stats.packetsRecv = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_IN_UCAST_PKTS);
            stats.inErrors = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_IN_ERRORS);
            stats.inDrops = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_IN_DISCARDS);
            stats.bytesSent = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_OUT_OCTETS);
            stats.packetsSent = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_OUT_UCAST_PKTS);
            stats.outErrors = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_OUT_ERRORS);
            stats.collisions = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_OUT_DISCARDS);
            // Alias is WCHAR[257] at OFFSET_ALIAS
            MemorySegment aliasSlice = ifRow.asSlice(IPHlpAPIFFM.OFFSET_ALIAS, 257 * 2L);
            stats.ifAlias = readWideString(aliasSlice);
            int operStatus = ifRow.get(ValueLayout.JAVA_INT, IPHlpAPIFFM.OFFSET_OPER_STATUS);
            stats.ifOperStatus = IfOperStatus.byValue(operStatus);
            return stats;
        }, LOG, ERROR, "Failed to retrieve data for interface " + queryNetworkInterface().getIndex() + ", " + getName(),
                null);
    }

    private static String readWideString(MemorySegment seg) {
        int len = 0;
        while (len < seg.byteSize() / 2 && seg.get(ValueLayout.JAVA_SHORT, len * 2L) != 0) {
            len++;
        }
        byte[] bytes = new byte[len * 2];
        MemorySegment.copy(seg, ValueLayout.JAVA_BYTE, 0, bytes, 0, bytes.length);
        return new String(bytes, StandardCharsets.UTF_16LE);
    }
}
