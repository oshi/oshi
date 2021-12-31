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
package oshi.jna.platform.mac;

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.Union;

import oshi.jna.platform.unix.CLibrary;

/**
 * System class. This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 */
public interface SystemB extends com.sun.jna.platform.mac.SystemB, CLibrary {

    SystemB INSTANCE = Native.load("System", SystemB.class);

    int PROC_PIDLISTFDS = 1;
    int PROX_FDTYPE_SOCKET = 2;
    int PROC_PIDFDSOCKETINFO = 3;
    int TSI_T_NTIMERS = 4;
    int SOCKINFO_IN = 1;
    int SOCKINFO_TCP = 2;

    int UTX_USERSIZE = 256;
    int UTX_LINESIZE = 32;
    int UTX_IDSIZE = 4;
    int UTX_HOSTSIZE = 256;

    int AF_INET = 2; // The Internet Protocol version 4 (IPv4) address family.
    int AF_INET6 = 30; // The Internet Protocol version 6 (IPv6) address family.

    /**
     * Mac connection info
     */
    @FieldOrder({ "ut_user", "ut_id", "ut_line", "ut_pid", "ut_type", "ut_tv", "ut_host", "ut_pad" })
    class MacUtmpx extends Structure {
        public byte[] ut_user = new byte[UTX_USERSIZE]; // login name
        public byte[] ut_id = new byte[UTX_IDSIZE]; // id
        public byte[] ut_line = new byte[UTX_LINESIZE]; // tty name
        public int ut_pid; // process id creating the entry
        public short ut_type; // type of this entry
        public Timeval ut_tv; // time entry was created
        public byte[] ut_host = new byte[UTX_HOSTSIZE]; // host name
        public byte[] ut_pad = new byte[16]; // reserved for future use
    }

    /**
     * Mac file descriptor info
     */
    @FieldOrder({ "proc_fd", "proc_fdtype" })
    class ProcFdInfo extends Structure {
        public int proc_fd;
        public int proc_fdtype;
    }

    /**
     * Mac internet socket info
     */
    @FieldOrder({ "insi_fport", "insi_lport", "insi_gencnt", "insi_flags", "insi_flow", "insi_vflag", "insi_ip_ttl",
            "rfu_1", "insi_faddr", "insi_laddr", "insi_v4", "insi_v6" })
    class InSockInfo extends Structure {
        public int insi_fport; /* foreign port */
        public int insi_lport; /* local port */
        public long insi_gencnt; /* generation count of this instance */
        public int insi_flags; /* generic IP/datagram flags */
        public int insi_flow;

        public byte insi_vflag; /* ini_IPV4 or ini_IPV6 */
        public byte insi_ip_ttl; /* time to live proto */
        public int rfu_1; /* reserved */
        /* protocol dependent part, v4 only in last element */
        public int[] insi_faddr = new int[4]; /* foreign host table entry */
        public int[] insi_laddr = new int[4]; /* local host table entry */
        public byte insi_v4; /* type of service */
        public byte[] insi_v6 = new byte[9];
    }

    /**
     * Mac TCP socket info
     */
    @FieldOrder({ "tcpsi_ini", "tcpsi_state", "tcpsi_timer", "tcpsi_mss", "tcpsi_flags", "rfu_1", "tcpsi_tp" })
    class TcpSockInfo extends Structure {
        public InSockInfo tcpsi_ini;
        public int tcpsi_state;
        public int[] tcpsi_timer = new int[TSI_T_NTIMERS];
        public int tcpsi_mss;
        public int tcpsi_flags;
        public int rfu_1; /* reserved */
        public long tcpsi_tp; /* opaque handle of TCP protocol control block */
    }

    /**
     * Mack IP Socket Info
     */
    @FieldOrder({ "soi_stat", "soi_so", "soi_pcb", "soi_type", "soi_protocol", "soi_family", "soi_options",
            "soi_linger", "soi_state", "soi_qlen", "soi_incqlen", "soi_qlimit", "soi_timeo", "soi_error", "soi_oobmark",
            "soi_rcv", "soi_snd", "soi_kind", "rfu_1", "soi_proto" })
    class SocketInfo extends Structure {
        public long[] soi_stat = new long[17]; // vinfo_stat
        public long soi_so; /* opaque handle of socket */
        public long soi_pcb; /* opaque handle of protocol control block */
        public int soi_type;
        public int soi_protocol;
        public int soi_family;
        public short soi_options;
        public short soi_linger;
        public short soi_state;
        public short soi_qlen;
        public short soi_incqlen;
        public short soi_qlimit;
        public short soi_timeo;
        public short soi_error;
        public int soi_oobmark;
        public int[] soi_rcv = new int[6]; // sockbuf_info
        public int[] soi_snd = new int[6]; // sockbuf_info
        public int soi_kind;
        public int rfu_1; /* reserved */
        public Pri soi_proto;
    }

    /**
     * Mac file info
     */
    @FieldOrder({ "fi_openflags", "fi_status", "fi_offset", "fi_type", "fi_guardflags" })
    class ProcFileInfo extends Structure {
        public int fi_openflags;
        public int fi_status;
        public long fi_offset;
        public int fi_type;
        public int fi_guardflags;
    }

    /**
     * Mac socket info
     */
    @FieldOrder({ "pfi", "psi" })
    class SocketFdInfo extends Structure {
        public ProcFileInfo pfi;
        public SocketInfo psi;
    }

    /**
     * Union for TCP or internet socket info
     */
    class Pri extends Union {
        public InSockInfo pri_in;
        public TcpSockInfo pri_tcp;
        // max element is 524 bytes
        public byte[] max_size = new byte[524];
    }

    /**
     * Reads a line from the current file position in the utmp file. It returns a
     * pointer to a structure containing the fields of the line.
     * <p>
     * Not thread safe
     *
     * @return a {@link MacUtmpx} on success, and NULL on failure (which includes
     *         the "record not found" case)
     */
    MacUtmpx getutxent();

    int proc_pidfdinfo(int pid, int fd, int flavor, Structure buffer, int buffersize);
}