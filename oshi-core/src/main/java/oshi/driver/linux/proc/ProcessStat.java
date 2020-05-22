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
package oshi.driver.linux.proc;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;
import oshi.util.tuples.Triplet;

/**
 * Utility to read process statistics from {@code /proc/[pid]/stat}
 */
@ThreadSafe
public final class ProcessStat {

    private static final Pattern DIGITS = Pattern.compile("\\d+");

    /**
     * Enum corresponding to the fields in the output of {@code /proc/[pid]/stat}
     */
    public enum PidStat {
        /**
         * The process ID.
         */
        PID,
        /**
         * The filename of the executable.
         */
        COMM,
        /**
         * One of the following characters, indicating process state:
         * <p>
         * R Running
         * <p>
         * S Sleeping in an interruptible wait
         * <p>
         * D Waiting in uninterruptible disk sleep
         * <p>
         * Z Zombie
         * <p>
         * T Stopped (on a signal) or (before Linux 2.6.33) trace stopped
         * <p>
         * t Tracing stop (Linux 2.6.33 onward)
         * <p>
         * W Paging (only before Linux 2.6.0)
         * <p>
         * X Dead (from Linux 2.6.0 onward)
         * <p>
         * x Dead (Linux 2.6.33 to 3.13 only)
         * <p>
         * K Wakekill (Linux 2.6.33 to 3.13 only)
         * <p>
         * W Waking (Linux 2.6.33 to 3.13 only)
         * <p>
         * P Parked (Linux 3.9 to 3.13 only)
         */
        STATE,
        /**
         * The PID of the parent of this process.
         */
        PPID,
        /**
         * The process group ID of the process.
         */
        PGRP,
        /**
         * The session ID of the process.
         */
        SESSION,
        /**
         * The controlling terminal of the process. (The minor device number is
         * contained in the combination of bits 31 to 20 and 7 to 0; the major device
         * number is in bits 15 to 8.)
         */
        TTY_NR,
        /**
         * The ID of the foreground process group of the controlling terminal of the
         * process.
         */
        PTGID,
        /**
         * The kernel flags word of the process. For bit meanings, see the PF_* defines
         * in the Linux kernel source file include/linux/sched.h. Details depend on the
         * kernel version.
         */
        FLAGS,
        /**
         * The number of minor faults the process has made which have not required
         * loading a memory page from disk.
         */
        MINFLT,
        /**
         * The number of minor faults that the process's waited-for children have made.
         */
        CMINFLT,
        /**
         * The number of major faults the process has made which have required loading a
         * memory page from disk.
         */
        MAJFLT,
        /**
         * The number of major faults that the process's waited-for children have made.
         */
        CMAJFLT,
        /**
         * Amount of time that this process has been scheduled in user mode, measured in
         * clock ticks. This includes guest time, cguest_time (time spent running a
         * virtual CPU), so that applications that are not aware of the guest time field
         * do not lose that time from their calculations.
         */
        UTIME,
        /**
         * Amount of time that this process has been scheduled in kernel mode, measured
         * in clock ticks.
         */
        STIME,
        /**
         * Amount of time that this process's waited-for children have been scheduled in
         * user mode, measured in clock ticks. This includes guest time, cguest_time
         * (time spent running a virtual CPU).
         */
        CUTIME,
        /**
         * Amount of time that this process's waited-for children have been scheduled in
         * kernel mode, measured in clock ticks.
         */
        CSTIME,
        /**
         * For processes running a real-time scheduling policy (policy below; see
         * sched_setscheduler(2)), this is the negated scheduling priority, minus one;
         * that is, a number in the range -2 to -100, corresponding to real-time
         * priorities 1 to 99. For processes running under a non-real-time scheduling
         * policy, this is the raw nice value (setpriority(2)) as represented in the
         * kernel. The kernel stores nice values as numbers in the range 0 (high) to 39
         * (low), corresponding to the user-visible nice range of -20 to 19.
         */
        PRIORITY,
        /**
         * The nice value (see setpriority(2)), a value in the range 19 (low priority)
         * to -20 (high priority).
         */
        NICE,
        /**
         * Number of threads in this process.
         */
        NUM_THREADS,
        /**
         * The time in jiffies before the next SIGALRM is sent to the process due to an
         * interval timer. Since ker‐nel 2.6.17, this field is no longer maintained, and
         * is hard coded as 0.
         */
        ITREALVALUE,
        /**
         * The time the process started after system boot, in clock ticks.
         */
        STARTTIME,
        /**
         * Virtual memory size in bytes.
         */
        VSIZE,
        /**
         * Resident Set Size: number of pages the process has in real memory. This is
         * just the pages which count toward text, data, or stack space. This does not
         * include pages which have not been demand-loaded in, or which are swapped out.
         */
        RSS,
        /**
         * Current soft limit in bytes on the rss of the process; see the description of
         * RLIMIT_RSS in getrlimit(2).
         */
        RSSLIM,
        /**
         * The address above which program text can run.
         */
        STARTCODE,

        /**
         * The address below which program text can run.
         */
        ENDCODE,
        /**
         * The address of the start (i.e., bottom) of the stack.
         */
        STARTSTACK,
        /**
         * The current value of ESP (stack pointer), as found in the kernel stack page
         * for the process.
         */
        KSTKESP,
        /**
         * The current EIP (instruction pointer).
         */
        KSTKEIP,
        /**
         * The bitmap of pending signals, displayed as a decimal number. Obsolete,
         * because it does not provide information on real-time signals; use
         * /proc/[pid]/status instead.
         */
        SIGNAL,
        /**
         * The bitmap of blocked signals, displayed as a decimal number. Obsolete,
         * because it does not provide information on real-time signals; use
         * /proc/[pid]/status instead.
         */
        BLOCKED,
        /**
         * The bitmap of ignored signals, displayed as a decimal number. Obsolete,
         * because it does not provide information on real-time signals; use
         * /proc/[pid]/status instead.
         */
        SIGIGNORE,
        /**
         * The bitmap of caught signals, displayed as a decimal number. Obsolete,
         * because it does not provide information on real-time signals; use
         * /proc/[pid]/status instead.
         */
        SIGCATCH,
        /**
         * This is the "channel" in which the process is waiting. It is the address of a
         * location in the kernel where the process is sleeping. The corresponding
         * symbolic name can be found in /proc/[pid]/wchan.
         */
        WCHAN,
        /**
         * Number of pages swapped (not maintained).
         */
        NSWAP,
        /**
         * Cumulative nswap for child processes (not maintained).
         */
        CNSWAP,
        /**
         * Signal to be sent to parent when we die.
         */
        EXIT_SIGNAL,
        /**
         * CPU number last executed on.
         */
        PROCESSOR,
        /**
         * Real-time scheduling priority, a number in the range 1 to 99 for processes
         * scheduled under a real-time policy, or 0, for non-real-time processes (see
         * sched_setscheduler(2)).
         */
        RT_PRIORITY,
        /**
         * Scheduling policy (see sched_setscheduler(2)). Decode using the SCHED_*
         * constants in linux/sched.h.
         */
        POLICY,
        /**
         * Aggregated block I/O delays, measured in clock ticks (centiseconds).
         */
        DELAYACCT_BLKIO_TICKS,
        /**
         * Guest time of the process (time spent running a vir‐ tual CPU for a guest
         * operating system), measured in clock ticks.
         */
        GUEST_TIME,
        /**
         * Guest time of the process's children, measured in clock ticks.
         */
        CGUEST_TIME,
        /**
         * Address above which program initialized and uninitialized (BSS) data are
         * placed.
         */
        START_DATA,
        /**
         * Address below which program initialized and uninitialized (BSS) data are
         * placed.
         */
        END_DATA,
        /**
         * Address above which program heap can be expanded with brk(2).
         */
        START_BRK,
        /**
         * Address above which program command-line arguments (argv) are placed.
         */
        ARG_START,

        /**
         * Address below program command-line arguments (argv) are placed.
         */
        ARG_END,

        /**
         * Address above which program environment is placed.
         */
        ENV_START,

        /**
         * Address below which program environment is placed.
         */
        ENV_END,

        /**
         * The thread's exit status in the form reported by waitpid(2).
         */
        EXIT_CODE;
    }

    /**
     * Enum corresponding to the fields in the output of {@code /proc/[pid]/statm}
     */
    public enum PidStatM {
        /**
         * Total program size
         */
        SIZE,
        /**
         * Resident set size
         */
        RESIDENT,
        /**
         * Number of resident shared pages (i.e., backed by a file)
         */
        SHARED,
        /**
         * Text (code)
         */
        TEXT,
        /**
         * Library (unused since Linux 2.6; always 0)
         */
        LIB,
        /**
         * Data + stack
         */
        DATA,
        /**
         * Dirty pages (unused since Linux 2.6; always 0)
         */
        DT;
    }

    /**
     * Constant defining the number of integer values in {@code /proc/pid/stat}. 2.6
     * Kernel has 44 elements, 3.3 has 47, and 3.5 has 52.
     */
    public static final int PROC_PID_STAT_LENGTH;
    static {
        String stat = FileUtil.getStringFromFile(ProcPath.SELF_STAT);
        if (!stat.isEmpty() && stat.contains(")")) {
            // add 3 to account for pid, process name in prarenthesis, and state
            PROC_PID_STAT_LENGTH = ParseUtil.countStringToLongArray(stat, ' ') + 3;
        } else {
            // Default assuming recent kernel
            PROC_PID_STAT_LENGTH = 52;
        }
    }

    private ProcessStat() {
    }

    /**
     * Reads the statistics in {@code /proc/[pid]/stat} and returns the results.
     *
     * @param pid
     *            The process ID for which to fetch stats
     * @return A triplet containing the process name as the first element, a
     *         character representing the process state as the second element, and
     *         an EnumMap as the third element, where the numeric values in
     *         {@link PidStat} are mapped to a {@link Long} value.
     *         <p>
     *         If the process doesn't exist, returns null.
     */
    public static Triplet<String, Character, Map<PidStat, Long>> getPidStats(int pid) {
        String stat = FileUtil.getStringFromFile(String.format(ProcPath.PID_STAT, pid));
        if (stat.isEmpty()) {
            // If pid doesn't exist
            return null;
        }
        // Get process name from between parentheses and state immediately after
        int nameStart = stat.indexOf('(') + 1;
        int nameEnd = stat.indexOf(')');
        String name = stat.substring(nameStart, nameEnd);
        Character state = stat.charAt(nameEnd + 2);
        // Split everything after the state
        String[] split = ParseUtil.whitespaces.split(stat.substring(nameEnd + 4).trim());

        Map<PidStat, Long> statMap = new EnumMap<>(PidStat.class);
        PidStat[] enumArray = PidStat.class.getEnumConstants();
        for (int i = 3; i < enumArray.length && i - 3 < split.length; i++) {
            statMap.put(enumArray[i], ParseUtil.parseLongOrDefault(split[i - 3], 0L));
        }
        return new Triplet<>(name, state, statMap);
    }

    /**
     * Reads the statistics in {@code /proc/[pid]/statm} and returns the results.
     *
     * @param pid
     *            The process ID for which to fetch stats
     * @return An EnumMap where the numeric values in {@link PidStatM} are mapped to
     *         a {@link Long} value.
     *         <p>
     *         If the process doesn't exist, returns null.
     */
    public static Map<PidStatM, Long> getPidStatM(int pid) {
        String statm = FileUtil.getStringFromFile(String.format(ProcPath.PID_STATM, pid));
        if (statm.isEmpty()) {
            // If pid doesn't exist
            return null;
        }
        // Split the fields
        String[] split = ParseUtil.whitespaces.split(statm);

        Map<PidStatM, Long> statmMap = new EnumMap<>(PidStatM.class);
        PidStatM[] enumArray = PidStatM.class.getEnumConstants();
        for (int i = 0; i < enumArray.length && i < split.length; i++) {
            statmMap.put(enumArray[i], ParseUtil.parseLongOrDefault(split[i], 0L));
        }
        return statmMap;
    }

    /**
     * Gets an array of files in the /proc directory with only numeric digit
     * filenames, corresponding to processes
     *
     * @return An array of File objects for the process files
     */
    public static File[] getPidFiles() {
        File procdir = new File(ProcPath.PROC);
        File[] pids = procdir.listFiles(f -> DIGITS.matcher(f.getName()).matches());
        return pids != null ? pids : new File[0];
    }
}
