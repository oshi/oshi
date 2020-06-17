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

import oshi.software.os.OSProcess.State;

/**
 * Represents a Thread/Task on the operating system.
 */
public interface OSThread {

    /**
     * The thread id. The meaning of this value is OS-dependent.
     *
     * @return Returns the id of the thread.
     */
    int getThreadId();

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return Returns the name of the task/thread.
     */

    String getName();

    /**
     * <p>
     * Getter for the field <code>state</code>.
     * </p>
     *
     * @return Returns the execution state of the task/thread.
     */
    State getState();

    /**
     * Gets cumulative CPU usage of this thread.
     *
     * @return The proportion of up time that the thread was executing in kernel or
     *         user mode.
     */
    double getThreadCpuLoadCumulative();

    /**
     * Gets CPU usage of this thread since a previous snapshot of the same thread,
     * provided as a parameter.
     *
     * @param thread
     *            An {@link OSThread} object containing statistics for this same
     *            thread collected at a prior point in time. May be null.
     *
     * @return If the prior snapshot is for the same thread at a prior point in
     *         time, the proportion of elapsed up time between the current thread
     *         snapshot and the previous one that the thread was executing in kernel
     *         or user mode. Returns cumulative load otherwise.
     */
    double getThreadCpuLoadBetweenTicks(OSThread thread);

    /**
     * <p>
     * Getter for the field <code>owningProcessId</code> which is the parent process
     * of this thread.
     * </p>
     * 
     * @return The owning process of this thread.
     */
    int getOwningProcessId();

    /**
     * <p>
     * Getter for the field <code>startMemoryAddress</code> which is the memory
     * address above which this thread can run.
     * </p>
     *
     * @return The start address.
     */
    long getStartMemoryAddress();

    /**
     * <p>
     * Getter for the field <code>contextSwitches</code> which gives a point in time
     * snapshot of the context switches the thread has done. Since the context
     * switches could be voluntary and non-voluntary, this gives the sum of both.
     * </p>
     * 
     * @return sum of both voluntary and involuntary context switches.
     */
    long getContextSwitches();

    /**
     * <p>
     * Getter for the field <code>minorFaults</code>, which gives the number of
     * minor faults the thread has made which have not required loading a memory
     * page disk. Linux only.
     * </p>
     * 
     * @return minor faults.
     */
    long getMinorFaults();

    /**
     * <p>
     * Getter for the field <code>minorFaults</code>, which gives the number of
     * major faults the thread has made which have required loading a memory page
     * disk. Linux only.
     * </p>
     * 
     * @return minor faults.
     */
    long getMajorFaults();

    /**
     * <p>
     * Getter for the field <code>kernelTime</code>.
     * </p>
     *
     * @return Returns the number of milliseconds the task/thread has executed in
     *         kernel/system mode.
     */
    long getKernelTime();

    /**
     * <p>
     * Getter for the field <code>userTime</code>.
     * </p>
     *
     * @return Returns the number of milliseconds the task/thread has executed in
     *         user mode.
     */
    long getUserTime();

    /**
     * <p>
     * Getter for the field <code>upTime</code>.
     * </p>
     *
     * @return Returns the number of milliseconds since the task/thread started.
     */
    long getUpTime();

    /**
     * <p>
     * Getter for the field <code>startTime</code>.
     * </p>
     *
     * @return Returns the start time of the task/thread in number of milliseconds
     *         since January 1, 1970.
     */
    long getStartTime();

    /**
     * Attempts to updates process attributes. Returns false if the update fails,
     * which will occur if the process no longer exists. Only implemented for Linux
     * and Windows.
     *
     * @return {@code true} if the update was successful, false if the update
     *         failed. In addition, on a failued update the process state will be
     *         changed to {@link State#INVALID}.
     */
    boolean updateAttributes();
}
