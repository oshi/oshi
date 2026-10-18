/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.perfstat.PerfstatProtocolFFM;
import oshi.software.common.os.unix.aix.AixInternetProtocolStats;

/**
 * FFM-backed AIX InternetProtocolStats.
 */
@ThreadSafe
public final class AixInternetProtocolStatsFFM extends AixInternetProtocolStats {

    private final Supplier<PerfstatProtocolFFM.Protocol[]> ipstats = memoize(PerfstatProtocolFFM::queryProtocols,
            defaultExpiration());

    @Override
    protected @Nullable TcpStats queryTcpStats() {
        for (PerfstatProtocolFFM.Protocol stat : ipstats.get()) {
            if ("tcp".equals(stat.name)) {
                return tcpStats(stat.tcpEstablished, stat.tcpInitiated, stat.tcpAccepted, stat.tcpDropped,
                        stat.tcpOpackets, stat.tcpIpackets, stat.tcpIerrors);
            }
        }
        return null;
    }

    @Override
    protected @Nullable UdpStats queryUdpStats() {
        for (PerfstatProtocolFFM.Protocol stat : ipstats.get()) {
            if ("udp".equals(stat.name)) {
                return udpStats(stat.udpOpackets, stat.udpIpackets, stat.udpNoSocket);
            }
        }
        return null;
    }
}
