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
package oshi.software.os.unix.aix;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess;
import oshi.software.os.unix.aix.AixOSProcess.PsThreadColumns;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * OSThread implementation
 */
@ThreadSafe
public class AixOSThread extends AbstractOSThread {

    private int threadId;
    private OSProcess.State state = OSProcess.State.INVALID;
    private long contextSwitches;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private int priority;

    public AixOSThread(int pid, Map<PsThreadColumns, String> threadMap) {
        super(pid);
        updateAttributes(threadMap);
    }

    @Override
    public int getThreadId() {
        return this.threadId;
    }

    @Override
    public OSProcess.State getState() {
        return this.state;
    }

    @Override
    public long getContextSwitches() {
        return this.contextSwitches;
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
        List<String> threadListInfoPs = ExecutingCommand.runNative("ps -m -o THREAD -p " + getOwningProcessId());
        // 1st row is header, 2nd row is process data.
        if (threadListInfoPs.size() > 2) {
            threadListInfoPs.remove(0); // header removed
            threadListInfoPs.remove(0); // process data removed
            String tidStr = Integer.toString(this.getThreadId());
            for (String threadInfo : threadListInfoPs) {
                Map<PsThreadColumns, String> threadMap = ParseUtil.stringToEnumMap(PsThreadColumns.class,
                        threadInfo.trim(), ' ');
                if (threadMap.containsKey(PsThreadColumns.COMMAND)
                        && tidStr.equals(threadMap.get(PsThreadColumns.TID))) {
                    return updateAttributes(threadMap);
                }
            }
        }
        this.state = OSProcess.State.INVALID;
        return false;
    }

    private boolean updateAttributes(Map<PsThreadColumns, String> threadMap) {
        this.threadId = ParseUtil.parseIntOrDefault(threadMap.get(PsThreadColumns.TID), 0);
        this.state = AixOSProcess.getStateFromOutput(threadMap.get(PsThreadColumns.ST).charAt(0));
        this.priority = ParseUtil.parseIntOrDefault(threadMap.get(PsThreadColumns.PRI), 0);
        return true;
    }
}
