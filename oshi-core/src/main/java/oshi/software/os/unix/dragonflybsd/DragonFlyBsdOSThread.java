/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.dragonflybsd;

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
import oshi.software.os.unix.dragonflybsd.DragonFlyBsdOSProcess.PsThreadColumns;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * OSThread implementation
 */
@ThreadSafe
public class DragonFlyBsdOSThread extends AbstractOSThread {

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

    public DragonFlyBsdOSThread(int processId, Map<PsThreadColumns, String> threadMap) {
        super(processId);
        updateAttributes(threadMap);
    }

    public DragonFlyBsdOSThread(int processId, int threadId) {
        super(processId);
        this.threadId = threadId;
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
        List<String> threadList = ExecutingCommand
                .runNative("ps -awwxo " + DragonFlyBsdOSProcess.PS_THREAD_COLUMNS + " -H -p " + getOwningProcessId());
        // there is no switch for thread in ps command, hence filtering.
        String lwpStr = Integer.toString(this.threadId);
        for (String psOutput : threadList) {
            Map<PsThreadColumns, String> threadMap = ParseUtil.stringToEnumMap(PsThreadColumns.class, psOutput.trim(),
                    ' ');
            if (threadMap.containsKey(PsThreadColumns.PRI) && lwpStr.equals(threadMap.get(PsThreadColumns.TID))) {
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
        long now = System.currentTimeMillis();
        this.startTime = now;
        this.upTime = 1L;
        this.userTime = ParseUtil.parseDHMSOrDefault(threadMap.get(PsThreadColumns.TIME), 0L);
        this.majorFaults = ParseUtil.parseLongOrDefault(threadMap.get(PsThreadColumns.MAJFLT), 0L);
        this.minorFaults = ParseUtil.parseLongOrDefault(threadMap.get(PsThreadColumns.MINFLT), 0L);
        long nvcsw = ParseUtil.parseLongOrDefault(threadMap.get(PsThreadColumns.NVCSW), 0L);
        long nivcsw = ParseUtil.parseLongOrDefault(threadMap.get(PsThreadColumns.NIVCSW), 0L);
        this.contextSwitches = nvcsw + nivcsw;
        this.priority = ParseUtil.parseIntOrDefault(threadMap.get(PsThreadColumns.PRI), 0);
        return true;
    }
}
