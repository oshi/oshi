/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.jna.platform.unix.freebsd;

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.IntByReference;

import oshi.jna.platform.unix.CLibrary;

/**
 * C library. This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 */
public interface FreeBsdLibc extends CLibrary {
    FreeBsdLibc INSTANCE = Native.load("libc", FreeBsdLibc.class);

    int UTX_USERSIZE = 32;
    int UTX_LINESIZE = 16;
    int UTX_IDSIZE = 8;
    int UTX_HOSTSIZE = 128;

    @FieldOrder({ "ut_type", "ut_tv", "ut_id", "ut_pid", "ut_user", "ut_line", "ut_host", "ut_spare" })
    class FreeBsdUtmpx extends Structure {
        public short ut_type; // type of entry
        public Timeval ut_tv; // time entry was made
        public byte[] ut_id = new byte[UTX_IDSIZE]; // etc/inittab id (usually line #)
        public int ut_pid; // process id
        public byte[] ut_user = new byte[UTX_USERSIZE]; // user login name
        public byte[] ut_line = new byte[UTX_LINESIZE]; // device name
        public byte[] ut_host = new byte[UTX_HOSTSIZE]; // host name
        public byte[] ut_spare = new byte[64];
    }

    /*
     * Data size
     */
    /** Constant <code>UINT64_SIZE=Native.getNativeSize(long.class)</code> */
    int UINT64_SIZE = Native.getNativeSize(long.class);
    /** Constant <code>INT_SIZE=Native.getNativeSize(int.class)</code> */
    int INT_SIZE = Native.getNativeSize(int.class);

    /*
     * CPU state indices
     */
    /** Constant <code>CPUSTATES=5</code> */
    int CPUSTATES = 5;
    /** Constant <code>CP_USER=0</code> */
    int CP_USER = 0;
    /** Constant <code>CP_NICE=1</code> */
    int CP_NICE = 1;
    /** Constant <code>CP_SYS=2</code> */
    int CP_SYS = 2;
    /** Constant <code>CP_INTR=3</code> */
    int CP_INTR = 3;
    /** Constant <code>CP_IDLE=4</code> */
    int CP_IDLE = 4;

    /**
     * Return type for BSD sysctl kern.boottime
     */
    @FieldOrder({ "tv_sec", "tv_usec" })
    class Timeval extends Structure {
        public long tv_sec; // seconds
        public long tv_usec; // microseconds
    }

    @FieldOrder({ "cpu_ticks" })
    class CpTime extends Structure {
        public long[] cpu_ticks = new long[CPUSTATES];
    }

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
    int sysctl(int[] name, int namelen, Pointer oldp, IntByReference oldlenp, Pointer newp, int newlen);

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
    int sysctlbyname(String name, Pointer oldp, IntByReference oldlenp, Pointer newp, int newlen);

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
     * @param size
     *            On input, number of elements in the returned array; on output, the
     *            number of entries copied.
     * @return 0 on success; sets errno on failure
     */
    int sysctlnametomib(String name, Pointer mibp, IntByReference size);

    /**
     * Reads a line from the current file position in the utmp file. It returns a
     * pointer to a structure containing the fields of the line.
     * <p>
     * Not thread safe
     *
     * @return a {@link FreeBsdUtmpx} on success, and NULL on failure (which
     *         includes the "record not found" case)
     */
    FreeBsdUtmpx getutxent();
}
