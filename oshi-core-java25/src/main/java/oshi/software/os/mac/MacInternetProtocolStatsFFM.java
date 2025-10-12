/*
 * Copyright 2020-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static oshi.ffm.mac.MacSystem.AF_INET;
import static oshi.ffm.mac.MacSystem.AF_INET6;
import static oshi.ffm.mac.MacSystem.INSI_FADDR;
import static oshi.ffm.mac.MacSystem.INSI_FPORT;
import static oshi.ffm.mac.MacSystem.INSI_LADDR;
import static oshi.ffm.mac.MacSystem.INSI_LPORT;
import static oshi.ffm.mac.MacSystem.INSI_VFLAG;
import static oshi.ffm.mac.MacSystem.IN_SOCK_INFO;
import static oshi.ffm.mac.MacSystem.PROC_ALL_PIDS;
import static oshi.ffm.mac.MacSystem.PROC_FD;
import static oshi.ffm.mac.MacSystem.PROC_FDTYPE;
import static oshi.ffm.mac.MacSystem.PROC_FD_INFO;
import static oshi.ffm.mac.MacSystem.PROC_PIDFDSOCKETINFO;
import static oshi.ffm.mac.MacSystem.PROC_PIDLISTFDS;
import static oshi.ffm.mac.MacSystem.PROX_FDTYPE_SOCKET;
import static oshi.ffm.mac.MacSystem.PSI;
import static oshi.ffm.mac.MacSystem.SOCKET_FD_INFO;
import static oshi.ffm.mac.MacSystem.SOCKET_INFO;
import static oshi.ffm.mac.MacSystem.SOCKINFO_IN;
import static oshi.ffm.mac.MacSystem.SOCKINFO_TCP;
import static oshi.ffm.mac.MacSystem.SOI_FAMILY;
import static oshi.ffm.mac.MacSystem.SOI_INCQLEN;
import static oshi.ffm.mac.MacSystem.SOI_KIND;
import static oshi.ffm.mac.MacSystem.SOI_PROTO;
import static oshi.ffm.mac.MacSystem.SOI_QLEN;
import static oshi.ffm.mac.MacSystem.TCPSI_INI;
import static oshi.ffm.mac.MacSystem.TCPSI_STATE;
import static oshi.ffm.mac.MacSystem.TCP_SOCK_INFO;
import static oshi.ffm.mac.MacSystemFunctions.proc_listpids;
import static oshi.ffm.mac.MacSystemFunctions.proc_pidfdinfo;
import static oshi.ffm.mac.MacSystemFunctions.proc_pidinfo;
import static oshi.software.os.InternetProtocolStats.TcpState.CLOSED;
import static oshi.software.os.InternetProtocolStats.TcpState.CLOSE_WAIT;
import static oshi.software.os.InternetProtocolStats.TcpState.CLOSING;
import static oshi.software.os.InternetProtocolStats.TcpState.ESTABLISHED;
import static oshi.software.os.InternetProtocolStats.TcpState.FIN_WAIT_1;
import static oshi.software.os.InternetProtocolStats.TcpState.FIN_WAIT_2;
import static oshi.software.os.InternetProtocolStats.TcpState.LAST_ACK;
import static oshi.software.os.InternetProtocolStats.TcpState.LISTEN;
import static oshi.software.os.InternetProtocolStats.TcpState.NONE;
import static oshi.software.os.InternetProtocolStats.TcpState.SYN_RECV;
import static oshi.software.os.InternetProtocolStats.TcpState.SYN_SENT;
import static oshi.software.os.InternetProtocolStats.TcpState.TIME_WAIT;
import static oshi.software.os.InternetProtocolStats.TcpState.UNKNOWN;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;
import static oshi.util.ParseUtil.parseIntToIP;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.NetStat;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtilFFM;
import oshi.util.tuples.Pair;

/**
 * Internet Protocol Stats implementation
 */
@ThreadSafe
public class MacInternetProtocolStatsFFM extends AbstractInternetProtocolStats {

    private boolean isElevated;

    public MacInternetProtocolStatsFFM(boolean elevated) {
        this.isElevated = elevated;
    }

    private Supplier<Pair<Long, Long>> establishedv4v6 = memoize(NetStat::queryTcpnetstat, defaultExpiration());
    private Supplier<BsdTcpstat> tcpstat = memoize(MacInternetProtocolStatsFFM::queryTcpstat, defaultExpiration());
    private Supplier<BsdUdpstat> udpstat = memoize(MacInternetProtocolStatsFFM::queryUdpstat, defaultExpiration());
    // With elevated permissions use tcpstat only
    // Backup estimate get ipstat and subtract off udp
    private Supplier<BsdIpstat> ipstat = memoize(MacInternetProtocolStatsFFM::queryIpstat, defaultExpiration());
    private Supplier<BsdIp6stat> ip6stat = memoize(MacInternetProtocolStatsFFM::queryIp6stat, defaultExpiration());

    @Override
    public TcpStats getTCPv4Stats() {
        BsdTcpstat tcp = tcpstat.get();
        if (this.isElevated) {
            return new TcpStats(establishedv4v6.get().getA(), ParseUtil.unsignedIntToLong(tcp.tcps_connattempt),
                    ParseUtil.unsignedIntToLong(tcp.tcps_accepts), ParseUtil.unsignedIntToLong(tcp.tcps_conndrops),
                    ParseUtil.unsignedIntToLong(tcp.tcps_drops), ParseUtil.unsignedIntToLong(tcp.tcps_sndpack),
                    ParseUtil.unsignedIntToLong(tcp.tcps_rcvpack), ParseUtil.unsignedIntToLong(tcp.tcps_sndrexmitpack),
                    ParseUtil.unsignedIntToLong(
                            tcp.tcps_rcvbadsum + tcp.tcps_rcvbadoff + tcp.tcps_rcvmemdrop + tcp.tcps_rcvshort),
                    0L);
        }
        BsdIpstat ip = ipstat.get();
        BsdUdpstat udp = udpstat.get();
        return new TcpStats(establishedv4v6.get().getA(), ParseUtil.unsignedIntToLong(tcp.tcps_connattempt),
                ParseUtil.unsignedIntToLong(tcp.tcps_accepts), ParseUtil.unsignedIntToLong(tcp.tcps_conndrops),
                ParseUtil.unsignedIntToLong(tcp.tcps_drops),
                Math.max(0L, ParseUtil.unsignedIntToLong(ip.ips_delivered - udp.udps_opackets)),
                Math.max(0L, ParseUtil.unsignedIntToLong(ip.ips_total - udp.udps_ipackets)),
                ParseUtil.unsignedIntToLong(tcp.tcps_sndrexmitpack),
                Math.max(0L, ParseUtil.unsignedIntToLong(ip.ips_badsum + ip.ips_tooshort + ip.ips_toosmall
                        + ip.ips_badhlen + ip.ips_badlen - udp.udps_hdrops + udp.udps_badsum + udp.udps_badlen)),
                0L);
    }

    @Override
    public TcpStats getTCPv6Stats() {
        BsdIp6stat ip6 = ip6stat.get();
        BsdUdpstat udp = udpstat.get();
        return new TcpStats(establishedv4v6.get().getB(), 0L, 0L, 0L, 0L,
                ip6.ip6s_localout - ParseUtil.unsignedIntToLong(udp.udps_snd6_swcsum),
                ip6.ip6s_total - ParseUtil.unsignedIntToLong(udp.udps_rcv6_swcsum), 0L, 0L, 0L);
    }

    @Override
    public UdpStats getUDPv4Stats() {
        BsdUdpstat stat = udpstat.get();
        return new UdpStats(ParseUtil.unsignedIntToLong(stat.udps_opackets),
                ParseUtil.unsignedIntToLong(stat.udps_ipackets), ParseUtil.unsignedIntToLong(stat.udps_noportmcast),
                ParseUtil.unsignedIntToLong(stat.udps_hdrops + stat.udps_badsum + stat.udps_badlen));
    }

    @Override
    public UdpStats getUDPv6Stats() {
        BsdUdpstat stat = udpstat.get();
        return new UdpStats(ParseUtil.unsignedIntToLong(stat.udps_snd6_swcsum),
                ParseUtil.unsignedIntToLong(stat.udps_rcv6_swcsum), 0L, 0L);
    }

    @Override
    public List<IPConnection> getConnections() {
        List<IPConnection> conns = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            int numProcs = proc_listpids(PROC_ALL_PIDS, 0, MemorySegment.NULL, 0) / Integer.BYTES;
            if (numProcs <= 0) {
                return conns;
            }
            MemorySegment pidBuffer = arena.allocate(numProcs * Integer.BYTES);
            int bytes = proc_listpids(PROC_ALL_PIDS, 0, pidBuffer, (int) pidBuffer.byteSize());
            numProcs = bytes / Integer.BYTES;

            for (int i = 0; i < numProcs; i++) {
                // Handle off-by-one bug in proc_listpids where the size returned
                // is: SystemB.INT_SIZE * (pids + 1)
                int pid = pidBuffer.get(JAVA_INT, i);
                if (pid > 0) {
                    for (Integer fd : queryFdList(pid)) {
                        IPConnection ipc = queryIPConnection(pid, fd);
                        if (ipc != null) {
                            conns.add(ipc);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            return conns;
        }
        return conns;
    }

    private static List<Integer> queryFdList(int pid) {
        List<Integer> fdList = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            int bufferSize = proc_pidinfo(pid, PROC_PIDLISTFDS, 0, MemorySegment.NULL, 0);
            if (bufferSize > 0) {
                MemorySegment buffer = arena.allocate(bufferSize);
                bufferSize = proc_pidinfo(pid, PROC_PIDLISTFDS, 0, buffer, bufferSize);
                int structSize = (int) PROC_FD_INFO.byteSize();
                int numStructs = bufferSize / structSize;
                for (int i = 0; i < numStructs; i++) {
                    MemorySegment fdInfo = buffer.asSlice(i * structSize, structSize);
                    int fdType = fdInfo.get(JAVA_INT, PROC_FD_INFO.byteOffset(PROC_FDTYPE));
                    if (fdType == PROX_FDTYPE_SOCKET) {
                        int fd = fdInfo.get(JAVA_INT, PROC_FD_INFO.byteOffset(PROC_FD));
                        fdList.add(fd);
                    }
                }
            }
        } catch (Throwable e) {
            return fdList;
        }
        return fdList;
    }

    private static IPConnection queryIPConnection(int pid, int fd) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment socketInfo = arena.allocate(SOCKET_FD_INFO);
            int ret = proc_pidfdinfo(pid, fd, PROC_PIDFDSOCKETINFO, socketInfo, (int) SOCKET_FD_INFO.byteSize());
            if (ret != SOCKET_FD_INFO.byteSize()) {
                return null;
            }

            MemorySegment psi = socketInfo.asSlice(SOCKET_FD_INFO.byteOffset(PSI));
            int family = psi.get(JAVA_INT, SOCKET_INFO.byteOffset(SOI_FAMILY));
            if (family != AF_INET && family != AF_INET6) {
                return null;
            }

            String type;
            TcpState state;
            MemorySegment ini;

            long protoOffset = SOCKET_INFO.byteOffset(SOI_PROTO);
            int kind = psi.get(JAVA_INT, SOCKET_INFO.byteOffset(SOI_KIND));
            if (kind == SOCKINFO_TCP) {
                type = "tcp";
                MemorySegment tcpsi = psi.asSlice(protoOffset);
                ini = tcpsi.asSlice(TCP_SOCK_INFO.byteOffset(TCPSI_INI));
                int tcpState = tcpsi.get(JAVA_INT, TCP_SOCK_INFO.byteOffset(TCPSI_STATE));
                state = stateLookup(tcpState);
            } else if (kind == SOCKINFO_IN) {
                type = "udp";
                ini = psi.asSlice(protoOffset);
                state = NONE;
            } else {
                return null;
            }

            byte[] laddr;
            byte[] faddr;

            byte vflag = ini.get(JAVA_BYTE, IN_SOCK_INFO.byteOffset(INSI_VFLAG));
            if (vflag == 1) { // IPv4
                laddr = parseIntToIP(ini.getAtIndex(JAVA_INT, IN_SOCK_INFO.byteOffset(INSI_LADDR) / 4 + 3));
                faddr = parseIntToIP(ini.getAtIndex(JAVA_INT, IN_SOCK_INFO.byteOffset(INSI_FADDR) / 4 + 3));
                type += "4";
            } else if (vflag == 2) { // IPv6
                laddr = parseIntArrayToIP(ini, IN_SOCK_INFO.byteOffset(INSI_LADDR));
                faddr = parseIntArrayToIP(ini, IN_SOCK_INFO.byteOffset(INSI_FADDR));
                type += "6";
            } else if (vflag == 3) { // IPv4/IPv6
                laddr = parseIntToIP(ini.getAtIndex(JAVA_INT, IN_SOCK_INFO.byteOffset(INSI_LADDR) / 4 + 3));
                faddr = parseIntToIP(ini.getAtIndex(JAVA_INT, IN_SOCK_INFO.byteOffset(INSI_FADDR) / 4 + 3));
                type += "46";
            } else {
                return null;
            }

            int lport = ParseUtil.bigEndian16ToLittleEndian(ini.get(JAVA_INT, IN_SOCK_INFO.byteOffset(INSI_LPORT)));
            int fport = ParseUtil.bigEndian16ToLittleEndian(ini.get(JAVA_INT, IN_SOCK_INFO.byteOffset(INSI_FPORT)));

            int qlen = psi.get(JAVA_SHORT, SOCKET_INFO.byteOffset(SOI_QLEN));
            int incqlen = psi.get(JAVA_SHORT, SOCKET_INFO.byteOffset(SOI_INCQLEN));

            return new IPConnection(type, laddr, lport, faddr, fport, state, qlen, incqlen, pid);
        } catch (Throwable e) {
            return null;
        }
    }

    private static byte[] parseIntArrayToIP(MemorySegment segment, long offset) {
        int[] array = new int[4];
        for (int i = 0; i < 4; i++) {
            array[i] = segment.get(JAVA_INT, offset + i * 4);
        }
        return ParseUtil.parseIntArrayToIP(array);
    }

    private static TcpState stateLookup(int state) {
        return switch (state) {
            case 0 -> CLOSED;
            case 1 -> LISTEN;
            case 2 -> SYN_SENT;
            case 3 -> SYN_RECV;
            case 4 -> ESTABLISHED;
            case 5 -> CLOSE_WAIT;
            case 6 -> FIN_WAIT_1;
            case 7 -> CLOSING;
            case 8 -> LAST_ACK;
            case 9 -> FIN_WAIT_2;
            case 10 -> TIME_WAIT;
            default -> UNKNOWN;
        };
    }

    /*
     * There are multiple versions of some tcp/udp/ip stats structures in macOS. Since we only need a few of the
     * hundreds of fields, we can improve performance by selectively reading the ints from the appropriate offsets,
     * which are consistent across the structure.
     */

    record BsdTcpstat(
            //
            int tcps_connattempt, // 0
            int tcps_accepts, // 4
            int tcps_drops, // 12
            int tcps_conndrops, // 16
            int tcps_sndpack, // 64
            int tcps_sndrexmitpack, // 72
            int tcps_rcvpack, // 104
            int tcps_rcvbadsum, // 112
            int tcps_rcvbadoff, // 116
            int tcps_rcvmemdrop, // 120
            int tcps_rcvshort // 124
    ) {
    }

    private static BsdTcpstat queryTcpstat() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffer = arena.allocate(128);
            if (SysctlUtilFFM.sysctl(new int[] { 1, 4, 8 }, buffer) > 0) {
                return new BsdTcpstat(buffer.get(JAVA_INT, 0), buffer.get(JAVA_INT, 4), buffer.get(JAVA_INT, 12),
                        buffer.get(JAVA_INT, 16), buffer.get(JAVA_INT, 64), buffer.get(JAVA_INT, 72),
                        buffer.get(JAVA_INT, 104), buffer.get(JAVA_INT, 112), buffer.get(JAVA_INT, 116),
                        buffer.get(JAVA_INT, 120), buffer.get(JAVA_INT, 124)

                );
            }
        }
        return new BsdTcpstat(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    record BsdUdpstat(
            //
            int udps_ipackets, // 0
            int udps_hdrops, // 4
            int udps_badsum, // 8
            int udps_badlen, // 12
            int udps_opackets, // 36
            int udps_noportmcast, // 48
            int udps_rcv6_swcsum, // 64
            int udps_snd6_swcsum // 89
    ) {
    }

    private static BsdUdpstat queryUdpstat() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffer = arena.allocate(1644);
            if (SysctlUtilFFM.sysctl("net.inet.udp.stats", buffer)) {
                return new BsdUdpstat(buffer.get(JAVA_INT, 0), buffer.get(JAVA_INT, 4), buffer.get(JAVA_INT, 8),
                        buffer.get(JAVA_INT, 12), buffer.get(JAVA_INT, 36), buffer.get(JAVA_INT, 48),
                        buffer.get(JAVA_INT, 64), buffer.get(JAVA_INT, 80));
            }
        }
        return new BsdUdpstat(0, 0, 0, 0, 0, 0, 0, 0);
    }

    record BsdIpstat(
            //
            int ips_total, // 0
            int ips_badsum, // 4
            int ips_tooshort, // 8
            int ips_toosmall, // 12
            int ips_badhlen, // 16
            int ips_badlen, // 20
            int ips_delivered // 56
    ) {
    }

    private static BsdIpstat queryIpstat() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffer = arena.allocate(60);
            if (SysctlUtilFFM.sysctl("net.inet.ip.stats", buffer)) {
                return new BsdIpstat(buffer.get(JAVA_INT, 0), buffer.get(JAVA_INT, 4), buffer.get(JAVA_INT, 8),
                        buffer.get(JAVA_INT, 12), buffer.get(JAVA_INT, 16), buffer.get(JAVA_INT, 20),
                        buffer.get(JAVA_INT, 56));
            }
        }
        return new BsdIpstat(0, 0, 0, 0, 0, 0, 0);
    }

    record BsdIp6stat(
            //
            long ip6s_total, // 0
            long ip6s_localout // 88
    ) {
    }

    private static BsdIp6stat queryIp6stat() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffer = arena.allocate(96);
            if (SysctlUtilFFM.sysctl("net.inet6.ip6.stats", buffer)) {
                return new BsdIp6stat(buffer.get(JAVA_LONG, 0), buffer.get(JAVA_LONG, 88));
            }
        }
        return new BsdIp6stat(0, 0);
    }

}
