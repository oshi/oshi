/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.InternetProtocolStats.IPConnection;
import oshi.software.os.InternetProtocolStats.TcpStats;
import oshi.software.os.InternetProtocolStats.UdpStats;
import oshi.ffm.windows.Win32Exception;
import oshi.util.ParseUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.foreign.MemoryLayout.PathElement;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.windows.IPHlpAPIFFM.FIXED_INFO_LAYOUT;
import static oshi.ffm.windows.IPHlpAPIFFM.IP_ADDR_STRING_LAYOUT;
import static oshi.ffm.windows.IPHlpAPIFFM.MIB_TCPSTATS_LAYOUT;
import static oshi.ffm.windows.IPHlpAPIFFM.MIB_UDPSTATS_LAYOUT;
import static oshi.ffm.windows.IPHlpAPIFFM.AF_INET;
import static oshi.ffm.windows.IPHlpAPIFFM.AF_INET6;
import static oshi.ffm.windows.IPHlpAPIFFM.TCP_TABLE_OWNER_PID_ALL;
import static oshi.ffm.windows.IPHlpAPIFFM.UDP_TABLE_OWNER_PID;
import static oshi.ffm.windows.IPHlpAPIFFM.MIB_TCPROW_OWNER_PID_LAYOUT;
import static oshi.ffm.windows.IPHlpAPIFFM.MIB_TCP6ROW_OWNER_PID_LAYOUT;
import static oshi.ffm.windows.IPHlpAPIFFM.MIB_UDPROW_OWNER_PID_LAYOUT;
import static oshi.ffm.windows.IPHlpAPIFFM.MIB_UDP6ROW_OWNER_PID_LAYOUT;
import static oshi.ffm.windows.IPHlpAPIFFM.GetExtendedTcpTable;
import static oshi.ffm.windows.IPHlpAPIFFM.GetExtendedUdpTable;
import static oshi.ffm.windows.IPHlpAPIFFM.GetTcpStatisticsEx;
import static oshi.ffm.windows.IPHlpAPIFFM.GetUdpStatisticsEx;
import static oshi.ffm.windows.IPHlpAPIFFM.GetNetworkParams;
import static oshi.ffm.windows.WinErrorFFM.ERROR_SUCCESS;
import static oshi.ffm.windows.WinErrorFFM.ERROR_INSUFFICIENT_BUFFER;
import static oshi.ffm.windows.WinErrorFFM.ERROR_BUFFER_OVERFLOW;
import static oshi.ffm.windows.WindowsForeignFunctions.readAnsiString;
import static oshi.software.os.InternetProtocolStats.TcpState;
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

public class IPHlpAPIUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(IPHlpAPIUtilFFM.class);

    public static String[] getDnsServers() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sizeSegment = arena.allocate(JAVA_INT);
            int ret = GetNetworkParams(MemorySegment.NULL, sizeSegment);
            if (ret != ERROR_BUFFER_OVERFLOW) {
                LOG.error("Failed to get network parameters size. Error: {}", ret);
                return new String[0];
            }

            int size = sizeSegment.get(JAVA_INT, 0);
            MemorySegment buffer = arena.allocate(size);
            ret = GetNetworkParams(buffer, sizeSegment);
            if (ret != 0) {
                LOG.error("Failed to get network parameters. Error: {}", ret);
                return new String[0];
            }

            MemorySegment fixedInfo = buffer.asSlice(0, FIXED_INFO_LAYOUT.byteSize());
            List<String> dnsServers = new ArrayList<>();

            MemorySegment dnsServerList = fixedInfo.asSlice(
                    FIXED_INFO_LAYOUT.byteOffset(PathElement.groupElement("DnsServerList")),
                    IP_ADDR_STRING_LAYOUT.byteSize());

            while (dnsServerList != MemorySegment.NULL) {
                MemorySegment ipStringSeg = dnsServerList
                        .asSlice(IP_ADDR_STRING_LAYOUT.byteOffset(PathElement.groupElement("IpAddress")), 16);

                String ipString = readAnsiString(ipStringSeg, 16);
                if (!ipString.isEmpty()) {
                    dnsServers.add(ipString);
                }

                dnsServerList = dnsServerList.get(ADDRESS,
                        IP_ADDR_STRING_LAYOUT.byteOffset(PathElement.groupElement("Next")));
                if (dnsServerList.address() == 0)
                    break;
            }

            return dnsServers.toArray(new String[0]);
        } catch (Throwable t) {
            LOG.error("GetNetworkParams failed: {}", t.getMessage());
            return new String[0];
        }
    }

    public static TcpStats getTcpStats(int family) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment stats = arena.allocate(MIB_TCPSTATS_LAYOUT);

            int rc = GetTcpStatisticsEx(stats, family);
            if (rc != ERROR_SUCCESS) {
                throw new Win32Exception(rc);
            }

            long connectionsEstablished = stats.get(JAVA_INT,
                    MIB_TCPSTATS_LAYOUT.byteOffset(PathElement.groupElement("dwCurrEstab")));
            long connectionsActive = stats.get(JAVA_INT,
                    MIB_TCPSTATS_LAYOUT.byteOffset(PathElement.groupElement("dwActiveOpens")));
            long connectionsPassive = stats.get(JAVA_INT,
                    MIB_TCPSTATS_LAYOUT.byteOffset(PathElement.groupElement("dwPassiveOpens")));
            long connectionFailures = stats.get(JAVA_INT,
                    MIB_TCPSTATS_LAYOUT.byteOffset(PathElement.groupElement("dwAttemptFails")));
            long connectionsReset = stats.get(JAVA_INT,
                    MIB_TCPSTATS_LAYOUT.byteOffset(PathElement.groupElement("dwEstabResets")));
            long segmentsSent = stats.get(JAVA_INT,
                    MIB_TCPSTATS_LAYOUT.byteOffset(PathElement.groupElement("dwOutSegs")));
            long segmentsReceived = stats.get(JAVA_INT,
                    MIB_TCPSTATS_LAYOUT.byteOffset(PathElement.groupElement("dwInSegs")));
            long segmentsRetransmitted = stats.get(JAVA_INT,
                    MIB_TCPSTATS_LAYOUT.byteOffset(PathElement.groupElement("dwRetransSegs")));
            long inErrors = stats.get(JAVA_INT, MIB_TCPSTATS_LAYOUT.byteOffset(PathElement.groupElement("dwInErrs")));
            long outResets = stats.get(JAVA_INT, MIB_TCPSTATS_LAYOUT.byteOffset(PathElement.groupElement("dwOutRsts")));

            return new TcpStats(connectionsEstablished, connectionsActive, connectionsPassive, connectionFailures,
                    connectionsReset, segmentsSent, segmentsReceived, segmentsRetransmitted, inErrors, outResets);
        } catch (Throwable t) {
            LOG.debug("getTcpStats failed: {}", t.getMessage());
            return null;
        }
    }

    public static UdpStats getUdpStats(int family) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment stats = arena.allocate(MIB_UDPSTATS_LAYOUT);

            int rc = GetUdpStatisticsEx(stats, family);
            if (rc != ERROR_SUCCESS) {
                throw new Win32Exception(rc);
            }

            int outDatagrams = stats.get(JAVA_INT,
                    MIB_UDPSTATS_LAYOUT.byteOffset(PathElement.groupElement("dwOutDatagrams")));
            int inDatagrams = stats.get(JAVA_INT,
                    MIB_UDPSTATS_LAYOUT.byteOffset(PathElement.groupElement("dwInDatagrams")));
            int noPorts = stats.get(JAVA_INT, MIB_UDPSTATS_LAYOUT.byteOffset(PathElement.groupElement("dwNoPorts")));
            int inErrors = stats.get(JAVA_INT, MIB_UDPSTATS_LAYOUT.byteOffset(PathElement.groupElement("dwInErrors")));

            return new UdpStats(outDatagrams, inDatagrams, noPorts, inErrors);
        } catch (Throwable t) {
            LOG.debug("GetUdpStats failed: {}", t.getMessage());
            return null;
        }
    }

    public static List<IPConnection> queryTCPv4Connections() {
        List<IPConnection> conns = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {

            MemorySegment sizeSegment = arena.allocate(JAVA_INT);
            int ret = GetExtendedTcpTable(MemorySegment.NULL, sizeSegment, 0, AF_INET, TCP_TABLE_OWNER_PID_ALL, 0);

            if (ret != ERROR_INSUFFICIENT_BUFFER && ret != ERROR_SUCCESS) {
                throw new Win32Exception(ret);
            }

            int size = sizeSegment.get(JAVA_INT, 0);
            MemorySegment buffer = arena.allocate(size);

            ret = GetExtendedTcpTable(buffer, sizeSegment, 0, AF_INET, TCP_TABLE_OWNER_PID_ALL, 0);
            if (ret != ERROR_SUCCESS) {
                throw new Win32Exception(ret);
            }

            int numEntries = buffer.get(JAVA_INT, 0);
            long entryOffset = JAVA_INT.byteSize();

            for (int i = 0; i < numEntries; i++) {
                MemorySegment rowSeg = buffer.asSlice(entryOffset + i * MIB_TCPROW_OWNER_PID_LAYOUT.byteSize(),
                        MIB_TCPROW_OWNER_PID_LAYOUT.byteSize());

                int localAddr = rowSeg.get(JAVA_INT,
                        MIB_TCPROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwLocalAddr")));
                int localPortBE = rowSeg.get(JAVA_INT,
                        MIB_TCPROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwLocalPort")));
                int remoteAddr = rowSeg.get(JAVA_INT,
                        MIB_TCPROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwRemoteAddr")));
                int remotePortBE = rowSeg.get(JAVA_INT,
                        MIB_TCPROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwRemotePort")));
                int state = rowSeg.get(JAVA_INT,
                        MIB_TCPROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwState")));
                int pid = rowSeg.get(JAVA_INT,
                        MIB_TCPROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwOwningPid")));

                conns.add(new IPConnection("tcp4", ParseUtil.parseIntToIP(localAddr),
                        ParseUtil.bigEndian16ToLittleEndian(localPortBE), ParseUtil.parseIntToIP(remoteAddr),
                        ParseUtil.bigEndian16ToLittleEndian(remotePortBE), stateLookup(state), 0, 0, pid));
            }
            return conns;
        } catch (Throwable t) {
            LOG.debug("queryTCPv4Connections failed: {}", t.getMessage());
            return Collections.emptyList();
        }
    }

    public static List<IPConnection> queryTCPv6Connections() {
        List<IPConnection> conns = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {

            MemorySegment sizeSegment = arena.allocate(JAVA_INT);
            int ret = GetExtendedTcpTable(MemorySegment.NULL, sizeSegment, 0, AF_INET6, TCP_TABLE_OWNER_PID_ALL, 0);

            if (ret != ERROR_INSUFFICIENT_BUFFER && ret != ERROR_SUCCESS) {
                throw new Win32Exception(ret);
            }

            int size = sizeSegment.get(JAVA_INT, 0);
            MemorySegment buffer = arena.allocate(size);

            ret = GetExtendedTcpTable(buffer, sizeSegment, 0, AF_INET, TCP_TABLE_OWNER_PID_ALL, 0);
            if (ret != ERROR_SUCCESS) {
                throw new Win32Exception(ret);
            }

            int numEntries = buffer.get(JAVA_INT, 0);
            long entryOffset = JAVA_INT.byteSize();

            for (int i = 0; i < numEntries; i++) {
                MemorySegment rowSeg = buffer.asSlice(entryOffset + i * MIB_TCP6ROW_OWNER_PID_LAYOUT.byteSize(),
                        MIB_TCP6ROW_OWNER_PID_LAYOUT.byteSize());

                byte[] localAddr = new byte[16];
                MemorySegment localAddrSeg = rowSeg.asSlice(
                    MIB_TCP6ROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("ucLocalAddr")), 16
                );
                for (int b = 0; b < 16; b++) {
                    localAddr[b] = localAddrSeg.get(JAVA_BYTE, b);
                }
                int localPortBE = rowSeg.get(JAVA_INT,
                        MIB_TCP6ROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwLocalPort")));
                byte[] remoteAddr = new byte[16];
                MemorySegment remoteAddrSeg = rowSeg.asSlice(
                    MIB_TCP6ROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("ucRemoteAddr")), 16
                );
                for (int b = 0; b < 16; b++) {
                    remoteAddr[b] = remoteAddrSeg.get(JAVA_BYTE, b);
                }
                int remotePortBE = rowSeg.get(JAVA_INT,
                        MIB_TCP6ROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwRemotePort")));
                int state = rowSeg.get(JAVA_INT,
                        MIB_TCP6ROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwState")));
                int pid = rowSeg.get(JAVA_INT,
                        MIB_TCP6ROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwOwningPid")));

                conns.add(new IPConnection("tcp6", localAddr,
                        ParseUtil.bigEndian16ToLittleEndian(localPortBE), remoteAddr,
                        ParseUtil.bigEndian16ToLittleEndian(remotePortBE), stateLookup(state), 0, 0, pid));
            }
            return conns;
        } catch (Throwable t) {
            LOG.debug("queryTCPv6Connections failed: {}", t.getMessage());
            return Collections.emptyList();
        }
    }

    public static List<IPConnection> queryUDPv4Connections() {
        List<IPConnection> conns = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sizeSegment = arena.allocate(JAVA_INT);
            int ret = GetExtendedUdpTable(MemorySegment.NULL, sizeSegment, 0, AF_INET, UDP_TABLE_OWNER_PID, 0);

            if (ret != ERROR_INSUFFICIENT_BUFFER && ret != ERROR_SUCCESS) {
                throw new Win32Exception(ret);
            }

            int size = sizeSegment.get(JAVA_INT, 0);
            MemorySegment buffer = arena.allocate(size);

            ret = GetExtendedUdpTable(buffer, sizeSegment, 0, AF_INET, UDP_TABLE_OWNER_PID, 0);
            if (ret != ERROR_SUCCESS) {
                throw new Win32Exception(ret);
            }

            int numEntries = buffer.get(JAVA_INT, 0);
            long entryOffset = JAVA_INT.byteSize();

            for (int i = 0; i < numEntries; i++) {
                MemorySegment rowSeg = buffer.asSlice(entryOffset + i * MIB_UDPROW_OWNER_PID_LAYOUT.byteSize(),
                        MIB_UDPROW_OWNER_PID_LAYOUT.byteSize());

                int localAddr = rowSeg.get(JAVA_INT,
                        MIB_UDPROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwLocalAddr")));
                int localPortBE = rowSeg.get(JAVA_INT,
                        MIB_UDPROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwLocalPort")));
                int pid = rowSeg.get(JAVA_INT,
                        MIB_UDPROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwOwningPid")));

                conns.add(new IPConnection("udp4", ParseUtil.parseIntToIP(localAddr),
                        ParseUtil.bigEndian16ToLittleEndian(localPortBE), new byte[0], 0, TcpState.NONE, 0, 0, pid));
            }
            return conns;
        } catch (Throwable t) {
            LOG.debug("queryUDPv4Connections failed: {}", t.getMessage());
            return Collections.emptyList();
        }
    }

    public static List<IPConnection> queryUDPv6Connections() {
        List<IPConnection> conns = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sizeSegment = arena.allocate(JAVA_INT);
            int ret = GetExtendedUdpTable(MemorySegment.NULL, sizeSegment, 0, AF_INET6, UDP_TABLE_OWNER_PID, 0);

            if (ret != ERROR_INSUFFICIENT_BUFFER && ret != ERROR_SUCCESS) {
                throw new Win32Exception(ret);
            }

            int size = sizeSegment.get(JAVA_INT, 0);
            MemorySegment buffer = arena.allocate(size);

            ret = GetExtendedUdpTable(buffer, sizeSegment, 0, AF_INET, UDP_TABLE_OWNER_PID, 0);
            if (ret != ERROR_SUCCESS) {
                throw new Win32Exception(ret);
            }

            int numEntries = buffer.get(JAVA_INT, 0);
            long entryOffset = JAVA_INT.byteSize();

            for (int i = 0; i < numEntries; i++) {
                MemorySegment rowSeg = buffer.asSlice(entryOffset + i * MIB_UDP6ROW_OWNER_PID_LAYOUT.byteSize(),
                        MIB_UDP6ROW_OWNER_PID_LAYOUT.byteSize());

                byte[] localAddr = new byte[16];
                MemorySegment localAddrSeg = rowSeg.asSlice(
                    MIB_TCP6ROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("ucLocalAddr")), 16
                );
                for (int b = 0; b < 16; b++) {
                    localAddr[b] = localAddrSeg.get(JAVA_BYTE, b);
                }
                int localPortBE = rowSeg.get(JAVA_INT,
                        MIB_UDP6ROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwLocalPort")));
                int pid = rowSeg.get(JAVA_INT,
                        MIB_UDP6ROW_OWNER_PID_LAYOUT.byteOffset(PathElement.groupElement("dwOwningPid")));

                conns.add(new IPConnection("udp6", localAddr,
                        ParseUtil.bigEndian16ToLittleEndian(localPortBE), new byte[0], 0, TcpState.NONE, 0, 0, pid));
            }
            return conns;
        } catch (Throwable t) {
            LOG.debug("queryUDPv6Connections failed: {}", t.getMessage());
            return Collections.emptyList();
        }
    }

    private static InternetProtocolStats.TcpState stateLookup(int state) {
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
