/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.unix;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.NativeLongByReference;

/**
 * C library. This class should be considered non-API as it may be removed if/when its code is incorporated into the JNA
 * project.
 */
public interface FreeBsdLibc extends CLibrary {
    FreeBsdLibc INSTANCE = Native.load("libc", FreeBsdLibc.class);

    int UTX_USERSIZE = 32;
    int UTX_LINESIZE = 16;
    int UTX_IDSIZE = 8;
    int UTX_HOSTSIZE = 128;

    /**
     * Connection info
     */
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

    /**
     * CPU Ticks
     */
    @FieldOrder({ "cpu_ticks" })
    class CpTime extends Structure implements AutoCloseable {
        public long[] cpu_ticks = new long[CPUSTATES];

        @Override
        public void close() {
            Pointer p = this.getPointer();
            if (p instanceof Memory) {
                ((Memory) p).close();
            }
        }
    }

    /**
     * Reads a line from the current file position in the utmp file. It returns a pointer to a structure containing the
     * fields of the line.
     * <p>
     * Not thread safe
     *
     * @return a {@link FreeBsdUtmpx} on success, and NULL on failure (which includes the "record not found" case)
     */
    FreeBsdUtmpx getutxent();

    /**
     * Stores the system-wide thread identifier for the current kernel-scheduled thread in the variable pointed by the
     * argument id.
     *
     * @param id The thread identifier is an integer in the range from PID_MAX + 2 (100001) to INT_MAX. The thread
     *           identifier is guaranteed to be unique at any given time, for each running thread in the system.
     * @return If successful, returns zero, otherwise -1 is returned, and errno is set to indicate the error.
     */
    int thr_self(NativeLongByReference id);
}
