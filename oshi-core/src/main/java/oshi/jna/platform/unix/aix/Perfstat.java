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
package oshi.jna.platform.unix.aix;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * The perfstat API uses the perfstat kernel extension to extract various AIX®
 * performance metrics.
 *
 * System component information is also retrieved from the Object Data Manager
 * (ODM) and returned with the performance metrics.
 *
 * The perfstat API is thread–safe, and does not require root authority.
 */
public interface Perfstat extends Library {

    Perfstat INSTANCE = getInstance();

    static Perfstat getInstance() {
        int RTLD_MEMBER = 0x40000; // allows "lib.a(obj.o)" syntax
        int RTLD_GLOBAL = 0x10000;
        int RTLD_LAZY = 0x4;
        Map<String, Object> options = new HashMap<>();
        options.put(Library.OPTION_OPEN_FLAGS, RTLD_MEMBER | RTLD_GLOBAL | RTLD_LAZY);
        options = Collections.unmodifiableMap(options);
        try {
            return Native.load("/usr/lib/libperfstat.a(shr_64.o)", Perfstat.class, options);
        } catch (UnsatisfiedLinkError e) {
            // failed 64 bit, try 32 bit
        }
        return Native.load("/usr/lib/libperfstat.a(shr.o)", Perfstat.class, options);
    }

    int IDENTIFIER_LENGTH = 64;

    @FieldOrder({ "name" })
    class perfstat_id_t extends Structure {
        public byte[] name = new byte[IDENTIFIER_LENGTH];
    }

    @FieldOrder({ "ncpus", "ncpus_cfg", "description", "processorHZ", "user", "sys", "idle", "wait", "pswitch",
            "syscall", "sysread", "syswrite", "sysfork", "sysexec", "readch", "writech", "devintrs", "softintrs",
            "lbolt", "loadavg", "runque", "swpque", "bread", "bwrite", "lread", "lwrite", "phread", "phwrite", "runocc",
            "swpocc", "iget", "namei", "dirblk", "msg", "sema", "rcvint", "xmtint", "mdmint", "tty_rawinch",
            "tty_caninch", "tty_rawoutch", "ksched", "koverf", "kexit", "rbread", "rcread", "rbwrt", "rcwrt", "traps",
            "ncpus_high", "puser", "psys", "pidle", "pwait", "decrintrs", "mpcrintrs", "mpcsintrs", "phantintrs",
            "idle_donated_purr", "idle_donated_spurr", "busy_donated_purr", "busy_donated_spurr", "idle_stolen_purr",
            "idle_stolen_spurr", "busy_stolen_purr", "busy_stolen_spurr", "iowait", "physio", "twait", "hpi", "hpit",
            "puser_spurr", "psys_spurr", "pidle_spurr", "pwait_spurr", "spurrflag", "version", "tb_last",
            "purr_coalescing", "spurr_coalescing" })
    class perfstat_cpu_total_t extends Structure {
        public int ncpus; // number of active logical processors
        public int ncpus_cfg; // number of configured processors
        public byte[] description = new byte[IDENTIFIER_LENGTH]; // processor description (type/official name)
        public long processorHZ; // processor speed in Hz
        public long user; // raw total number of clock ticks spent in user mode
        public long sys; // raw total number of clock ticks spent in system mode
        public long idle; // raw total number of clock ticks spent idle
        public long wait; // raw total number of clock ticks spent waiting for I/O
        public long pswitch; // number of process switches (change in currently running process)
        public long syscall; // number of system calls executed
        public long sysread; // number of read system calls executed
        public long syswrite; // number of write system calls executed
        public long sysfork; // number of forks system calls executed
        public long sysexec; // number of execs system calls executed
        public long readch; // number of characters tranferred with read system call
        public long writech; // number of characters tranferred with write system call
        public long devintrs; // number of device interrupts
        public long softintrs; // number of software interrupts
        public NativeLong lbolt; // number of ticks since last reboot
        public long[] loadavg = new long[3]; // (1<<SBITS) times the average number of runnables processes during the
                                             // last 1, 5 and 15 minutes. To calculate the load average, divide the
                                             // numbers by (1<<SBITS). SBITS is defined in <sys/proc.h>.
        public long runque; // length of the run queue (processes ready)
        public long swpque; // ength of the swap queue (processes waiting to be paged in)
        public long bread; // number of blocks read
        public long bwrite; // number of blocks written
        public long lread; // number of logical read requests
        public long lwrite; // number of logical write requests
        public long phread; // number of physical reads (reads on raw devices)
        public long phwrite; // number of physical writes (writes on raw devices)
        public long runocc; // updated whenever runque is updated, i.e. the runqueue is occupied. This can
                            // be used to compute the simple average of ready processes
        public long swpocc; // updated whenever swpque is updated. i.e. the swpqueue is occupied. This can
                            // be used to compute the simple average processes waiting to be paged in
        public long iget; // number of inode lookups
        public long namei; // number of vnode lookup from a path name
        public long dirblk; // number of 512-byte block reads by the directory search routine to locate an
                            // entry for a file
        public long msg; // number of IPC message operations
        public long sema; // number of IPC semaphore operations
        public long rcvint; // number of tty receive interrupts
        public long xmtint; // number of tyy transmit interrupts
        public long mdmint; // number of modem interrupts
        public long tty_rawinch; // number of raw input characters
        public long tty_caninch; // number of canonical input characters (always zero)
        public long tty_rawoutch; // number of raw output characters
        public long ksched; // number of kernel processes created
        public long koverf; // kernel process creation attempts where:
                            // -the user has forked to their maximum limit
                            // -the configuration limit of processes has been reached
        public long kexit; // number of kernel processes that became zombies
        public long rbread; // number of remote read requests
        public long rcread; // number of cached remote reads
        public long rbwrt; // number of remote writes
        public long rcwrt; // number of cached remote writes
        public long traps; // number of traps
        public int ncpus_high; // index of highest processor online
        public long puser; // raw number of physical processor tics in user mode
        public long psys; // raw number of physical processor tics in system mode
        public long pidle; // raw number of physical processor tics idle
        public long pwait; // raw number of physical processor tics waiting for I/O
        public long decrintrs; // number of decrementer tics interrupts
        public long mpcrintrs; // number of mpc's received interrupts
        public long mpcsintrs; // number of mpc's sent interrupts
        public long phantintrs; // number of phantom interrupts
        public long idle_donated_purr; // number of idle cycles donated by a dedicated partition enabled for donation
        public long idle_donated_spurr; // number of idle spurr cycles donated by a dedicated partition enabled for
                                        // donation
        public long busy_donated_purr; // number of busy cycles donated by a dedicated partition enabled for donation
        public long busy_donated_spurr; // number of busy spurr cycles donated by a dedicated partition enabled for
                                        // donation
        public long idle_stolen_purr; // number of idle cycles stolen by the hypervisor from a dedicated partition
        public long idle_stolen_spurr; // number of idle spurr cycles stolen by the hypervisor from a dedicated
                                       // partition
        public long busy_stolen_purr; // number of busy cycles stolen by the hypervisor from a dedicated partition
        public long busy_stolen_spurr; // number of busy spurr cycles stolen by the hypervisor from a dedicated
                                       // partition
        public short iowait; // number of processes that are asleep waiting for buffered I/O
        public short physio; // number of processes waiting for raw I/O
        public long twait; // number of threads that are waiting for filesystem direct(cio)
        public long hpi; // number of hypervisor page-ins
        public long hpit; // Time spent in hypervisor page-ins (in nanoseconds)
        public long puser_spurr; // number of spurr cycles spent in user mode
        public long psys_spurr; // number of spurr cycles spent in kernel mode
        public long pidle_spurr; // number of spurr cycles spent in idle mode
        public long pwait_spurr; // number of spurr cycles spent in wait mode
        public int spurrflag; // set if running in spurr mode
        public long version; // version number (1, 2, etc.,)
        public long tb_last; // time base counter
        public long purr_coalescing; // If the calling partition is authorized to see pool wide statistics then PURR
                                     // cycles consumed to coalesce dataelse set to zero.
        public long spurr_coalescing; // If the calling partition isauthorized to see pool wide statistics then SPURR
                                      // cycles consumed to coalesce data else set to zero.
    }

    @FieldOrder({ "name", "user", "sys", "idle", "wait", "pswitch", "syscall", "sysread", "syswrite", "sysfork",
            "sysexec", "readch", "writech", "bread", "bwrite", "lread", "lwrite", "phread", "phwrite", "iget", "namei",
            "dirblk", "msg", "sema", "minfaults", "majfaults", "puser", "psys", "pidle", "pwait", "redisp_sd0",
            "redisp_sd1", "redisp_sd2", "redisp_sd3", "redisp_sd4", "redisp_sd5", "migration_push", "migration_S3grq",
            "migration_S3pul", "invol_cswitch", "vol_cswitch", "runque", "bound", "decrintrs", "mpcrintrs", "mpcsintrs",
            "devintrs", "softintrs", "phantintrs", "idle_donated_purr", "idle_donated_spurr", "busy_donated_purr",
            "busy_donated_spurr", "idle_stolen_purr", "idle_stolen_spurr", "busy_stolen_purr", "busy_stolen_spurr",
            "hpi", "hpit", "puser_spurr", "psys_spurr", "pidle_spurr", "pwait_spurr", "spurrflag", "localdispatch",
            "neardispatch", "fardispatch", "cswitches", "version", "tb_last" })
    class perfstat_cpu_t extends Structure {
        public byte[] name = new byte[IDENTIFIER_LENGTH]; // logical processor name (cpu0, cpu1, ..)
        public long user; // raw number of clock ticks spent in user mode
        public long sys; // raw number of clock ticks spent in system mode
        public long idle; // raw number of clock ticks spent idle
        public long wait; // raw number of clock ticks spent waiting for I/O
        public long pswitch; // number of context switches (changes of currently running process)
        public long syscall; // number of system calls executed
        public long sysread; // number of read system calls executed
        public long syswrite; // number of write system calls executed
        public long sysfork; // number of fork system call executed
        public long sysexec; // number of exec system call executed
        public long readch; // number of characters tranferred with read system call
        public long writech; // number of characters tranferred with write system call
        public long bread; // number of block reads
        public long bwrite; // number of block writes
        public long lread; // number of logical read requests
        public long lwrite; // number of logical write requests
        public long phread; // number of physical reads (reads on raw device)
        public long phwrite; // number of physical writes (writes on raw device)
        public long iget; // number of inode lookups
        public long namei; // number of vnode lookup from a path name
        public long dirblk; // number of 512-byte block reads by the directory search routine to locate an
                            // entry for a file
        public long msg; // number of IPC message operations
        public long sema; // number of IPC semaphore operations
        public long minfaults; // number of page faults with no I/O
        public long majfaults; // number of page faults with disk I/O
        public long puser; // raw number of physical processor tics in user mode
        public long psys; // raw number of physical processor tics in system mode
        public long pidle; // raw number of physical processor tics idle
        public long pwait; // raw number of physical processor tics waiting for I/O
        public long redisp_sd0; // number of thread redispatches within the scheduler affinity domain 0
        public long redisp_sd1; // number of thread redispatches within the scheduler affinity domain 1
        public long redisp_sd2; // number of thread redispatches within the scheduler affinity domain 2
        public long redisp_sd3; // number of thread redispatches within the scheduler affinity domain 3
        public long redisp_sd4; // number of thread redispatches within the scheduler affinity domain 4
        public long redisp_sd5; // number of thread redispatches within the scheduler affinity domain 5
        public long migration_push; // number of thread migrations from the local runque to another queue due to
                                    // starvation load balancing
        public long migration_S3grq; // number of thread migrations from the global runque to the local runque
                                     // resulting in a move accross scheduling domain 3
        public long migration_S3pul; // number of thread migrations from another processor's runque resulting in a
                                     // move accross scheduling domain 3
        public long invol_cswitch; // number of involuntary thread context switches
        public long vol_cswitch; // number of voluntary thread context switches
        public long runque; // number of threads on the runque
        public long bound; // number of bound threads
        public long decrintrs; // number of decrementer tics interrupts
        public long mpcrintrs; // number of mpc's received interrupts
        public long mpcsintrs; // number of mpc's sent interrupts
        public long devintrs; // number of device interrupts
        public long softintrs; // number of offlevel handlers called
        public long phantintrs; // number of phantom interrupts
        public long idle_donated_purr; // number of idle cycles donated by a dedicated partition enabled for donation
        public long idle_donated_spurr; // number of idle spurr cycles donated by a dedicated partition enabled for
                                        // donation
        public long busy_donated_purr; // number of busy cycles donated by a dedicated partition enabled for donation
        public long busy_donated_spurr; // number of busy spurr cycles donated by a dedicated partition enabled for
                                        // donation
        public long idle_stolen_purr; // number of idle cycles stolen by the hypervisor from a dedicated partition
        public long idle_stolen_spurr; // number of idle spurr cycles stolen by the hypervisor from a dedicated
                                       // partition
        public long busy_stolen_purr; // number of busy cycles stolen by the hypervisor from a dedicated partition
        public long busy_stolen_spurr; // number of busy spurr cycles stolen by the hypervisor from a dedicated
                                       // partition
        public long hpi; // number of hypervisor page-ins
        public long hpit; // Time spent in hypervisor page-ins (in nanoseconds)
        public long puser_spurr; // number of spurr cycles spent in user mode
        public long psys_spurr; // number of spurr cycles spent in kernel mode
        public long pidle_spurr; // number of spurr cycles spent in idle mode
        public long pwait_spurr; // number of spurr cycles spent in wait mode
        public int spurrflag; // set if running in spurr mode

        public long localdispatch; // number of local thread dispatches on this logical CPU
        public long neardispatch; // number of near thread dispatches on this logical CPU
        public long fardispatch; // number of far thread dispatches on this logical CPU
        public long cswitches; // Context switches
        public long version; // version number (1, 2, etc.,)
        public long tb_last; // timebase counter
    }

    /**
     * Retrieves total processor usage metrics
     *
     * @param name
     *            Reserved for future use, must be NULL
     * @param cpu
     *            Populated with structure
     * @param sizeof_struct
     *            Should be set to sizeof(perfstat_cpu_t)
     * @param desired_number
     *            Reserved for future use, must be set to 0 or 1
     * @return The return value is -1 in case of errors. Otherwise, the number of
     *         structures copied is returned. This is always 1.
     */
    int perfstat_cpu_total(perfstat_id_t name, perfstat_cpu_total_t cpu, int sizeof_struct, int desired_number);

    /**
     * Retrieves individual processor usage metrics
     *
     * @param name
     *            Structure containing empty string when collecting all cpu stats,
     *            or null to count CPUs
     * @param cpu
     *            Populated with structures, or null to count CPUs
     * @param sizeof_struct
     *            Should be set to sizeof(perfstat_cpu_t)
     * @param desired_number
     *            Set to 0 to count CPUs, 0 or 1 otherwise
     * @return The return value is -1 in case of errors. Otherwise, the number of
     *         structures copied is returned.
     */
    int perfstat_cpu(perfstat_id_t name, perfstat_cpu_t[] cpu, int sizeof_struct, int desired_number);
}
