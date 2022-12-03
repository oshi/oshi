/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.unix;

import java.nio.ByteBuffer;

import com.sun.jna.Native;

import oshi.util.FileUtil;

/**
 * C library. This class should be considered non-API as it may be removed if/when its code is incorporated into the JNA
 * project.
 */
public interface AixLibc extends CLibrary {

    AixLibc INSTANCE = Native.load("c", AixLibc.class);

    int PRCLSZ = 8;
    int PRFNSZ = 16;
    int PRARGSZ = 80;

    class AixPsInfo {
        public int pr_flag; // process flags from proc struct p_flag
        public int pr_flag2; // process flags from proc struct p_flag2
        public int pr_nlwp; // number of threads in process
        public int pr__pad1; // reserved for future use
        public long pr_uid; // real user id
        public long pr_euid; // effective user id
        public long pr_gid; // real group id
        public long pr_egid; // effective group id
        public long pr_pid; // unique process id
        public long pr_ppid; // process id of parent
        public long pr_pgid; // pid of process group leader
        public long pr_sid; // session id
        public long pr_ttydev; // controlling tty device
        public long pr_addr; // internal address of proc struct
        public long pr_size; // size of process image in KB (1024) units
        public long pr_rssize; // resident set size in KB (1024) units
        public Timestruc pr_start; // process start time, time since epoch
        public Timestruc pr_time; // usr+sys cpu time for this process
        public short pr_cid; // corral id
        public short pr__pad2; // reserved for future use
        public int pr_argc; // initial argument count
        public long pr_argv; // address of initial argument vector in user process
        public long pr_envp; // address of initial environment vector in user process
        public byte[] pr_fname = new byte[PRFNSZ]; // last component of exec()ed pathname
        public byte[] pr_psargs = new byte[PRARGSZ]; // initial characters of arg list
        public long[] pr__pad = new long[8]; // reserved for future use
        public AixLwpsInfo pr_lwp; // "representative" thread info

        public AixPsInfo(ByteBuffer buff) {
            this.pr_flag = FileUtil.readIntFromBuffer(buff);
            this.pr_flag2 = FileUtil.readIntFromBuffer(buff);
            this.pr_nlwp = FileUtil.readIntFromBuffer(buff);
            this.pr__pad1 = FileUtil.readIntFromBuffer(buff);
            this.pr_uid = FileUtil.readLongFromBuffer(buff);
            this.pr_euid = FileUtil.readLongFromBuffer(buff);
            this.pr_gid = FileUtil.readLongFromBuffer(buff);
            this.pr_egid = FileUtil.readLongFromBuffer(buff);
            this.pr_pid = FileUtil.readLongFromBuffer(buff);
            this.pr_ppid = FileUtil.readLongFromBuffer(buff);
            this.pr_pgid = FileUtil.readLongFromBuffer(buff);
            this.pr_sid = FileUtil.readLongFromBuffer(buff);
            this.pr_ttydev = FileUtil.readLongFromBuffer(buff);
            this.pr_addr = FileUtil.readLongFromBuffer(buff);
            this.pr_size = FileUtil.readLongFromBuffer(buff);
            this.pr_rssize = FileUtil.readLongFromBuffer(buff);
            this.pr_start = new Timestruc(buff);
            this.pr_time = new Timestruc(buff);
            this.pr_cid = FileUtil.readShortFromBuffer(buff);
            this.pr__pad2 = FileUtil.readShortFromBuffer(buff);
            this.pr_argc = FileUtil.readIntFromBuffer(buff);
            this.pr_argv = FileUtil.readLongFromBuffer(buff);
            this.pr_envp = FileUtil.readLongFromBuffer(buff);
            FileUtil.readByteArrayFromBuffer(buff, this.pr_fname);
            FileUtil.readByteArrayFromBuffer(buff, this.pr_psargs);
            for (int i = 0; i < pr__pad.length; i++) {
                this.pr__pad[i] = FileUtil.readLongFromBuffer(buff);
            }
            this.pr_lwp = new AixLwpsInfo(buff);
        }

    }

    class AixLwpsInfo {
        public long pr_lwpid; // thread id
        public long pr_addr; // internal address of thread
        public long pr_wchan; // wait addr for sleeping thread
        public int pr_flag; // thread flags
        public byte pr_wtype; // type of thread wait
        public byte pr_state; // numeric scheduling state
        public byte pr_sname; // printable character representing pr_state
        public byte pr_nice; // nice for cpu usage
        public int pr_pri; // priority, high value = high priority
        public int pr_policy; // scheduling policy
        public byte[] pr_clname = new byte[PRCLSZ]; // printable character representing pr_policy
        public int pr_onpro; // processor on which thread last ran
        public int pr_bindpro; // processor to which thread is bound

        public AixLwpsInfo(ByteBuffer buff) {
            this.pr_lwpid = FileUtil.readLongFromBuffer(buff);
            this.pr_addr = FileUtil.readLongFromBuffer(buff);
            this.pr_wchan = FileUtil.readLongFromBuffer(buff);
            this.pr_flag = FileUtil.readIntFromBuffer(buff);
            this.pr_wtype = FileUtil.readByteFromBuffer(buff);
            this.pr_state = FileUtil.readByteFromBuffer(buff);
            this.pr_sname = FileUtil.readByteFromBuffer(buff);
            this.pr_nice = FileUtil.readByteFromBuffer(buff);
            this.pr_pri = FileUtil.readIntFromBuffer(buff);
            this.pr_policy = FileUtil.readIntFromBuffer(buff);
            FileUtil.readByteArrayFromBuffer(buff, this.pr_clname);
            this.pr_onpro = FileUtil.readIntFromBuffer(buff);
            this.pr_bindpro = FileUtil.readIntFromBuffer(buff);
        }
    }

    /**
     * 64-bit timestruc required for psinfo structure
     */
    class Timestruc {
        public long tv_sec; // seconds
        public int tv_nsec; // nanoseconds
        public int pad; // nanoseconds

        public Timestruc(ByteBuffer buff) {
            this.tv_sec = FileUtil.readLongFromBuffer(buff);
            this.tv_nsec = FileUtil.readIntFromBuffer(buff);
            this.pad = FileUtil.readIntFromBuffer(buff);
        }
    }

    /**
     * Returns the caller's kernel thread ID.
     *
     * @return the caller's kernel thread ID.
     */
    int thread_self();
}
