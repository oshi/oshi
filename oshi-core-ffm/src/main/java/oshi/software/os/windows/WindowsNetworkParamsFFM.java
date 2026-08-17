/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.NativeHandle;
import oshi.ffm.platform.windows.IPHlpAPIFFM;
import oshi.ffm.util.platform.windows.IPHlpAPIUtilFFM;
import oshi.ffm.util.platform.windows.Kernel32UtilFFM;
import oshi.software.common.os.windows.WindowsNetworkParams;

public final class WindowsNetworkParamsFFM extends WindowsNetworkParams {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsNetworkParamsFFM.class);

    @Override
    public String[] getDnsServers() {
        return IPHlpAPIUtilFFM.getDnsServers();
    }

    @Override
    public String getDomainName() {
        return Kernel32UtilFFM.getComputerNameEx();
    }

    @Override
    public String getHostName() {
        String name = Kernel32UtilFFM.getComputerName();
        return name.isEmpty() ? super.getHostName() : name;
    }

    @Override
    protected List<RouteRow> queryRouteRows() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pTable = arena.allocate(ADDRESS);
            int ret = IPHlpAPIFFM.GetIpForwardTable2(IPHlpAPIFFM.AF_UNSPEC, pTable);
            if (ret != 0) {
                LOG.error("Failed to get the IP forward table. Error code: {}", ret);
                return new ArrayList<>();
            }
            MemorySegment table = pTable.get(ADDRESS, 0);
            // The table is allocated by the system, so it must be released with FreeMibTable however this exits
            try (var _ = NativeHandle.of(table, IPHlpAPIFFM::FreeMibTable)) {
                return readRows(table);
            }
        } catch (Throwable e) { // NOSONAR java:S1181 - an FFM downcall can throw any Throwable
            LOG.error("Failed to read the IP forward table.", e);
            return new ArrayList<>();
        }
    }

    private static List<RouteRow> readRows(MemorySegment table) {
        // Read NumEntries from a minimal view first, then re-reinterpret to the full extent the count implies
        int numEntries = table.reinterpret(IPHlpAPIFFM.OFFSET_IPFORWARD_TABLE2_TABLE).get(JAVA_INT,
                IPHlpAPIFFM.OFFSET_IPFORWARD_TABLE2_NUM_ENTRIES);
        if (numEntries <= 0) {
            return new ArrayList<>();
        }
        MemorySegment sized = table.reinterpret(
                IPHlpAPIFFM.OFFSET_IPFORWARD_TABLE2_TABLE + (long) IPHlpAPIFFM.MIB_IPFORWARD_ROW2_SIZE * numEntries);
        List<RouteRow> rows = new ArrayList<>(numEntries);
        for (int i = 0; i < numEntries; i++) {
            long base = IPHlpAPIFFM.OFFSET_IPFORWARD_TABLE2_TABLE + (long) IPHlpAPIFFM.MIB_IPFORWARD_ROW2_SIZE * i;
            RouteRow row = new RouteRow();
            row.destination = addressBytes(sized, base + IPHlpAPIFFM.OFFSET_ROUTE_DEST_FAMILY,
                    base + IPHlpAPIFFM.OFFSET_ROUTE_DEST_IPV4, base + IPHlpAPIFFM.OFFSET_ROUTE_DEST_IPV6);
            row.prefixLength = sized.get(JAVA_BYTE, base + IPHlpAPIFFM.OFFSET_ROUTE_PREFIX_LENGTH) & 0xff;
            row.nextHop = addressBytes(sized, base + IPHlpAPIFFM.OFFSET_ROUTE_NEXTHOP_FAMILY,
                    base + IPHlpAPIFFM.OFFSET_ROUTE_NEXTHOP_IPV4, base + IPHlpAPIFFM.OFFSET_ROUTE_NEXTHOP_IPV6);
            row.interfaceIndex = sized.get(JAVA_INT, base + IPHlpAPIFFM.OFFSET_ROUTE_INTERFACE_INDEX);
            row.metric = Integer.toUnsignedLong(sized.get(JAVA_INT, base + IPHlpAPIFFM.OFFSET_ROUTE_METRIC));
            rows.add(row);
        }
        return rows;
    }

    /**
     * Reads the address out of a SOCKADDR_INET union, whose family selects which arm and therefore which offset holds
     * the address. The bytes are in network order in both arms, so they are copied rather than reinterpreted.
     */
    private static byte[] addressBytes(MemorySegment segment, long familyOffset, long ipv4Offset, long ipv6Offset) {
        short family = segment.get(JAVA_SHORT, familyOffset);
        if (family == IPHlpAPIFFM.AF_INET) {
            return segment.asSlice(ipv4Offset, 4).toArray(JAVA_BYTE);
        } else if (family == IPHlpAPIFFM.AF_INET6) {
            return segment.asSlice(ipv6Offset, 16).toArray(JAVA_BYTE);
        }
        return new byte[0];
    }
}
