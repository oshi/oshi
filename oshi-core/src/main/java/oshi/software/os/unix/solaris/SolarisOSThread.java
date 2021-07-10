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
package oshi.software.os.unix.solaris;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess;
import oshi.software.os.OSProcess.State;
import oshi.software.os.unix.solaris.SolarisOSProcess.PsThreadColumns;
import oshi.software.os.unix.solaris.SolarisOperatingSystem.PrstatKeywords;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * OSThread implementation
 */
@ThreadSafe
public class SolarisOSThread extends AbstractOSThread {

    private int threadId;
    private OSProcess.State state = OSProcess.State.INVALID;
    private long startMemoryAddress;
    private long contextSwitches;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private int priority;

    public SolarisOSThread(int pid, Map<PsThreadColumns, String> psMap, Map<PrstatKeywords, String> prstatMap) {
        super(pid);
        updateAttributes(psMap, prstatMap);
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
    public long getStartMemoryAddress() {
        return this.startMemoryAddress;
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
        int pid = getOwningProcessId();
        List<String> threadList = ExecutingCommand
                .runNative("ps -o " + SolarisOSProcess.PS_THREAD_COLUMNS + " -p " + pid);
        if (threadList.size() > 1) {
            // there is no switch for thread in ps command, hence filtering.
            String lwpStr = Integer.toString(this.threadId);
            for (String psOutput : threadList) {
                Map<PsThreadColumns, String> threadMap = ParseUtil.stringToEnumMap(PsThreadColumns.class,
                        psOutput.trim(), ' ');
                if (threadMap.containsKey(PsThreadColumns.PRI) && lwpStr.equals(threadMap.get(PsThreadColumns.LWP))) {
                    List<String> prstatList = ExecutingCommand.runNative("prstat -L -v -p " + pid + " 1 1");
                    String prstatRow = "";
                    for (String s : prstatList) {
                        String row = s.trim();
                        if (row.endsWith("/" + lwpStr)) {
                            prstatRow = row;
                            break;
                        }
                    }
                    Map<PrstatKeywords, String> prstatMap = ParseUtil.stringToEnumMap(PrstatKeywords.class, prstatRow,
                            ' ');
                    return updateAttributes(threadMap, prstatMap);
                }
            }
        }
        this.state = State.INVALID;
        return false;
    }

    private boolean updateAttributes(Map<PsThreadColumns, String> psMap, Map<PrstatKeywords, String> prstatMap) {
        this.threadId = ParseUtil.parseIntOrDefault(psMap.get(PsThreadColumns.LWP), 0);
        this.state = SolarisOSProcess.getStateFromOutput(psMap.get(PsThreadColumns.S).charAt(0));
        // Avoid divide by zero for processes up less than a second
        long elapsedTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsThreadColumns.ETIME), 0L);
        this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
        long now = System.currentTimeMillis();
        this.startTime = now - this.upTime;
        this.kernelTime = 0L;
        this.userTime = ParseUtil.parseDHMSOrDefault(psMap.get(PsThreadColumns.TIME), 0L);
        this.startMemoryAddress = ParseUtil.hexStringToLong(psMap.get(PsThreadColumns.ADDR), 0L);
        this.priority = ParseUtil.parseIntOrDefault(psMap.get(PsThreadColumns.PRI), 0);
        long nonVoluntaryContextSwitches = ParseUtil.parseLongOrDefault(prstatMap.get(PrstatKeywords.ICX), 0L);
        long voluntaryContextSwitches = ParseUtil.parseLongOrDefault(prstatMap.get(PrstatKeywords.VCX), 0L);
        this.contextSwitches = voluntaryContextSwitches + nonVoluntaryContextSwitches;
        return true;
    }
}
