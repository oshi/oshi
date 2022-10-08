/*
 * Copyright 2020-2022 The OSHI Project Contributors
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

    private int processID;

    protected AbstractOSProcess(int pid) {
        this.processID = pid;
    }

    @Override
    public int getProcessID() {
        return this.processID;
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
