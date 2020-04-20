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

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.InternetProtocolStats;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
public class LinuxInternetProtocolStats implements InternetProtocolStats {

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
}
