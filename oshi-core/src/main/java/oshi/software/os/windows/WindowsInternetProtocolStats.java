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
package oshi.software.os.windows;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.perfmon.TCPStats;
import oshi.driver.windows.perfmon.TCPStats.TcpProperty;
import oshi.driver.windows.perfmon.UDPStats;
import oshi.driver.windows.perfmon.UDPStats.UdpProperty;
import oshi.software.common.AbstractInternetProtocolStats;

@ThreadSafe
public class WindowsInternetProtocolStats extends AbstractInternetProtocolStats {

    @Override
    public TcpStats getTCPv4Stats() {
        return getTCPStats(TCPStats.queryTCPv4Stats());
    }

    @Override
    public TcpStats getTCPv6Stats() {
        return getTCPStats(TCPStats.queryTCPv6Stats());
    }

    private TcpStats getTCPStats(Map<TcpProperty, Long> valueMap) {
        return new TcpStats(valueMap.getOrDefault(TcpProperty.CONNECTIONSESTABLISHED, 0L),
                valueMap.getOrDefault(TcpProperty.CONNECTIONSACTIVE, 0L),
                valueMap.getOrDefault(TcpProperty.CONNECTIONSPASSIVE, 0L),
                valueMap.getOrDefault(TcpProperty.CONNECTIONFAILURES, 0L),
                valueMap.getOrDefault(TcpProperty.CONNECTIONSRESET, 0L),
                valueMap.getOrDefault(TcpProperty.SEGMENTSSENTPERSEC, 0L),
                valueMap.getOrDefault(TcpProperty.SEGMENTSRECEIVEDPERSEC, 0L),
                valueMap.getOrDefault(TcpProperty.SEGMENTSRETRANSMITTEDPERSEC, 0L));
    }

    @Override
    public UdpStats getUDPv4Stats() {
        return getUDPStats(UDPStats.queryUDPv4Stats());
    }

    @Override
    public UdpStats getUDPv6Stats() {
        return getUDPStats(UDPStats.queryUDPv6Stats());
    }

    private UdpStats getUDPStats(Map<UdpProperty, Long> valueMap) {
        return new UdpStats(valueMap.getOrDefault(UdpProperty.DATAGRAMSSENTPERSEC, 0L),
                valueMap.getOrDefault(UdpProperty.DATAGRAMSRECEIVEDPERSEC, 0L),
                valueMap.getOrDefault(UdpProperty.DATAGRAMSNOPORTPERSEC, 0L),
                valueMap.getOrDefault(UdpProperty.DATAGRAMSRECEIVEDERRORS, 0L));
    }
}
