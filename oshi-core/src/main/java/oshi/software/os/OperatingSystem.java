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
package oshi.software.os;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.Who;
import oshi.driver.unix.Xwininfo;
import oshi.software.os.OSProcess.State;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;

/**
 * An operating system (OS) is the software on a computer that manages the way
 * different programs use its hardware, and regulates the ways that a user
 * controls the computer.
 * <p>
 * Considered thread safe, but see remarks for the {@link #getSessions()}
 * method.
 */
@ThreadSafe
public interface OperatingSystem {

    /**
     * Controls sorting of Process lists.
     *
     * @deprecated Use comparators from {@link ProcessSorting}.
     */
    @Deprecated
    enum ProcessSort {
        CPU, MEMORY, OLDEST, NEWEST, PID, PARENTPID, NAME
    }

    /**
     * Constants which may be used to filter Process lists in
     * {@link #getProcesses(Predicate, Comparator, int)},
     * {@link #getChildProcesses(int, Predicate, Comparator, int)}, and
     * {@link #getDescendantProcesses(int, Predicate, Comparator, int)}.
     */
    final class ProcessFiltering {
        private ProcessFiltering() {
        }

        /**
         * No filtering.
         */
        public static final Predicate<OSProcess> ALL_PROCESSES = p -> true;
        /**
         * Exclude processes with {@link State#INVALID} process state.
         */
        public static final Predicate<OSProcess> VALID_PROCESS = p -> !p.getState().equals(State.INVALID);
        /**
         * Exclude child processes. Only include processes which are their own parent.
         */
        public static final Predicate<OSProcess> NO_PARENT = p -> p.getParentProcessID() == p.getProcessID();
        /**
         * Only incude 64-bit processes.
         */
        public static final Predicate<OSProcess> BITNESS_64 = p -> p.getBitness() == 64;
        /**
         * Only include 32-bit processes.
         */
        public static final Predicate<OSProcess> BITNESS_32 = p -> p.getBitness() == 32;
    }

    /**
     * Constants which may be used to sort Process lists in
     * {@link #getProcesses(Predicate, Comparator, int)},
     * {@link #getChildProcesses(int, Predicate, Comparator, int)}, and
     * {@link #getDescendantProcesses(int, Predicate, Comparator, int)}.
     */
    final class ProcessSorting {
        private ProcessSorting() {
        }

        /**
         * No sorting
         */
        public static final Comparator<OSProcess> NO_SORTING = (p1, p2) -> 0;
        /**
         * Sort by decreasing cumulative CPU percentage
         */
        public static final Comparator<OSProcess> CPU_DESC = Comparator
                .comparingDouble(OSProcess::getProcessCpuLoadCumulative).reversed();
        /**
         * Sort by decreasing Resident Set Size (RSS)
         */
        public static final Comparator<OSProcess> RSS_DESC = Comparator.comparingLong(OSProcess::getResidentSetSize)
                .reversed();
        /**
         * Sort by up time, newest processes first
         */
        public static final Comparator<OSProcess> UPTIME_ASC = Comparator.comparingLong(OSProcess::getUpTime);
        /**
         * Sort by up time, oldest processes first
         */
        public static final Comparator<OSProcess> UPTIME_DESC = UPTIME_ASC.reversed();
        /**
         * Sort by Process Id
         */
        public static final Comparator<OSProcess> PID_ASC = Comparator.comparingInt(OSProcess::getProcessID);
        /**
         * Sort by Parent Process Id
         */
        public static final Comparator<OSProcess> PARENTPID_ASC = Comparator
                .comparingInt(OSProcess::getParentProcessID);
        /**
         * Sort by Process Name (case insensitive)
         */
        public static final Comparator<OSProcess> NAME_ASC = Comparator.comparing(OSProcess::getName,
                String.CASE_INSENSITIVE_ORDER);

        /**
         * Temporary method to convert deprecated ProcessSort to its corresponding
         * comparator. Remove when the deprecated enum is removed.
         *
         * @param sort
         *            The process sort
         * @return The corresponding comparator
         */
        private static Comparator<OSProcess> convertSortToComparator(ProcessSort sort) {
            if (sort != null) {
                switch (sort) {
                case CPU:
                    return CPU_DESC;
                case MEMORY:
                    return RSS_DESC;
                case OLDEST:
                    return UPTIME_DESC;
                case NEWEST:
                    return UPTIME_ASC;
                case PID:
                    return PID_ASC;
                case PARENTPID:
                    return PARENTPID_ASC;
                case NAME:
                    return NAME_ASC;
                default:
                    // Should never get here! If you get this exception you've
                    // added something to the enum without adding it here. Tsk.
                    // But that enum is now deprecated so double-tsk if you add!
                    throw new IllegalArgumentException("Unimplemented enum type: " + sort.toString());
                }
            }
            return NO_SORTING;
        }
    }

    /**
     * Get the Operating System family.
     *
     * @return the family
     */
    String getFamily();

    /**
     * Get the Operating System manufacturer.
     *
     * @return the manufacturer
     */
    String getManufacturer();

    /**
     * Get Operating System version information.
     *
     * @return version information
     */
    OSVersionInfo getVersionInfo();

    /**
     * Instantiates a {@link oshi.software.os.FileSystem} object.
     *
     * @return A {@link oshi.software.os.FileSystem} object.
     */
    FileSystem getFileSystem();

    /**
     * Instantiates a {@link oshi.software.os.InternetProtocolStats} object.
     *
     * @return a {@link oshi.software.os.InternetProtocolStats} object.
     */
    InternetProtocolStats getInternetProtocolStats();

    /**
     * Gets currently running processes. No order is guaranteed.
     *
     * @return A list of {@link oshi.software.os.OSProcess} objects for the
     *         specified number (or all) of currently running processes, sorted as
     *         specified. The list may contain null elements or processes with a
     *         state of {@link OSProcess.State#INVALID} if a process terminates
     *         during iteration.
     */
    default List<OSProcess> getProcesses() {
        return getProcesses(null, null, 0);
    }

    /**
     * Gets currently running processes, optionally filtering, sorting, and limited
     * to the top "N".
     *
     * @param filter
     *            An optional {@link Predicate} limiting the results to the
     *            specified filter. Some common predicates are available in
     *            {@link ProcessSorting}. May be {@code null} for no filtering.
     * @param sort
     *            An optional {@link Comparator} specifying the sorting order. Some
     *            common comparators are available in {@link ProcessSorting}. May be
     *            {@code null} for no sorting.
     * @param limit
     *            Max number of results to return, or 0 to return all results
     * @return A list of {@link oshi.software.os.OSProcess} objects, optionally
     *         filtered, sorted, and limited to the specified number.
     *         <p>
     *         The list may contain processes with a state of
     *         {@link OSProcess.State#INVALID} if a process terminates during
     *         iteration.
     */
    List<OSProcess> getProcesses(Predicate<OSProcess> filter, Comparator<OSProcess> sort, int limit);

    /**
     * Gets currently running processes, optionally limited to the top "N" for a
     * particular sorting order. If a positive limit is specified, returns only that
     * number of processes; zero will return all processes. The order may be
     * specified by the sort parameter, for example, to return the top cpu or memory
     * consuming processes; if the sort is {@code null}, no order is guaranteed.
     *
     * @param limit
     *            Max number of results to return, or 0 to return all results
     * @param sort
     *            If not null, determines sorting of results
     * @return A list of {@link oshi.software.os.OSProcess} objects for the
     *         specified number (or all) of currently running processes, sorted as
     *         specified. The list may contain null elements or processes with a
     *         state of {@link OSProcess.State#INVALID} if a process terminates
     *         during iteration.
     * @deprecated Use {@link #getProcesses(Predicate, Comparator, int)} with
     *             sorting constants from {@link ProcessSorting}.
     */
    @Deprecated
    default List<OSProcess> getProcesses(int limit, ProcessSort sort) {
        return getProcesses(null, ProcessSorting.convertSortToComparator(sort), limit);
    }

    /**
     * Gets information on a {@link Collection} of currently running processes. This
     * has potentially improved performance vs. iterating individual processes.
     *
     * @param pids
     *            A collection of process IDs
     * @return A list of {@link oshi.software.os.OSProcess} objects for the
     *         specified process ids if it is running
     */
    default List<OSProcess> getProcesses(Collection<Integer> pids) {
        return pids.stream().map(this::getProcess).filter(Objects::nonNull).filter(ProcessFiltering.VALID_PROCESS)
                .collect(Collectors.toList());
    }

    /**
     * Gets information on a currently running process
     *
     * @param pid
     *            A process ID
     * @return An {@link oshi.software.os.OSProcess} object for the specified
     *         process id if it is running; null otherwise
     */
    OSProcess getProcess(int pid);

    /**
     * Gets currently running child processes of provided parent PID, optionally
     * limited to the top "N" for a particular sorting order. If a positive limit is
     * specified, returns only that number of processes; zero will return all
     * processes. The order may be specified by the sort parameter, for example, to
     * return the top cpu or memory consuming processes; if the sort is
     * {@code null}, no order is guaranteed.
     *
     * @param parentPid
     *            A process ID
     * @param limit
     *            Max number of results to return, or 0 to return all results
     * @param sort
     *            If not null, determines sorting of results
     * @return A list of {@link oshi.software.os.OSProcess} objects representing the
     *         specified number (or all) of currently running child processes of the
     *         provided PID, sorted as specified. The list may contain null elements
     *         or processes with a state of {@link OSProcess.State#INVALID} if a
     *         process terminates during iteration.
     * @deprecated Use {@link #getChildProcesses(int, Predicate, Comparator, int)}
     *             with sorting constants from {@link ProcessSorting}.
     */
    @Deprecated
    default List<OSProcess> getChildProcesses(int parentPid, int limit, ProcessSort sort) {
        return getChildProcesses(parentPid, null, ProcessSorting.convertSortToComparator(sort), limit);
    }

    /**
     * Gets currently running child processes of provided parent PID, optionally
     * filtering, sorting, and limited to the top "N".
     *
     * @param parentPid
     *            The Process ID whose children to list.
     * @param filter
     *            An optional {@link Predicate} limiting the results to the
     *            specified filter. Some common predicates are available in
     *            {@link ProcessSorting}. May be {@code null} for no filtering.
     * @param sort
     *            An optional {@link Comparator} specifying the sorting order. Some
     *            common comparators are available in {@link ProcessSorting}. May be
     *            {@code null} for no sorting.
     * @param limit
     *            Max number of results to return, or 0 to return all results
     * @return A list of {@link oshi.software.os.OSProcess} objects representing the
     *         currently running child processes of the provided PID, optionally
     *         filtered, sorted, and limited to the specified number.
     *         <p>
     *         The list may contain processes with a state of
     *         {@link OSProcess.State#INVALID} if a process terminates during
     *         iteration.
     */
    List<OSProcess> getChildProcesses(int parentPid, Predicate<OSProcess> filter, Comparator<OSProcess> sort,
            int limit);

    /**
     * Gets currently running processes of provided parent PID's descendants,
     * including their children, the children's children, etc., optionally
     * filtering, sorting, and limited to the top "N".
     *
     * @param parentPid
     *            The Process ID whose children to list.
     * @param filter
     *            An optional {@link Predicate} limiting the results to the
     *            specified filter. Some common predicates are available in
     *            {@link ProcessSorting}. May be {@code null} for no filtering.
     * @param sort
     *            An optional {@link Comparator} specifying the sorting order. Some
     *            common comparators are available in {@link ProcessSorting}. May be
     *            {@code null} for no sorting.
     * @param limit
     *            Max number of results to return, or 0 to return all results
     * @return A list of {@link oshi.software.os.OSProcess} objects representing the
     *         currently running descendant processes of the provided PID,
     *         optionally filtered, sorted, and limited to the specified number.
     *         <p>
     *         The list may contain processes with a state of
     *         {@link OSProcess.State#INVALID} if a process terminates during
     *         iteration.
     */
    List<OSProcess> getDescendantProcesses(int parentPid, Predicate<OSProcess> filter, Comparator<OSProcess> sort,
            int limit);

    /**
     * Gets the current process ID
     *
     * @return the Process ID of the current process
     */
    int getProcessId();

    /**
     * Get the number of processes currently running
     *
     * @return The number of processes running
     */
    int getProcessCount();

    /**
     * Get the number of threads currently running
     *
     * @return The number of threads running
     */
    int getThreadCount();

    /**
     * Gets the bitness (32 or 64) of the operating system.
     *
     * @return The number of bits supported by the operating system.
     */
    int getBitness();

    /**
     * Get the System up time (time since boot).
     *
     * @return Number of seconds since boot.
     */
    long getSystemUptime();

    /**
     * Get Unix time of boot.
     *
     * @return The approximate time at which the system booted, in seconds since the
     *         Unix epoch.
     */
    long getSystemBootTime();

    /**
     * Determine whether the current process has elevated permissions such as sudo /
     * Administrator
     *
     * @return True if this process has elevated permissions
     */
    default boolean isElevated() {
        return 0 == ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("id -u"), -1);
    }

    /**
     * Instantiates a {@link oshi.software.os.NetworkParams} object.
     *
     * @return A {@link oshi.software.os.NetworkParams} object.
     */
    NetworkParams getNetworkParams();

    /**
     * Gets the all services on the system. The definition of what is a service is
     * platform-dependent.
     *
     * @return An array of {@link OSService} objects
     */
    default OSService[] getServices() {
        return new OSService[0];
    }

    /**
     * Gets currently logged in users.
     * <p>
     * On macOS, Linux, and Unix systems, the default implementation uses native
     * code (see {@code man getutxent}) that is not thread safe. OSHI's use of this
     * code is synchronized and may be used in a multi-threaded environment without
     * introducing any additional conflicts. Users should note, however, that other
     * operating system code may access the same native code.
     * <p>
     * The {@link oshi.driver.unix.Who#queryWho()} method produces similar output
     * parsing the output of the Posix-standard {@code who} command, and may
     * internally employ reentrant code on some platforms. Users may opt to use this
     * command-line variant by default using the {@code oshi.os.unix.whoCommand}
     * configuration property.
     *
     * @return A list of {@link oshi.software.os.OSSession} objects representing
     *         logged-in users
     */
    default List<OSSession> getSessions() {
        return Who.queryWho();
    }

    /**
     * Gets windows on the operating system's GUI desktop.
     * <p>
     * On Unix-like systems, reports X11 windows only, which may be limited to the
     * current display and will not report windows used by other window managers.
     * <p>
     * While not a guarantee, a best effort is made to return windows in
     * foreground-to-background order. This ordering may be used along with
     * {@link OSDesktopWindow#getOrder()} to (probably) determine the frontmost
     * window.
     *
     * @param visibleOnly
     *            Whether to restrict the list to only windows visible to the user.
     *            <p>
     *            This is a best effort attempt at a reasonable definition of
     *            visibility. Visible windows may be completely transparent.
     * @return A list of {@link oshi.software.os.OSDesktopWindow} objects
     *         representing the desktop windows.
     */
    default List<OSDesktopWindow> getDesktopWindows(boolean visibleOnly) {
        // Default X11 implementation for Unix-like operating systems.
        // Overridden on Windows and macOS
        return Xwininfo.queryXWindows(visibleOnly);
    }

    /**
     * A class representing the Operating System version details.
     */
    @Immutable
    class OSVersionInfo {
        private final String version;
        private final String codeName;
        private final String buildNumber;
        private final String versionStr;

        public OSVersionInfo(String version, String codeName, String buildNumber) {
            this.version = version;
            this.codeName = codeName;
            this.buildNumber = buildNumber;

            StringBuilder sb = new StringBuilder(getVersion() != null ? getVersion() : Constants.UNKNOWN);
            if (!Util.isBlank(getCodeName())) {
                sb.append(" (").append(getCodeName()).append(')');
            }
            if (!Util.isBlank(getBuildNumber())) {
                sb.append(" build ").append(getBuildNumber());
            }
            this.versionStr = sb.toString();
        }

        /**
         * Gets the operating system version.
         *
         * @return The version, if any. May be {@code null}.
         */
        public String getVersion() {
            return version;
        }

        /**
         * Gets the operating system codename.
         *
         * @return The code name, if any. May be {@code null}.
         */
        public String getCodeName() {
            return codeName;
        }

        /**
         * Gets the operating system build number.
         *
         * @return The build number, if any. May be {@code null}.
         */
        public String getBuildNumber() {
            return buildNumber;
        }

        @Override
        public String toString() {
            return this.versionStr;
        }
    }
}
