/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.json.software.os;

import oshi.json.json.OshiJsonObject;
import oshi.software.os.OperatingSystem.ProcessSort;

/**
 * An operating system (OS) is the software on a computer that manages the way
 * different programs use its hardware, and regulates the ways that a user
 * controls the computer.
 *
 * @author dblock[at]dblock[dot]org
 */
public interface OperatingSystem extends OshiJsonObject {

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
     * Operating system version.
     *
     * @return Version.
     */
    OperatingSystemVersion getVersion();

    /**
     * Instantiates a {@link FileSystem} object.
     *
     * @return A {@link FileSystem} object.
     */
    FileSystem getFileSystem();

    /**
     * Gets currently running processes. If a positive limit is specified,
     * returns only that number of processes; zero will return all processes.
     * The order may be specified by the sort parameter, for example, to return
     * the top cpu or memory consuming processes; if null, no order is
     * guaranteed.
     *
     * In the JSON output, these parameters may be specified by
     * "operatingSystem.processes.limit" and "operatingSystem.processes.sort"
     * property values.
     *
     * @param limit
     *            Max number of results to return, or 0 to return all results
     * @param sort
     *            If not null, determines sorting of results
     * @return An array of {@link oshi.software.os.OSProcess} objects for the
     *         specified number (or all) of currently running processes, sorted
     *         as specified. The array may contain null elements if a process
     *         terminates during iteration.
     */
    OSProcess[] getProcesses(int limit, ProcessSort sort);

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
     * Instantiates a {@link NetworkParams} object.
     *
     * @return A {@link NetworkParams} object.
     */
    NetworkParams getNetworkParams();
}
