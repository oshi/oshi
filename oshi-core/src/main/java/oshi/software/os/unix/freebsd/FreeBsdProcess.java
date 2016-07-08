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
package oshi.software.os.unix.freebsd;

import oshi.software.common.AbstractProcess;
import oshi.software.os.OSProcess;

/**
 * A process is an instance of a computer program that is being executed. It
 * contains the program code and its current activity. Depending on the
 * operating system (OS), a process may be made up of multiple threads of
 * execution that execute instructions concurrently.
 * 
 * @author widdis[at]gmail[dot]com
 */
public class FreeBsdProcess extends AbstractProcess {

    private static final long serialVersionUID = 1L;

    public FreeBsdProcess(String name, String path, char state, int processID, int parentProcessID, int threadCount,
            int priority, long virtualSize, long residentSetSize, long elapsedTime, long systemTime, long processTime,
            long now) {
        this.name = name;
        this.path = path;
        switch (state) {
        case 'R':
            this.state = OSProcess.State.RUNNING;
            break;
        case 'I':
        case 'S':
            this.state = OSProcess.State.SLEEPING;
            break;
        case 'D':
        case 'L':
        case 'W':
            this.state = OSProcess.State.WAITING;
            break;
        case 'Z':
            this.state = OSProcess.State.ZOMBIE;
            break;
        case 'T':
            this.state = OSProcess.State.STOPPED;
            break;
        default:
            this.state = OSProcess.State.OTHER;
            break;
        }
        this.processID = processID;
        this.parentProcessID = parentProcessID;
        this.threadCount = threadCount;
        this.priority = priority;
        // These are in KB, multiply
        this.virtualSize = virtualSize * 1024L;
        this.residentSetSize = residentSetSize * 1024L;
        this.kernelTime = systemTime;
        this.userTime = processTime - kernelTime;
        // Avoid divide by zero for processes up less than a second
        this.upTime = elapsedTime < 1L ? 1000L : elapsedTime * 1000;
        this.startTime = now - upTime;
    }
}
