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
package oshi.software.common;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.software.os.OSProcess.State;
import oshi.software.os.OSThread;

public abstract class AbstractOSThread implements OSThread {

    private final Supplier<Double> cumulativeCpuLoad = memoize(this::queryCumulativeCpuLoad, defaultExpiration());

    private final int owningProcessId;

    public AbstractOSThread(int processId) {
        this.owningProcessId = processId;
    }

    @Override
    public int getOwningProcessId() {
        return this.owningProcessId;
    }

    @Override
    public double getThreadCpuLoadCumulative() {
        return cumulativeCpuLoad.get();
    }

    private double queryCumulativeCpuLoad() {
        return (getKernelTime() + getUserTime()) / (double) getUpTime();
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

    /*
     * Default returns so we don't have to override in subclasses that don't have
     * them
     */

    @Override
    public String getName() {
        return "";
    }

    @Override
    public State getState() {
        return State.OTHER;
    }

    @Override
    public long getStartTime() {
        return 0L;
    }

    @Override
    public long getStartMemoryAddress() {
        return 0L;
    }

    @Override
    public long getContextSwitches() {
        return 0L;
    }

    @Override
    public long getMinorFaults() {
        return 0L;
    }

    @Override
    public long getMajorFaults() {
        return 0L;
    }

    @Override
    public long getKernelTime() {
        return 0L;
    }

    @Override
    public long getUserTime() {
        return 0L;
    }

    @Override
    public long getUpTime() {
        return 0L;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean updateAttributes() {
        return false;
    }

    @Override
    public String toString() {
        return "OSThread [threadId=" + getThreadId() + ", owningProcessId=" + getOwningProcessId() + ", name="
                + getName() + ", state=" + getState() + ", kernelTime=" + getKernelTime() + ", userTime="
                + getUserTime() + ", upTime=" + getUpTime() + ", startTime=" + getStartTime() + ", startMemoryAddress="
                + getStartMemoryAddress() + ", contextSwitches=" + getContextSwitches() + ", minorFaults="
                + getMinorFaults() + ", majorFaults=" + getMajorFaults() + "]";
    }
}
