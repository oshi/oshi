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
package oshi.software.common;

import static oshi.software.os.OperatingSystem.ProcessFiltering.ALL_PROCESSES;
import static oshi.software.os.OperatingSystem.ProcessSorting.NO_SORTING;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.sun.jna.Platform; // NOSONAR squid:S1191

import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.GlobalConfig;
import oshi.util.tuples.Pair;

/**
 * Common methods for OperatingSystem implementations
 */
public abstract class AbstractOperatingSystem implements OperatingSystem {

    public static final String OSHI_OS_UNIX_WHOCOMMAND = "oshi.os.unix.whoCommand";
    protected static final boolean USE_WHO_COMMAND = GlobalConfig.get(OSHI_OS_UNIX_WHOCOMMAND, false);

    private final Supplier<String> manufacturer = memoize(this::queryManufacturer);
    private final Supplier<Pair<String, OSVersionInfo>> familyVersionInfo = memoize(this::queryFamilyVersionInfo);
    private final Supplier<Integer> bitness = memoize(this::queryPlatformBitness);

    @Override
    public String getManufacturer() {
        return manufacturer.get();
    }

    protected abstract String queryManufacturer();

    @Override
    public String getFamily() {
        return familyVersionInfo.get().getA();
    }

    @Override
    public OSVersionInfo getVersionInfo() {
        return familyVersionInfo.get().getB();
    }

    protected abstract Pair<String, OSVersionInfo> queryFamilyVersionInfo();

    @Override
    public int getBitness() {
        return bitness.get();
    }

    private int queryPlatformBitness() {
        if (Platform.is64Bit()) {
            return 64;
        }
        // Initialize based on JVM Bitness. Individual OS implementations will test
        // if 32-bit JVM running on 64-bit OS
        int jvmBitness = System.getProperty("os.arch").indexOf("64") != -1 ? 64 : 32;
        return queryBitness(jvmBitness);
    }

    /**
     * Backup OS-specific query to determine bitness if previous checks fail
     *
     * @param jvmBitness
     *            The bitness of the JVM
     * @return The operating system bitness
     */
    protected abstract int queryBitness(int jvmBitness);

    @Override
    public List<OSProcess> getProcesses(Predicate<OSProcess> filter, Comparator<OSProcess> sort, int limit) {
        return queryAllProcesses().stream().filter(filter == null ? ALL_PROCESSES : filter)
                .sorted(sort == null ? NO_SORTING : sort).limit(limit > 0 ? limit : Long.MAX_VALUE)
                .collect(Collectors.toList());
    }

    protected abstract List<OSProcess> queryAllProcesses();

    @Override
    public List<OSProcess> getChildProcesses(int parentPid, Predicate<OSProcess> filter, Comparator<OSProcess> sort,
            int limit) {
        // Get this pid and its children
        List<OSProcess> childProcs = queryChildProcesses(parentPid);
        // Extract the parent from the list
        OSProcess parent = childProcs.stream().filter(p -> p.getParentProcessID() == parentPid).findAny().orElse(null);
        // Get the parent's start time
        long parentStartTime = parent == null ? 0 : parent.getStartTime();
        // Get children after parent
        return queryChildProcesses(parentPid).stream().filter(filter == null ? ALL_PROCESSES : filter)
                .filter(p -> p.getProcessID() != parentPid && p.getStartTime() >= parentStartTime)
                .sorted(sort == null ? NO_SORTING : sort).limit(limit > 0 ? limit : Long.MAX_VALUE)
                .collect(Collectors.toList());
    }

    protected abstract List<OSProcess> queryChildProcesses(int parentPid);

    @Override
    public List<OSProcess> getDescendantProcesses(int parentPid, Predicate<OSProcess> filter,
            Comparator<OSProcess> sort, int limit) {
        // Get this pid and its descendants
        List<OSProcess> descendantProcs = queryDescendantProcesses(parentPid);
        // Extract the parent from the list
        OSProcess parent = descendantProcs.stream().filter(p -> p.getParentProcessID() == parentPid).findAny()
                .orElse(null);
        // Get the parent's start time
        long parentStartTime = parent == null ? 0 : parent.getStartTime();
        // Get descendants after parent
        return queryDescendantProcesses(parentPid).stream().filter(filter == null ? ALL_PROCESSES : filter)
                .filter(p -> p.getProcessID() != parentPid && p.getStartTime() >= parentStartTime)
                .sorted(sort == null ? NO_SORTING : sort).limit(limit > 0 ? limit : Long.MAX_VALUE)
                .collect(Collectors.toList());
    }

    protected abstract List<OSProcess> queryDescendantProcesses(int parentPid);

    /**
     * Utility method for subclasses to take a full process list as input and return
     * the children or descendants of a particular process. The process itself is
     * also returned to more efficiently extract its start time for filtering
     *
     * @param allProcs
     *            A collection of all processes
     * @param parentPid
     *            The process ID whose children or descendants to return
     * @param allDescendants
     *            If false, only gets immediate children of this process. If true,
     *            gets all descendants.
     * @return Set of children or descendants of parentPid
     */
    protected static Set<Integer> getChildrenOrDescendants(Collection<OSProcess> allProcs, int parentPid,
            boolean allDescendants) {
        Map<Integer, Integer> parentPidMap = allProcs.stream()
                .collect(Collectors.toMap(OSProcess::getProcessID, OSProcess::getParentProcessID));
        return getChildrenOrDescendants(parentPidMap, parentPid, allDescendants);
    }

    /**
     * Utility method for subclasses to take a map of pid to parent as input and
     * return the children or descendants of a particular process.
     *
     * @param parentPidMap
     *            a map of all processes with processID as key and parentProcessID
     *            as value
     * @param parentPid
     *            The process ID whose children or descendants to return
     * @param allDescendants
     *            If false, only gets immediate children of this process. If true,
     *            gets all descendants.
     * @return Set of children or descendants of parentPid, including the parent
     */
    protected static Set<Integer> getChildrenOrDescendants(Map<Integer, Integer> parentPidMap, int parentPid,
            boolean allDescendants) {
        // Set to hold results
        Set<Integer> descendantPids = new HashSet<>();
        descendantPids.add(parentPid);
        // Queue for BFS algorithm
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(parentPid);
        // Add children, repeating if recursive
        do {
            for (int pid : getChildren(parentPidMap, queue.poll())) {
                if (!descendantPids.contains(pid)) {
                    descendantPids.add(pid);
                    queue.add(pid);
                }
            }
        } while (allDescendants && !queue.isEmpty());
        return descendantPids;
    }

    private static Set<Integer> getChildren(Map<Integer, Integer> parentPidMap, int parentPid) {
        return parentPidMap.entrySet().stream()
                .filter(e -> e.getValue().equals(parentPid) && !e.getKey().equals(parentPid)).map(Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getManufacturer()).append(' ').append(getFamily()).append(' ').append(getVersionInfo());
        return sb.toString();
    }
}
