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
}
