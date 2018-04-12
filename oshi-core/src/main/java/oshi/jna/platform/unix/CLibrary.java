/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.jna.platform.unix;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * C library with code common to all *nix-based operating systems. This class
 * should be considered non-API as it may be removed if/when its code is
 * incorporated into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface CLibrary extends Library {

    /*
     * For getaddrinfo()
     */
    int AI_CANONNAME = 2;

    /**
     * Return type for BSD sysctl kern.boottime
     */
    class Timeval extends Structure {
        public long tv_sec; // seconds
        public long tv_usec; // microseconds

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "tv_sec", "tv_usec" });
        }
    }

    class Sockaddr extends Structure {
        public short sa_family;
        public byte[] sa_data = new byte[14];

        public static class ByReference extends Sockaddr implements Structure.ByReference {
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "sa_family", "sa_data" });
        }
    }

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

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "ai_flags", "ai_family", "ai_socktype", "ai_protocol", "ai_addrlen",
                    "ai_addr", "ai_canonname", "ai_next" });
        }

        public Addrinfo() {
        }

        public Addrinfo(Pointer p) {
            super(p);
            read();
        }
    }

    /**
     * The sysctl() function retrieves system information and allows processes
     * with appropriate privileges to set system information. The information
     * available from sysctl() consists of integers, strings, and tables.
     *
     * The state is described using a "Management Information Base" (MIB) style
     * name, listed in name, which is a namelen length array of integers.
     *
     * The information is copied into the buffer specified by oldp. The size of
     * the buffer is given by the location specified by oldlenp before the call,
     * and that location gives the amount of data copied after a successful call
     * and after a call that returns with the error code ENOMEM. If the amount
     * of data available is greater than the size of the buffer supplied, the
     * call supplies as much data as fits in the buffer provided and returns
     * with the error code ENOMEM. If the old value is not desired, oldp and
     * oldlenp should be set to NULL.
     *
     * The size of the available data can be determined by calling sysctl() with
     * the NULL argument for oldp. The size of the available data will be
     * returned in the location pointed to by oldlenp. For some operations, the
     * amount of space may change often. For these operations, the system
     * attempts to round up so that the returned size is large enough for a call
     * to return the data shortly thereafter.
     *
     * To set a new value, newp is set to point to a buffer of length newlen
     * from which the requested value is to be taken. If a new value is not to
     * be set, newp should be set to NULL and newlen set to 0.
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
    int sysctl(int[] name, int namelen, Pointer oldp, IntByReference oldlenp, Pointer newp, int newlen);

    /**
     * The sysctlbyname() function accepts an ASCII representation of the name
     * and internally looks up the integer name vector. Apart from that, it
     * behaves the same as the standard sysctl() function.
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
    int sysctlbyname(String name, Pointer oldp, IntByReference oldlenp, Pointer newp, int newlen);

    /**
     * The sysctlnametomib() function accepts an ASCII representation of the
     * name, looks up the integer name vector, and returns the numeric
     * representation in the mib array pointed to by mibp. The number of
     * elements in the mib array is given by the location specified by sizep
     * before the call, and that location gives the number of entries copied
     * after a successful call. The resulting mib and size may be used in
     * subsequent sysctl() calls to get the data associated with the requested
     * ASCII name. This interface is intended for use by applications that want
     * to repeatedly request the same variable (the sysctl() function runs in
     * about a third the time as the same request made via the sysctlbyname()
     * function).
     *
     * The number of elements in the mib array can be determined by calling
     * sysctlnametomib() with the NULL argument for mibp.
     *
     * The sysctlnametomib() function is also useful for fetching mib prefixes.
     * If size on input is greater than the number of elements written, the
     * array still contains the additional elements which may be written
     * programmatically.
     *
     * @param name
     *            ASCII representation of the name
     * @param mibp
     *            Integer array containing the corresponding name vector.
     * @param size
     *            On input, number of elements in the returned array; on output,
     *            the number of entries copied.
     * @return 0 on success; sets errno on failure
     */
    int sysctlnametomib(String name, Pointer mibp, IntByReference size);

    /**
     * The getloadavg() function returns the number of processes in the system
     * run queue averaged over various periods of time. Up to nelem samples are
     * retrieved and assigned to successive elements of loadavg[]. The system
     * imposes a maximum of 3 samples, representing averages over the last 1, 5,
     * and 15 minutes, respectively.
     *
     * @param loadavg
     *            An array of doubles which will be filled with the results
     * @param nelem
     *            Number of samples to return
     * @return If the load average was unobtainable, -1 is returned; otherwise,
     *         the number of samples actually retrieved is returned.
     * @see <A HREF=
     *      "https://www.freebsd.org/cgi/man.cgi?query=getloadavg&sektion=3">
     *      getloadavg(3)</A>
     */
    int getloadavg(double[] loadavg, int nelem);

    /**
     * Returns the process ID of the calling process. The ID is guaranteed to be
     * unique and is useful for constructing temporary file names.
     *
     * @return the process ID of the calling process.
     */
    int getpid();

    /**
     * Given node and service, which identify an Internet host and a service,
     * getaddrinfo() returns one or more addrinfo structures, each of which
     * contains an Internet address that can be specified in a call to bind(2)
     * or connect(2).
     *
     * @param node
     *            a numerical network address or a network hostname, whose
     *            network addresses are looked up and resolved.
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
     * Frees the memory that was allocated for the dynamically allocated linked
     * list res.
     *
     * @param res
     *            Pointer to linked list returned by getaddrinfo
     */
    void freeaddrinfo(Pointer res);

    /**
     * Translates getaddrinfo error codes to a human readable string, suitable
     * for error reporting.
     *
     * @param e
     *            Error code from getaddrinfo
     * @return A human-readable version of the error code
     */
    String gai_strerror(int e);

    /**
     * Places the contents of the symbolic link path in the buffer buf, which
     * has size bufsiz.
     *
     * @param path
     *            A symbolic link
     * @param buf
     *            Holds actual path to location pointed to by symlink
     * @param bufsize
     *            size of data in buffer
     * @return readlink() places the contents of the symbolic link path in the
     *         buffer buf, which has size bufsiz. readlink() does not append a
     *         null byte to buf. It will truncate the contents (to a length of
     *         bufsiz characters), in case the buffer is too small to hold all
     *         of the contents.
     */
    int readlink(String path, Pointer buf, int bufsize);

    /**
     * Returns the number of bytes in a memory page, where "page" is a
     * fixed-length block, the unit for memory allocation and file mapping
     * performed by mmap(2).
     *
     * @return the memory page size
     */
    int getpagesize();
}
