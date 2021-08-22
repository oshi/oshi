/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.jna.platform.unix;

import com.sun.jna.Library;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.unix.LibCAPI;
import com.sun.jna.ptr.PointerByReference;

/**
 * C library with code common to all *nix-based operating systems. This class
 * should be considered non-API as it may be removed if/when its code is
 * incorporated into the JNA project.
 */
public interface CLibrary extends LibCAPI, Library {

    int AI_CANONNAME = 2;

    int UT_LINESIZE = 32;
    int UT_NAMESIZE = 32;
    int UT_HOSTSIZE = 256;
    int LOGIN_PROCESS = 6; // Session leader of a logged in user.
    int USER_PROCESS = 7; // Normal process.

    @FieldOrder({ "sa_family", "sa_data" })
    class Sockaddr extends Structure {
        public short sa_family;
        public byte[] sa_data = new byte[14];

        public static class ByReference extends Sockaddr implements Structure.ByReference {
        }
    }

    @FieldOrder({ "ai_flags", "ai_family", "ai_socktype", "ai_protocol", "ai_addrlen", "ai_addr", "ai_canonname",
            "ai_next" })
    class Addrinfo extends Structure {
        public int ai_flags;
        public int ai_family;
        public int ai_socktype;
        public int ai_protocol;
        public int ai_addrlen;
        public Sockaddr.ByReference ai_addr;
        public String ai_canonname;
        public ByReference ai_next;

        public static class ByReference extends Addrinfo implements Structure.ByReference {
        }

        public Addrinfo() {
        }

        public Addrinfo(Pointer p) {
            super(p);
            read();
        }
    }

    /*
     * Between macOS and FreeBSD there are multiple versions of some tcp/udp/ip
     * stats structures. Since we only need a few of the hundreds of fields, we can
     * improve performance by selectively reading the ints from the appropriate
     * offsets, which are consistent across the structure. These classes include the
     * common fields and offsets.
     */

    class BsdTcpstat {
        public int tcps_connattempt; // 0
        public int tcps_accepts; // 4
        public int tcps_drops; // 12
        public int tcps_conndrops; // 16
        public int tcps_sndpack; // 64
        public int tcps_sndrexmitpack; // 72
        public int tcps_rcvpack; // 104
        public int tcps_rcvbadsum; // 112
        public int tcps_rcvbadoff; // 116
        public int tcps_rcvmemdrop; // 120
        public int tcps_rcvshort; // 124
    }

    class BsdUdpstat {
        public int udps_ipackets; // 0
        public int udps_hdrops; // 4
        public int udps_badsum; // 8
        public int udps_badlen; // 12
        public int udps_opackets; // 36
        public int udps_noportmcast; // 48
        public int udps_rcv6_swcsum; // 64
        public int udps_snd6_swcsum; // 89
    }

    class BsdIpstat {
        public int ips_total; // 0
        public int ips_badsum; // 4
        public int ips_tooshort; // 8
        public int ips_toosmall; // 12
        public int ips_badhlen; // 16
        public int ips_badlen; // 20
        public int ips_delivered; // 56
    }

    class BsdIp6stat {
        public long ip6s_total; // 0
        public long ip6s_localout; // 88
    }

    /**
     * Returns the process ID of the calling process. The ID is guaranteed to be
     * unique and is useful for constructing temporary file names.
     *
     * @return the process ID of the calling process.
     */
    int getpid();

    /**
     * Given node and service, which identify an Internet host and a service,
     * getaddrinfo() returns one or more addrinfo structures, each of which contains
     * an Internet address that can be specified in a call to bind(2) or connect(2).
     *
     * @param node
     *            a numerical network address or a network hostname, whose network
     *            addresses are looked up and resolved.
     * @param service
     *            sets the port in each returned address structure.
     * @param hints
     *            specifies criteria for selecting the socket address structures
     *            returned in the list pointed to by res.
     * @param res
     *            returned address structure
     * @return 0 on success; sets errno on failure
     */
    int getaddrinfo(String node, String service, Addrinfo hints, PointerByReference res);

    /**
     * Frees the memory that was allocated for the dynamically allocated linked list
     * res.
     *
     * @param res
     *            Pointer to linked list returned by getaddrinfo
     */
    void freeaddrinfo(Pointer res);

    /**
     * Translates getaddrinfo error codes to a human readable string, suitable for
     * error reporting.
     *
     * @param e
     *            Error code from getaddrinfo
     * @return A human-readable version of the error code
     */
    String gai_strerror(int e);

    /**
     * Rewinds the file pointer to the beginning of the utmp file. It is generally a
     * good idea to call it before any of the other functions.
     */
    void setutxent();

    /**
     * Closes the utmp file. It should be called when the user code is done
     * accessing the file with the other functions.
     */
    void endutxent();

    /**
     * The sysctl() function retrieves system information and allows processes with
     * appropriate privileges to set system information. The information available
     * from sysctl() consists of integers, strings, and tables.
     *
     * The state is described using a "Management Information Base" (MIB) style
     * name, listed in name, which is a namelen length array of integers.
     *
     * The information is copied into the buffer specified by oldp. The size of the
     * buffer is given by the location specified by oldlenp before the call, and
     * that location gives the amount of data copied after a successful call and
     * after a call that returns with the error code ENOMEM. If the amount of data
     * available is greater than the size of the buffer supplied, the call supplies
     * as much data as fits in the buffer provided and returns with the error code
     * ENOMEM. If the old value is not desired, oldp and oldlenp should be set to
     * NULL.
     *
     * The size of the available data can be determined by calling sysctl() with the
     * NULL argument for oldp. The size of the available data will be returned in
     * the location pointed to by oldlenp. For some operations, the amount of space
     * may change often. For these operations, the system attempts to round up so
     * that the returned size is large enough for a call to return the data shortly
     * thereafter.
     *
     * To set a new value, newp is set to point to a buffer of length newlen from
     * which the requested value is to be taken. If a new value is not to be set,
     * newp should be set to NULL and newlen set to 0.
     *
     * @param name
     *            MIB array of integers
     * @param namelen
     *            length of the MIB array
     * @param oldp
     *            Information retrieved
     * @param oldlenp
     *            Size of information retrieved
     * @param newp
     *            Information to be written
     * @param newlen
     *            Size of information to be written
     * @return 0 on success; sets errno on failure
     */
    int sysctl(int[] name, int namelen, Pointer oldp, size_t.ByReference oldlenp, Pointer newp, size_t newlen);

    /**
     * The sysctlbyname() function accepts an ASCII representation of the name and
     * internally looks up the integer name vector. Apart from that, it behaves the
     * same as the standard sysctl() function.
     *
     * @param name
     *            ASCII representation of the MIB name
     * @param oldp
     *            Information retrieved
     * @param oldlenp
     *            Size of information retrieved
     * @param newp
     *            Information to be written
     * @param newlen
     *            Size of information to be written
     * @return 0 on success; sets errno on failure
     */
    int sysctlbyname(String name, Pointer oldp, size_t.ByReference oldlenp, Pointer newp, size_t newlen);

    /**
     * The sysctlnametomib() function accepts an ASCII representation of the name,
     * looks up the integer name vector, and returns the numeric representation in
     * the mib array pointed to by mibp. The number of elements in the mib array is
     * given by the location specified by sizep before the call, and that location
     * gives the number of entries copied after a successful call. The resulting mib
     * and size may be used in subsequent sysctl() calls to get the data associated
     * with the requested ASCII name. This interface is intended for use by
     * applications that want to repeatedly request the same variable (the sysctl()
     * function runs in about a third the time as the same request made via the
     * sysctlbyname() function).
     *
     * The number of elements in the mib array can be determined by calling
     * sysctlnametomib() with the NULL argument for mibp.
     *
     * The sysctlnametomib() function is also useful for fetching mib prefixes. If
     * size on input is greater than the number of elements written, the array still
     * contains the additional elements which may be written programmatically.
     *
     * @param name
     *            ASCII representation of the name
     * @param mibp
     *            Integer array containing the corresponding name vector.
     * @param sizep
     *            On input, number of elements in the returned array; on output, the
     *            number of entries copied.
     * @return 0 on success; sets errno on failure
     */
    int sysctlnametomib(String name, Pointer mibp, size_t.ByReference sizep);

    int open(String absolutePath, int i);

    // Last argument is really off_t
    ssize_t pread(int fildes, Pointer buf, size_t nbyte, NativeLong offset);
}
