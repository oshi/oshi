/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A process is an instance of a computer program that is being executed. It
 * contains the program code and its current activity. Depending on the
 * operating system (OS), a process may be made up of multiple threads of
 * execution that execute instructions concurrently.
 */
public class OSProcess implements Serializable {

    private static final long serialVersionUID = 3L;

    private static final Logger LOG = LoggerFactory.getLogger(OSProcess.class);

    private final OperatingSystem operatingSystem;

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
    private int bitness;
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
     * <p>
     * Constructor for OSProcess.
     * </p>
     *
     * @param operatingSystem
     *            a {@link oshi.software.os.OperatingSystem} instance
     */
    public OSProcess(OperatingSystem operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    /**
     * <p>
     * Constructor for OSProcess given a Process ID. Instantiates an object with
     * current statistics for that process ID, and is equivalent to
     * {@link oshi.software.os.OperatingSystem#getProcess(int)}.
     * </p>
     * <p>
     * If a process with that ID does not exist, this constructor will throw an
     * {@link java.lang.InstantiationException}.
     * </p>
     * 
     * @param operatingSystem
     *            a {@link oshi.software.os.OperatingSystem} instance
     * @param processID
     *            process ID
     * @throws InstantiationException
     *             If a process by that ID does not exist.
     */
    public OSProcess(OperatingSystem operatingSystem, int processID) throws InstantiationException {
        this.processID = processID;
        this.operatingSystem = operatingSystem;
        if (!updateAttributes()) {
            throw new InstantiationException("A process with ID " + processID + " does not exist.");
        }
    }

    /**
     * Attempts to updates all process attributes. Returns false if the update
     * fails, which will occur if the process no longer exists.
     * 
     * @return True if the update was successful, false if the update failed
     */
    public boolean updateAttributes() {
        OSProcess process = operatingSystem.getProcess(this.processID);
        if (process == null) {
            LOG.debug("No process found: {}", this.processID);
            return false;
        }
        copyValuesToThisProcess(process);
        return true;
    }

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return Returns the name of the process.
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * Getter for the field <code>path</code>.
     * </p>
     *
     * @return Returns the full path of the executing process.
     */
    public String getPath() {
        return this.path;
    }

    /**
     * <p>
     * Getter for the field <code>commandLine</code>.
     * </p>
     *
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
     * <p>
     * Getter for the field <code>currentWorkingDirectory</code>.
     * </p>
     *
     * @return Returns the process current working directory.
     *
     *         On Windows, this value is only populated for the current process.
     */
    public String getCurrentWorkingDirectory() {
        return this.currentWorkingDirectory;
    }

    /**
     * <p>
     * Getter for the field <code>user</code>.
     * </p>
     *
     * @return Returns the user name. On Windows systems, also returns the
     *         domain prepended to the username.
     */
    public String getUser() {
        return this.user;
    }

    /**
     * <p>
     * Getter for the field <code>userID</code>.
     * </p>
     *
     * @return Returns the userID. On Windows systems, returns the Security ID
     *         (SID)
     */
    public String getUserID() {
        return this.userID;
    }

    /**
     * <p>
     * Getter for the field <code>group</code>.
     * </p>
     *
     * @return Returns the group.
     *
     *         On Windows systems, populating this value for processes other
     *         than the current user requires administrative privileges (and
     *         still may fail for some system processes) and can incur
     *         significant latency. The value is only calculated for single
     *         process queries using
     *         {@link oshi.software.os.OperatingSystem#getProcess(int)}. When
     *         successful, returns a comma-delimited list of groups with access
     *         to this process, corresponding to the SIDs in
     *         {@link #getGroupID()}.
     */
    public String getGroup() {
        return this.group;
    }

    /**
     * <p>
     * Getter for the field <code>groupID</code>.
     * </p>
     *
     * @return Returns the groupID.
     *
     *         On Windows systems, populating this value for processes other
     *         than the current user requires administrative privileges (and
     *         still may fail for some system processes) and can incur
     *         significant latency. The value is only calculated for single
     *         process queries using
     *         {@link oshi.software.os.OperatingSystem#getProcess(int)}. When
     *         successful, returns a comma-delimited list of group SIDs with
     *         access to this process, corresponding to the names in
     *         {@link #getGroup()}.
     */
    public String getGroupID() {
        return this.groupID;
    }

    /**
     * <p>
     * Getter for the field <code>state</code>.
     * </p>
     *
     * @return Returns the execution state of the process.
     */
    public State getState() {
        return this.state;
    }

    /**
     * <p>
     * Getter for the field <code>processID</code>.
     * </p>
     *
     * @return Returns the processID.
     */
    public int getProcessID() {
        return this.processID;
    }

    /**
     * <p>
     * Getter for the field <code>parentProcessID</code>.
     * </p>
     *
     * @return Returns the parentProcessID, if any; 0 otherwise.
     */
    public int getParentProcessID() {
        return this.parentProcessID;
    }

    /**
     * <p>
     * Getter for the field <code>threadCount</code>.
     * </p>
     *
     * @return Returns the number of threads in this process.
     */
    public int getThreadCount() {
        return this.threadCount;
    }

    /**
     * <p>
     * Getter for the field <code>priority</code>.
     * </p>
     *
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
     * <p>
     * Getter for the field <code>virtualSize</code>.
     * </p>
     *
     * @return Returns the Virtual Memory Size (VSZ). It includes all memory
     *         that the process can access, including memory that is swapped out
     *         and memory that is from shared libraries.
     */
    public long getVirtualSize() {
        return this.virtualSize;
    }

    /**
     * <p>
     * Getter for the field <code>residentSetSize</code>.
     * </p>
     *
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
     * <p>
     * Getter for the field <code>kernelTime</code>.
     * </p>
     *
     * @return Returns the number of milliseconds the process has executed in
     *         kernel/system mode.
     */
    public long getKernelTime() {
        return this.kernelTime;
    }

    /**
     * <p>
     * Getter for the field <code>userTime</code>.
     * </p>
     *
     * @return Returns the number of milliseconds the process has executed in
     *         user mode.
     */
    public long getUserTime() {
        return this.userTime;
    }

    /**
     * <p>
     * Getter for the field <code>upTime</code>.
     * </p>
     *
     * @return Returns the number of milliseconds since the process started.
     */
    public long getUpTime() {
        return this.upTime;
    }

    /**
     * <p>
     * Getter for the field <code>startTime</code>.
     * </p>
     *
     * @return Returns the start time of the process in number of milliseconds
     *         since January 1, 1970.
     */
    public long getStartTime() {
        return this.startTime;
    }

    /**
     * <p>
     * Getter for the field <code>bytesRead</code>.
     * </p>
     *
     * @return Returns the number of bytes the process has read from disk.
     */
    public long getBytesRead() {
        return this.bytesRead;
    }

    /**
     * <p>
     * Getter for the field <code>bytesWritten</code>.
     * </p>
     *
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
        return this.openFiles;
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

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("OSProcess@");
        builder.append(Integer.toHexString(hashCode()));
        builder.append("[processID=").append(this.processID);
        builder.append(", name=").append(this.name).append(']');
        return builder.toString();
    }

    /**
     * Attempts to get the bitness (32 or 64) of the process.
     *
     * @return The bitness, if able to be determined, 0 otherwise.
     */
    public int getBitness() {
        return this.bitness;
    }

    /**
     * <p>
     * Setter for the field <code>bitness</code>.
     * </p>
     *
     * @param bitness
     *            The bitness to set.
     */
    public void setBitness(int bitness) {
        this.bitness = bitness;
    }

    private void copyValuesToThisProcess(OSProcess sourceProcess) {
        this.name = sourceProcess.name;
        this.path = sourceProcess.path;
        this.commandLine = sourceProcess.commandLine;
        this.currentWorkingDirectory = sourceProcess.currentWorkingDirectory;
        this.user = sourceProcess.user;
        this.userID = sourceProcess.userID;
        this.group = sourceProcess.group;
        this.groupID = sourceProcess.groupID;
        this.state = sourceProcess.state;
        this.processID = sourceProcess.processID;
        this.parentProcessID = sourceProcess.parentProcessID;
        this.threadCount = sourceProcess.threadCount;
        this.priority = sourceProcess.priority;
        this.virtualSize = sourceProcess.virtualSize;
        this.residentSetSize = sourceProcess.residentSetSize;
        this.kernelTime = sourceProcess.kernelTime;
        this.userTime = sourceProcess.userTime;
        this.startTime = sourceProcess.startTime;
        this.upTime = sourceProcess.upTime;
        this.bytesRead = sourceProcess.bytesRead;
        this.bytesWritten = sourceProcess.bytesWritten;
        this.openFiles = sourceProcess.openFiles;
        this.bitness = sourceProcess.bitness;
        this.cpuPercent = sourceProcess.cpuPercent;
    }
}
