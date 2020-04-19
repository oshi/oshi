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
package oshi.driver.windows.perfmon;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.PerfCounterQuery;
import oshi.util.platform.windows.PerfCounterQuery.PdhCounterProperty;

/**
 * Utility to query Memory performance counter
 */
@ThreadSafe
public final class TCPStats {

    private static final String TCP_V4 = "TCPv4";
    private static final String TCP_V6 = "TCPv6";
    private static final String WIN32_PERF_RAW_DATA_TCPIP_TCP_V4 = "Win32_PerfRawData_Tcpip_TCPv4";
    private static final String WIN32_PERF_RAW_DATA_TCPIP_TCP_V6 = "Win32_PerfRawData_Tcpip_TCPv6";

    public enum TcpProperty implements PdhCounterProperty {
        CONNECTIONFAILURES(null, "Connection Failures"), CONNECTIONSACTIVE(null, "Connections Active"),
        CONNECTIONSESTABLISHED(null, "Connections Established"), CONNECTIONSPASSIVE(null, "Connections Passive"),
        CONNECTIONSRESET(null, "Connections Reset"), SEGMENTSPERSEC(null, "Segments/sec"),
        SEGMENTSRECEIVEDPERSEC(null, "Segments Received/sec"),
        SEGMENTSRETRANSMITTEDPERSEC(null, "Segments Retransmitted/sec"), SEGMENTSSENTPERSEC(null, "Segments Sent/sec");

        private final String instance;
        private final String counter;

        TcpProperty(String instance, String counter) {
            this.instance = instance;
            this.counter = counter;
        }

        @Override
        public String getInstance() {
            return instance;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    private TCPStats() {
    }

    /**
     * Gets the TCP v4 Stats
     *
     * @return the TCP v4 Stats
     */
    public static Map<TcpProperty, Long> queryTCPv4Stats() {
        PerfCounterQuery<TcpProperty> tcpPerfCounters = new PerfCounterQuery<>(TcpProperty.class, TCP_V4,
                WIN32_PERF_RAW_DATA_TCPIP_TCP_V4);
        return tcpPerfCounters.queryValues();
    }

    /**
     * Gets the TCP v6 Stats
     *
     * @return the TCP v6 Stats
     */
    public static Map<TcpProperty, Long> queryTCPv6Stats() {
        PerfCounterQuery<TcpProperty> tcpPerfCounters = new PerfCounterQuery<>(TcpProperty.class, TCP_V6,
                WIN32_PERF_RAW_DATA_TCPIP_TCP_V6);
        return tcpPerfCounters.queryValues();
    }
}
