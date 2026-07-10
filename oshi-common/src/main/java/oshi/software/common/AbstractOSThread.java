/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.Locale;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSProcess;
import oshi.software.os.OSThread;

/**
 * Common methods for OSThread implementation
 */
@ThreadSafe
public abstract class AbstractOSThread implements OSThread {

    private final Supplier<Double> cumulativeCpuLoad = memoize(this::queryCumulativeCpuLoad, defaultExpiration());

    private final int owningProcessId;

    /** The thread id (OS-dependent meaning). */
    protected int threadId;
    /** The thread name; defaults to empty and is never returned as {@code null}. */
    protected String name = "";
    /** The thread execution state. */
    protected OSProcess.State state = OSProcess.State.INVALID;
    /** Minor (soft) page faults; populated on Linux/BSD only. */
    protected long minorFaults;
    /** Major (hard) page faults; populated on Linux/BSD only. */
    protected long majorFaults;
    /** The start memory address. */
    protected long startMemoryAddress;
    /** Voluntary + involuntary context switches. */
    protected long contextSwitches;
    /** Kernel (privileged) time in milliseconds. */
    protected long kernelTime;
    /** User time in milliseconds. */
    protected long userTime;
    /** Start time in milliseconds since the epoch. */
    protected long startTime;
    /** Elapsed up-time in milliseconds. */
    protected long upTime;
    /** OS-dependent priority. */
    protected int priority;

    /**
     * Creates an AbstractOSThread for the given owning process ID.
     *
     * @param processId the owning process ID
     */
    protected AbstractOSThread(int processId) {
        this.owningProcessId = processId;
    }

    @Override
    public int getOwningProcessId() {
        return this.owningProcessId;
    }

    @Override
    public int getThreadId() {
        return this.threadId;
    }

    @Override
    public String getName() {
        return this.name != null ? this.name : "";
    }

    @Override
    public OSProcess.State getState() {
        return this.state;
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
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public double getThreadCpuLoadCumulative() {
        return cumulativeCpuLoad.get();
    }

    private double queryCumulativeCpuLoad() {
        return getUpTime() > 0d ? (getKernelTime() + getUserTime()) / (double) getUpTime() : 0d;
    }

    @Override
    public double getThreadCpuLoadBetweenTicks(OSThread priorSnapshot) {
        if (priorSnapshot != null && owningProcessId == priorSnapshot.getOwningProcessId()
                && getThreadId() == priorSnapshot.getThreadId() && getUpTime() > priorSnapshot.getUpTime()) {
            return (getUserTime() - priorSnapshot.getUserTime() + getKernelTime() - priorSnapshot.getKernelTime())
                    / (double) (getUpTime() - priorSnapshot.getUpTime());
        }
        return getThreadCpuLoadCumulative();
    }

    @Override
    public String toString() {
        return "OSThread [threadId=" + getThreadId() + ", owningProcessId=" + getOwningProcessId() + ", name="
                + getName() + ", state=" + getState() + ", kernelTime=" + getKernelTime() + ", userTime="
                + getUserTime() + ", upTime=" + getUpTime() + ", startTime=" + getStartTime()
                + ", startMemoryAddress=0x" + String.format(Locale.ROOT, "%x", getStartMemoryAddress())
                + ", contextSwitches=" + getContextSwitches() + ", minorFaults=" + getMinorFaults() + ", majorFaults="
                + getMajorFaults() + "]";
    }
}
