/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import static oshi.util.Memoizer.memoize;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import oshi.annotation.concurrent.GuardedBy;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Process.CommandLineProperty;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiUtilFFM;
import oshi.util.tuples.Pair;

/**
 * Utility to query WMI class {@code Win32_Process} using cache (FFM).
 */
@ThreadSafe
public final class Win32ProcessCachedFFM {

    private static final Supplier<Win32ProcessCachedFFM> INSTANCE = memoize(Win32ProcessCachedFFM::createInstance);

    @GuardedBy("commandLineCacheLock")
    private final Map<Integer, Pair<Long, String>> commandLineCache = new HashMap<>();
    private final ReentrantLock commandLineCacheLock = new ReentrantLock();

    private Win32ProcessCachedFFM() {
    }

    /**
     * Get the singleton instance of this class.
     *
     * @return the singleton instance
     */
    public static Win32ProcessCachedFFM getInstance() {
        return INSTANCE.get();
    }

    private static Win32ProcessCachedFFM createInstance() {
        return new Win32ProcessCachedFFM();
    }

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
                WmiResult<CommandLineProperty> commandLineAllProcs = Win32ProcessFFM.queryCommandLines(null);
                if (commandLineCache.size() > commandLineAllProcs.getResultCount() * 2) {
                    commandLineCache.clear();
                }
                String result = "";
                for (int i = 0; i < commandLineAllProcs.getResultCount(); i++) {
                    int pid = WmiUtilFFM.getUint32(commandLineAllProcs, CommandLineProperty.PROCESSID, i);
                    String cl = WmiUtilFFM.getString(commandLineAllProcs, CommandLineProperty.COMMANDLINE, i);
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
