/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSProcess;

/**
 * A process is an instance of a computer program that is being executed. It contains the program code and its current
 * activity. Depending on the operating system (OS), a process may be made up of multiple threads of execution that
 * execute instructions concurrently.
 */
@ThreadSafe
public abstract class AbstractOSProcess implements OSProcess {

    private final Supplier<Double> cumulativeCpuLoad = memoize(this::queryCumulativeCpuLoad, defaultExpiration());

    private final int processID;

    // Common attributes populated by each platform's updateAttributes(). Declared protected (rather than private with
    // setters) so the platform subclasses can assign them directly in their native refresh methods; see the
    // VisibilityModifier suppression for this file.
    protected volatile String name;
    protected volatile String path = "";
    protected volatile State state = State.INVALID;
    protected volatile int parentProcessID;
    protected volatile int threadCount;
    protected volatile int priority;
    protected volatile long virtualSize;
    protected volatile long kernelTime;
    protected volatile long userTime;
    protected volatile long startTime;
    protected volatile long upTime;
    protected volatile long bytesRead;
    protected volatile long bytesWritten;

    /**
     * Creates an AbstractOSProcess for the given process ID.
     *
     * @param pid the process ID
     */
    protected AbstractOSProcess(int pid) {
        this.processID = pid;
    }

    @Override
    public int getProcessID() {
        return this.processID;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public int getParentProcessID() {
        return this.parentProcessID;
    }

    @Override
    public int getThreadCount() {
        return this.threadCount;
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
    public double getProcessCpuLoadCumulative() {
        return cumulativeCpuLoad.get();
    }

    private double queryCumulativeCpuLoad() {
        return getUpTime() > 0d ? (getKernelTime() + getUserTime()) / (double) getUpTime() : 0d;
    }

    @Override
    public double getProcessCpuLoadBetweenTicks(OSProcess priorSnapshot) {
        if (priorSnapshot != null && this.processID == priorSnapshot.getProcessID()
                && getUpTime() > priorSnapshot.getUpTime()) {
            return (getUserTime() - priorSnapshot.getUserTime() + getKernelTime() - priorSnapshot.getKernelTime())
                    / (double) (getUpTime() - priorSnapshot.getUpTime());
        }
        return getProcessCpuLoadCumulative();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("OSProcess@");
        builder.append(Integer.toHexString(hashCode()));
        builder.append("[processID=").append(this.processID);
        builder.append(", name=").append(getName()).append(']');
        return builder.toString();
    }
}
