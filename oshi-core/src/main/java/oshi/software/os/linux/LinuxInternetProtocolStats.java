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
package oshi.software.os.linux;

import static oshi.software.os.InternetProtocolStats.TcpState.CLOSED;
import static oshi.software.os.InternetProtocolStats.TcpState.CLOSE_WAIT;
import static oshi.software.os.InternetProtocolStats.TcpState.CLOSING;
import static oshi.software.os.InternetProtocolStats.TcpState.ESTABLISHED;
import static oshi.software.os.InternetProtocolStats.TcpState.FIN_WAIT1;
import static oshi.software.os.InternetProtocolStats.TcpState.FIN_WAIT2;
import static oshi.software.os.InternetProtocolStats.TcpState.LAST_ACK;
import static oshi.software.os.InternetProtocolStats.TcpState.LISTEN;
import static oshi.software.os.InternetProtocolStats.TcpState.SYN_RECV;
import static oshi.software.os.InternetProtocolStats.TcpState.SYN_SENT;
import static oshi.software.os.InternetProtocolStats.TcpState.TIME_WAIT;
import static oshi.software.os.InternetProtocolStats.TcpState.UNKNOWN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.proc.ProcessStat;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;
import oshi.util.tuples.Pair;

@ThreadSafe
public class LinuxInternetProtocolStats extends AbstractInternetProtocolStats {

    @Override
    public TcpStats getTCPv4Stats() {
        return getTcpStats("netstat -st4");
    }

    @Override
    public TcpStats getTCPv6Stats() {
        // "netstat -st6" returns the same as -st4
        return new TcpStats(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    private static TcpStats getTcpStats(String netstatStr) {
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
        List<String> netstat = ExecutingCommand.runNative(netstatStr);
        for (String s : netstat) {
            String[] split = s.trim().split(" ", 2);
            if (split.length == 2) {
                switch (split[1]) {
                case "connections established":
                    connectionsEstablished = ParseUtil.parseLongOrDefault(split[0], 0L);
                    break;
                case "active connection openings":
                    connectionsActive = ParseUtil.parseLongOrDefault(split[0], 0L);
                    break;
                case "passive connection openings":
                    connectionsPassive = ParseUtil.parseLongOrDefault(split[0], 0L);
                    break;
                case "failed connection attempts":
                    connectionFailures = ParseUtil.parseLongOrDefault(split[0], 0L);
                    break;
                case "connection resets received":
                    connectionsReset = ParseUtil.parseLongOrDefault(split[0], 0L);
                    break;
                case "segments sent out":
                    segmentsSent = ParseUtil.parseLongOrDefault(split[0], 0L);
                    break;
                case "segments received":
                    segmentsReceived = ParseUtil.parseLongOrDefault(split[0], 0L);
                    break;
                case "segments retransmitted":
                    segmentsRetransmitted = ParseUtil.parseLongOrDefault(split[0], 0L);
                    break;
                case "bad segments received":
                    inErrors = ParseUtil.parseLongOrDefault(split[0], 0L);
                    break;
                case "resets sent":
                    outResets = ParseUtil.parseLongOrDefault(split[0], 0L);
                    break;
                default:
                    break;
                }

            }
        }
        return new TcpStats(connectionsEstablished, connectionsActive, connectionsPassive, connectionFailures,
                connectionsReset, segmentsSent, segmentsReceived, segmentsRetransmitted, inErrors, outResets);
    }

    @Override
    public UdpStats getUDPv4Stats() {
        return getUdpStats("netstat -su4");
    }

    @Override
    public UdpStats getUDPv6Stats() {
        return getUdpStats("netstat -su6");
    }

    private static UdpStats getUdpStats(String netstatStr) {
        long datagramsSent = 0;
        long datagramsReceived = 0;
        long datagramsNoPort = 0;
        long datagramsReceivedErrors = 0;
        List<String> netstat = ExecutingCommand.runNative(netstatStr);
        for (String s : netstat) {
            String[] split = s.trim().split(" ", 2);
            if (split.length == 2) {
                switch (split[1]) {
                case "packets sent":
                    datagramsSent = ParseUtil.parseLongOrDefault(split[0], 0L);
                    break;
                case "packets received":
                    datagramsReceived = ParseUtil.parseLongOrDefault(split[0], 0L);
                    break;
                case "packets to unknown port received":
                    datagramsNoPort = ParseUtil.parseLongOrDefault(split[0], 0L);
                    break;
                case "packet receive errors":
                    datagramsReceivedErrors = ParseUtil.parseLongOrDefault(split[0], 0L);
                    break;
                default:
                    break;
                }
            }
        }
        return new UdpStats(datagramsSent, datagramsReceived, datagramsNoPort, datagramsReceivedErrors);
    }

    @Override
    public List<IPConnection> getConnections() {
        List<IPConnection> conns = new ArrayList<>();
        Map<Integer, Integer> pidMap = ProcessStat.querySocketToPidMap();
        conns.addAll(queryConnections("tcp", 4, pidMap));
        conns.addAll(queryConnections("tcp", 6, pidMap));
        conns.addAll(queryConnections("udp", 4, pidMap));
        conns.addAll(queryConnections("udp", 6, pidMap));
        return Collections.unmodifiableList(conns);
    }

    private List<IPConnection> queryConnections(String protocol, int ipver, Map<Integer, Integer> pidMap) {
        List<IPConnection> conns = new ArrayList<>();
        for (String s : FileUtil.readFile(ProcPath.NET + "/" + protocol + (ipver == 6 ? "6" : ""))) {
            if (s.indexOf(':') >= 0) {
                String[] split = ParseUtil.whitespaces.split(s.trim());
                if (split.length > 9) {
                    Pair<byte[], Integer> lAddr = parseIpAddr(split[1]);
                    Pair<byte[], Integer> fAddr = parseIpAddr(split[2]);
                    TcpState state = stateLookup(ParseUtil.hexStringToInt(split[3], 0));
                    Pair<Integer, Integer> txQrxQ = parseHexColonHex(split[4]);
                    int inode = ParseUtil.parseIntOrDefault(split[9], 0);
                    conns.add(new IPConnection(protocol + ipver, lAddr.getA(), lAddr.getB(), fAddr.getA(), fAddr.getB(),
                            state, txQrxQ.getA(), txQrxQ.getB(), pidMap.getOrDefault(inode, -1)));
                }
            }
        }
        return conns;
    }

    private Pair<byte[], Integer> parseIpAddr(String s) {
        int colon = s.indexOf(':');
        if (colon > 0 && colon < s.length()) {
            byte[] first = ParseUtil.hexStringToByteArray(s.substring(0, colon));
            // Bytes are in __be32 endianness. we must invert each set of 4 bytes
            for (int i = 0; i + 3 < first.length; i += 4) {
                byte tmp = first[i];
                first[i] = first[i + 3];
                first[i + 3] = tmp;
                tmp = first[i + 1];
                first[i + 1] = first[i + 2];
                first[i + 2] = tmp;
            }
            int second = ParseUtil.hexStringToInt(s.substring(colon + 1), 0);
            return new Pair<>(first, second);
        }
        return new Pair<>(new byte[0], 0);
    }

    private Pair<Integer, Integer> parseHexColonHex(String s) {
        int colon = s.indexOf(':');
        if (colon > 0 && colon < s.length()) {
            int first = ParseUtil.hexStringToInt(s.substring(0, colon), 0);
            int second = ParseUtil.hexStringToInt(s.substring(colon + 1), 0);
            return new Pair<>(first, second);
        }
        return new Pair<>(0, 0);
    }

    private static TcpState stateLookup(int state) {
        switch (state) {
        case 0x01:
            return ESTABLISHED;
        case 0x02:
            return SYN_SENT;
        case 0x03:
            return SYN_RECV;
        case 0x04:
            return FIN_WAIT1;
        case 0x05:
            return FIN_WAIT2;
        case 0x06:
            return TIME_WAIT;
        case 0x07:
            return CLOSED;
        case 0x08:
            return CLOSE_WAIT;
        case 0x09:
            return LAST_ACK;
        case 0x0A:
            return LISTEN;
        case 0x0B:
            return CLOSING;
        case 0x00:
        default:
            return UNKNOWN;
        }
    }
}
