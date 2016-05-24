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
package oshi.software.os.windows;

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
public class WindowsProcess extends AbstractProcess {
    /*
     * Windows Execution States:
     */
    private static final int UNKNOWN = 0;
    private static final int OTHER = 1;
    private static final int READY = 2;
    private static final int RUNNING = 3;
    private static final int BLOCKED = 4;
    private static final int SUSPENDED_BLOCKED = 5;
    private static final int SUSPENDED_READY = 6;
    private static final int TERMINATED = 7;
    private static final int STOPPED = 8;
    private static final int GROWING = 9;

    public WindowsProcess(String name, String path, int winState, int processID, int parentProcessID, int threadCount,
            int priority, long virtualSize, long residentSetSize, long kernelTime, long userTime, long startTime,
            long now) {
        this.name = name;
        this.path = path;
        switch (winState) {
        case READY:
        case SUSPENDED_READY:
            this.state = OSProcess.State.SLEEPING;
            break;
        case BLOCKED:
        case SUSPENDED_BLOCKED:
            this.state = OSProcess.State.WAITING;
            break;
        case RUNNING:
            this.state = OSProcess.State.RUNNING;
            break;
        case GROWING:
            this.state = OSProcess.State.NEW;
            break;
        case TERMINATED:
            this.state = OSProcess.State.ZOMBIE;
            break;
        case STOPPED:
            this.state = OSProcess.State.STOPPED;
            break;
        case UNKNOWN:
        case OTHER:
        default:
            this.state = OSProcess.State.OTHER;
            break;
        }
        this.processID = processID;
        this.parentProcessID = parentProcessID;
        this.threadCount = threadCount;
        this.priority = priority;
        this.virtualSize = virtualSize;
        this.residentSetSize = residentSetSize;
        this.kernelTime = kernelTime;
        this.userTime = userTime;
        this.startTime = startTime;
        this.upTime = now - startTime;
    }

}
