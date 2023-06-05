/*
 * Copyright 2019-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.linux;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.linux.LibC;

import oshi.jna.platform.unix.CLibrary;

/**
 * Linux C Library. This class should be considered non-API as it may be removed if/when its code is incorporated into
 * the JNA project.
 */
public interface LinuxLibc extends LibC, CLibrary {

    LinuxLibc INSTANCE = Native.load("c", LinuxLibc.class);

    /**
     * SYS_gettid Defined in one of: arch/arm64/include/asm/unistd32.h, 224 arch/x86/include/uapi/asm/unistd_32.h, 224
     * arch/x86/include/uapi/asm/unistd_64.h, 186 include/uapi/asm-generic/unistd.h, 178
     */
    NativeLong SYS_GETTID = new NativeLong(Platform.isIntel() ? (Platform.is64Bit() ? 186 : 224)
            : ((Platform.isARM() && Platform.is64Bit()) ? 224 : 178));

    /**
     * Return type for getutxent()
     */
    @FieldOrder({ "ut_type", "ut_pid", "ut_line", "ut_id", "ut_user", "ut_host", "ut_exit", "ut_session", "ut_tv",
            "ut_addr_v6", "reserved" })
    class LinuxUtmpx extends Structure {
        public short ut_type; // Type of login.
        public int ut_pid; // Process ID of login process.
        public byte[] ut_line = new byte[UT_LINESIZE]; // Devicename.
        public byte[] ut_id = new byte[4]; // Inittab ID.
        public byte[] ut_user = new byte[UT_NAMESIZE]; // Username.
        public byte[] ut_host = new byte[UT_HOSTSIZE]; // Hostname for remote login.
        public Exit_status ut_exit; // Exit status of a process marked as DEAD_PROCESS.
        public int ut_session; // Session ID, used for windowing.
        public Ut_Tv ut_tv; // Time entry was made.
        public int[] ut_addr_v6 = new int[4]; // Internet address of remote host; IPv4 address uses just ut_addr_v6[0]
        public byte[] reserved = new byte[20]; // Reserved for future use.
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
     * 32-bit timeval required for utmpx structure
     */
    @FieldOrder({ "tv_sec", "tv_usec" })
    class Ut_Tv extends Structure {
        public int tv_sec; // seconds
        public int tv_usec; // microseconds
    }

    /**
     * Reads a line from the current file position in the utmp file. It returns a pointer to a structure containing the
     * fields of the line.
     * <p>
     * Not thread safe
     *
     * @return a {@link LinuxUtmpx} on success, and NULL on failure (which includes the "record not found" case)
     */
    LinuxUtmpx getutxent();

    /**
     * Returns the caller's thread ID (TID). In a single-threaded process, the thread ID is equal to the process ID. In
     * a multithreaded process, all threads have the same PID, but each one has a unique TID.
     *
     * @return the thread ID of the calling thread.
     */
    int gettid();

    /**
     * syscall() performs the system call whose assembly language interface has the specified number with the specified
     * arguments.
     *
     * @param number sys call number
     * @param args   sys call arguments
     * @return The return value is defined by the system call being invoked. In general, a 0 return value indicates
     *         success. A -1 return value indicates an error, and an error code is stored in errno.
     */
    NativeLong syscall(NativeLong number, Object... args);
}
