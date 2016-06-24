/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os;

import java.io.Serializable;

/**
 * A process is an instance of a computer program that is being executed. It
 * contains the program code and its current activity. Depending on the
 * operating system (OS), a process may be made up of multiple threads of
 * execution that execute instructions concurrently.
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface OSProcess extends Serializable {
    /**
     * Process Execution States
     */
    public enum State {
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
        OTHER
    }

    /**
     * @return Returns the name of the process.
     */
    public String getName();

    /**
     * @return Returns the full path of the executing process.
     */
    public String getPath();

    /**
     * @return Returns the execution state of the process.
     */
    public State getState();

    /**
     * @return Returns the processID.
     */
    public int getProcessID();

    /**
     * @return Returns the parentProcessID, if any; 0 otherwise.
     */
    public int getParentProcessID();

    /**
     * @return Returns the number of threads in this process.
     */
    public int getThreadCount();

    /**
     * @return Returns the priority of this process.
     * 
     *         For Linux, priority is a value in the range -20 to 19 (20 on some
     *         systems). The default priority is 0; lower priorities cause more
     *         favorable scheduling.
     * 
     *         For Windows, priority values can range from 0 (lowest priority)
     *         to 31 (highest priority).
     * 
     *         Mac OS X has 128 priority levels, ranging from 0 (lowest
     *         priority) to 127 (highest priority). They are divided into
     *         several major bands: 0 through 51 are the normal levels; the
     *         default priority is 31. 52 through 79 are the highest priority
     *         regular threads; 80 through 95 are for kernel mode threads; and
     *         96 through 127 correspond to real-time threads, which are treated
     *         differently than other threads by the scheduler.
     */
    public int getPriority();

    /**
     * @return Returns the Virtual Memory Size (VSZ). It includes all memory
     *         that the process can access, including memory that is swapped out
     *         and memory that is from shared libraries.
     */
    public long getVirtualSize();

    /**
     * @return Returns the Resident Set Size (RSS). It is used to show how much
     *         memory is allocated to that process and is in RAM. It does not
     *         include memory that is swapped out. It does include memory from
     *         shared libraries as long as the pages from those libraries are
     *         actually in memory. It does include all stack and heap memory.
     */
    public long getResidentSetSize();

    /**
     * @return Returns the number of milliseconds the process has executed in
     *         kernel mode.
     */
    public long getKernelTime();

    /**
     * @return Returns the number of milliseconds the process has executed in
     *         user mode.
     */
    public long getUserTime();

    /**
     * @return Returns the number of milliseconds since the process started.
     */
    public long getUpTime();

    /**
     * @return Returns the start time of the process in number of milliseconds
     *         since January 1, 1970.
     */
    public long getStartTime();
}
