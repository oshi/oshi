/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
 * https://github.com/oshi/oshi/graphs/contributors
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
public class OSProcess implements Serializable {

    private static final long serialVersionUID = 3L;

    private String name = "";
    private String path = "";
    private String commandLine = "";
    private String currentWorkingDirectory = "";
    private String user = "";
    private String userID = "";
    private String group = "";
    private String groupID = "";
    private State state = State.OTHER;
    private int processID;
    private int parentProcessID;
    private int threadCount;
    private int priority;
    private long virtualSize;
    private long residentSetSize;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private long bytesRead;
    private long bytesWritten;
    private long openFiles;
    // cache calculation for sorting
    private transient double cpuPercent = -1d;

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
    public String getName() {
        return this.name;
    }

    /**
     * @return Returns the full path of the executing process.
     */
    public String getPath() {
        return this.path;
    }

    /**
     * @return Returns the process command line. The format of this string is
     *         platform-dependent and may require the end user to parse the
     *         result.
     *
     *         On Linux and macOS systems, the string is
     *         null-character-delimited, to permit the end user to parse the
     *         executable and arguments if desired. Further, the macOS variant
     *         may include environment variables which the end user may wish to
     *         exclude from display. On Solaris, the string is truncated to 80
     *         characters.
     */
    public String getCommandLine() {
        return this.commandLine;
    }

    /**
     * @return Returns the process current working directory.
     * 
     *         On Windows, this value is only populated for the current process.
     */
    public String getCurrentWorkingDirectory() {
        return this.currentWorkingDirectory;
    }

    /**
     * @return Returns the user name. On Windows systems, also returns the
     *         domain prepended to the username.
     */
    public String getUser() {
        return this.user;
    }

    /**
     * @return Returns the userID. On Windows systems, returns the Security ID
     *         (SID)
     */
    public String getUserID() {
        return this.userID;
    }

    /**
     * @return Returns the group.
     *
     *         On Windows systems, populating this value for processes other
     *         than the current user requires administrative privileges (and
     *         still may fail for some system processes) and can incur
     *         significant latency. The value is only calculated for single
     *         process queries using {@link OperatingSystem#getProcess(int)}.
     *         When successful, returns a comma-delimited list of groups with
     *         access to this process, corresponding to the SIDs in
     *         {@link #getGroupID()}.
     */
    public String getGroup() {
        return this.group;
    }

    /**
     * @return Returns the groupID.
     *
     *         On Windows systems, populating this value for processes other
     *         than the current user requires administrative privileges (and
     *         still may fail for some system processes) and can incur
     *         significant latency. The value is only calculated for single
     *         process queries using {@link OperatingSystem#getProcess(int)}.
     *         When successful, returns a comma-delimited list of group SIDs
     *         with access to this process, corresponding to the names in
     *         {@link #getGroup()}.
     */
    public String getGroupID() {
        return this.groupID;
    }

    /**
     * @return Returns the execution state of the process.
     */
    public State getState() {
        return this.state;
    }

    /**
     * @return Returns the processID.
     */
    public int getProcessID() {
        return this.processID;
    }

    /**
     * @return Returns the parentProcessID, if any; 0 otherwise.
     */
    public int getParentProcessID() {
        return this.parentProcessID;
    }

    /**
     * @return Returns the number of threads in this process.
     */
    public int getThreadCount() {
        return this.threadCount;
    }

    /**
     * @return Returns the priority of this process.
     *
     *         For Linux and Unix, priority is a value in the range -20 to 19
     *         (20 on some systems). The default priority is 0; lower priorities
     *         cause more favorable scheduling.
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
    public int getPriority() {
        return this.priority;
    }

    /**
     * @return Returns the Virtual Memory Size (VSZ). It includes all memory
     *         that the process can access, including memory that is swapped out
     *         and memory that is from shared libraries.
     */
    public long getVirtualSize() {
        return this.virtualSize;
    }

    /**
     * @return Returns the Resident Set Size (RSS). On Windows, returns the
     *         Private Working Set size. It is used to show how much memory is
     *         allocated to that process and is in RAM. It does not include
     *         memory that is swapped out. It does include memory from shared
     *         libraries as long as the pages from those libraries are actually
     *         in memory. It does include all stack and heap memory.
     */
    public long getResidentSetSize() {
        return this.residentSetSize;
    }

    /**
     * @return Returns the number of milliseconds the process has executed in
     *         kernel/system mode.
     */
    public long getKernelTime() {
        return this.kernelTime;
    }

    /**
     * @return Returns the number of milliseconds the process has executed in
     *         user mode.
     */
    public long getUserTime() {
        return this.userTime;
    }

    /**
     * @return Returns the number of milliseconds since the process started.
     */
    public long getUpTime() {
        return this.upTime;
    }

    /**
     * @return Returns the start time of the process in number of milliseconds
     *         since January 1, 1970.
     */
    public long getStartTime() {
        return this.startTime;
    }

    /**
     * @return Returns the number of bytes the process has read from disk.
     */
    public long getBytesRead() {
        return this.bytesRead;
    }

    /**
     * @return Returns the number of bytes the process has written to disk.
     */
    public long getBytesWritten() {
        return this.bytesWritten;
    }

    /**
     * Set the name of the process.
     *
     * @param name
     *            process name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the full path of the executing process.
     *
     * @param path
     *            process path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Sets the process command line.
     *
     * @param commandLine
     *            The commandLine to set.
     */
    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    /**
     * Sets the process current working directory
     *
     * @param currentWorkingDirectory
     *            The currentWorkingDirectory to set.
     */
    public void setCurrentWorkingDirectory(String currentWorkingDirectory) {
        this.currentWorkingDirectory = currentWorkingDirectory;
    }

    /**
     * Sets the user.
     *
     * @param user
     *            The user to set.
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Sets the User ID.
     *
     * @param userID
     *            The userID to set.
     */
    public void setUserID(String userID) {
        this.userID = userID;
    }

    /**
     * Sets the group.
     *
     * @param group
     *            The group to set.
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Sets the Group ID.
     *
     * @param groupID
     *            The groupID to set.
     */
    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    /**
     * Set the execution state of the process.
     *
     * @param state
     *            execution state
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Set the processID.
     *
     * @param processID
     *            process ID
     */
    public void setProcessID(int processID) {
        this.processID = processID;
    }

    /**
     * Set the parentProcessID.
     *
     * @param parentProcessID
     *            parent process ID
     */
    public void setParentProcessID(int parentProcessID) {
        this.parentProcessID = parentProcessID;
    }

    /**
     * Set the number of threads in this process.
     *
     * @param threadCount
     *            number of threads
     */
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    /**
     * Set the priority of this process.
     *
     * For Linux, priority is a value in the range -20 to 19 (20 on some
     * systems). The default priority is 0; lower priorities cause more
     * favorable scheduling.
     *
     * For Windows, priority values can range from 0 (lowest priority) to 31
     * (highest priority).
     *
     * Mac OS X has 128 priority levels, ranging from 0 (lowest priority) to 127
     * (highest priority). They are divided into several major bands: 0 through
     * 51 are the normal levels; the default priority is 31. 52 through 79 are
     * the highest priority regular threads; 80 through 95 are for kernel mode
     * threads; and 96 through 127 correspond to real-time threads, which are
     * treated differently than other threads by the scheduler.
     *
     * @param priority
     *            priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Set the Virtual Memory Size (VSZ). It includes all memory that the
     * process can access, including memory that is swapped out and memory that
     * is from shared libraries.
     *
     * @param virtualSize
     *            virtual size
     */
    public void setVirtualSize(long virtualSize) {
        this.virtualSize = virtualSize;
    }

    /**
     * Set the Resident Set Size (RSS). It is used to show how much memory is
     * allocated to that process and is in RAM. It does not include memory that
     * is swapped out. It does include memory from shared libraries as long as
     * the pages from those libraries are actually in memory. It does include
     * all stack and heap memory.
     *
     * @param residentSetSize
     *            resident set size
     */
    public void setResidentSetSize(long residentSetSize) {
        this.residentSetSize = residentSetSize;
    }

    /**
     * Set the number of milliseconds the process has executed in kernel mode.
     *
     * @param kernelTime
     *            kernel time
     */
    public void setKernelTime(long kernelTime) {
        this.kernelTime = kernelTime;
    }

    /**
     * Set the number of milliseconds the process has executed in user mode.
     *
     * @param userTime
     *            user time
     */
    public void setUserTime(long userTime) {
        this.userTime = userTime;
    }

    /**
     * Set the start time of the process in number of milliseconds since January
     * 1, 1970.
     *
     * @param startTime
     *            start time
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Set the number of milliseconds since the process started.
     *
     * @param upTime
     *            up time
     */
    public void setUpTime(long upTime) {
        this.upTime = upTime;
    }

    /**
     * Set the number of bytes the process has read from disk.
     *
     * @param bytesRead
     *            number of bytes read
     */
    public void setBytesRead(long bytesRead) {
        this.bytesRead = bytesRead;
    }

    /**
     * Set the number of bytes the process has written to disk.
     *
     * @param bytesWritten
     *            number of bytes written
     */
    public void setBytesWritten(long bytesWritten) {
        this.bytesWritten = bytesWritten;
    }

    /**
     * Sets the number of open file handles (or network connections) that
     * belongs to the process
     * 
     * @param count
     *            The number of handles
     */
    public void setOpenFiles(long count) {
        this.openFiles = count;
    }

    /**
     * Sets the number of open file handles (or network connections) that
     * belongs to the process
     *
     * On FreeBSD and Solaris, this value is only populated if information for a
     * single process id is requested.
     * 
     * @return open files or -1 if unknown or not supported
     */
    public long getOpenFiles() {
        return openFiles;
    }

    /**
     * Calculates CPU usage of this process.
     * 
     * @return The proportion of up time that the process was executing in
     *         kernel or user mode.
     */
    public double calculateCpuPercent() {
        if (this.cpuPercent < 0d) {
            this.cpuPercent = (getKernelTime() + getUserTime()) / (double) getUpTime();
        }
        return this.cpuPercent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("OSProcess@");
        builder.append(Integer.toHexString(hashCode()));
        builder.append("[processID=").append(processID);
        builder.append(", name=").append(name);
        builder.append("]");
        return builder.toString();
    }
}
