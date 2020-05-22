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
package oshi.jna.platform.windows;

import com.sun.jna.Native; // NOSONAR squid:S1192
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.win32.W32APIOptions;

public interface IPHlpAPI extends com.sun.jna.platform.win32.IPHlpAPI {

    IPHlpAPI INSTANCE = Native.load("iphlpapi", IPHlpAPI.class, W32APIOptions.DEFAULT_OPTIONS);

    int AF_INET = 2; // Internet IP Protocol.
    int AF_INET6 = 23; // IP version 6.

    /**
     * The MIB_TCPSTATS structure contains statistics for the TCP protocol running
     * on the local computer.
     * <p>
     * In the Windows SDK, the version of the structure for use on Windows Vista and
     * later is defined as MIB_TCPSTATS_LH. In the Windows SDK, the version of this
     * structure to be used on earlier systems including Windows 2000 and later is
     * defined as MIB_TCPSTATS_W2K.
     */
    @FieldOrder({ "dwRtoAlgorithm", "dwRtoMin", "dwRtoMax", "dwMaxConn", "dwActiveOpens", "dwPassiveOpens",
            "dwAttemptFails", "dwEstabResets", "dwCurrEstab", "dwInSegs", "dwOutSegs", "dwRetransSegs", "dwInErrs",
            "dwOutRsts", "dwNumConns" })
    class MIB_TCPSTATS extends Structure {
        public int dwRtoAlgorithm; // Union for _W2K version, doesn't change mapping
        public int dwRtoMin;
        public int dwRtoMax;
        public int dwMaxConn;
        public int dwActiveOpens;
        public int dwPassiveOpens;
        public int dwAttemptFails;
        public int dwEstabResets;
        public int dwCurrEstab;
        public int dwInSegs;
        public int dwOutSegs;
        public int dwRetransSegs;
        public int dwInErrs;
        public int dwOutRsts;
        public int dwNumConns;
    }

    /**
     * The MIB_UDPSTATS structure contains statistics for the User Datagram Protocol
     * (UDP) running on the local computer.
     */
    @FieldOrder({ "dwInDatagrams", "dwNoPorts", "dwInErrors", "dwOutDatagrams", "dwNumAddrs" })
    class MIB_UDPSTATS extends Structure {
        public int dwInDatagrams;
        public int dwNoPorts;
        public int dwInErrors;
        public int dwOutDatagrams;
        public int dwNumAddrs;
    }

    /**
     * The GetTcpStatistics function retrieves the TCP statistics for the local
     * computer.
     *
     * @param Statistics
     *            A {@link MIB_TCPSTATS} structure that receives the TCP statistics
     *            for the local computer.
     * @return If the function succeeds, the return value is {@code NO_ERROR}.
     */
    int GetTcpStatistics(MIB_TCPSTATS Statistics);

    /**
     * The GetTcpStatisticsEx function retrieves the Transmission Control Protocol
     * (TCP) statistics for the current computer. The GetTcpStatisticsEx function
     * differs from the {@link #GetTcpStatistics} function in that
     * GetTcpStatisticsEx also supports the Internet Protocol version 6 (IPv6)
     * protocol family.
     *
     * @param Statistics
     *            A {@link MIB_TCPSTATS} structure that receives the TCP statistics
     *            for the local computer.
     * @param Family
     *            The protocol family for which to retrieve statistics. This
     *            parameter must be {@link #AF_INET} or {@link #AF_INET6}.
     * @return If the function succeeds, the return value is
     *         {@link WinError#NO_ERROR}.
     */
    int GetTcpStatisticsEx(MIB_TCPSTATS Statistics, int Family);

    /**
     * The GetUdpStatistics function retrieves the User Datagram Protocol (UDP)
     * statistics for the local computer.
     *
     * @param Stats
     *            A {@link MIB_UDPSTATS} structure that receives the UDP statistics
     *            for the local computer.
     * @return If the function succeeds, the return value is
     *         {@link WinError#NO_ERROR}.
     */
    int GetUdpStatistics(MIB_UDPSTATS Stats);

    /**
     * The GetUdpStatisticsEx function retrieves the User Datagram Protocol (UDP)
     * statistics for the current computer. The GetUdpStatisticsEx function differs
     * from the {@link #GetUdpStatistics} function in that GetUdpStatisticsEx also
     * supports the Internet Protocol version 6 (IPv6) protocol family.
     *
     * @param Statistics
     *            A {@link MIB_UDPSTATS} structure that receives the UDP statistics
     *            for the local computer.
     * @param Family
     *            The protocol family for which to retrieve statistics. This
     *            parameter must be {@link #AF_INET} or {@link #AF_INET6}.
     * @return If the function succeeds, the return value is
     *         {@link WinError#NO_ERROR}.
     */
    int GetUdpStatisticsEx(MIB_UDPSTATS Statistics, int Family);
}
