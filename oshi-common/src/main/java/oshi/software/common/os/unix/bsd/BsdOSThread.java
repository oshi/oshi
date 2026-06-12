/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.bsd;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Abstract base shared by the BSD-family OSThread implementations (FreeBSD, OpenBSD, DragonFly, NetBSD). Holds the
 * field storage, the trivial accessors, and the shared {@code ps} thread-row parsing. Differences in the available
 * columns (thread-id keyword, name, elapsed/cpu time, thread address) are handled by checking which keys are present.
 * Platform-specific work (the ordered column list and the {@code ps} command used to refresh a single thread) is
 * provided by the per-platform subclasses.
 */
@ThreadSafe
public abstract class BsdOSThread extends AbstractOSThread {

    protected int threadId;
    protected String name = "";
    protected OSProcess.State state = OSProcess.State.INVALID;
    protected long minorFaults;
    protected long majorFaults;
    protected long startMemoryAddress;
    protected long contextSwitches;
    protected long kernelTime;
    protected long userTime;
    protected long startTime;
    protected long upTime;
    protected int priority;

    protected BsdOSThread(int processId) {
        super(processId);
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
        List<BsdPsThreadKeyword> cols = psThreadKeywords();
        BsdPsThreadKeyword idKey = threadIdKeyword(cols);
        BsdPsThreadKeyword lastKey = cols.get(cols.size() - 1);
        // There is no -p switch for a single thread, so refresh the owning process's thread rows and filter to ours.
        String idStr = Integer.toString(this.threadId);
        for (String psOutput : ExecutingCommand.runNative(psThreadCommand() + " -p " + getOwningProcessId())) {
            Map<BsdPsThreadKeyword, String> threadMap = ParseUtil.stringToEnumMap(BsdPsThreadKeyword.class, cols,
                    psOutput.trim(), ' ');
            if (threadMap.containsKey(lastKey) && idStr.equals(threadMap.get(idKey))) {
                return updateAttributes(threadMap);
            }
        }
        this.state = OSProcess.State.INVALID;
        return false;
    }

    /**
     * Populates this thread's attributes from a parsed {@code ps} thread row. Shared by every BSD platform; differences
     * in the available columns are handled by checking which keys are present in the map.
     *
     * @param threadMap the parsed {@code ps} columns for this thread
     * @return {@code true} once the attributes are populated
     */
    protected boolean updateAttributes(Map<BsdPsThreadKeyword, String> threadMap) {
        // Thread id: lwp (FreeBSD), tid (OpenBSD/DragonFly), or lid (NetBSD)
        this.threadId = ParseUtil.parseIntOrDefault(threadIdValue(threadMap), 0);
        this.state = BsdOSProcess.getStateFromOutput(threadMap.get(BsdPsThreadKeyword.STATE).charAt(0));
        // Name: tdname (FreeBSD), args (OpenBSD/NetBSD), or absent (DragonFly leaves the default empty string)
        if (threadMap.containsKey(BsdPsThreadKeyword.TDNAME)) {
            this.name = threadMap.get(BsdPsThreadKeyword.TDNAME);
        } else if (threadMap.containsKey(BsdPsThreadKeyword.ARGS)) {
            this.name = threadMap.get(BsdPsThreadKeyword.ARGS);
        }
        // Kernel/user time: FreeBSD reports systime separately (time is user+sys); the others fold kernel time into a
        // single cputime/time column.
        if (threadMap.containsKey(BsdPsThreadKeyword.SYSTIME)) {
            this.kernelTime = ParseUtil.parseDHMSOrDefault(threadMap.get(BsdPsThreadKeyword.SYSTIME), 0L);
            this.userTime = ParseUtil.parseDHMSOrDefault(threadMap.get(BsdPsThreadKeyword.TIME), 0L) - this.kernelTime;
        } else if (threadMap.containsKey(BsdPsThreadKeyword.CPUTIME)) {
            this.kernelTime = 0L;
            this.userTime = ParseUtil.parseDHMSOrDefault(threadMap.get(BsdPsThreadKeyword.CPUTIME), 0L);
        } else {
            this.kernelTime = 0L;
            this.userTime = ParseUtil.parseDHMSOrDefault(threadMap.get(BsdPsThreadKeyword.TIME), 0L);
        }
        // Start/up time: derived from the ps elapsed-time column where present; DragonFly's thread ps has none.
        long now = System.currentTimeMillis();
        if (threadMap.containsKey(BsdPsThreadKeyword.ETIMES) || threadMap.containsKey(BsdPsThreadKeyword.ETIME)) {
            BsdPsThreadKeyword elapsedKey = threadMap.containsKey(BsdPsThreadKeyword.ETIMES) ? BsdPsThreadKeyword.ETIMES
                    : BsdPsThreadKeyword.ETIME;
            long elapsedTime = ParseUtil.parseDHMSOrDefault(threadMap.get(elapsedKey), 0L);
            this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
            this.startTime = now - this.upTime;
        } else {
            this.startTime = now;
            this.upTime = 1L;
        }
        this.startMemoryAddress = threadMap.containsKey(BsdPsThreadKeyword.TDADDR)
                ? ParseUtil.hexStringToLong(threadMap.get(BsdPsThreadKeyword.TDADDR), 0L)
                : 0L;
        this.contextSwitches = ParseUtil.parseLongOrDefault(threadMap.get(BsdPsThreadKeyword.NVCSW), 0L)
                + ParseUtil.parseLongOrDefault(threadMap.get(BsdPsThreadKeyword.NIVCSW), 0L);
        this.majorFaults = ParseUtil.parseLongOrDefault(threadMap.get(BsdPsThreadKeyword.MAJFLT), 0L);
        this.minorFaults = ParseUtil.parseLongOrDefault(threadMap.get(BsdPsThreadKeyword.MINFLT), 0L);
        this.priority = ParseUtil.parseIntOrDefault(threadMap.get(BsdPsThreadKeyword.PRI), 0);
        return true;
    }

    private static String threadIdValue(Map<BsdPsThreadKeyword, String> threadMap) {
        if (threadMap.containsKey(BsdPsThreadKeyword.LWP)) {
            return threadMap.get(BsdPsThreadKeyword.LWP);
        }
        if (threadMap.containsKey(BsdPsThreadKeyword.TID)) {
            return threadMap.get(BsdPsThreadKeyword.TID);
        }
        return threadMap.get(BsdPsThreadKeyword.LID);
    }

    private static BsdPsThreadKeyword threadIdKeyword(List<BsdPsThreadKeyword> cols) {
        if (cols.contains(BsdPsThreadKeyword.LWP)) {
            return BsdPsThreadKeyword.LWP;
        }
        if (cols.contains(BsdPsThreadKeyword.TID)) {
            return BsdPsThreadKeyword.TID;
        }
        return BsdPsThreadKeyword.LID;
    }

    /**
     * Returns this platform's ordered {@code ps} thread column keys, used to parse thread rows positionally.
     *
     * @return the ordered thread column keys
     */
    protected abstract List<BsdPsThreadKeyword> psThreadKeywords();

    /**
     * Returns the {@code ps} command (without the trailing {@code -p <pid>}) used to enumerate this process's threads
     * when refreshing a single thread.
     *
     * @return the {@code ps} command prefix
     */
    protected abstract String psThreadCommand();
}
