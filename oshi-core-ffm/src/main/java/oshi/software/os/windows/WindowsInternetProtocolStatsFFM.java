/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import static oshi.ffm.platform.windows.IPHlpAPIFFM.AF_INET;
import static oshi.ffm.platform.windows.IPHlpAPIFFM.AF_INET6;
import static oshi.ffm.util.platform.windows.IPHlpAPIUtilFFM.queryTCPv4Connections;
import static oshi.ffm.util.platform.windows.IPHlpAPIUtilFFM.queryTCPv6Connections;
import static oshi.ffm.util.platform.windows.IPHlpAPIUtilFFM.queryUDPv4Connections;
import static oshi.ffm.util.platform.windows.IPHlpAPIUtilFFM.queryUDPv6Connections;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.util.platform.windows.IPHlpAPIUtilFFM;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.util.ExceptionUtil;

public class WindowsInternetProtocolStatsFFM extends AbstractInternetProtocolStats {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsInternetProtocolStatsFFM.class);

    // Returned when a stats query fails, matching the JNA backend which returns a zeroed struct rather than null.
    private static final TcpStats ZERO_TCP_STATS = new TcpStats(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    private static final UdpStats ZERO_UDP_STATS = new UdpStats(0L, 0L, 0L, 0L);

    @Override
    public List<IPConnection> getConnections() {
        List<IPConnection> conns = new ArrayList<>();
        conns.addAll(queryTCPv4Connections());
        conns.addAll(queryTCPv6Connections());
        conns.addAll(queryUDPv4Connections());
        conns.addAll(queryUDPv6Connections());
        return conns;
    }

    @Override
    public TcpStats getTCPv4Stats() {
        TcpStats stats = ExceptionUtil.getOrDefault(() -> IPHlpAPIUtilFFM.getTcpStats(AF_INET), ZERO_TCP_STATS, LOG,
                "Failed to read TCPv4 stats: {}");
        return stats == null ? ZERO_TCP_STATS : stats;
    }

    @Override
    public TcpStats getTCPv6Stats() {
        TcpStats stats = ExceptionUtil.getOrDefault(() -> IPHlpAPIUtilFFM.getTcpStats(AF_INET6), ZERO_TCP_STATS, LOG,
                "Failed to read TCPv6 stats: {}");
        return stats == null ? ZERO_TCP_STATS : stats;
    }

    @Override
    public UdpStats getUDPv4Stats() {
        UdpStats stats = ExceptionUtil.getOrDefault(() -> IPHlpAPIUtilFFM.getUdpStats(AF_INET), ZERO_UDP_STATS, LOG,
                "Failed to read UDPv4 stats: {}");
        return stats == null ? ZERO_UDP_STATS : stats;
    }

    @Override
    public UdpStats getUDPv6Stats() {
        UdpStats stats = ExceptionUtil.getOrDefault(() -> IPHlpAPIUtilFFM.getUdpStats(AF_INET6), ZERO_UDP_STATS, LOG,
                "Failed to read UDPv6 stats: {}");
        return stats == null ? ZERO_UDP_STATS : stats;
    }
}
