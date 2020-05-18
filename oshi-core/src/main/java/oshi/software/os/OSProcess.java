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
package oshi.software.os;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.wmi.Win32ProcessCached;

/**
 * Represents a Process on the operating system, which may contain multiple
 * threads.
 */
@ThreadSafe
public interface OSProcess {

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return Returns the name of the process.
     */
    String getName();

    /**
     * <p>
     * Getter for the field <code>path</code>.
     * </p>
     *
     * @return Returns the full path of the executing process.
     */
    String getPath();

    /**
     * <p>
     * Getter for the field <code>commandLine</code>.
     * </p>
     *
     * @return Returns the process command line. The format of this string is
     *         platform-dependent and may require the end user to parse the result.
     *         <p>
     *         On Linux and macOS systems, the string is null-character-delimited,
     *         to permit the end user to parse the executable and arguments if
     *         desired. Further, the macOS variant may include environment variables
     *         which the end user may wish to exclude from display.
     *         <p>
     *         On Solaris, the string is truncated to 80 characters.
     *         <p>
     *         On Windows, by default, performs a single WMI query for this process,
     *         with some latency. If this method will be frequently called for
     *         multiple processes, see the configuration file to enable a batch
     *         query mode leveraging {@link Win32ProcessCached#getCommandLine} to
     *         improve performance.
     */
    String getCommandLine();

    /**
     * <p>
     * Getter for the field <code>currentWorkingDirectory</code>.
     * </p>
     *
     * @return Returns the process current working directory.
     *
     *         On Windows, this value is only populated for the current process.
     */
    String getCurrentWorkingDirectory();

    /**
     * <p>
     * Getter for the field <code>user</code>.
     * </p>
     *
     * @return Returns the user name. On Windows systems, also returns the domain
     *         prepended to the username.
     */
    String getUser();

    /**
     * <p>
     * Getter for the field <code>userID</code>.
     * </p>
     *
     * @return Returns the userID. On Windows systems, returns the Security ID (SID)
     */
    String getUserID();

    /**
     * <p>
     * Getter for the field <code>group</code>.
     * </p>
     *
     * @return Returns the group.
     *
     *         On Windows systems, populating this value for processes other than
     *         the current user requires administrative privileges (and still may
     *         fail for some system processes) and can incur significant latency.
     *         When successful, returns a the default primary group with access to
     *         this process, corresponding to the SID in {@link #getGroupID()}.
     */
    String getGroup();

    /**
     * <p>
     * Getter for the field <code>groupID</code>.
     * </p>
     *
     * @return Returns the groupID.
     *
     *         On Windows systems, populating this value for processes other than
     *         the current user requires administrative privileges (and still may
     *         fail for some system processes) and can incur significant latency.
     *         When successful, returns the default primary group SID with access to
     *         this process, corresponding to the name in {@link #getGroup()}.
     */
    String getGroupID();

    /**
     * <p>
     * Getter for the field <code>state</code>.
     * </p>
     *
     * @return Returns the execution state of the process.
     */
    State getState();

    /**
     * <p>
     * Getter for the field <code>processID</code>.
     * </p>
     *
     * @return Returns the processID.
     */
    int getProcessID();

    /**
     * <p>
     * Getter for the field <code>parentProcessID</code>.
     * </p>
     *
     * @return Returns the parentProcessID, if any; 0 otherwise.
     */
    int getParentProcessID();

    /**
     * <p>
     * Getter for the field <code>threadCount</code>.
     * </p>
     *
     * @return Returns the number of threads in this process.
     */
    int getThreadCount();

    /**
     * <p>
     * Getter for the field <code>priority</code>.
     * </p>
     *
     * @return Returns the priority of this process.
     *
     *         For Linux and Unix, priority is a value in the range -20 to 19 (20 on
     *         some systems). The default priority is 0; lower priorities cause more
     *         favorable scheduling.
     *
     *         For Windows, priority values can range from 0 (lowest priority) to 31
     *         (highest priority).
     *
     *         Mac OS X has 128 priority levels, ranging from 0 (lowest priority) to
     *         127 (highest priority). They are divided into several major bands: 0
     *         through 51 are the normal levels; the default priority is 31. 52
     *         through 79 are the highest priority regular threads; 80 through 95
     *         are for kernel mode threads; and 96 through 127 correspond to
     *         real-time threads, which are treated differently than other threads
     *         by the scheduler.
     */
    int getPriority();

    /**
     * <p>
     * Getter for the field <code>virtualSize</code>.
     * </p>
     *
     * @return Returns the Virtual Memory Size (VSZ). It includes all memory that
     *         the process can access, including memory that is swapped out and
     *         memory that is from shared libraries.
     */
    long getVirtualSize();

    /**
     * <p>
     * Getter for the field <code>residentSetSize</code>.
     * </p>
     *
     * @return Returns the Resident Set Size (RSS). On Windows, returns the Private
     *         Working Set size. It is used to show how much memory is allocated to
     *         that process and is in RAM. It does not include memory that is
     *         swapped out. It does include memory from shared libraries as long as
     *         the pages from those libraries are actually in memory. It does
     *         include all stack and heap memory.
     */
    long getResidentSetSize();

    /**
     * <p>
     * Getter for the field <code>kernelTime</code>.
     * </p>
     *
     * @return Returns the number of milliseconds the process has executed in
     *         kernel/system mode.
     */
    long getKernelTime();

    /**
     * <p>
     * Getter for the field <code>userTime</code>.
     * </p>
     *
     * @return Returns the number of milliseconds the process has executed in user
     *         mode.
     */
    long getUserTime();

    /**
     * <p>
     * Getter for the field <code>upTime</code>.
     * </p>
     *
     * @return Returns the number of milliseconds since the process started.
     */
    long getUpTime();

    /**
     * <p>
     * Getter for the field <code>startTime</code>.
     * </p>
     *
     * @return Returns the start time of the process in number of milliseconds since
     *         January 1, 1970.
     */
    long getStartTime();

    /**
     * <p>
     * Getter for the field <code>bytesRead</code>.
     * </p>
     *
     * @return Returns the number of bytes the process has read from disk.
     */
    long getBytesRead();

    /**
     * <p>
     * Getter for the field <code>bytesWritten</code>.
     * </p>
     *
     * @return Returns the number of bytes the process has written to disk.
     */
    long getBytesWritten();

    /**
     * Sets the number of open file handles (or network connections) that belongs to
     * the process
     *
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
     * Retrieves the process affinity mask for this process.
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
     * Attempts to updates process attributes. Returns false if the update fails,
     * which will occur if the process no longer exists.
     *
     * @return {@code true} if the update was successful, false if the update
     *         failed. In addition, on a failued update the process state will be
     *         changed to {@link State#INVALID}.
     */
    boolean updateAttributes();

    /**
     * Process Execution States
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
        INVALID
    }
}