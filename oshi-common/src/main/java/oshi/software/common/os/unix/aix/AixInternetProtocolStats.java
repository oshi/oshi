/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

import org.jspecify.annotations.Nullable;

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
    protected abstract @Nullable TcpStats queryTcpStats();

    /**
     * Returns the UDP stats from the subclass's perfstat data source.
     *
     * @return populated {@link UdpStats}, or {@code null} if the {@code udp} entry was not found
     */
    protected abstract @Nullable UdpStats queryUdpStats();

    /**
     * Maps the TCP counters of {@code perfstat_protocol_t} onto {@link TcpStats}.
     * <p>
     * {@code perfstat} has no count of connections that failed before being established, nor of retransmitted segments,
     * so both are 0. Its {@code dropped}, the established connections dropped, is the reset count, which
     * {@code netstat -s} prints as the drops among connections closed. Its packets sent exclude retransmissions.
     *
     * @param established connections established
     * @param initiated   connections initiated, the active opens
     * @param accepted    connections accepted, the passive opens
     * @param dropped     established connections dropped
     * @param opackets    packets sent
     * @param ipackets    packets received
     * @param ierrors     input errors
     * @return the TCP statistics
     */
    protected static TcpStats tcpStats(long established, long initiated, long accepted, long dropped, long opackets,
            long ipackets, long ierrors) {
        return new TcpStats(established, initiated, accepted, 0L, dropped, opackets, ipackets, 0L, ierrors, 0L);
    }

    /**
     * Maps the UDP counters of {@code perfstat_protocol_t} onto {@link UdpStats}.
     * <p>
     * {@code perfstat}'s {@code ierrors} counts every datagram dropped on input, including those with no socket on
     * their port, and it keeps no count of the broadcast and multicast ones among them that would let the rest be
     * separated out. So the receive errors, which exclude datagrams with no application at their port, are 0. Its
     * {@code no_socket} counts only unicast datagrams with no socket.
     *
     * @param opackets packets sent
     * @param ipackets packets received
     * @param noSocket unicast datagrams with no socket on their port
     * @return the UDP statistics
     */
    protected static UdpStats udpStats(long opackets, long ipackets, long noSocket) {
        return new UdpStats(opackets, ipackets, noSocket, 0L);
    }
}
