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

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSProcess;

/**
 * A process is an instance of a computer program that is being executed. It
 * contains the program code and its current activity. Depending on the
 * operating system (OS), a process may be made up of multiple threads of
 * execution that execute instructions concurrently.
 */
@ThreadSafe
public abstract class AbstractOSProcess implements OSProcess {

    private final Supplier<Double> cumulativeCpuLoad = memoize(this::queryCumulativeCpuLoad, defaultExpiration());

    private int processID;

    public AbstractOSProcess(int pid) {
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
        return (getKernelTime() + getUserTime()) / (double) getUpTime();
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
