/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import oshi.annotation.concurrent.GuardedBy;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Process.CommandLineProperty;
import oshi.util.tuples.Pair;

/**
 * Base for the cached {@code Win32_Process} command-line query. Holds the command-line cache and the lookup logic; the
 * per-backend (JNA/FFM) WMI query is supplied by {@link #queryCommandLines()}.
 */
@ThreadSafe
public abstract class Win32ProcessCached {

    @GuardedBy("commandLineCacheLock")
    private final Map<Integer, Pair<Long, String>> commandLineCache = new HashMap<>();
    private final ReentrantLock commandLineCacheLock = new ReentrantLock();

    /**
     * Default constructor.
     */
    protected Win32ProcessCached() {
    }

    /**
     * Queries the command lines of all running processes via WMI.
     *
     * @return the {@code Win32_Process} command-line WMI result
     */
    protected abstract WmiResult<CommandLineProperty> queryCommandLines();

    /**
     * Gets the process command line, while also querying and caching command lines for all running processes if the
     * specified process is not in the cache.
     *
     * @param processId The process ID for which to return the command line.
     * @param startTime The start time of the process, in milliseconds since the 1970 epoch.
     * @return The command line of the specified process.
     */
    public String getCommandLine(int processId, long startTime) {
        commandLineCacheLock.lock();
        try {
            Pair<Long, String> pair = commandLineCache.get(processId);
            if (pair != null && startTime < pair.getA()) {
                return pair.getB();
            } else {
                long now = System.currentTimeMillis();
                WmiResult<CommandLineProperty> commandLineAllProcs = queryCommandLines();
                if (commandLineCache.size() > commandLineAllProcs.getResultCount() * 2) {
                    commandLineCache.clear();
                }
                String result = "";
                for (int i = 0; i < commandLineAllProcs.getResultCount(); i++) {
                    int pid = WmiUtil.getUint32(commandLineAllProcs, CommandLineProperty.PROCESSID, i);
                    String cl = WmiUtil.getString(commandLineAllProcs, CommandLineProperty.COMMANDLINE, i);
                    commandLineCache.put(pid, new Pair<>(now, cl));
                    if (pid == processId) {
                        result = cl;
                    }
                }
                return result;
            }
        } finally {
            commandLineCacheLock.unlock();
        }
    }
}
