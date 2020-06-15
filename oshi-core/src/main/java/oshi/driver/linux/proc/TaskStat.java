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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess;
import oshi.software.os.linux.LinuxOSProcess;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;

public class TaskStat extends AbstractOSThread {

    private static final Logger LOG = LoggerFactory.getLogger(TaskStat.class);
    private static final int PAGE_SIZE = ParseUtil
            .parseIntOrDefault(ExecutingCommand.getFirstAnswer("getconf PAGESIZE"), 4096);

    private static final int[] THREAD_PID_STAT_ORDERS = new int[TaskStat.ThreadPidStat.values().length];
    static {
        for (TaskStat.ThreadPidStat stat : TaskStat.ThreadPidStat.values()) {
            // The PROC_PID_STAT enum indices are 1-indexed.
            // Subtract one to get a zero-based index
            THREAD_PID_STAT_ORDERS[stat.ordinal()] = stat.getOrder() - 1;
        }
    }

    private final int threadId;
    private State state = State.INVALID;
    private int priority;
    private long virtualSize;
    private long residentSetSize;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private long bytesRead;
    private long bytesWritten;

    public TaskStat(LinuxOSProcess process, int tid) {
        super(process);
        this.threadId = tid;
        updateAttributes();
    }

    @Override
    public String getName() {
        return getParentProcess().getName();
    }

    @Override
    public String getPath() {
        return getParentProcess().getPath();
    }

    @Override
    public String getCommandLine() {
        return getParentProcess().getCommandLine();
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return getParentProcess().getCurrentWorkingDirectory();
    }

    @Override
    public String getUser() {
        return getParentProcess().getUser();
    }

    @Override
    public String getUserID() {
        return getParentProcess().getUserID();
    }

    @Override
    public String getGroup() {
        return getParentProcess().getGroup();
    }

    @Override
    public String getGroupID() {
        return getParentProcess().getGroupID();
    }

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public int getThreadCount() {
        return 0;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public long getVirtualSize() {
        return this.virtualSize;
    }

    @Override
    public long getResidentSetSize() {
        return this.residentSetSize;
    }

    @Override
    public long getKernelTime() {
        return this.kernelTime;
    }

    @Override
    public long getUserTime() {
        return this.userTime;
    }

    @Override
    public long getUpTime() {
        return this.upTime;
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public long getBytesRead() {
        return this.bytesRead;
    }

    @Override
    public long getBytesWritten() {
        return this.bytesWritten;
    }

    @Override
    public long getOpenFiles() {
        return 0;
    }

    @Override
    public double getProcessCpuLoadCumulative() {
        return 0;
    }

    @Override
    public double getProcessCpuLoadBetweenTicks(OSProcess proc) {
        return 0;
    }

    @Override
    public int getBitness() {
        return 0;
    }

    @Override
    public long getAffinityMask() {
        return 0;
    }

    @Override
    public int getThreadId() {
        return this.threadId;
    }

    @Override
    public boolean updateAttributes() {
        Map<String, String> io = FileUtil.getKeyValueMapFromFile(
                String.format(ProcPath.TASK_IO, this.getParentProcess().getProcessID(), this.threadId), ":");
        Map<String, String> status = FileUtil.getKeyValueMapFromFile(
                String.format(ProcPath.TASK_STATUS, this.getParentProcess().getProcessID(), this.threadId), ":");
        String stat = FileUtil.getStringFromFile(
                String.format(ProcPath.TASK_STAT, this.getParentProcess().getProcessID(), this.threadId));
        if (stat.isEmpty()) {
            this.state = State.INVALID;
            return false;
        }
        long now = System.currentTimeMillis();
        long[] statArray = ParseUtil.parseStringToLongArray(stat, THREAD_PID_STAT_ORDERS,
                ProcessStat.PROC_PID_STAT_LENGTH, ' ');

        // The /proc/pid/cmdline value is null-delimited
        /*
         * this.startTime = LinuxOperatingSystem.BOOTTIME +
         * statArray[LinuxOSThread.ThreadPidStat.START_TIME.ordinal()] * 1000L /
         * LinuxOperatingSystem.getHz();
         */
        // BOOT_TIME could be up to 5ms off. In rare cases when a process has
        // started within 5ms of boot it is possible to get negative uptime.
        if (startTime >= now) {
            startTime = now - 1;
        }
        this.priority = (int) statArray[TaskStat.ThreadPidStat.PRIORITY.ordinal()];
        this.virtualSize = statArray[TaskStat.ThreadPidStat.VSZ.ordinal()];
        this.residentSetSize = statArray[TaskStat.ThreadPidStat.RSS.ordinal()] * PAGE_SIZE;
        this.kernelTime = statArray[TaskStat.ThreadPidStat.KERNEL_TIME.ordinal()] * 1000L
                / LinuxOperatingSystem.getHz();
        this.userTime = statArray[TaskStat.ThreadPidStat.USER_TIME.ordinal()] * 1000L / LinuxOperatingSystem.getHz();
        this.upTime = now - startTime;

        // See man proc for how to parse /proc/[pid]/io
        this.bytesRead = ParseUtil.parseLongOrDefault(io.getOrDefault("read_bytes", ""), 0L);
        this.bytesWritten = ParseUtil.parseLongOrDefault(io.getOrDefault("write_bytes", ""), 0L);
        this.state = ProcessStat.getState(status.getOrDefault("State", "U").charAt(0));
        return true;
    }

    /**
     * Enum used to update attributes. The order field represents the 1-indexed
     * numeric order of the stat in /proc/pid/task/tid/stat per the man file.
     */
    private enum ThreadPidStat {
        // The parsing implementation in ParseUtil requires these to be declared
        // in increasing order
        PPID(4), USER_TIME(14), KERNEL_TIME(15), PRIORITY(18), THREAD_COUNT(20), START_TIME(22), VSZ(23), RSS(24);

        private final int order;

        ThreadPidStat(int order) {
            this.order = order;
        }

        public int getOrder() {
            return this.order;
        }
    }
}
