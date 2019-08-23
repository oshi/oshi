/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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
import com.sun.jna.ptr.PointerByReference;

/**
 * C library with code common to all *nix-based operating systems. This class
 * should be considered non-API as it may be removed if/when its code is
 * incorporated into the JNA project.
 */
public interface CLibrary extends Library {

    /*
     * For getaddrinfo()
     */
    /** Constant <code>AI_CANONNAME=2</code> */
    int AI_CANONNAME = 2;

    /**
     * Return type for BSD sysctl kern.boottime
     */
    @FieldOrder({ "tv_sec", "tv_usec" })
    class Timeval extends Structure {
        public long tv_sec; // seconds
        public long tv_usec; // microseconds
    }

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

    /**
     * The getloadavg() function returns the number of processes in the system run
     * queue averaged over various periods of time. Up to nelem samples are
     * retrieved and assigned to successive elements of loadavg[]. The system
     * imposes a maximum of 3 samples, representing averages over the last 1, 5, and
     * 15 minutes, respectively.
     *
     * @param loadavg
     *            An array of doubles which will be filled with the results
     * @param nelem
     *            Number of samples to return
     * @return If the load average was unobtainable, -1 is returned; otherwise, the
     *         number of samples actually retrieved is returned.
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
     * Places the contents of the symbolic link path in the buffer buf, which has
     * size bufsiz.
     *
     * @param path
     *            A symbolic link
     * @param buf
     *            Holds actual path to location pointed to by symlink
     * @param bufsize
     *            size of data in buffer
     * @return readlink() places the contents of the symbolic link path in the
     *         buffer buf, which has size bufsiz. readlink() does not append a null
     *         byte to buf. It will truncate the contents (to a length of bufsiz
     *         characters), in case the buffer is too small to hold all of the
     *         contents.
     */
    int readlink(String path, Pointer buf, int bufsize);
}
