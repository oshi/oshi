/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOSProcess;
import oshi.util.Constants;

/**
 * Minimal stand-in for the current process, returned by {@link OperatingSystem#getCurrentProcess()} only when the
 * platform query fails to produce it.
 * <p>
 * The current process exists by definition, so a failed lookup is a failed read — a truncated native process list, a
 * denied {@code /proc} entry, a race with the query — rather than a missing process. {@code getProcess(int)} is
 * documented to return null and {@code getCurrentProcess()} is not, so this carries the facts still known to be true
 * (the process ID, and that it is running) and reports every other value as the unknown/zero/empty default that callers
 * already handle from a live process.
 */
@ThreadSafe
final class CurrentProcessStub extends AbstractOSProcess {

    CurrentProcessStub(int pid) {
        super(pid);
        this.name = "";
        this.state = State.RUNNING;
    }

    @Override
    public List<OSThread> getThreadDetails() {
        // Empty rather than absent: every platform's getCurrentThread() already falls back to constructing a thread
        // from the process ID and thread ID when the list holds no match.
        return Collections.emptyList();
    }

    @Override
    public String getUser() {
        return Constants.UNKNOWN;
    }

    @Override
    public String getUserID() {
        return Constants.UNKNOWN;
    }

    @Override
    public String getGroup() {
        return Constants.UNKNOWN;
    }

    @Override
    public String getGroupID() {
        return Constants.UNKNOWN;
    }

    @Override
    public String getCommandLine() {
        return "";
    }

    @Override
    public List<String> getArguments() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        return Collections.emptyMap();
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return "";
    }

    @Override
    public long getAffinityMask() {
        return 0L;
    }

    @Override
    public int getBitness() {
        return 0;
    }

    @Override
    public long getResidentMemory() {
        return 0L;
    }

    @Override
    public long getOpenFiles() {
        return 0L;
    }

    @Override
    public long getSoftOpenFileLimit() {
        return -1L;
    }

    @Override
    public long getHardOpenFileLimit() {
        return -1L;
    }

    @Override
    public boolean updateAttributes() {
        // Nothing to refresh: this instance exists precisely because the platform query did not return this process.
        return false;
    }
}
