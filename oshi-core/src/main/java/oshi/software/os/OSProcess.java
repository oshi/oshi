/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.software.os;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.wmi.Win32ProcessCached;
import oshi.util.FileUtil;
import oshi.util.GlobalConfig;

/**
 * Represents a Process on the operating system, which may contain multiple
 * threads.
 */
@ThreadSafe
public interface OSProcess {

    /**
     * Gets the name of the process, often the executable program.
     *
     * @return the name of the process.
     */
    String getName();

    /**
     * Gets the full filesystem path of the executing process.
     *
     * @return the full path of the executing process.
     */
    String getPath();

    /**
     * Gets the process command line used to start the process, including arguments
     * if available to be determined. This method generally returns the same
     * information as {@link #getArguments()} in a more user-readable format, and is
     * more robust to non-elevated access.
     * <p>
     * The format of this string is platform-dependent, may be truncated, and may
     * require the end user to parse the result. Users should generally prefer
     * {@link #getArguments()} which already parses the results, and use this method
     * as a backup.
     * <p>
     * On Linux and macOS systems, the string is null-character-delimited, to permit
     * the end user to parse the executable and arguments if desired. This
     * null-delimited behavior may change in future versions and should not be
     * relied upon; use {@link #getArguments()} instead.
     * <p>
     * On AIX and Solaris, the string may be truncated to 80 characters if there was
     * insufficient permission to read the process memory.
     * <p>
     * On Windows, attempts to retrieve the value from process memory, which
     * requires that the process be owned by the same user as the executing process,
     * or elevated permissions, and additionally requires the target process to have
     * the same bitness (e.g., this will fail on a 32-bit process if queried by
     * 64-bit and vice versa). If reading process memory fails, by default, performs
     * a single WMI query for this process, with some latency. If this method will
     * be frequently called for multiple processes, see the configuration file to
     * enable a batch query mode leveraging
     * {@link Win32ProcessCached#getCommandLine} to improve performance, or setting
     * that parameter via {@link GlobalConfig#set(String, Object)} before
     * instantiating any {@link OSProcess} object.
     *
     * @return the process command line.
     */
    String getCommandLine();

    /**
     * Makes a best effort attempt to get a list of the the command-line arguments
     * of the process. Returns the same information as {@link #getCommandLine()} but
     * parsed to a list. May require elevated permissions or same-user ownership.
     *
     * @return A list of Strings representing the arguments. May return an empty
     *         list if there was a failure (for example, because the process is
     *         already dead or permission was denied).
     */
    List<String> getArguments();

    /**
     * Makes a best effort attempt to obtain the environment variables of the
     * process. May require elevated permissions or same-user ownership.
     *
     * @return A map representing the environment variables and their values. May
     *         return an empty map if there was a failure (for example, because the
     *         process is already dead or permission was denied).
     */
    Map<String, String> getEnvironmentVariables();

    /**
     * Makes a best effort attempt to obtain the current working directory for the
     * process.
     *
     * @return the process current working directory.
     */
    String getCurrentWorkingDirectory();

    /**
     * Gets the user name of the process owner.
     *
     * @return the user name. On Windows systems, also returns the domain prepended
     *         to the username.
     */
    String getUser();

    /**
     * Gets the user id of the process owner.
     *
     * @return the userID. On Windows systems, returns the Security ID (SID)
     */
    String getUserID();

    /**
     * Gets the group under which the process is executing.
     * <p>
     * On Windows systems, populating this value for processes other than the
     * current user requires administrative privileges (and still may fail for some
     * system processes) and can incur significant latency. When successful, returns
     * a the default primary group with access to this process, corresponding to the
     * SID in {@link #getGroupID()}.
     *
     * @return the group.
     */
    String getGroup();

    /**
     * Gets the group id under which the process is executing.
     * <p>
     * On Windows systems, populating this value for processes other than the
     * current user requires administrative privileges (and still may fail for some
     * system processes) and can incur significant latency. When successful, returns
     * the default primary group SID with access to this process, corresponding to
     * the name in {@link #getGroup()}.
     *
     * @return the groupID.
     */
    String getGroupID();

    /**
     * Gets the process state.
     *
     * @return the execution state of the process.
     */
    State getState();

    /**
     * Gets the process ID.
     * <p>
     * While this is a 32-bit value, it is unsigned on Windows and in extremely rare
     * circumstances may return a negative value.
     *
     * @return the processID.
     */
    int getProcessID();

    /**
     * Gets the process ID of this process's parent.
     *
     * @return the parentProcessID, if any; 0 otherwise.
     */
    int getParentProcessID();

    /**
     * Gets the number of threads being executed by this process. More information
     * is available using {@link #getThreadDetails()}.
     *
     * @return the number of threads in this process.
     */
    int getThreadCount();

    /**
     * Gets the priority of this process.
     * <p>
     * For Linux and Unix, priority is a value in the range -20 to 19 (20 on some
     * systems). The default priority is 0; lower priorities cause more favorable
     * scheduling.
     * <p>
     * For Windows, priority values can range from 0 (lowest priority) to 31
     * (highest priority).
     * <p>
     * macOS has 128 priority levels, ranging from 0 (lowest priority) to 127
     * (highest priority). They are divided into several major bands: 0 through 51
     * are the normal levels; the default priority is 31. 52 through 79 are the
     * highest priority regular threads; 80 through 95 are for kernel mode threads;
     * and 96 through 127 correspond to real-time threads, which are treated
     * differently than other threads by the scheduler.
     *
     * @return the priority of this process.
     */
    int getPriority();

    /**
     * Gets the Virtual Memory Size (VSZ). Includes all memory that the process can
     * access, including memory that is swapped out and memory that is from shared
     * libraries.
     *
     * @return the Virtual Memory Size
     */
    long getVirtualSize();

    /**
     * Gets the Resident Set Size (RSS). Used to show how much memory is allocated
     * to that process and is in RAM. It does not include memory that is swapped
     * out. It does include memory from shared libraries as long as the pages from
     * those libraries are actually in memory. It does include all stack and heap
     * memory.
     * <p>
     * On Windows, returns the Private Working Set size, which should match the
     * "Memory" column in the Windows Task Manager.
     * <p>
     * On Linux, returns the RSS value from {@code /proc/[pid]/stat}, which may be
     * inaccurate because of a kernel-internal scalability optimization. If accurate
     * values are required, read {@code /proc/[pid]/smaps} using
     * {@link FileUtil#getKeyValueMapFromFile(String, String)}.
     *
     * @return the Resident Set Size
     */
    long getResidentSetSize();

    /**
     * Gets kernel/system (privileged) time used by the process.
     *
     * @return the number of milliseconds the process has executed in kernel/system
     *         mode.
     */
    long getKernelTime();

    /**
     * Gets user time used by the process.
     *
     * @return the number of milliseconds the process has executed in user mode.
     */
    long getUserTime();

    /**
     * Gets up time / elapsed time since the process started.
     *
     * @return the number of milliseconds since the process started.
     */
    long getUpTime();

    /**
     * Gets the process start time.
     *
     * @return the start time of the process in number of milliseconds since January
     *         1, 1970 UTC.
     */
    long getStartTime();

    /**
     * Gets the bytes read by the process.
     *
     * @return the number of bytes the process has read from disk.
     */
    long getBytesRead();

    /**
     * Gets the bytes written by the process.
     *
     * @return the number of bytes the process has written to disk.
     */
    long getBytesWritten();

    /**
     * Gets the number of open file handles (or network connections) that belongs to
     * the process.
     * <p>
     * On FreeBSD and Solaris, this value is only populated if information for a
     * single process id is requested.
     *
     * @return open files or -1 if unknown or not supported
     */
    long getOpenFiles();

    /**
     * Gets cumulative CPU usage of this process.
     * <p>
     * This calculation sums CPU ticks across all processors and may exceed 100% for
     * multi-threaded processes. This is consistent with the cumulative CPU
     * presented by the "top" command on Linux/Unix machines.
     *
     * @return The proportion of up time that the process was executing in kernel or
     *         user mode.
     */
    double getProcessCpuLoadCumulative();

    /**
     * Gets CPU usage of this process since a previous snapshot of the same process,
     * provided as a parameter.
     * <p>
     * This calculation sums CPU ticks across all processors and may exceed 100% for
     * multi-threaded processes. This is consistent with process usage calulations
     * on Linux/Unix machines, but should be divided by the number of logical
     * processors to match the value displayed by the Windows Task Manager.
     * <p>
     * The accuracy of this calculation is dependent on both the number of threads
     * on which the process is executing, and the precision of the Operating
     * System's tick counters. A polling interval of at least a few seconds is
     * recommended.
     *
     * @param proc
     *            An {@link OSProcess} object containing statistics for this same
     *            process collected at a prior point in time. May be null.
     *
     * @return If the prior snapshot is for the same process at a prior point in
     *         time, the proportion of elapsed up time between the current process
     *         snapshot and the previous one that the process was executing in
     *         kernel or user mode. Returns cumulative load otherwise.
     */
    double getProcessCpuLoadBetweenTicks(OSProcess proc);

    /**
     * Attempts to get the bitness (32 or 64) of the process.
     *
     * @return The bitness, if able to be determined, 0 otherwise.
     */
    int getBitness();

    /**
     * Gets the process affinity mask for this process.
     * <p>
     * On Windows systems with more than 64 processors, if the threads of the
     * calling process are in a single processor group, returns the process affinity
     * mask for that group (which may be zero if the specified process is running in
     * a different group). If the calling process contains threads in multiple
     * groups, returns zero.
     * <p>
     * Because macOS does not export interfaces that identify processors or control
     * thread placement, explicit thread to processor binding is not supported and
     * this method will return a bitmask of all logical processors.
     * <p>
     * If the Operating System fails to retrieve an affinity mask (e.g., the process
     * has terminated), returns zero.
     *
     * @return a bit vector in which each bit represents the processors that a
     *         process is allowed to run on.
     */
    long getAffinityMask();

    /**
     * Attempts to update process attributes. Returns false if the update fails,
     * which will occur if the process no longer exists.
     *
     * @return {@code true} if the update was successful, false if the update
     *         failed. In addition, on a failed update the process state will be
     *         changed to {@link State#INVALID}.
     */
    boolean updateAttributes();

    /**
     * Retrieves the threads of the process and their details.
     * <p>
     * The amount of returned information is operating-system dependent and may
     * incur some latency.
     *
     * @return a list of threads
     */
    List<OSThread> getThreadDetails();

    /**
     * Gets the number of minor (soft) faults the process has made which have not
     * required loading a memory page from disk. Sometimes called reclaims.
     * <p>
     * Not available on Solaris. On Windows, this includes the total of major and
     * minor faults.
     *
     * @return minor page faults (reclaims).
     */
    default long getMinorFaults() {
        return 0L;
    }

    /**
     * Gets the number of major (hard) faults the process has made which have
     * required loading a memory page from disk.
     * <p>
     * Not available on Solaris. Windows does not distinguish major and minor faults
     * at the process level, so this value returns 0 and major faults are included
     * in {@link #getMinorFaults()}.
     *
     * @return major page faults.
     */
    default long getMajorFaults() {
        return 0L;
    }

    /**
     * A snapshot of the context switches the process has done. Since the context
     * switches could be voluntary and non-voluntary, this gives the sum of both.
     * <p>
     * Not available on Windows. An approximation may be made by summing associated
     * values from {@link OSThread#getContextSwitches()}.
     * <p>
     * Not available on AIX.
     *
     * @return sum of both voluntary and involuntary context switches if available,
     *         0 otherwise.
     */
    default long getContextSwitches() {
        return 0L;
    }

    /**
     * Process and Thread Execution States
     */
    enum State {
        /**
         * Intermediate state in process creation
         */
        NEW,
        /**
         * Actively executing process
         */
        RUNNING,
        /**
         * Interruptible sleep state
         */
        SLEEPING,
        /**
         * Blocked, uninterruptible sleep state
         */
        WAITING,
        /**
         * Intermediate state in process termination
         */
        ZOMBIE,
        /**
         * Stopped by the user, such as for debugging
         */
        STOPPED,
        /**
         * Other or unknown states not defined
         */
        OTHER,
        /**
         * The state resulting if the process fails to update statistics, probably due
         * to termination.
         */
        INVALID,
        /**
         * Special case of waiting if the process has been intentionally suspended
         * (Windows only)
         */
        SUSPENDED
    }
}