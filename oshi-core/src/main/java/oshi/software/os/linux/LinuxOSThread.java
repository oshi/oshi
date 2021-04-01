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
package oshi.software.os.linux;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.proc.ProcessStat;
import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess.State;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;

/**
 * OSThread implementation
 */
@ThreadSafe
public class LinuxOSThread extends AbstractOSThread {

    private static final int[] PROC_TASK_STAT_ORDERS = new int[LinuxOSThread.ThreadPidStat.values().length];
    static {
        for (LinuxOSThread.ThreadPidStat stat : LinuxOSThread.ThreadPidStat.values()) {
            // The PROC_PID_STAT enum indices are 1-indexed.
            // Subtract one to get a zero-based index
            PROC_TASK_STAT_ORDERS[stat.ordinal()] = stat.getOrder() - 1;
        }
    }

    private final int threadId;
    private String name;
    private State state = State.INVALID;
    private long minorFaults;
    private long majorFaults;
    private long startMemoryAddress;
    private long contextSwitches;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private int priority;

    public LinuxOSThread(int processId, int tid) {
        super(processId);
        this.threadId = tid;
        updateAttributes();
    }

    @Override
    public int getThreadId() {
        return this.threadId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public long getStartMemoryAddress() {
        return this.startMemoryAddress;
    }

    @Override
    public long getContextSwitches() {
        return this.contextSwitches;
    }

    @Override
    public long getMinorFaults() {
        return this.minorFaults;
    }

    @Override
    public long getMajorFaults() {
        return this.majorFaults;
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
    public int getPriority() {
        return this.priority;
    }

    @Override
    public boolean updateAttributes() {
        this.name = FileUtil
                .getStringFromFile(String.format(ProcPath.TASK_COMM, this.getOwningProcessId(), this.threadId));
        Map<String, String> status = FileUtil.getKeyValueMapFromFile(
                String.format(ProcPath.TASK_STATUS, this.getOwningProcessId(), this.threadId), ":");
        String stat = FileUtil
                .getStringFromFile(String.format(ProcPath.TASK_STAT, this.getOwningProcessId(), this.threadId));
        if (stat.isEmpty()) {
            this.state = State.INVALID;
            return false;
        }
        long now = System.currentTimeMillis();
        long[] statArray = ParseUtil.parseStringToLongArray(stat, PROC_TASK_STAT_ORDERS,
                ProcessStat.PROC_PID_STAT_LENGTH, ' ');

        // BOOTTIME is in seconds and start time from proc/pid/stat is in jiffies.
        // Combine units to jiffies and convert to millijiffies before hz division to
        // avoid precision loss without having to cast
        this.startTime = (LinuxOperatingSystem.BOOTTIME * LinuxOperatingSystem.getHz()
                + statArray[LinuxOSThread.ThreadPidStat.START_TIME.ordinal()]) * 1000L / LinuxOperatingSystem.getHz();
        // BOOT_TIME could be up to 500ms off and start time up to 5ms off. A process
        // that has started within last 505ms could produce a future start time/negative
        // up time, so insert a sanity check.
        if (this.startTime >= now) {
            this.startTime = now - 1;
        }
        this.minorFaults = statArray[ThreadPidStat.MINOR_FAULTS.ordinal()];
        this.majorFaults = statArray[ThreadPidStat.MAJOR_FAULT.ordinal()];
        this.startMemoryAddress = statArray[ThreadPidStat.START_CODE.ordinal()];
        long voluntaryContextSwitches = ParseUtil.parseLongOrDefault(status.get("voluntary_ctxt_switches"), 0L);
        long nonVoluntaryContextSwitches = ParseUtil.parseLongOrDefault(status.get("nonvoluntary_ctxt_switches"), 0L);
        this.contextSwitches = voluntaryContextSwitches + nonVoluntaryContextSwitches;
        this.state = ProcessStat.getState(status.getOrDefault("State", "U").charAt(0));
        this.kernelTime = statArray[ThreadPidStat.KERNEL_TIME.ordinal()] * 1000L / LinuxOperatingSystem.getHz();
        this.userTime = statArray[ThreadPidStat.USER_TIME.ordinal()] * 1000L / LinuxOperatingSystem.getHz();
        this.upTime = now - startTime;
        this.priority = (int) statArray[ThreadPidStat.PRIORITY.ordinal()];
        return true;
    }

    /**
     * Enum used to update attributes. The order field represents the 1-indexed
     * numeric order of the stat in /proc/pid/task/tid/stat per the man file.
     */
    private enum ThreadPidStat {
        // The parsing implementation in ParseUtil requires these to be declared
        // in increasing order
        PPID(4), MINOR_FAULTS(10), MAJOR_FAULT(12), USER_TIME(14), KERNEL_TIME(15), PRIORITY(18), THREAD_COUNT(20),
        START_TIME(22), VSZ(23), RSS(24), START_CODE(26);

        private final int order;

        ThreadPidStat(int order) {
            this.order = order;
        }

        public int getOrder() {
            return this.order;
        }
    }
}
