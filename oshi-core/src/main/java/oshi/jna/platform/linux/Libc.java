/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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

/**
 * Linux C Library. This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface Libc extends Library {

    Libc INSTANCE = (Libc) Native.loadLibrary("c", Libc.class);

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
}
