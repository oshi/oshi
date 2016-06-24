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
package oshi.software.common;

import oshi.software.os.OSProcess;

/**
 * A process is an instance of a computer program that is being executed. It
 * contains the program code and its current activity. Depending on the
 * operating system (OS), a process may be made up of multiple threads of
 * execution that execute instructions concurrently.
 * 
 * @author widdis[at]gmail[dot]com
 */
public class AbstractProcess implements OSProcess {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String path;
    protected State state;
    protected int processID;
    protected int parentProcessID;
    protected int threadCount;
    protected int priority;
    protected long virtualSize;
    protected long residentSetSize;
    protected long kernelTime;
    protected long userTime;
    protected long startTime;
    protected long upTime;

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    public String getPath() {
        return this.path;
    }

    /**
     * {@inheritDoc}
     */
    public State getState() {
        return this.state;
    }

    /**
     * {@inheritDoc}
     */
    public int getProcessID() {
        return this.processID;
    }

    /**
     * {@inheritDoc}
     */
    public int getParentProcessID() {
        return this.parentProcessID;
    }

    /**
     * {@inheritDoc}
     */
    public int getThreadCount() {
        return this.threadCount;
    }

    /**
     * {@inheritDoc}
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * {@inheritDoc}
     */
    public long getVirtualSize() {
        return this.virtualSize;
    }

    /**
     * {@inheritDoc}
     */
    public long getResidentSetSize() {
        return this.residentSetSize;
    }

    /**
     * {@inheritDoc}
     */
    public long getKernelTime() {
        return this.kernelTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getUserTime() {
        return this.userTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getUpTime() {
        if (this.upTime < this.kernelTime + this.userTime) {
            return this.kernelTime + this.userTime;
        }
        return this.upTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getStartTime() {
        return this.startTime;
    }
}
