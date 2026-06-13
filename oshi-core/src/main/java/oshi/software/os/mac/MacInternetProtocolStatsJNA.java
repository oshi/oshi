/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import static com.sun.jna.platform.mac.SystemB.AF_INET;
import static com.sun.jna.platform.mac.SystemB.AF_INET6;
import static com.sun.jna.platform.mac.SystemB.INT_SIZE;
import static com.sun.jna.platform.mac.SystemB.PROC_ALL_PIDS;
import static com.sun.jna.platform.mac.SystemB.PROC_PIDFDSOCKETINFO;
import static com.sun.jna.platform.mac.SystemB.PROC_PIDLISTFDS;
import static com.sun.jna.platform.mac.SystemB.PROX_FDTYPE_SOCKET;
import static com.sun.jna.platform.mac.SystemB.SOCKINFO_IN;
import static com.sun.jna.platform.mac.SystemB.SOCKINFO_TCP;
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

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Memory;
import com.sun.jna.platform.mac.SystemB.InSockInfo;
import com.sun.jna.platform.mac.SystemB.ProcFdInfo;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.Struct.CloseableSocketFdInfo;
import oshi.jna.platform.mac.SystemB;
import oshi.software.common.os.mac.MacInternetProtocolStats;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtil;

/**
 * Internet Protocol Stats implementation
 */
@ThreadSafe
public class MacInternetProtocolStatsJNA extends MacInternetProtocolStats {

    public MacInternetProtocolStatsJNA(boolean elevated) {
        super(elevated);
    }

    @Override
    public List<IPConnection> getConnections() {
        List<IPConnection> conns = new ArrayList<>();
        int[] pids = new int[1024];
        int numberOfProcesses = SystemB.INSTANCE.proc_listpids(PROC_ALL_PIDS, 0, pids, pids.length * INT_SIZE)
                / INT_SIZE;
        for (int i = 0; i < numberOfProcesses; i++) {
            // Handle off-by-one bug in proc_listpids where the size returned
            // is: SystemB.INT_SIZE * (pids + 1)
            if (pids[i] > 0) {
                for (Integer fd : queryFdList(pids[i])) {
                    IPConnection ipc = queryIPConnection(pids[i], fd);
                    if (ipc != null) {
                        conns.add(ipc);
                    }
                }
            }
        }
        return conns;
    }

    private static List<Integer> queryFdList(int pid) {
        List<Integer> fdList = new ArrayList<>();
        int bufferSize = SystemB.INSTANCE.proc_pidinfo(pid, PROC_PIDLISTFDS, 0, null, 0);
        if (bufferSize > 0) {
            ProcFdInfo fdInfo = new ProcFdInfo();
            int numStructs = bufferSize / fdInfo.size();
            ProcFdInfo[] fdArray = (ProcFdInfo[]) fdInfo.toArray(numStructs);
            bufferSize = SystemB.INSTANCE.proc_pidinfo(pid, PROC_PIDLISTFDS, 0, fdArray[0], bufferSize);
            numStructs = bufferSize / fdInfo.size();
            for (int i = 0; i < numStructs; i++) {
                if (fdArray[i].proc_fdtype == PROX_FDTYPE_SOCKET) {
                    fdList.add(fdArray[i].proc_fd);
                }
            }
        }
        return fdList;
    }

    private static IPConnection queryIPConnection(int pid, int fd) {
        try (CloseableSocketFdInfo si = new CloseableSocketFdInfo()) {
            int ret = SystemB.INSTANCE.proc_pidfdinfo(pid, fd, PROC_PIDFDSOCKETINFO, si, si.size());
            if (si.size() == ret && si.psi.soi_family == AF_INET || si.psi.soi_family == AF_INET6) {
                InSockInfo ini;
                String type;
                TcpState state;
                if (si.psi.soi_kind == SOCKINFO_TCP) {
                    si.psi.soi_proto.setType("pri_tcp");
                    si.psi.soi_proto.read();
                    ini = si.psi.soi_proto.pri_tcp.tcpsi_ini;
                    state = stateLookup(si.psi.soi_proto.pri_tcp.tcpsi_state);
                    type = "tcp";
                } else if (si.psi.soi_kind == SOCKINFO_IN) {
                    si.psi.soi_proto.setType("pri_in");
                    si.psi.soi_proto.read();
                    ini = si.psi.soi_proto.pri_in;
                    state = NONE;
                    type = "udp";
                } else {
                    return null;
                }

                byte[] laddr;
                byte[] faddr;
                if (ini.insi_vflag == 1) {
                    laddr = ParseUtil.parseIntToIP(ini.insi_laddr.ina_46.i46a_addr4);
                    faddr = ParseUtil.parseIntToIP(ini.insi_faddr.ina_46.i46a_addr4);
                    type += "4";
                } else if (ini.insi_vflag == 2) {
                    laddr = ini.insi_laddr.ina_6.__u6_addr;
                    faddr = ini.insi_faddr.ina_6.__u6_addr;
                    type += "6";
                } else if (ini.insi_vflag == 3) {
                    laddr = ParseUtil.parseIntToIP(ini.insi_laddr.ina_46.i46a_addr4);
                    faddr = ParseUtil.parseIntToIP(ini.insi_faddr.ina_46.i46a_addr4);
                    type += "46";
                } else {
                    return null;
                }
                int lport = ParseUtil.bigEndian16ToLittleEndian(ini.insi_lport);
                int fport = ParseUtil.bigEndian16ToLittleEndian(ini.insi_fport);
                return new IPConnection(type, laddr, lport, faddr, fport, state, si.psi.soi_qlen, si.psi.soi_incqlen,
                        pid);
            }
        }
        return null;
    }

    private static TcpState stateLookup(int state) {
        switch (state) {
            case 0:
                return CLOSED;
            case 1:
                return LISTEN;
            case 2:
                return SYN_SENT;
            case 3:
                return SYN_RECV;
            case 4:
                return ESTABLISHED;
            case 5:
                return CLOSE_WAIT;
            case 6:
                return FIN_WAIT_1;
            case 7:
                return CLOSING;
            case 8:
                return LAST_ACK;
            case 9:
                return FIN_WAIT_2;
            case 10:
                return TIME_WAIT;
            default:
                return UNKNOWN;
        }
    }

    @Override
    protected BsdTcpstat queryTcpstat() {
        try (Memory m = SysctlUtil.sysctl("net.inet.tcp.stats")) {
            if (m != null && m.size() >= 128) {
                return new BsdTcpstat(m.getInt(0), m.getInt(4), m.getInt(12), m.getInt(16), m.getInt(64), m.getInt(72),
                        m.getInt(104), m.getInt(112), m.getInt(116), m.getInt(120), m.getInt(124));
            }
        }
        return new BsdTcpstat(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    @Override
    protected BsdIpstat queryIpstat() {
        try (Memory m = SysctlUtil.sysctl("net.inet.ip.stats")) {
            if (m != null && m.size() >= 60) {
                return new BsdIpstat(m.getInt(0), m.getInt(4), m.getInt(8), m.getInt(12), m.getInt(16), m.getInt(20),
                        m.getInt(56));
            }
        }
        return new BsdIpstat(0, 0, 0, 0, 0, 0, 0);
    }

    @Override
    protected BsdIp6stat queryIp6stat() {
        try (Memory m = SysctlUtil.sysctl("net.inet6.ip6.stats")) {
            if (m != null && m.size() >= 96) {
                return new BsdIp6stat(m.getLong(0), m.getLong(88));
            }
        }
        return new BsdIp6stat(0, 0);
    }

    @Override
    protected BsdUdpstat queryUdpstat() {
        try (Memory m = SysctlUtil.sysctl("net.inet.udp.stats")) {
            if (m != null && m.size() >= 84) {
                return new BsdUdpstat(m.getInt(0), m.getInt(4), m.getInt(8), m.getInt(12), m.getInt(36), m.getInt(48),
                        m.getInt(64), m.getInt(80));
            }
        }
        return new BsdUdpstat(0, 0, 0, 0, 0, 0, 0, 0);
    }
}
