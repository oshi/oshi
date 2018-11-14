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
package oshi.json.software.os;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.json.OshiJsonObject;
import oshi.json.util.PropertiesUtil;
import oshi.software.os.OSProcess.State;
import oshi.software.os.OperatingSystem;

/**
 * A process is an instance of a computer program that is being executed. It
 * contains the program code and its current activity. Depending on the
 * operating system (OS), a process may be made up of multiple threads of
 * execution that execute instructions concurrently.
 *
 * @author widdis[at]gmail[dot]com
 */
public class OSProcess extends AbstractOshiJsonObject implements OshiJsonObject {
    private static final long serialVersionUID = 3L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.software.os.OSProcess osProcess;

    /**
     * Creates a new OSProcess object
     */
    public OSProcess() {
        this.osProcess = new oshi.software.os.OSProcess();
    }

    /**
     * Creates a new OSProcess object wrapping the provided argument
     *
     * @param osProcess
     *            an OSProcessor object
     */
    public OSProcess(oshi.software.os.OSProcess osProcess) {
        this.osProcess = osProcess;
    }

    /**
     * @return Returns the name of the process.
     */
    public String getName() {
        return this.osProcess.getName();
    }

    /**
     * @return Returns the full path of the executing process.
     */
    public String getPath() {
        return this.osProcess.getPath();
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
        return this.osProcess.getCommandLine();
    }

    /**
     * @return Returns the process current working directory.
     * 
     *         On Windows, this value is only populated for the current process.
     */
    public String getCurrentWorkingDirectory() {
        return this.osProcess.getCurrentWorkingDirectory();
    }

    /**
     * @return Returns the user name. On Windows systems, also returns the
     *         domain prepended to the username.
     */
    public String getUser() {
        return this.osProcess.getUser();
    }

    /**
     * @return Returns the userID. On Windows systems, returns the Security ID
     *         (SID)
     */
    public String getUserID() {
        return this.osProcess.getUserID();
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
        return this.osProcess.getGroup();
    }

    /**
     * @return Returns the groupID. On Windows systems, returns a
     *         comma-delimited list of group SIDs with access to this process,
     *         corresponding to the names in {@link #getGroup()}.
     */
    public String getGroupID() {
        return this.osProcess.getGroupID();
    }

    /**
     * @return Returns the execution state of the process.
     */
    public State getState() {
        return this.osProcess.getState();
    }

    /**
     * @return Returns the processID.
     */
    public int getProcessID() {
        return this.osProcess.getProcessID();
    }

    /**
     * @return Returns the parentProcessID, if any; 0 otherwise.
     */
    public int getParentProcessID() {
        return this.osProcess.getParentProcessID();
    }

    /**
     * @return Returns the number of threads in this process.
     */
    public int getThreadCount() {
        return this.osProcess.getThreadCount();
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
        return this.osProcess.getPriority();
    }

    /**
     * @return Returns the Virtual Memory Size (VSZ). It includes all memory
     *         that the process can access, including memory that is swapped out
     *         and memory that is from shared libraries.
     */
    public long getVirtualSize() {
        return this.osProcess.getVirtualSize();
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
        return this.osProcess.getResidentSetSize();
    }

    /**
     * @return Returns the number of milliseconds the process has executed in
     *         kernel/system mode.
     */
    public long getKernelTime() {
        return this.osProcess.getKernelTime();
    }

    /**
     * @return Returns the number of milliseconds the process has executed in
     *         user mode.
     */
    public long getUserTime() {
        return this.osProcess.getUserTime();
    }

    /**
     * @return Returns the number of milliseconds since the process started.
     */
    public long getUpTime() {
        return this.osProcess.getUpTime();
    }

    /**
     * @return Returns the start time of the process in number of milliseconds
     *         since January 1, 1970.
     */
    public long getStartTime() {
        return this.osProcess.getStartTime();
    }

    /**
     * @return Returns the number of bytes the process has read from disk.
     */
    public long getBytesRead() {
        return this.osProcess.getBytesRead();
    }

    /**
     * @return Returns the number of bytes the process has written to disk.
     */
    public long getBytesWritten() {
        return this.osProcess.getBytesWritten();
    }

    /**
     * Set the name of the process.
     *
     * @param name
     *            process name
     */
    public void setName(String name) {
        this.osProcess.setName(name);
    }

    /**
     * Set the full path of the executing process.
     *
     * @param path
     *            process path
     */
    public void setPath(String path) {
        this.osProcess.setPath(path);
    }

    /**
     * Sets the process command line.
     *
     * @param commandLine
     *            The commandLine to set.
     */
    public void setCommandLine(String commandLine) {
        this.osProcess.setCommandLine(commandLine);
    }

    /**
     * Sets the process current working directory
     *
     * @param currentWorkingDirectory
     *            The currentWorkingDirectory to set.
     */
    public void setCurrentWorkingDirectory(String currentWorkingDirectory) {
        this.osProcess.setCurrentWorkingDirectory(currentWorkingDirectory);
    }

    /**
     * Sets the user.
     *
     * @param user
     *            The user to set.
     */
    public void setUser(String user) {
        this.osProcess.setUser(user);
    }

    /**
     * Sets the User ID.
     *
     * @param userID
     *            The userID to set.
     */
    public void setUserID(String userID) {
        this.osProcess.setUserID(userID);
    }

    /**
     * Sets the group.
     *
     * @param group
     *            The group to set.
     */
    public void setGroup(String group) {
        this.osProcess.setGroup(group);
    }

    /**
     * Sets the Group ID.
     *
     * @param groupID
     *            The groupID to set.
     */
    public void setGroupID(String groupID) {
        this.osProcess.setGroupID(groupID);
    }

    /**
     * Set the execution state of the process.
     *
     * @param state
     *            execution state
     */
    public void setState(State state) {
        this.osProcess.setState(state);
    }

    /**
     * Set the processID.
     *
     * @param processID
     *            process ID
     */
    public void setProcessID(int processID) {
        this.osProcess.setProcessID(processID);
    }

    /**
     * Set the parentProcessID.
     *
     * @param parentProcessID
     *            parent process ID
     */
    public void setParentProcessID(int parentProcessID) {
        this.osProcess.setParentProcessID(parentProcessID);
    }

    /**
     * Set the number of threads in this process.
     *
     * @param threadCount
     *            number of threads
     */
    public void setThreadCount(int threadCount) {
        this.osProcess.setThreadCount(threadCount);
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
        this.osProcess.setPriority(priority);
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
        this.osProcess.setVirtualSize(virtualSize);
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
        this.osProcess.setResidentSetSize(residentSetSize);
    }

    /**
     * Set the number of milliseconds the process has executed in kernel mode.
     *
     * @param kernelTime
     *            kernel time
     */
    public void setKernelTime(long kernelTime) {
        this.osProcess.setKernelTime(kernelTime);
    }

    /**
     * Set the number of milliseconds the process has executed in user mode.
     *
     * @param userTime
     *            user time
     */
    public void setUserTime(long userTime) {
        this.osProcess.setUserTime(userTime);
    }

    /**
     * Set the start time of the process in number of milliseconds since January
     * 1, 1970.
     *
     * @param startTime
     *            start time
     */
    public void setStartTime(long startTime) {
        this.osProcess.setStartTime(startTime);
    }

    /**
     * Set the number of milliseconds since the process started.
     *
     * @param upTime
     *            up time
     */
    public void setUpTime(long upTime) {
        this.osProcess.setUpTime(upTime);
    }

    /**
     * Set the number of bytes the process has read from disk.
     *
     * @param bytesRead
     *            number of bytes read
     */
    public void setBytesRead(long bytesRead) {
        this.osProcess.setBytesRead(bytesRead);
    }

    /**
     * Set the number of bytes the process has written to disk.
     *
     * @param bytesWritten
     *            number of bytes written
     */
    public void setBytesWritten(long bytesWritten) {
        this.osProcess.setBytesWritten(bytesWritten);
    }

    /**
     * Sets the number of open file handles (or network connections) that
     * belongs to the process
     * 
     * @param count
     *            The number of handles
     */
    public void setOpenFiles(long count) {
        this.osProcess.setOpenFiles(count);
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
        return this.osProcess.getOpenFiles();
    }

    /**
     * Calculates CPU usage of this process.
     * 
     * @return The proportion of up time that the process was executing in
     *         kernel or user mode.
     */
    public double calculateCpuPercent() {
        return this.osProcess.calculateCpuPercent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.osProcess.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.name")) {
            json.add("name", getName());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.path")) {
            json.add("path", getPath());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.commandLine")) {
            json.add("commandLine", getCommandLine());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.currentWorkingDirectory")) {
            json.add("currentWorkingDirectory", getCurrentWorkingDirectory());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.user")) {
            json.add("user", getUser());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.userID")) {
            json.add("userID", getUserID());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.group")) {
            json.add("group", getGroup());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.groupID")) {
            json.add("groupID", getGroupID());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.state")) {
            json.add("state", getState().name());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.processID")) {
            json.add("processID", getProcessID());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.parentProcessID")) {
            json.add("parentProcessID", getParentProcessID());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.threadCount")) {
            json.add("threadCount", getThreadCount());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.priority")) {
            json.add("priority", getPriority());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.virtualSize")) {
            json.add("virtualSize", getVirtualSize());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.residentSetSize")) {
            json.add("residentSetSize", getResidentSetSize());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.kernelTime")) {
            json.add("kernelTime", getKernelTime());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.userTime")) {
            json.add("userTime", getUserTime());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.upTime")) {
            json.add("upTime", getUpTime());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.startTime")) {
            json.add("startTime", getStartTime());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.bytesRead")) {
            json.add("bytesRead", getBytesRead());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.bytesWritten")) {
            json.add("bytesWritten", getBytesWritten());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.handles")) {
            json.add("handles", getOpenFiles());
        }
        return json.build();
    }
}
