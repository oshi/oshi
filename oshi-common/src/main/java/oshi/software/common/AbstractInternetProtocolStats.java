/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import java.util.List;

import oshi.software.os.InternetProtocolStats;
import oshi.util.driver.unix.NetStat;

/**
 * Common implementations for IP Stats
 */
public abstract class AbstractInternetProtocolStats implements InternetProtocolStats {

    @Override
    public TcpStats getTCPv6Stats() {
        // Default when OS doesn't have separate TCPv6 stats
        return new TcpStats(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    @Override
    public UdpStats getUDPv6Stats() {
        // Default when OS doesn't have separate UDPv6 stats
        return new UdpStats(0L, 0L, 0L, 0L);
    }

    @Override
    public List<IPConnection> getConnections() {
        return NetStat.queryNetstat();
    }
}
