/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import com.sun.jna.Native;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_protocol_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.perfstat.PerfstatProtocolJNA;
import oshi.software.common.os.unix.aix.AixInternetProtocolStats;

/**
 * JNA-backed AIX InternetProtocolStats.
 */
@ThreadSafe
public final class AixInternetProtocolStatsJNA extends AixInternetProtocolStats {

    private final Supplier<perfstat_protocol_t[]> ipstats = memoize(PerfstatProtocolJNA::queryProtocols,
            defaultExpiration());

    @Override
    protected TcpStats queryTcpStats() {
        for (perfstat_protocol_t stat : ipstats.get()) {
            if ("tcp".equals(Native.toString(stat.name))) {
                return new TcpStats(stat.u.tcp.established, stat.u.tcp.initiated, stat.u.tcp.accepted,
                        stat.u.tcp.dropped, stat.u.tcp.dropped, stat.u.tcp.opackets, stat.u.tcp.ipackets, 0L,
                        stat.u.tcp.ierrors, 0L);
            }
        }
        return null;
    }

    @Override
    protected UdpStats queryUdpStats() {
        for (perfstat_protocol_t stat : ipstats.get()) {
            if ("udp".equals(Native.toString(stat.name))) {
                return new UdpStats(stat.u.udp.opackets, stat.u.udp.ipackets, stat.u.udp.no_socket, stat.u.udp.ierrors);
            }
        }
        return null;
    }
}
