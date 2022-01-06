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
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

/**
 * C library. This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 */
public interface AixLibc extends CLibrary {

    AixLibc INSTANCE = Native.load("c", AixLibc.class);

    int PRCLSZ = 8;
    int PRFNSZ = 16;
    int PRARGSZ = 80;

    @FieldOrder({ "pr_flag", "pr_flag2", "pr_nlwp", "pr__pad1", "pr_uid", "pr_euid", "pr_gid", "pr_egid", "pr_pid",
            "pr_ppid", "pr_pgid", "pr_sid", "pr_ttydev", "pr_addr", "pr_size", "pr_rssize", "pr_start", "pr_time",
            "pr_cid", "pr__pad2", "pr_argc", "pr_argv", "pr_envp", "pr_fname", "pr_psargs", "pr__pad", "pr_lwp" })
    class AixPsInfo extends Structure {
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
        public AIXLwpsInfo pr_lwp; // "representative" thread info

        public AixPsInfo() {
            super();
        }

        public AixPsInfo(byte[] bytes) {
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

    @FieldOrder({ "pr_lwpid", "pr_addr", "pr_wchan", "pr_flag", "pr_wtype", "pr_state", "pr_sname", "pr_nice", "pr_pri",
            "pr_policy", "pr_clname", "pr_onpro", "pr_bindpro" })
    class AIXLwpsInfo extends Structure {
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

        public AIXLwpsInfo() {
            super();
        }

        public AIXLwpsInfo(byte[] bytes) {
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
     * 64-bit timestruc required for psinfo structure
     */
    @FieldOrder({ "tv_sec", "tv_nsec", "pad" })
    class Timestruc extends Structure {
        public long tv_sec; // seconds
        public int tv_nsec; // nanoseconds
        public int pad; // nanoseconds
    }
}
