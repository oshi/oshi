/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.driver.unix.aix.perfstat.PerfstatProtocolFFM;
import oshi.software.common.os.unix.aix.AixInternetProtocolStats;

/**
 * FFM-backed AIX InternetProtocolStats.
 */
@ThreadSafe
public final class AixInternetProtocolStatsFFM extends AixInternetProtocolStats {

    private final Supplier<PerfstatProtocolFFM.Protocol[]> ipstats = memoize(PerfstatProtocolFFM::queryProtocols,
            defaultExpiration());

    @Override
    protected TcpStats queryTcpStats() {
        for (PerfstatProtocolFFM.Protocol stat : ipstats.get()) {
            if ("tcp".equals(stat.name)) {
                return new TcpStats(stat.tcpEstablished, stat.tcpInitiated, stat.tcpAccepted, stat.tcpDropped,
                        stat.tcpDropped, stat.tcpOpackets, stat.tcpIpackets, 0L, stat.tcpIerrors, 0L);
            }
        }
        return null;
    }

    @Override
    protected UdpStats queryUdpStats() {
        for (PerfstatProtocolFFM.Protocol stat : ipstats.get()) {
            if ("udp".equals(stat.name)) {
                return new UdpStats(stat.udpOpackets, stat.udpIpackets, stat.udpNoSocket, stat.udpIerrors);
            }
        }
        return null;
    }
}
