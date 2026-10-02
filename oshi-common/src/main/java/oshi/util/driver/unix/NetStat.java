/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.InternetProtocolStats.IPConnection;
import oshi.software.os.InternetProtocolStats.TcpState;
import oshi.software.os.InternetProtocolStats.TcpStats;
import oshi.software.os.InternetProtocolStats.UdpStats;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Utility to query TCP connections
 */
@ThreadSafe
public final class NetStat {

    private NetStat() {
    }

    /**
     * Query netstat to obtain number of established TCP connections
     *
     * @return A pair with number of established IPv4 and IPv6 connections
     */
    public static Pair<Long, Long> queryTcpnetstat() {
        return queryTcpnetstat(ExecutingCommand.runNative("netstat -n -p tcp"));
    }

    /**
     * Parse netstat output to count established TCP connections.
     *
     * @param lines output of {@code netstat -n -p tcp}
     * @return A pair with number of established IPv4 and IPv6 connections
     */
    static Pair<Long, Long> queryTcpnetstat(List<String> lines) {
        long tcp4 = 0L;
        long tcp6 = 0L;
        for (String s : lines) {
            if (s.endsWith("ESTABLISHED")) {
                if (s.startsWith("tcp4")) {
                    tcp4++;
                } else if (s.startsWith("tcp6")) {
                    tcp6++;
                }
            }
        }
        return new Pair<>(tcp4, tcp6);
    }

    /**
     * Query netstat to all TCP and UDP connections
     *
     * @return A list of TCP and UDP connections
     */
    public static List<IPConnection> queryNetstat() {
        return queryNetstat(ExecutingCommand.runNative("netstat -n"));
    }

    /**
     * Parse netstat output for TCP and UDP connections.
     *
     * @param lines output of {@code netstat -n}
     * @return A list of TCP and UDP connections
     */
    static List<IPConnection> queryNetstat(List<String> lines) {
        List<IPConnection> connections = new ArrayList<>();
        for (String s : lines) {
            String[] split = null;
            if (s.startsWith("tcp") || s.startsWith("udp")) {
                split = ParseUtil.whitespaces.split(s);
                if (split.length >= 5) {
                    String state = (split.length == 6) ? split[5] : null;
                    // Substitution if required
                    if ("SYN_RCVD".equals(state)) {
                        state = "SYN_RECV";
                    }
                    String type = split[0];
                    Pair<byte[], Integer> local = parseIP(split[3]);
                    Pair<byte[], Integer> foreign = parseIP(split[4]);
                    connections.add(new IPConnection(type, local.getA(), local.getB(), foreign.getA(), foreign.getB(),
                            state == null ? TcpState.NONE : TcpState.valueOf(state),
                            ParseUtil.parseIntOrDefault(split[2], 0), ParseUtil.parseIntOrDefault(split[1], 0), -1));
                }
            }
        }
        return connections;
    }

    /**
     * Query Solaris netstat for all TCP and UDP connections, including listening and unconnected sockets.
     *
     * @return A list of TCP and UDP connections
     */
    public static List<IPConnection> querySolarisNetstat() {
        return querySolarisNetstat(ExecutingCommand.runNative("netstat -an"));
    }

    /**
     * Parse Solaris netstat output for TCP and UDP connections. Solaris groups connections into sections headed by
     * protocol and address family, such as {@code TCP: IPv4}, and each row starts with the local address.
     *
     * @param lines output of {@code netstat -an}
     * @return A list of TCP and UDP connections
     */
    static List<IPConnection> querySolarisNetstat(List<String> lines) {
        List<IPConnection> connections = new ArrayList<>();
        String type = null;
        for (String s : lines) {
            String line = s.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] split = ParseUtil.whitespaces.split(line, -1);
            if (split[0].indexOf('.') < 0) {
                // Every row starts with an address.port; anything else is a section heading, or a column heading or
                // rule within one
                type = solarisSectionType(line, type);
                continue;
            }
            if (type == null) {
                continue;
            }
            Pair<byte[], Integer> local = parseIP(split[0]);
            // An unconnected UDP socket leaves the remote address blank, putting the state in the second field
            Pair<byte[], Integer> foreign = split.length > 1 && split[1].indexOf('.') >= 0 ? parseIP(split[1])
                    : new Pair<>(new byte[0], 0);
            if (type.startsWith("tcp")) {
                // Local Address, Remote Address, Swind, Send-Q, Rwind, Recv-Q, State, and If on IPv6
                if (split.length >= 7) {
                    connections.add(new IPConnection(type, local.getA(), local.getB(), foreign.getA(), foreign.getB(),
                            solarisTcpState(split[6]), ParseUtil.parseIntOrDefault(split[3], 0),
                            ParseUtil.parseIntOrDefault(split[5], 0), -1));
                }
            } else if (split.length >= 3) {
                // Local Address, Remote Address, State, buffer sizes and overflow counts; UDP has no queues
                connections.add(new IPConnection(type, local.getA(), local.getB(), foreign.getA(), foreign.getB(),
                        TcpState.NONE, 0, 0, -1));
            }
        }
        return connections;
    }

    private static @Nullable String solarisSectionType(String line, @Nullable String type) {
        switch (line) {
            case "TCP: IPv4":
                return "tcp4";
            case "TCP: IPv6":
                return "tcp6";
            case "UDP: IPv4":
                return "udp4";
            case "UDP: IPv6":
                return "udp6";
            default:
                // Column headings and rules stay in the current section; any other heading leaves it
                return line.startsWith("Local Address") || line.startsWith("-") ? type : null;
        }
    }

    private static TcpState solarisTcpState(String state) {
        switch (state) {
            case "SYN_RCVD":
            case "SYN_RECEIVED":
                return TcpState.SYN_RECV;
            case "CLOSED":
            case "LISTEN":
            case "SYN_SENT":
            case "ESTABLISHED":
            case "FIN_WAIT_1":
            case "FIN_WAIT_2":
            case "CLOSE_WAIT":
            case "CLOSING":
            case "LAST_ACK":
            case "TIME_WAIT":
                return TcpState.valueOf(state);
            default:
                // Solaris also reports IDLE and BOUND, which have no equivalent
                return TcpState.UNKNOWN;
        }
    }

    private static Pair<byte[], Integer> parseIP(String s) {
        // 73.169.134.6.9599 to 73.169.134.6 port 9599
        // or
        // 2001:558:600a:a5.123 to 2001:558:600a:a5 port 123
        int portPos = s.lastIndexOf('.');
        if (portPos > 0 && s.length() > portPos) {
            int port = ParseUtil.parseIntOrDefault(s.substring(portPos + 1), 0);
            String ip = s.substring(0, portPos);
            if ("*".equals(ip)) {
                // Any address; not a host name to look up
                return new Pair<>(new byte[0], port);
            }
            try {
                // Try to parse existing IP
                return new Pair<>(toAddressBytes(ip), port);
            } catch (UnknownHostException e) {
                try {
                    // Try again with trailing ::
                    if (ip.endsWith(":") && ip.contains("::")) {
                        ip = ip + "0";
                    } else if (ip.endsWith(":") || ip.contains("::")) {
                        ip = ip + ":0";
                    } else {
                        ip = ip + "::0";
                    }
                    return new Pair<>(toAddressBytes(ip), port);
                } catch (UnknownHostException e2) {
                    return new Pair<>(new byte[0], port);
                }
            }
        }
        return new Pair<>(new byte[0], 0);
    }

    private static byte[] toAddressBytes(String ip) throws UnknownHostException {
        byte[] addr = InetAddress.getByName(ip).getAddress();
        // InetAddress collapses an IPv4-mapped IPv6 address (::ffff:a.b.c.d) to its 4-byte IPv4 form, but it was
        // written in IPv6 notation, so return the 16 bytes the socket actually holds
        if (addr.length == 4 && ip.indexOf(':') >= 0) {
            byte[] mapped = new byte[16];
            mapped[10] = (byte) 0xff;
            mapped[11] = (byte) 0xff;
            System.arraycopy(addr, 0, mapped, 12, 4);
            return mapped;
        }
        return addr;
    }

    /**
     * Gets TCP stats via {@code netstat -s}. Used for Linux and OpenBSD formats
     *
     * @param netstatStr The command string
     * @return The statistics
     */
    public static TcpStats queryTcpStats(String netstatStr) {
        return queryTcpStats(ExecutingCommand.runNative(netstatStr));
    }

    /**
     * Parse TCP statistics from netstat -s output.
     *
     * @param netstat output lines from {@code netstat -s} (TCP section)
     * @return The statistics
     */
    static TcpStats queryTcpStats(List<String> netstat) {
        long connectionsEstablished = 0;
        long connectionsActive = 0;
        long connectionsPassive = 0;
        long connectionFailures = 0;
        long connectionsReset = 0;
        long segmentsSent = 0;
        long segmentsReceived = 0;
        long segmentsRetransmitted = 0;
        long inErrors = 0;
        long outResets = 0;
        for (String s : netstat) {
            String[] split = s.trim().split(" ", 2);
            if (split.length == 2) {
                switch (split[1]) {
                    case "connections established":
                    case "connection established (including accepts)":
                    case "connections established (including accepts)":
                        connectionsEstablished = ParseUtil.parseLongOrDefault(split[0], 0L);
                        break;
                    case "active connection openings":
                        connectionsActive = ParseUtil.parseLongOrDefault(split[0], 0L);
                        break;
                    case "passive connection openings":
                        connectionsPassive = ParseUtil.parseLongOrDefault(split[0], 0L);
                        break;
                    case "failed connection attempts":
                    case "bad connection attempts":
                        connectionFailures = ParseUtil.parseLongOrDefault(split[0], 0L);
                        break;
                    case "connection resets received":
                    case "dropped due to RST":
                        connectionsReset = ParseUtil.parseLongOrDefault(split[0], 0L);
                        break;
                    case "segments sent out":
                    case "packet sent":
                    case "packets sent":
                        segmentsSent = ParseUtil.parseLongOrDefault(split[0], 0L);
                        break;
                    case "segments received":
                    case "packet received":
                    case "packets received":
                        segmentsReceived = ParseUtil.parseLongOrDefault(split[0], 0L);
                        break;
                    case "segments retransmitted":
                        segmentsRetransmitted = ParseUtil.parseLongOrDefault(split[0], 0L);
                        break;
                    case "bad segments received":
                    case "discarded for bad checksum":
                    case "discarded for bad checksums":
                    case "discarded for bad header offset field":
                    case "discarded for bad header offset fields":
                    case "discarded because packet too short":
                    case "discarded for missing IPsec protection":
                        inErrors += ParseUtil.parseLongOrDefault(split[0], 0L);
                        break;
                    case "resets sent":
                        outResets = ParseUtil.parseLongOrDefault(split[0], 0L);
                        break;
                    default:
                        // handle special case variable strings
                        if (split[1].contains("retransmitted") && split[1].contains("data packet")) {
                            segmentsRetransmitted += ParseUtil.parseLongOrDefault(split[0], 0L);
                        }
                        break;
                }

            }

        }
        return new TcpStats(connectionsEstablished, connectionsActive, connectionsPassive, connectionFailures,
                connectionsReset, segmentsSent, segmentsReceived, segmentsRetransmitted, inErrors, outResets);
    }

    /**
     * Gets UDP stats via {@code netstat -s}. Used for Linux and OpenBSD formats
     *
     * @param netstatStr The command string
     * @return The statistics
     */
    public static UdpStats queryUdpStats(String netstatStr) {
        return queryUdpStats(ExecutingCommand.runNative(netstatStr));
    }

    /**
     * Parse UDP statistics from netstat -s output.
     *
     * @param netstat output lines from {@code netstat -s} (UDP section)
     * @return The statistics
     */
    static UdpStats queryUdpStats(List<String> netstat) {
        long datagramsSent = 0;
        long datagramsReceived = 0;
        long datagramsNoPort = 0;
        long datagramsReceivedErrors = 0;
        for (String s : netstat) {
            String[] split = s.trim().split(" ", 2);
            if (split.length == 2) {
                switch (split[1]) {
                    case "packets sent":
                    case "datagram output":
                    case "datagrams output":
                        datagramsSent = ParseUtil.parseLongOrDefault(split[0], 0L);
                        break;
                    case "packets received":
                    case "datagram received":
                    case "datagrams received":
                        datagramsReceived = ParseUtil.parseLongOrDefault(split[0], 0L);
                        break;
                    case "packets to unknown port received":
                    case "dropped due to no socket":
                    case "broadcast/multicast datagram dropped due to no socket":
                    case "broadcast/multicast datagrams dropped due to no socket":
                        datagramsNoPort += ParseUtil.parseLongOrDefault(split[0], 0L);
                        break;
                    case "packet receive errors":
                    case "with incomplete header":
                    case "with bad data length field":
                    case "with bad checksum":
                    case "with no checksum":
                        datagramsReceivedErrors += ParseUtil.parseLongOrDefault(split[0], 0L);
                        break;
                    default:
                        break;
                }
            }
        }
        return new UdpStats(datagramsSent, datagramsReceived, datagramsNoPort, datagramsReceivedErrors);
    }
}
