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
package oshi.software.os.unix.aix;

import java.util.List;

import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

public class AixOSThread extends AbstractOSThread {

    private int threadId;
    private OSProcess.State state = OSProcess.State.INVALID;
    private long contextSwitches;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private int priority;

    public AixOSThread(int pid, String[] split) {
        super(pid);
        updateAttributes(split);
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
    public double getThreadCpuLoadCumulative() {
        return 0D;
    }

    @Override
    public double getThreadCpuLoadBetweenTicks(OSThread priorSnapshot) {
        return 0D;
    }

    @Override
    public boolean updateAttributes() {
        List<String> threadListInfoPs = ExecutingCommand.runNative("ps -m -o THREAD -p " + getOwningProcessId());
        // 1st row is header, 2nd row is process data.
        if (threadListInfoPs.size() > 2) {
            threadListInfoPs.remove(0); // header removed
            threadListInfoPs.remove(0); // process data removed
            for (String threadInfo : threadListInfoPs) {
                // USER,PID,PPID,TID,ST,CP,PRI,SC,WCHAN,F,TT,BND,COMMAND
                String[] threadInfoSplit = ParseUtil.whitespaces.split(threadInfo.trim());
                if (threadInfoSplit.length == 13 && threadInfoSplit[3].equals(String.valueOf(this.getThreadId()))) {
                    String[] split = new String[3];
                    split[0] = threadInfoSplit[3]; // tid
                    split[1] = threadInfoSplit[4]; // state
                    split[2] = threadInfoSplit[6]; // priority
                    updateAttributes(split);
                }
            }
        }
        this.state = OSProcess.State.INVALID;
        return false;
    }

    private boolean updateAttributes(String[] split) {
        this.threadId = ParseUtil.parseIntOrDefault(split[0], 0);
        this.state = AixOSProcess.getStateFromOutput(split[1].charAt(0));
        this.priority = ParseUtil.parseIntOrDefault(split[2], 0);
        return true;
    }
}
