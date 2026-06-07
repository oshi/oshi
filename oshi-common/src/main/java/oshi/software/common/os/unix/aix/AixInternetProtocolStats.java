/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractInternetProtocolStats;

/**
 * Abstract base for AIX InternetProtocolStats. Both backends iterate {@code perfstat_protocol_t} and surface the TCP
 * and UDP variants; the shared decoding lives here, the per-backend data fetch in concrete subclasses.
 */
@ThreadSafe
public abstract class AixInternetProtocolStats extends AbstractInternetProtocolStats {

    @Override
    public TcpStats getTCPv4Stats() {
        TcpStats stats = queryTcpStats();
        return stats == null ? new TcpStats(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L) : stats;
    }

    @Override
    public UdpStats getUDPv4Stats() {
        UdpStats stats = queryUdpStats();
        return stats == null ? new UdpStats(0L, 0L, 0L, 0L) : stats;
    }

    /**
     * Returns the TCP stats from the subclass's perfstat data source.
     *
     * @return populated {@link TcpStats}, or {@code null} if the {@code tcp} entry was not found
     */
    protected abstract TcpStats queryTcpStats();

    /**
     * Returns the UDP stats from the subclass's perfstat data source.
     *
     * @return populated {@link UdpStats}, or {@code null} if the {@code udp} entry was not found
     */
    protected abstract UdpStats queryUdpStats();
}
