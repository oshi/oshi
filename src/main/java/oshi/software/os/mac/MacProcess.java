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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.mac;

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
public class MacProcess extends AbstractProcess {
    /*
     * OS X States:
     */
    private static final int SSLEEP = 1; // sleeping on high priority
    private static final int SWAIT = 2; // sleeping on low priority
    private static final int SRUN = 3; // running
    private static final int SIDL = 4; // intermediate state in process creation
    private static final int SZOMB = 5; // intermediate state in process
                                        // termination
    private static final int SSTOP = 6; // process being traced

    public MacProcess(String name, String path, int osXState, int processID, int parentProcessID, int threadCount,
            int priority, long virtualSize, long residentSetSize, long kernelTime, long userTime, long startTime) {
        this.name = name;
        this.path = path;
        switch (osXState) {
        case SSLEEP:
            this.state = OSProcess.State.SLEEPING;
            break;
        case SWAIT:
            this.state = OSProcess.State.WAITING;
            break;
        case SRUN:
            this.state = OSProcess.State.RUNNING;
            break;
        case SIDL:
            this.state = OSProcess.State.NEW;
            break;
        case SZOMB:
            this.state = OSProcess.State.ZOMBIE;
            break;
        case SSTOP:
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
        this.virtualSize = virtualSize;
        this.residentSetSize = residentSetSize;
        this.kernelTime = kernelTime;
        this.userTime = userTime;
        this.startTime = startTime;
    }

}
