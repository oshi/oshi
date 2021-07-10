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
package oshi.software.os.unix.openbsd;

import static oshi.software.os.OSProcess.State.INVALID;
import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess;
import oshi.software.os.unix.openbsd.OpenBsdOSProcess.PsThreadColumns;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * OSThread implementation
 */
@ThreadSafe
public class OpenBsdOSThread extends AbstractOSThread {

    private int threadId;
    private String name = "";
    private OSProcess.State state = INVALID;
    private long minorFaults;
    private long majorFaults;
    private long startMemoryAddress;
    private long contextSwitches;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private int priority;

    public OpenBsdOSThread(int processId, Map<PsThreadColumns, String> threadMap) {
        super(processId);
        updateAttributes(threadMap);
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
    public boolean updateAttributes() {
        String psCommand = "ps -aHwwxo " + OpenBsdOSProcess.PS_THREAD_COLUMNS + " -p " + getOwningProcessId();
        // there is no switch for thread in ps command, hence filtering.
        List<String> threadList = ExecutingCommand.runNative(psCommand);
        String tidStr = Integer.toString(this.threadId);
        for (String psOutput : threadList) {
            Map<PsThreadColumns, String> threadMap = ParseUtil.stringToEnumMap(PsThreadColumns.class, psOutput.trim(),
                    ' ');
            if (threadMap.containsKey(PsThreadColumns.ARGS) && tidStr.equals(threadMap.get(PsThreadColumns.TID))) {
                return updateAttributes(threadMap);
            }
        }
        this.state = INVALID;
        return false;
    }

    private boolean updateAttributes(Map<PsThreadColumns, String> threadMap) {
        this.threadId = ParseUtil.parseIntOrDefault(threadMap.get(PsThreadColumns.TID), 0);
        switch (threadMap.get(PsThreadColumns.STATE).charAt(0)) {
        case 'R':
            this.state = RUNNING;
            break;
        case 'I':
        case 'S':
            this.state = SLEEPING;
            break;
        case 'D':
        case 'L':
        case 'U':
            this.state = WAITING;
            break;
        case 'Z':
            this.state = ZOMBIE;
            break;
        case 'T':
            this.state = STOPPED;
            break;
        default:
            this.state = OTHER;
            break;
        }
        // Avoid divide by zero for processes up less than a second
        long elapsedTime = ParseUtil.parseDHMSOrDefault(threadMap.get(PsThreadColumns.ETIME), 0L);
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        long now = System.currentTimeMillis();
        this.startTime = now - this.upTime;
        // ps does not provide kerneltime on OpenBSD
        this.kernelTime = 0L;
        this.userTime = ParseUtil.parseDHMSOrDefault(threadMap.get(PsThreadColumns.CPUTIME), 0L);
        this.startMemoryAddress = 0L;
        long nonVoluntaryContextSwitches = ParseUtil.parseLongOrDefault(threadMap.get(PsThreadColumns.NIVCSW), 0L);
        long voluntaryContextSwitches = ParseUtil.parseLongOrDefault(threadMap.get(PsThreadColumns.NVCSW), 0L);
        this.contextSwitches = voluntaryContextSwitches + nonVoluntaryContextSwitches;
        this.majorFaults = ParseUtil.parseLongOrDefault(threadMap.get(PsThreadColumns.MAJFLT), 0L);
        this.minorFaults = ParseUtil.parseLongOrDefault(threadMap.get(PsThreadColumns.MINFLT), 0L);
        this.priority = ParseUtil.parseIntOrDefault(threadMap.get(PsThreadColumns.PRI), 0);
        this.name = threadMap.get(PsThreadColumns.ARGS);
        return true;
    }
}
