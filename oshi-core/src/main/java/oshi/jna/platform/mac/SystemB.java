/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.mac;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

import oshi.jna.platform.unix.CLibrary;

/**
 * System class. This class should be considered non-API as it may be removed if/when its code is incorporated into the
 * JNA project.
 */
public interface SystemB extends com.sun.jna.platform.mac.SystemB, CLibrary {

    SystemB INSTANCE = Native.load("System", SystemB.class);

    int UTX_USERSIZE = 256;
    int UTX_LINESIZE = 32;
    int UTX_IDSIZE = 4;
    int UTX_HOSTSIZE = 256;

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
     * Reads a line from the current file position in the utmp file. It returns a pointer to a structure containing the
     * fields of the line.
     * <p>
     * Not thread safe
     *
     * @return a {@link MacUtmpx} on success, and NULL on failure (which includes the "record not found" case)
     */
    MacUtmpx getutxent();

    @FieldOrder({ "ru_utime_sec", "ru_utime_usec", "ru_stime_sec", "ru_stime_usec", "ru_maxrss", "ru_ixrss", "ru_idrss",
            "ru_isrss", "ru_minflt", "ru_majflt", "ru_nswap", "ru_inblock", "ru_oublock", "ru_msgsnd", "ru_msgrcv",
            "ru_nsignals", "ru_nvcsw", "ru_nivcsw" })
    class Rusage extends Structure {
        public NativeLong ru_utime_sec;
        public int ru_utime_usec;
        public NativeLong ru_stime_sec;
        public int ru_stime_usec;
        public NativeLong ru_maxrss;
        public NativeLong ru_ixrss;
        public NativeLong ru_idrss;
        public NativeLong ru_isrss;
        public NativeLong ru_minflt;
        public NativeLong ru_majflt;
        public NativeLong ru_nswap;
        public NativeLong ru_inblock;
        public NativeLong ru_oublock;
        public NativeLong ru_msgsnd;
        public NativeLong ru_msgrcv;
        public NativeLong ru_nsignals;
        public NativeLong ru_nvcsw;
        public NativeLong ru_nivcsw;
    }

    int getrusage(int who, Rusage rusage);
}
