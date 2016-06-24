/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os;

import java.io.Serializable;

/**
 * An operating system (OS) is the software on a computer that manages the way
 * different programs use its hardware, and regulates the ways that a user
 * controls the computer.
 * 
 * @author dblock[at]dblock[dot]org
 */
public interface OperatingSystem extends Serializable {

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
     * Gets currently running processes
     * 
     * @return An array of {@link oshi.software.os.OSProcess} objects for
     *         currently running processes
     */
    OSProcess[] getProcesses();

    /**
     * Gets information on a currently running process
     * 
     * @param pid
     *            A process ID
     * @return An {@link oshi.software.os.OSProcess} object for the specified
     *         process id if it is running; null otherwise currently running
     *         processes
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
}
