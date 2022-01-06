/*
 * MIT License
 *
 * Copyright (c) 2021-2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.jna.platform.unix;

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * C library. This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 */
public interface SolarisLibc extends CLibrary {

    SolarisLibc INSTANCE = Native.load("c", SolarisLibc.class);

    int UTX_USERSIZE = 32;
    int UTX_LINESIZE = 32;
    int UTX_IDSIZE = 4;
    int UTX_HOSTSIZE = 257;

    int PRCLSZ = 8;
    int PRFNSZ = 16;
    int PRLNSZ = 32;
    int PRARGSZ = 80;

    /**
     * Connection info
     */
    @FieldOrder({ "ut_user", "ut_id", "ut_line", "ut_pid", "ut_type", "ut_tv", "ut_session", "ut_syslen", "ut_host" })
    class SolarisUtmpx extends Structure {
        public byte[] ut_user = new byte[UTX_USERSIZE]; // user login name
        public byte[] ut_id = new byte[UTX_IDSIZE]; // etc/inittab id (usually line #)
        public byte[] ut_line = new byte[UTX_LINESIZE]; // device name
        public int ut_pid; // process id
        public short ut_type; // type of entry
        public Timeval ut_tv; // time entry was made
        public int ut_session; // session ID, used for windowing
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
     * Reads a line from the current file position in the utmp file. It returns a
     * pointer to a structure containing the fields of the line.
     * <p>
     * Not thread safe
     *
     * @return a {@link SolarisUtmpx} on success, and NULL on failure (which
     *         includes the "record not found" case)
     */
    SolarisUtmpx getutxent();

    /**
     * Structure for psinfo file
     */
    @FieldOrder({ "pr_flag", "pr_nlwp", "pr_pid", "pr_ppid", "pr_pgid", "pr_sid", "pr_uid", "pr_euid", "pr_gid",
            "pr_egid", "pr_addr", "pr_size", "pr_rssize", "pr_rssizepriv", "pr_ttydev", "pr_pctcpu", "pr_pctmem",
            "pr_start", "pr_time", "pr_ctime", "pr_fname", "pr_psargs", "pr_wstat", "pr_argc", "pr_argv", "pr_envp",
            "pr_dmodel", "pr_pad2", "pr_taskid", "pr_projid", "pr_nzomb", "pr_poolid", "pr_zoneid", "pr_contract",
            "pr_filler", "pr_lwp" })
    class SolarisPsInfo extends Structure {
        public int pr_flag; // process flags (DEPRECATED; do not use)
        public int pr_nlwp; // number of active lwps in the process
        public int pr_pid; // unique process id
        public int pr_ppid; // process id of parent
        public int pr_pgid; // pid of process group leader
        public int pr_sid; // session id
        public int pr_uid; // real user id
        public int pr_euid; // effective user id
        public int pr_gid; // real group id
        public int pr_egid; // effective group id
        public Pointer pr_addr; // address of process
        public size_t pr_size; // size of process image in Kbytes
        public size_t pr_rssize; // resident set size in Kbytes
        public size_t pr_rssizepriv; // resident set size of private mappings
        public NativeLong pr_ttydev; // controlling tty device (or PRNODEV)
        // The following percent numbers are 16-bit binary
        // fractions [0 .. 1] with the binary point to the
        // right of the high-order bit (1.0 == 0x8000)
        public short pr_pctcpu; // % of recent cpu time used by all lwps
        public short pr_pctmem; // % of system memory used by process
        public Timestruc pr_start; // process start time, from the epoch
        public Timestruc pr_time; // usr+sys cpu time for this process
        public Timestruc pr_ctime; // usr+sys cpu time for reaped children
        public byte[] pr_fname = new byte[PRFNSZ]; // name of exec'ed file
        public byte[] pr_psargs = new byte[PRARGSZ]; // initial characters of arg list
        public int pr_wstat; // if zombie, the wait() status
        public int pr_argc; // initial argument count
        public Pointer pr_argv; // address of initial argument vector
        public Pointer pr_envp; // address of initial environment vector
        public byte pr_dmodel; // data model of the process
        public byte[] pr_pad2 = new byte[3];
        public int pr_taskid; // task id
        public int pr_projid; // project id
        public int pr_nzomb; // number of zombie lwps in the process
        public int pr_poolid; // pool id
        public int pr_zoneid; // zone id
        public int pr_contract; // process contract id
        public int[] pr_filler = new int[1]; // reserved for future use
        public SolarisLwpsInfo pr_lwp; // information for representative lwp

        public SolarisPsInfo() {
            super();
        }

        public SolarisPsInfo(byte[] bytes) {
            super();
            // Truncate bytes and pad with 0 if necessary
            byte[] structBytes = new byte[size()];
            System.arraycopy(bytes, 0, structBytes, 0, structBytes.length);
            // Write bytes to native
            this.getPointer().write(0, structBytes, 0, structBytes.length);
            // Read bytes to struct
            read();
        }
    }

    /**
     * Nested Structure for psinfo file
     */
    @FieldOrder({ "pr_flag", "pr_lwpid", "pr_addr", "pr_wchan", "pr_stype", "pr_state", "pr_sname", "pr_nice",
            "pr_syscall", "pr_oldpri", "pr_cpu", "pr_pri", "pr_pctcpu", "pr_pad", "pr_start", "pr_time", "pr_clname",
            "pr_oldname", "pr_onpro", "pr_bindpro", "pr_bindpset", "pr_lgrp", "pr_last_onproc", "pr_name" })
    class SolarisLwpsInfo extends Structure {
        public int pr_flag; // lwp flags (DEPRECATED; do not use)
        public int pr_lwpid; // lwp id
        public Pointer pr_addr; // DEPRECATED was internal address of lwp
        public Pointer pr_wchan; // DEPRECATED was wait addr for sleeping lwp
        public byte pr_stype; // synchronization event type
        public byte pr_state; // numeric lwp state
        public byte pr_sname; // printable character for pr_state
        public byte pr_nice; // nice for cpu usage
        public short pr_syscall; // system call number (if in syscall)
        public byte pr_oldpri; // pre-SVR4, low value is high priority
        public byte pr_cpu; // pre-SVR4, cpu usage for scheduling
        public int pr_pri; // priority, high value = high priority
        // The following percent numbers are 16-bit binary
        // fractions [0 .. 1] with the binary point to the
        // right of the high-order bit (1.0 == 0x8000)
        public short pr_pctcpu; // % of recent cpu time used by this lwp
        public short pr_pad;
        public Timestruc pr_start; // lwp start time, from the epoch
        public Timestruc pr_time; // cpu time for this lwp
        public byte[] pr_clname = new byte[PRCLSZ]; // scheduling class name
        public byte[] pr_oldname = new byte[PRFNSZ]; // binary compatibility -- unused
        public int pr_onpro; // processor which last ran this lwp
        public int pr_bindpro; // processor to which lwp is bound
        public int pr_bindpset; // processor set to which lwp is bound
        public int pr_lgrp; // home lgroup
        public long pr_last_onproc; // Timestamp of when thread last ran on a processor
        public byte[] pr_name = new byte[PRLNSZ]; // name of system lwp

        public SolarisLwpsInfo() {
            super();
        }

        public SolarisLwpsInfo(byte[] bytes) {
            super();
            // Truncate bytes and pad with 0 if necessary
            byte[] structBytes = new byte[size()];
            System.arraycopy(bytes, 0, structBytes, 0, structBytes.length);
            // Write bytes to native
            this.getPointer().write(0, structBytes, 0, structBytes.length);
            // Read bytes to struct
            read();
        }
    }

    /**
     * Structure for usage file
     */
    @FieldOrder({ "pr_lwpid", "pr_count", "pr_tstamp", "pr_create", "pr_term", "pr_rtime", "pr_utime", "pr_stime",
            "pr_ttime", "pr_tftime", "pr_dftime", "pr_kftime", "pr_ltime", "pr_slptime", "pr_wtime", "pr_stoptime",
            "filltime", "pr_minf", "pr_majf", "pr_nswap", "pr_inblk", "pr_oublk", "pr_msnd", "pr_mrcv", "pr_sigs",
            "pr_vctx", "pr_ictx", "pr_sysc", "pr_ioch", "filler" })
    class SolarisPrUsage extends Structure {
        public int pr_lwpid; // lwp id. 0: process or defunct
        public int pr_count; // number of contributing lwps
        public Timestruc pr_tstamp; // current time stamp
        public Timestruc pr_create; // process/lwp creation time stamp
        public Timestruc pr_term; // process/lwp termination time stamp
        public Timestruc pr_rtime; // total lwp real (elapsed) time
        public Timestruc pr_utime; // user level cpu time
        public Timestruc pr_stime; // system call cpu time
        public Timestruc pr_ttime; // other system trap cpu time
        public Timestruc pr_tftime; // text page fault sleep time
        public Timestruc pr_dftime; // data page fault sleep time
        public Timestruc pr_kftime; // kernel page fault sleep time
        public Timestruc pr_ltime; // user lock wait sleep time
        public Timestruc pr_slptime; // all other sleep time
        public Timestruc pr_wtime; // wait-cpu (latency) time
        public Timestruc pr_stoptime; // stopped time
        public Timestruc[] filltime = new Timestruc[6]; // filler for future expansion
        public NativeLong pr_minf; // minor page faults
        public NativeLong pr_majf; // major page faults
        public NativeLong pr_nswap; // swaps
        public NativeLong pr_inblk; // input blocks
        public NativeLong pr_oublk; // output blocks
        public NativeLong pr_msnd; // messages sent
        public NativeLong pr_mrcv; // messages received
        public NativeLong pr_sigs; // signals received
        public NativeLong pr_vctx; // voluntary context switches
        public NativeLong pr_ictx; // involuntary context switches
        public NativeLong pr_sysc; // system calls
        public NativeLong pr_ioch; // chars read and written
        public NativeLong[] filler = new NativeLong[10]; // filler for future expansion

        public SolarisPrUsage() {
            super();
        }

        public SolarisPrUsage(byte[] bytes) {
            super();
            // Truncate bytes and pad with 0 if necessary
            byte[] structBytes = new byte[size()];
            System.arraycopy(bytes, 0, structBytes, 0, structBytes.length);
            // Write bytes to native
            this.getPointer().write(0, structBytes, 0, structBytes.length);
            // Read bytes to struct
            read();
        }
    }

    /**
     * 32/64-bit timestruc required for psinfo and lwpsinfo structures
     */
    @FieldOrder({ "tv_sec", "tv_nsec" })
    class Timestruc extends Structure {
        public NativeLong tv_sec; // seconds
        public NativeLong tv_nsec; // nanoseconds
    }
}
