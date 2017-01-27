/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.jna.platform.linux;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

/**
 * Linux C Library. This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface Libc extends Library {

    Libc INSTANCE = (Libc) Native.loadLibrary("c", Libc.class);

    int AI_CANONNAME = 2;

    class Sysinfo extends Structure {
        public NativeLong uptime; // Seconds since boot
        // 1, 5, and 15 minute load averages

        public NativeLong[] loads = new NativeLong[3];

        public NativeLong totalram; // Total usable main memory size

        public NativeLong freeram; // Available memory size

        public NativeLong sharedram; // Amount of shared memory

        public NativeLong bufferram; // Memory used by buffers

        public NativeLong totalswap; // Total swap space size

        public NativeLong freeswap; // swap space still available

        public short procs; // Number of current processes

        public NativeLong totalhigh; // Total high memory size

        public NativeLong freehigh; // Available high memory size

        public int mem_unit; // Memory unit size in bytes

        public byte[] _f = new byte[8]; // Won't be written for 64-bit systems

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "uptime", "loads", "totalram", "freeram", "sharedram", "bufferram",
                    "totalswap", "freeswap", "procs", "totalhigh", "freehigh", "mem_unit", "_f" });
        }
    }

    class Sockaddr extends Structure {
        public short sa_family;
        public byte[] sa_data = new byte[14];

        public static class ByReference extends Sockaddr implements Structure.ByReference {
        }

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

    int sysinfo(Sysinfo info);

    /**
     * The getloadavg() function returns the number of processes in the system
     * run queue averaged over various periods of time. Up to nelem samples are
     * retrieved and assigned to successive elements of loadavg[]. The system
     * imposes a maximum of 3 samples, representing averages over the last 1, 5,
     * and 15 minutes, respectively.
     *
     * @param loadavg
     *            array to be filled
     * @param nelem
     *            number of elements in the array to fill
     * @return If the load average was unobtainable, -1 is returned; otherwise,
     *         the number of samples actually retrieved is returned.
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
    int getaddrinfo(String node, String service, Libc.Addrinfo hints, PointerByReference res);

    void freeaddrinfo(Pointer res);

    String gai_strerror(int e);
}
