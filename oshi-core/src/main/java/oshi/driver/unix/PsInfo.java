/*
 * MIT License
 *
 * Copyright (c) 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.unix;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystem.ProcessFiltering;
import oshi.software.os.OperatingSystem.ProcessSorting;

public class PsInfo {

    /*-
    @FieldOrder({ "pr_flag", "pr_flag2", "pr_nlwp", "pr_uid", "pr_euid", "pr_gid", "pr_egid", "pr_argc", "pr_pid",
            "pr_ppid", "pr_pgid", "pr_sid", "pr_ttydev", "pr_addr", "pr_size", "pr_rssize", "pr_timestruc64_t",
            "pr_timestruc64_t", "pr_argv", "pr_envp", "pr_fname", "pr_psargs", "pr__pad[8]",
            "lwpsinfo" })
    class AIXPsInfo extends Structure {
        public int pr_flag; // process flags from proc struct p_flag
        public int pr_flag2; // process flags from proc struct p_flag2
        public int pr_nlwp; // number of threads in process
        public uid_t pr_uid; // real user id
        public uid_t pr_euid; // effective user id
        public gid_t pr_gid; // real group id
        public gid_t pr_egid; // effective group id
        public int pr_argc; // initial argument count
        public long pr_pid; // unique process id
        public long pr_ppid; // process id of parent
        public long pr_pgid; // pid of process group leader
        public long pr_sid; // session id
        public dev64_t pr_ttydev; // controlling tty device
        public long pr_addr; // internal address of proc struct
        public long pr_size; // size of process image in KB (1024) units
        public long pr_rssize; // resident set size in KB (1024) units
        public struct pr_timestruc64_t; // process start time, time since epoch
        public struct pr_timestruc64_t; // usr+sys cpu time for this process
        public long pr_argv; // address of initial argument vector in user process
        public long pr_envp; // address of initial environment vector in user process
        public byte pr_fname[PRFNSZ]; // last component of exec()ed pathname
        public byte pr_psargs[PRARGSZ]; // initial characters of arg list
        public long pr__pad[8]; // reserved for future use
        public struct lwpsinfo; // "representative" thread info
    }
    */

    public static void main(String[] args) {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        int pid = os.getProcessId();
        os.getProcess(pid);

        for (OSProcess proc : os.getProcesses(ProcessFiltering.VALID_PROCESS, ProcessSorting.CPU_DESC, -1)) {
            System.out.println(proc.getProcessID() + " " + proc.getName() + ": " + proc.getUser() + "/"
                    + proc.getGroup() + " " + proc.getState() + " " + (proc.getUserTime() / (double) proc.getUpTime()));
        }
    }
}
