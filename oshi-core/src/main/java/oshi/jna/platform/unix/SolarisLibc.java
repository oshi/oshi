/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.unix;

import java.nio.ByteBuffer;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

import oshi.util.FileUtil;

/**
 * C library. This class should be considered non-API as it may be removed if/when its code is incorporated into the JNA
 * project.
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
     * Structure for psinfo file
     */
    class SolarisPsInfo {
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
        public int pr_filler; // 4 bytes reserved for future use
        public SolarisLwpsInfo pr_lwp; // information for representative lwp

        public SolarisPsInfo(ByteBuffer buff) {
            this.pr_flag = FileUtil.readIntFromBuffer(buff);
            this.pr_nlwp = FileUtil.readIntFromBuffer(buff);
            this.pr_pid = FileUtil.readIntFromBuffer(buff);
            this.pr_ppid = FileUtil.readIntFromBuffer(buff);
            this.pr_pgid = FileUtil.readIntFromBuffer(buff);
            this.pr_sid = FileUtil.readIntFromBuffer(buff);
            this.pr_uid = FileUtil.readIntFromBuffer(buff);
            this.pr_euid = FileUtil.readIntFromBuffer(buff);
            this.pr_gid = FileUtil.readIntFromBuffer(buff);
            this.pr_egid = FileUtil.readIntFromBuffer(buff);
            this.pr_addr = FileUtil.readPointerFromBuffer(buff);
            this.pr_size = FileUtil.readSizeTFromBuffer(buff);
            this.pr_rssize = FileUtil.readSizeTFromBuffer(buff);
            this.pr_rssizepriv = FileUtil.readSizeTFromBuffer(buff);
            this.pr_ttydev = FileUtil.readNativeLongFromBuffer(buff);
            this.pr_pctcpu = FileUtil.readShortFromBuffer(buff);
            this.pr_pctmem = FileUtil.readShortFromBuffer(buff);
            // Force 8 byte alignment
            if (Native.LONG_SIZE > 4) {
                FileUtil.readIntFromBuffer(buff);
            }
            this.pr_start = new Timestruc(buff);
            this.pr_time = new Timestruc(buff);
            this.pr_ctime = new Timestruc(buff);
            FileUtil.readByteArrayFromBuffer(buff, this.pr_fname);
            FileUtil.readByteArrayFromBuffer(buff, this.pr_psargs);
            this.pr_wstat = FileUtil.readIntFromBuffer(buff);
            this.pr_argc = FileUtil.readIntFromBuffer(buff);
            this.pr_argv = FileUtil.readPointerFromBuffer(buff);
            this.pr_envp = FileUtil.readPointerFromBuffer(buff);
            this.pr_dmodel = FileUtil.readByteFromBuffer(buff);
            FileUtil.readByteArrayFromBuffer(buff, this.pr_pad2);
            this.pr_taskid = FileUtil.readIntFromBuffer(buff);
            this.pr_projid = FileUtil.readIntFromBuffer(buff);
            this.pr_nzomb = FileUtil.readIntFromBuffer(buff);
            this.pr_poolid = FileUtil.readIntFromBuffer(buff);
            this.pr_zoneid = FileUtil.readIntFromBuffer(buff);
            this.pr_contract = FileUtil.readIntFromBuffer(buff);
            this.pr_filler = FileUtil.readIntFromBuffer(buff);
            this.pr_lwp = new SolarisLwpsInfo(buff);
        }
    }

    /**
     * Nested Structure for psinfo file
     */
    class SolarisLwpsInfo {
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

        public SolarisLwpsInfo(ByteBuffer buff) {
            this.pr_flag = FileUtil.readIntFromBuffer(buff);
            this.pr_lwpid = FileUtil.readIntFromBuffer(buff);
            this.pr_addr = FileUtil.readPointerFromBuffer(buff);
            this.pr_wchan = FileUtil.readPointerFromBuffer(buff);
            this.pr_stype = FileUtil.readByteFromBuffer(buff);
            this.pr_state = FileUtil.readByteFromBuffer(buff);
            this.pr_sname = FileUtil.readByteFromBuffer(buff);
            this.pr_nice = FileUtil.readByteFromBuffer(buff);
            this.pr_syscall = FileUtil.readShortFromBuffer(buff);
            this.pr_oldpri = FileUtil.readByteFromBuffer(buff);
            this.pr_cpu = FileUtil.readByteFromBuffer(buff);
            this.pr_pri = FileUtil.readIntFromBuffer(buff);
            this.pr_pctcpu = FileUtil.readShortFromBuffer(buff);
            this.pr_pad = FileUtil.readShortFromBuffer(buff);
            this.pr_start = new Timestruc(buff);
            this.pr_time = new Timestruc(buff);
            FileUtil.readByteArrayFromBuffer(buff, this.pr_clname);
            FileUtil.readByteArrayFromBuffer(buff, this.pr_oldname);
            this.pr_onpro = FileUtil.readIntFromBuffer(buff);
            this.pr_bindpro = FileUtil.readIntFromBuffer(buff);
            this.pr_bindpset = FileUtil.readIntFromBuffer(buff);
            this.pr_lgrp = FileUtil.readIntFromBuffer(buff);
            this.pr_last_onproc = FileUtil.readLongFromBuffer(buff);
            FileUtil.readByteArrayFromBuffer(buff, this.pr_name);
        }
    }

    /**
     * Structure for usage file
     */
    class SolarisPrUsage {
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

        public SolarisPrUsage(ByteBuffer buff) {
            this.pr_lwpid = FileUtil.readIntFromBuffer(buff);
            this.pr_count = FileUtil.readIntFromBuffer(buff);
            this.pr_tstamp = new Timestruc(buff);
            this.pr_create = new Timestruc(buff);
            this.pr_term = new Timestruc(buff);
            this.pr_rtime = new Timestruc(buff);
            this.pr_utime = new Timestruc(buff);
            this.pr_stime = new Timestruc(buff);
            this.pr_ttime = new Timestruc(buff);
            this.pr_tftime = new Timestruc(buff);
            this.pr_dftime = new Timestruc(buff);
            this.pr_kftime = new Timestruc(buff);
            this.pr_ltime = new Timestruc(buff);
            this.pr_slptime = new Timestruc(buff);
            this.pr_wtime = new Timestruc(buff);
            this.pr_stoptime = new Timestruc(buff);
            for (int i = 0; i < filltime.length; i++) {
                this.filltime[i] = new Timestruc(buff);
            }
            this.pr_minf = FileUtil.readNativeLongFromBuffer(buff);
            this.pr_majf = FileUtil.readNativeLongFromBuffer(buff);
            this.pr_nswap = FileUtil.readNativeLongFromBuffer(buff);
            this.pr_inblk = FileUtil.readNativeLongFromBuffer(buff);
            this.pr_oublk = FileUtil.readNativeLongFromBuffer(buff);
            this.pr_msnd = FileUtil.readNativeLongFromBuffer(buff);
            this.pr_mrcv = FileUtil.readNativeLongFromBuffer(buff);
            this.pr_sigs = FileUtil.readNativeLongFromBuffer(buff);
            this.pr_vctx = FileUtil.readNativeLongFromBuffer(buff);
            this.pr_ictx = FileUtil.readNativeLongFromBuffer(buff);
            this.pr_sysc = FileUtil.readNativeLongFromBuffer(buff);
            this.pr_ioch = FileUtil.readNativeLongFromBuffer(buff);
            for (int i = 0; i < filler.length; i++) {
                this.filler[i] = FileUtil.readNativeLongFromBuffer(buff);
            }
        }
    }

    /**
     * 32/64-bit timestruc required for psinfo and lwpsinfo structures
     */
    class Timestruc {
        public NativeLong tv_sec; // seconds
        public NativeLong tv_nsec; // nanoseconds

        public Timestruc(ByteBuffer buff) {
            this.tv_sec = FileUtil.readNativeLongFromBuffer(buff);
            this.tv_nsec = FileUtil.readNativeLongFromBuffer(buff);
        }
    }

    /**
     * Returns the thread ID of the calling thread.
     *
     * @return the thread ID of the calling thread.
     */
    int thr_self();
}
