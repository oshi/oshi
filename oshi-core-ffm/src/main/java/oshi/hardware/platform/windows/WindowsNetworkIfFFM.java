/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static org.slf4j.event.Level.ERROR;
import static oshi.ffm.ForeignFunctions.callInArenaBooleanOrDefault;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.windows.IPHlpAPIFFM;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;

/**
 * WindowsNetworks class using FFM.
 */
@ThreadSafe
public final class WindowsNetworkIfFFM extends AbstractNetworkIF {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsNetworkIfFFM.class);

    private static final byte CONNECTOR_PRESENT_BIT = 0b00000100;

    private int ifType;
    private int ndisPhysicalMediumType;
    private boolean connectorPresent;
    private String ifAlias;
    private IfOperStatus ifOperStatus;

    public WindowsNetworkIfFFM(NetworkInterface netint) throws InstantiationException {
        super(netint);
        updateAttributes();
    }

    public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
        List<NetworkIF> ifList = new ArrayList<>();
        for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
            try {
                ifList.add(new WindowsNetworkIfFFM(ni));
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
    public String getIfAlias() {
        return ifAlias;
    }

    @Override
    public IfOperStatus getIfOperStatus() {
        return ifOperStatus;
    }

    @Override
    public boolean updateAttributes() {
        // FFM targets JDK 25+ which requires Vista+; use GetIfEntry2 directly
        boolean success = callInArenaBooleanOrDefault(arena -> {
            MemorySegment ifRow = arena.allocate(IPHlpAPIFFM.MIB_IF_ROW2_SIZE);
            ifRow.fill((byte) 0);
            // Set InterfaceIndex at offset 8
            ifRow.set(ValueLayout.JAVA_INT, IPHlpAPIFFM.OFFSET_INTERFACE_INDEX, queryNetworkInterface().getIndex());
            if (0 != IPHlpAPIFFM.GetIfEntry2(ifRow)) {
                LOG.error("Failed to retrieve data for interface {}, {}", queryNetworkInterface().getIndex(),
                        getName());
                return false;
            }
            this.ifType = ifRow.get(ValueLayout.JAVA_INT, IPHlpAPIFFM.OFFSET_TYPE);
            this.ndisPhysicalMediumType = ifRow.get(ValueLayout.JAVA_INT, IPHlpAPIFFM.OFFSET_PHYSICAL_MEDIUM_TYPE);
            byte flags = ifRow.get(ValueLayout.JAVA_BYTE, IPHlpAPIFFM.OFFSET_FLAGS);
            this.connectorPresent = (flags & CONNECTOR_PRESENT_BIT) > 0;
            this.speed = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_RECEIVE_LINK_SPEED);
            this.bytesRecv = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_IN_OCTETS);
            this.packetsRecv = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_IN_UCAST_PKTS);
            this.inErrors = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_IN_ERRORS);
            this.inDrops = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_IN_DISCARDS);
            this.bytesSent = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_OUT_OCTETS);
            this.packetsSent = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_OUT_UCAST_PKTS);
            this.outErrors = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_OUT_ERRORS);
            this.collisions = ifRow.get(ValueLayout.JAVA_LONG, IPHlpAPIFFM.OFFSET_OUT_DISCARDS);
            // Alias is WCHAR[257] at OFFSET_ALIAS
            MemorySegment aliasSlice = ifRow.asSlice(IPHlpAPIFFM.OFFSET_ALIAS, 257 * 2L);
            this.ifAlias = readWideString(aliasSlice);
            int operStatus = ifRow.get(ValueLayout.JAVA_INT, IPHlpAPIFFM.OFFSET_OPER_STATUS);
            this.ifOperStatus = IfOperStatus.byValue(operStatus);
            return true;
        }, LOG, ERROR, "Failed to retrieve data for interface " + queryNetworkInterface().getIndex() + ", " + getName(),
                false);
        if (success) {
            this.timeStamp = System.currentTimeMillis();
        }
        return success;
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
