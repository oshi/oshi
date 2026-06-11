/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.unix;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * C library. This class should be considered non-API as it may be removed if/when its code is incorporated into the JNA
 * project.
 */
public interface SolarisLibc extends CLibrary {

    SolarisLibc INSTANCE = Native.load("c", SolarisLibc.class);

    /**
     * Solaris/illumos {@code RLIMIT_NOFILE} from {@code <sys/resource.h>} is {@code 5}, not {@code 7} like Linux. The
     * inherited {@code Resource.RLIMIT_NOFILE} from JNA's platform module is the Linux value, so calling
     * {@code getrlimit(Resource.RLIMIT_NOFILE, …)} on Solaris asks for resource {@code 7} (which is
     * {@code RLIM_NLIMITS} — not a valid limit) and returns {@code -1}/{@code EINVAL}. Shadow it here.
     */
    int RLIMIT_NOFILE = 5;

    int UTX_USERSIZE = 32;
    int UTX_LINESIZE = 32;
    int UTX_IDSIZE = 4;
    int UTX_HOSTSIZE = 257;

    /**
     * Connection info
     */
    @FieldOrder({ "ut_user", "ut_id", "ut_line", "ut_pid", "ut_type", "ut_exit", "ut_tv", "ut_session", "pad",
            "ut_syslen", "ut_host" })
    class SolarisUtmpx extends Structure {
        public byte[] ut_user = new byte[UTX_USERSIZE]; // user login name
        public byte[] ut_id = new byte[UTX_IDSIZE]; // etc/inittab id (usually line #)
        public byte[] ut_line = new byte[UTX_LINESIZE]; // device name
        public int ut_pid; // process id
        public short ut_type; // type of entry
        public Exit_status ut_exit; // process termination/exit status
        public Timeval ut_tv; // time entry was made
        public int ut_session; // session ID, used for windowing
        public int[] pad = new int[5]; // reserved for future use
        public short ut_syslen; // significant length of ut_host including terminating null
        public byte[] ut_host = new byte[UTX_HOSTSIZE]; // host name
    }

    /**
     * Part of utmpx structure
     */
    @FieldOrder({ "e_termination", "e_exit" })
    class Exit_status extends Structure {
        public short e_termination; // Process termination status
        public short e_exit; // Process exit status
    }

    /**
     * 32/64-bit timeval required for utmpx structure
     */
    @FieldOrder({ "tv_sec", "tv_usec" })
    class Timeval extends Structure {
        public NativeLong tv_sec; // seconds
        public NativeLong tv_usec; // microseconds
    }

    /**
     * Reads a line from the current file position in the utmp file. It returns a pointer to a structure containing the
     * fields of the line.
     * <p>
     * Not thread safe
     *
     * @return a {@link SolarisUtmpx} on success, and NULL on failure (which includes the "record not found" case)
     */
    SolarisUtmpx getutxent();

    /**
     * Returns the thread ID of the calling thread.
     *
     * @return the thread ID of the calling thread.
     */
    int thr_self();
}
