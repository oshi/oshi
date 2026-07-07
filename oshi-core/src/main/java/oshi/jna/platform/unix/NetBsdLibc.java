/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.unix;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * C library. This class should be considered non-API as it may be removed if/when its code is incorporated into the JNA
 * project.
 */
public interface NetBsdLibc extends CLibrary {
    NetBsdLibc INSTANCE = BsdLibcLoader.loadLibc(NetBsdLibc.class);

    int CTL_KERN = 1; // "high kernel": proc, limits
    int CTL_VM = 2; // virtual memory
    int CTL_HW = 6; // generic cpu/io
    int CTL_MACHDEP = 7; // machine dependent

    int KERN_OSTYPE = 1; // string: system version
    int KERN_OSRELEASE = 2; // string: system release
    int KERN_OSREV = 3; // int: system revision
    int KERN_VERSION = 4; // string: compile time info
    int KERN_MAXPROC = 6; // int: max processes
    int KERN_ARGMAX = 8; // int: max arguments to exec
    int KERN_CP_TIME = 51; // struct: cp_time

    int VM_UVMEXP2 = 5; // struct uvmexp_sysctl

    int HW_MACHINE = 1; // string: machine class
    int HW_MODEL = 2; // string: specific machine model
    int HW_NCPU = 3; // int: number of cpus
    int HW_PAGESIZE = 7; // int: software page size
    int HW_MACHINE_ARCH = 11; // string: machine architecture
    int HW_NCPUONLINE = 16; // number of cpus being used

    /*
     * CPU state indices
     */
    int CPUSTATES = 5;
    int CP_USER = 0;
    int CP_NICE = 1;
    int CP_SYS = 2;
    int CP_INTR = 3;
    int CP_IDLE = 4;

    int UINT64_SIZE = Native.getNativeSize(long.class);
    int INT_SIZE = Native.getNativeSize(int.class);

    /**
     * Return type for BSD sysctl kern.boottime
     */
    @FieldOrder({ "tv_sec", "tv_usec" })
    class Timeval extends Structure {
        public long tv_sec; // seconds
        public long tv_usec; // microseconds
    }

    /**
     * NetBSD's {@code RLIMIT_NOFILE} from {@code <sys/resource.h>} is 8, not 7 like Linux. JNA's inherited
     * {@code Resource.RLIMIT_NOFILE} from the platform module is the Linux value, so calling
     * {@code getrlimit(Resource.RLIMIT_NOFILE, …)} on NetBSD would query the wrong resource.
     */
    int RLIMIT_NOFILE = 8;

    /**
     * Returns the LWP (lightweight process/thread) ID of the calling thread.
     *
     * @return the LWP ID of the calling thread.
     */
    int _lwp_self();
}
