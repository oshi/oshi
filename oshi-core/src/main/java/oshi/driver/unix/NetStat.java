/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.driver.unix;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

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

    private static final String NETSTAT_S = "netstat -s -";

    private NetStat() {
    }

    /**
     * Query netstat to obtain number of established TCP connections
     *
     * @return A pair with number of established IPv4 and IPv6 connections
     */
    public static Pair<Long, Long> queryTcpnetstat() {
        long tcp4 = 0L;
        long tcp6 = 0L;
        List<String> activeConns = ExecutingCommand.runNative("netstat -n -p tcp");
        for (String s : activeConns) {
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
        List<IPConnection> connections = new ArrayList<>();
        List<String> activeConns = ExecutingCommand.runNative("netstat -n");
        for (String s : activeConns) {
            String[] split = null;
            if (s.startsWith("tcp") || s.startsWith("udp")) {
                split = ParseUtil.whitespaces.split(s);
                if (split.length >= 5) {
                    String state = (split.length == 6) ? split[5] : null;
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

    private static Pair<byte[], Integer> parseIP(String s) {
        // 73.169.134.6.9599 to 73.169.134.6 port 9599
        // or
        // 2001:558:600a:a5.123 to 2001:558:600a:a5 port 123
        int portPos = s.lastIndexOf('.');
        if (portPos > 0 && s.length() > portPos) {
            int port = ParseUtil.parseIntOrDefault(s.substring(portPos + 1), 0);
            String ip = s.substring(0, portPos);
            try {
                // Try to parse existing IP
                return new Pair<>(InetAddress.getByName(ip).getAddress(), port);
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
                    return new Pair<>(InetAddress.getByName(ip).getAddress(), port);
                } catch (UnknownHostException e2) {
                    return new Pair<>(new byte[0], port);
                }
            }
        }
        return new Pair<>(new byte[0], 0);
    }

    /**
     * Queries {@code netstat -s} for the tcp protocol
     *
     * @param protocolSwitch
     *            the netstat command switch character for protocol
     * @return the stats
     */
    public static TcpStats queryTcpStats(Character protocolSwitch) {
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
        List<String> netstat = ExecutingCommand.runNative(NETSTAT_S + protocolSwitch + " tcp");
        // append IP
        netstat.addAll(ExecutingCommand.runNative(NETSTAT_S + protocolSwitch + " ip"));
        for (String s : netstat) {
            // Two stats per line. Split the strings by index of "tcp"
            String[] stats = NetStat.splitOnPrefix(s, "tcp");
            // Now of form tcpXX = 123
            for (String stat : stats) {
                if (stat != null) {
                    String[] split = stat.split("=");
                    if (split.length == 2) {
                        switch (split[0].trim()) {
                        case "tcpCurrEstab":
                            connectionsEstablished = ParseUtil.parseLongOrDefault(split[1].trim(), 0L);
                            break;
                        case "tcpActiveOpens":
                            connectionsActive = ParseUtil.parseLongOrDefault(split[1].trim(), 0L);
                            break;
                        case "tcpPassiveOpens":
                            connectionsPassive = ParseUtil.parseLongOrDefault(split[1].trim(), 0L);
                            break;
                        case "tcpAttemptFails":
                            connectionFailures = ParseUtil.parseLongOrDefault(split[1].trim(), 0L);
                            break;
                        case "tcpEstabResets":
                            connectionsReset = ParseUtil.parseLongOrDefault(split[1].trim(), 0L);
                            break;
                        case "tcpOutSegs":
                            segmentsSent = ParseUtil.parseLongOrDefault(split[1].trim(), 0L);
                            break;
                        case "tcpInSegs":
                            segmentsReceived = ParseUtil.parseLongOrDefault(split[1].trim(), 0L);
                            break;
                        case "tcpRetransSegs":
                            segmentsRetransmitted = ParseUtil.parseLongOrDefault(split[1].trim(), 0L);
                            break;
                        case "tcpInErr":
                            // doesn't have tcp in second column
                            inErrors = ParseUtil.getFirstIntValue(split[1].trim());
                            break;
                        case "tcpOutRsts":
                            outResets = ParseUtil.parseLongOrDefault(split[1].trim(), 0L);
                            break;
                        default:
                            break;
                        }
                    }
                }
            }
        }
        return new TcpStats(connectionsEstablished, connectionsActive, connectionsPassive, connectionFailures,
                connectionsReset, segmentsSent, segmentsReceived, segmentsRetransmitted, inErrors, outResets);
    }

    /**
     * Queries {@code netstat -s} for the udp protocol
     *
     * @param protocolSwitch
     *            the netstat command switch character for protocol
     * @return the stats
     */
    public static UdpStats queryUdpStats(Character protocolSwitch) {
        long datagramsSent = 0;
        long datagramsReceived = 0;
        long datagramsNoPort = 0;
        long datagramsReceivedErrors = 0;
        List<String> netstat = ExecutingCommand.runNative(NETSTAT_S + protocolSwitch + " tcp");
        // append IP
        netstat.addAll(ExecutingCommand.runNative(NETSTAT_S + protocolSwitch + " ip"));
        for (String s : netstat) {
            // Two stats per line. Split the strings by index of "udp"
            String[] stats = NetStat.splitOnPrefix(s, "udp");
            // Now of form udpXX = 123
            for (String stat : stats) {
                if (stat != null) {
                    String[] split = stat.split("=");
                    if (split.length == 2) {
                        switch (split[0].trim()) {
                        case "udpOutDatagrams":
                            datagramsSent = ParseUtil.parseLongOrDefault(split[1].trim(), 0L);
                            break;
                        case "udpInDatagrams":
                            datagramsReceived = ParseUtil.parseLongOrDefault(split[1].trim(), 0L);
                            break;
                        case "udpNoPorts":
                            datagramsNoPort = ParseUtil.parseLongOrDefault(split[1].trim(), 0L);
                            break;
                        case "udpInErrors":
                            datagramsReceivedErrors = ParseUtil.parseLongOrDefault(split[1].trim(), 0L);
                            break;
                        default:
                            break;
                        }
                    }
                }
            }
        }
        return new UdpStats(datagramsSent, datagramsReceived, datagramsNoPort, datagramsReceivedErrors);
    }

    private static String[] splitOnPrefix(String s, String prefix) {
        String[] stats = new String[2];
        int first = s.indexOf(prefix);
        if (first >= 0) {
            int second = s.indexOf(prefix, first + 1);
            if (second >= 0) {
                stats[0] = s.substring(first, second).trim();
                stats[1] = s.substring(second).trim();
            } else {
                stats[0] = s.substring(first).trim();
            }
        }
        return stats;
    }
}
