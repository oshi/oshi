/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import oshi.software.os.OSProcess.State;

import java.util.function.Predicate;

/**
 * Represents a Thread/Task on the operating system.
 */
public interface OSThread {

    /**
     * Constants which may be used to filter Thread lists
     */
    final class ThreadFiltering {
        private ThreadFiltering() {
        }

        /**
         * Exclude processes with {@link State#INVALID} process state.
         */
        public static final Predicate<OSThread> VALID_THREAD = p -> !p.getState().equals(State.INVALID);
    }

    /**
     * The thread id. The meaning of this value is OS-dependent.
     *
     * @return Returns the id of the thread.
     */
    int getThreadId();

    /**
     * The name of the thread. Presence of a name is operating-system dependent and may include information (such as an
     * index of running threads) that changes during execution.
     *
     * @return Returns the name of the task/thread.
     */
    default String getName() {
        return "";
    }

    /**
     * Gets the execution state of the task/thread.
     *
     * @return Returns the execution state of the task/thread.
     */
    State getState();

    /**
     * Gets cumulative CPU usage of this thread.
     *
     * @return The proportion of up time that the thread was executing in kernel or user mode.
     */
    double getThreadCpuLoadCumulative();

    /**
     * Gets CPU usage of this thread since a previous snapshot of the same thread, provided as a parameter.
     *
     * @param thread An {@link OSThread} object containing statistics for this same thread collected at a prior point in
     *               time. May be null.
     *
     * @return If the prior snapshot is for the same thread at a prior point in time, the proportion of elapsed up time
     *         between the current thread snapshot and the previous one that the thread was executing in kernel or user
     *         mode. Returns cumulative load otherwise.
     */
    double getThreadCpuLoadBetweenTicks(OSThread thread);

    /**
     * The owning process of this thread. For single-threaded processes, the owning process ID may be the same as the
     * thread's ID.
     *
     * @return The owning process of this thread.
     */
    int getOwningProcessId();

    /**
     * The memory address above which this thread can run.
     *
     * @return The start address.
     */
    default long getStartMemoryAddress() {
        return 0L;
    }

    /**
     * A snapshot of the context switches the thread has done. Since the context switches could be voluntary and
     * non-voluntary, this gives the sum of both.
     * <p>
     * Not available on AIX.
     *
     * @return sum of both voluntary and involuntary context switches.
     */
    default long getContextSwitches() {
        return 0L;
    }

    /**
     * The number of minor (soft) faults the thread has made which have not required loading a memory page from disk.
     * Sometimes called reclaims. Linux only.
     *
     * @return minor faults.
     */
    default long getMinorFaults() {
        return 0L;
    }

    /**
     * The number of major (hard) faults the thread has made which have required loading a memory page from disk. Linux
     * only.
     *
     * @return major faults.
     */
    default long getMajorFaults() {
        return 0L;
    }

    /**
     * Kernel (privileged) time used by the thread.
     *
     * @return Returns the number of milliseconds the task/thread has executed in kernel/system mode.
     */
    long getKernelTime();

    /**
     * User time used by the thread.
     *
     * @return Returns the number of milliseconds the task/thread has executed in user mode.
     */
    long getUserTime();

    /**
     * Elapsed/up-time of the thread.
     *
     * @return Returns the number of milliseconds since the task/thread started.
     */
    long getUpTime();

    /**
     * The start time of the thread.
     *
     * @return Returns the start time of the task/thread in number of milliseconds since January 1, 1970.
     */
    long getStartTime();

    /**
     * Priority of the thread, the meaning of which is dependent on the OS.
     *
     * @return priority.
     */
    int getPriority();

    /**
     * Attempts to updates process attributes. Returns false if the update fails, which will occur if the process no
     * longer exists. Not implemented for macOS, as thread ID is simply an index and not unique.
     *
     * @return {@code true} if the update was successful, false if the update failed. In addition, on a failed update
     *         the thread state will be changed to {@link State#INVALID}.
     */
    default boolean updateAttributes() {
        return false;
    }
}
