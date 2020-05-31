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
package oshi.software.os;

import java.util.Collection;
import java.util.List;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.Constants;
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
     * Controls sorting of Process output
     */
    enum ProcessSort {
        CPU, MEMORY, OLDEST, NEWEST, PID, PARENTPID, NAME
    }

    /**
     * Operating system family.
     *
     * @return String.
     */
    String getFamily();

    /**
     * Manufacturer.
     *
     * @return String.
     */
    String getManufacturer();

    /**
     * Operating system version information.
     *
     * @return Version information.
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
     * @return An {@code UnmodifiableList} of {@link oshi.software.os.OSSession}
     *         objects representing logged-in users
     */
    List<OSSession> getSessions();

    /**
     * Gets currently running processes. No order is guaranteed.
     *
     * @return An {@code UnmodifiableList} of {@link oshi.software.os.OSProcess}
     *         objects for the specified number (or all) of currently running
     *         processes, sorted as specified. The list may contain null elements or
     *         processes with a state of {@link OSProcess.State#INVALID} if a
     *         process terminates during iteration.
     */
    List<OSProcess> getProcesses();

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
     * @return An {@code UnmodifiableList} of {@link oshi.software.os.OSProcess}
     *         objects for the specified number (or all) of currently running
     *         processes, sorted as specified. The list may contain null elements or
     *         processes with a state of {@link OSProcess.State#INVALID} if a
     *         process terminates during iteration.
     */
    List<OSProcess> getProcesses(int limit, ProcessSort sort);

    /**
     * Gets information on a {@link Collection} of currently running processes. This
     * has potentially improved performance vs. iterating individual processes.
     *
     * @param pids
     *            A collection of process IDs
     * @return An {@code UnmodifiableList} of {@link oshi.software.os.OSProcess}
     *         objects for the specified process ids if it is running
     */
    List<OSProcess> getProcesses(Collection<Integer> pids);

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
     * @return An {@code UnmodifiableList} of {@link oshi.software.os.OSProcess}
     *         objects representing the specified number (or all) of currently
     *         running child processes of the provided PID, sorted as specified. The
     *         list may contain null elements or processes with a state of
     *         {@link OSProcess.State#INVALID} if a process terminates during
     *         iteration.
     */
    List<OSProcess> getChildProcesses(int parentPid, int limit, ProcessSort sort);

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
    boolean isElevated();

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
    OSService[] getServices();

    /*
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
