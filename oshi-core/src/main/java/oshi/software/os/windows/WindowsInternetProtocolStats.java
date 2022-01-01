/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static com.sun.jna.platform.win32.IPHlpAPI.AF_INET; // NOSONAR squid:S1191
import static com.sun.jna.platform.win32.IPHlpAPI.AF_INET6;
import static com.sun.jna.platform.win32.IPHlpAPI.TCP_TABLE_CLASS.TCP_TABLE_OWNER_PID_ALL;
import static com.sun.jna.platform.win32.IPHlpAPI.UDP_TABLE_CLASS.UDP_TABLE_OWNER_PID;
import static oshi.software.os.InternetProtocolStats.TcpState.CLOSED;
import static oshi.software.os.InternetProtocolStats.TcpState.CLOSE_WAIT;
import static oshi.software.os.InternetProtocolStats.TcpState.CLOSING;
import static oshi.software.os.InternetProtocolStats.TcpState.ESTABLISHED;
import static oshi.software.os.InternetProtocolStats.TcpState.FIN_WAIT_1;
import static oshi.software.os.InternetProtocolStats.TcpState.FIN_WAIT_2;
import static oshi.software.os.InternetProtocolStats.TcpState.LAST_ACK;
import static oshi.software.os.InternetProtocolStats.TcpState.LISTEN;
import static oshi.software.os.InternetProtocolStats.TcpState.SYN_RECV;
import static oshi.software.os.InternetProtocolStats.TcpState.SYN_SENT;
import static oshi.software.os.InternetProtocolStats.TcpState.TIME_WAIT;
import static oshi.software.os.InternetProtocolStats.TcpState.UNKNOWN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.jna.Memory; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.IPHlpAPI;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_TCP6ROW_OWNER_PID;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_TCP6TABLE_OWNER_PID;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_TCPROW_OWNER_PID;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_TCPSTATS;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_TCPTABLE_OWNER_PID;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_UDP6ROW_OWNER_PID;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_UDP6TABLE_OWNER_PID;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_UDPROW_OWNER_PID;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_UDPSTATS;
import com.sun.jna.platform.win32.IPHlpAPI.MIB_UDPTABLE_OWNER_PID;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.ptr.IntByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.util.ParseUtil;

/**
 * Internet Protocol Stats implementation
 */
@ThreadSafe
public class WindowsInternetProtocolStats extends AbstractInternetProtocolStats {

    private static final IPHlpAPI IPHLP = IPHlpAPI.INSTANCE;

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

    @Override
    public TcpStats getTCPv4Stats() {
        MIB_TCPSTATS stats = new MIB_TCPSTATS();
        IPHLP.GetTcpStatisticsEx(stats, AF_INET);
        return new TcpStats(stats.dwCurrEstab, stats.dwActiveOpens, stats.dwPassiveOpens, stats.dwAttemptFails,
                stats.dwEstabResets, stats.dwOutSegs, stats.dwInSegs, stats.dwRetransSegs, stats.dwInErrs,
                stats.dwOutRsts);
    }

    @Override
    public TcpStats getTCPv6Stats() {
        MIB_TCPSTATS stats = new MIB_TCPSTATS();
        IPHLP.GetTcpStatisticsEx(stats, AF_INET6);
        return new TcpStats(stats.dwCurrEstab, stats.dwActiveOpens, stats.dwPassiveOpens, stats.dwAttemptFails,
                stats.dwEstabResets, stats.dwOutSegs, stats.dwInSegs, stats.dwRetransSegs, stats.dwInErrs,
                stats.dwOutRsts);
    }

    @Override
    public UdpStats getUDPv4Stats() {
        MIB_UDPSTATS stats = new MIB_UDPSTATS();
        IPHLP.GetUdpStatisticsEx(stats, AF_INET);
        return new UdpStats(stats.dwOutDatagrams, stats.dwInDatagrams, stats.dwNoPorts, stats.dwInErrors);
    }

    @Override
    public UdpStats getUDPv6Stats() {
        MIB_UDPSTATS stats = new MIB_UDPSTATS();
        IPHLP.GetUdpStatisticsEx(stats, AF_INET6);
        return new UdpStats(stats.dwOutDatagrams, stats.dwInDatagrams, stats.dwNoPorts, stats.dwInErrors);
    }

    @Override
    public List<IPConnection> getConnections() {
        if (IS_VISTA_OR_GREATER) {
            List<IPConnection> conns = new ArrayList<>();
            conns.addAll(queryTCPv4Connections());
            conns.addAll(queryTCPv6Connections());
            conns.addAll(queryUDPv4Connections());
            conns.addAll(queryUDPv6Connections());
            return conns;
        }
        return Collections.emptyList();
    }

    private static List<IPConnection> queryTCPv4Connections() {
        List<IPConnection> conns = new ArrayList<>();
        // Get size needed
        IntByReference sizePtr = new IntByReference();
        IPHLP.GetExtendedTcpTable(null, sizePtr, false, AF_INET, TCP_TABLE_OWNER_PID_ALL, 0);
        // Get buffer and populate table
        int size;
        Memory buf;
        do {
            size = sizePtr.getValue();
            buf = new Memory(size);
            IPHLP.GetExtendedTcpTable(buf, sizePtr, false, AF_INET, TCP_TABLE_OWNER_PID_ALL, 0);
            // In case size changes and buffer was too small, repeat
        } while (size < sizePtr.getValue());
        MIB_TCPTABLE_OWNER_PID tcpTable = new MIB_TCPTABLE_OWNER_PID(buf);
        for (int i = 0; i < tcpTable.dwNumEntries; i++) {
            MIB_TCPROW_OWNER_PID row = tcpTable.table[i];
            conns.add(new IPConnection("tcp4", ParseUtil.parseIntToIP(row.dwLocalAddr),
                    ParseUtil.bigEndian16ToLittleEndian(row.dwLocalPort), ParseUtil.parseIntToIP(row.dwRemoteAddr),
                    ParseUtil.bigEndian16ToLittleEndian(row.dwRemotePort), stateLookup(row.dwState), 0, 0,
                    row.dwOwningPid));
        }
        return conns;
    }

    private static List<IPConnection> queryTCPv6Connections() {
        List<IPConnection> conns = new ArrayList<>();
        // Get size needed
        IntByReference sizePtr = new IntByReference();
        IPHLP.GetExtendedTcpTable(null, sizePtr, false, AF_INET6, TCP_TABLE_OWNER_PID_ALL, 0);
        // Get buffer and populate table
        int size;
        Memory buf;
        do {
            size = sizePtr.getValue();
            buf = new Memory(size);
            IPHLP.GetExtendedTcpTable(buf, sizePtr, false, AF_INET6, TCP_TABLE_OWNER_PID_ALL, 0);
            // In case size changes and buffer was too small, repeat
        } while (size < sizePtr.getValue());
        MIB_TCP6TABLE_OWNER_PID tcpTable = new MIB_TCP6TABLE_OWNER_PID(buf);
        for (int i = 0; i < tcpTable.dwNumEntries; i++) {
            MIB_TCP6ROW_OWNER_PID row = tcpTable.table[i];
            conns.add(new IPConnection("tcp6", row.LocalAddr, ParseUtil.bigEndian16ToLittleEndian(row.dwLocalPort),
                    row.RemoteAddr, ParseUtil.bigEndian16ToLittleEndian(row.dwRemotePort), stateLookup(row.State), 0, 0,
                    row.dwOwningPid));
        }
        return conns;
    }

    private static List<IPConnection> queryUDPv4Connections() {
        List<IPConnection> conns = new ArrayList<>();
        // Get size needed
        IntByReference sizePtr = new IntByReference();
        IPHLP.GetExtendedUdpTable(null, sizePtr, false, AF_INET, UDP_TABLE_OWNER_PID, 0);
        // Get buffer and populate table
        int size;
        Memory buf;
        do {
            size = sizePtr.getValue();
            buf = new Memory(size);
            IPHLP.GetExtendedUdpTable(buf, sizePtr, false, AF_INET, UDP_TABLE_OWNER_PID, 0);
            // In case size changes and buffer was too small, repeat
        } while (size < sizePtr.getValue());
        MIB_UDPTABLE_OWNER_PID udpTable = new MIB_UDPTABLE_OWNER_PID(buf);
        for (int i = 0; i < udpTable.dwNumEntries; i++) {
            MIB_UDPROW_OWNER_PID row = udpTable.table[i];
            conns.add(new IPConnection("udp4", ParseUtil.parseIntToIP(row.dwLocalAddr),
                    ParseUtil.bigEndian16ToLittleEndian(row.dwLocalPort), new byte[0], 0, TcpState.NONE, 0, 0,
                    row.dwOwningPid));
        }
        return conns;
    }

    private static List<IPConnection> queryUDPv6Connections() {
        List<IPConnection> conns = new ArrayList<>();
        // Get size needed
        IntByReference sizePtr = new IntByReference();
        IPHLP.GetExtendedUdpTable(null, sizePtr, false, AF_INET6, UDP_TABLE_OWNER_PID, 0);
        // Get buffer and populate table
        int size;
        Memory buf;
        do {
            size = sizePtr.getValue();
            buf = new Memory(size);
            IPHLP.GetExtendedUdpTable(buf, sizePtr, false, AF_INET6, UDP_TABLE_OWNER_PID, 0);
            // In case size changes and buffer was too small, repeat
        } while (size < sizePtr.getValue());
        MIB_UDP6TABLE_OWNER_PID udpTable = new MIB_UDP6TABLE_OWNER_PID(buf);
        for (int i = 0; i < udpTable.dwNumEntries; i++) {
            MIB_UDP6ROW_OWNER_PID row = udpTable.table[i];
            conns.add(new IPConnection("udp6", row.ucLocalAddr, ParseUtil.bigEndian16ToLittleEndian(row.dwLocalPort),
                    new byte[0], 0, TcpState.NONE, 0, 0, row.dwOwningPid));
        }
        return conns;
    }

    private static TcpState stateLookup(int state) {
        switch (state) {
        case 1:
        case 12:
            return CLOSED;
        case 2:
            return LISTEN;
        case 3:
            return SYN_SENT;
        case 4:
            return SYN_RECV;
        case 5:
            return ESTABLISHED;
        case 6:
            return FIN_WAIT_1;
        case 7:
            return FIN_WAIT_2;
        case 8:
            return CLOSE_WAIT;
        case 9:
            return CLOSING;
        case 10:
            return LAST_ACK;
        case 11:
            return TIME_WAIT;
        default:
            return UNKNOWN;
        }
    }
}
