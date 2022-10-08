/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.aix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import com.sun.jna.Native;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_protocol_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.perfstat.PerfstatProtocol;
import oshi.software.common.AbstractInternetProtocolStats;

/**
 * Internet Protocol Stats implementation
 */
@ThreadSafe
public class AixInternetProtocolStats extends AbstractInternetProtocolStats {

    private Supplier<perfstat_protocol_t[]> ipstats = memoize(PerfstatProtocol::queryProtocols, defaultExpiration());

    @Override
    public TcpStats getTCPv4Stats() {
        for (perfstat_protocol_t stat : ipstats.get()) {
            if ("tcp".equals(Native.toString(stat.name))) {
                return new TcpStats(stat.u.tcp.established, stat.u.tcp.initiated, stat.u.tcp.accepted,
                        stat.u.tcp.dropped, stat.u.tcp.dropped, stat.u.tcp.opackets, stat.u.tcp.ipackets, 0L,
                        stat.u.tcp.ierrors, 0L);
            }
        }
        return new TcpStats(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    @Override
    public UdpStats getUDPv4Stats() {
        for (perfstat_protocol_t stat : ipstats.get()) {
            if ("udp".equals(Native.toString(stat.name))) {
                return new UdpStats(stat.u.udp.opackets, stat.u.udp.ipackets, stat.u.udp.no_socket, stat.u.udp.ierrors);
            }
        }
        return new UdpStats(0L, 0L, 0L, 0L);
    }
}
