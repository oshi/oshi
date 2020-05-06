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
package oshi.driver.windows.wmi;

import static oshi.util.Memoizer.memoize;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult; // NOSONAR squid:S1191

import oshi.annotation.concurrent.GuardedBy;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.wmi.Win32Process.CommandLineProperty;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;

/**
 * Utility to query WMI class {@code Win32_Process} using cache
 */
@ThreadSafe
public final class Win32ProcessCached {

    private static final Supplier<Win32ProcessCached> INSTANCE = memoize(Win32ProcessCached::createInstance);

    // Use a map to cache command line queries
    @GuardedBy("commandLineCacheLock")
    private final Map<Integer, Pair<Long, String>> commandLineCache = new HashMap<>();
    private final ReentrantLock commandLineCacheLock = new ReentrantLock();

    private Win32ProcessCached() {
    }

    /**
     * Get the singleton instance of this class, instantiating the map which caches
     * command lines.
     *
     * @return the singleton instance
     */
    public static Win32ProcessCached getInstance() {
        return INSTANCE.get();
    }

    private static Win32ProcessCached createInstance() {
        return new Win32ProcessCached();
    }

    /**
     * Gets the process command line, while also querying and caching command lines
     * for all running processes if the specified process is not in the cache.
     * <p>
     * When iterating over a process list, the WMI overhead of querying each single
     * command line can quickly exceed the time it takes to query all command lines.
     * This method permits access to cached queries from a previous call,
     * significantly improving aggregate performance.
     *
     * @param processId
     *            The process ID for which to return the command line.
     * @param startTime
     *            The start time of the process, in milliseconds since the 1970
     *            epoch. If this start time is after the time this process was
     *            previously queried, the prior entry will be deemed invalid and the
     *            cache refreshed.
     * @return The command line of the specified process. If the command line is
     *         cached from a previous call and the start time is prior to the time
     *         it was cached, this method will quickly return the cached value.
     *         Otherwise, will refresh the cache with all running processes prior to
     *         returning, which may incur some latency.
     *         <p>
     *         May return a command line from the cache even after a process has
     *         terminated. Otherwise will return the empty string.
     */
    public String getCommandLine(int processId, long startTime) {
        // We could use synchronized method but this is more clear
        commandLineCacheLock.lock();
        try {
            // See if this process is in the cache already
            Pair<Long, String> pair = commandLineCache.get(processId);
            // Valid process must have been started before map insertion
            if (pair != null && startTime < pair.getA()) {
                // Entry is valid, return it!
                return pair.getB();
            } else {
                // Invalid entry, rebuild cache
                // Processes started after this time will be invalid
                long now = System.currentTimeMillis();
                // Gets all processes. Takes ~200ms
                WmiResult<CommandLineProperty> commandLineAllProcs = Win32Process.queryCommandLines(null);
                // Stale processes use resources. Periodically clear before building
                // Use a threshold of map size > 2x # of processes
                if (commandLineCache.size() > commandLineAllProcs.getResultCount() * 2) {
                    commandLineCache.clear();
                }
                // Iterate results and put in map. Save value for current PID along the way
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
