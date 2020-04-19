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
public final class UDPStats {

    private static final String UDP_V4 = "UDPv4";
    private static final String UDP_V6 = "UDPv6";
    private static final String WIN32_PERF_RAW_DATA_TCPIP_UDP_V4 = "Win32_PerfRawData_Tcpip_UDPv4";
    private static final String WIN32_PERF_RAW_DATA_TCPIP_UDP_V6 = "Win32_PerfRawData_Tcpip_UDPv6";

    public enum UdpProperty implements PdhCounterProperty {
        DATAGRAMSNOPORTPERSEC(null, "Datagrams No Port/sec"), DATAGRAMSPERSEC(null, "Datagrams/sec"),
        DATAGRAMSRECEIVEDERRORS(null, "Datagrams Received Errors"),
        DATAGRAMSRECEIVEDPERSEC(null, "Datagrams Received/sec"), DATAGRAMSSENTPERSEC(null, "Datagrams Sent/sec");

        private final String instance;
        private final String counter;

        UdpProperty(String instance, String counter) {
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

    private UDPStats() {
    }

    /**
     * Gets the UDP v4 Stats
     *
     * @return the UDP v4 Stats
     */
    public static Map<UdpProperty, Long> queryUDPv4Stats() {
        PerfCounterQuery<UdpProperty> udpPerfCounters = new PerfCounterQuery<>(UdpProperty.class, UDP_V4,
                WIN32_PERF_RAW_DATA_TCPIP_UDP_V4);
        return udpPerfCounters.queryValues();
    }

    /**
     * Gets the UDP v6 Stats
     *
     * @return the UDP v6 Stats
     */
    public static Map<UdpProperty, Long> queryUDPv6Stats() {
        PerfCounterQuery<UdpProperty> udpPerfCounters = new PerfCounterQuery<>(UdpProperty.class, UDP_V6,
                WIN32_PERF_RAW_DATA_TCPIP_UDP_V6);
        return udpPerfCounters.queryValues();
    }
}
