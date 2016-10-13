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

    private static final long serialVersionUID = 2L;

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
    protected long bytesRead;
    protected long bytesWritten;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() {
        return this.path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public State getState() {
        return this.state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessID() {
        return this.processID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getParentProcessID() {
        return this.parentProcessID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThreadCount() {
        return this.threadCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        return this.priority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getVirtualSize() {
        return this.virtualSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getResidentSetSize() {
        return this.residentSetSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getKernelTime() {
        return this.kernelTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUserTime() {
        return this.userTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUpTime() {
        if (this.upTime < this.kernelTime + this.userTime) {
            return this.kernelTime + this.userTime;
        }
        return this.upTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getStartTime() {
        return this.startTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getBytesRead() {
        return bytesRead;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getBytesWritten() {
        return bytesWritten;
    }
}
