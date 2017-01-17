/**
 * Oshi (https://github.com/dblock/oshi)
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
public abstract class AbstractProcess implements OSProcess {

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

    public AbstractProcess() {}

    public AbstractProcess(String name, String path, int processID, int parentProcessID, int threadCount,
                           int priority, long virtualSize, long residentSetSize, long kernelTime, long userTime,
                           long startTime, long upTime, long bytesRead, long bytesWritten) {
        this.name = name;
        this.path = path;
        this.processID = processID;
        this.parentProcessID = parentProcessID;
        this.threadCount = threadCount;
        this.priority = priority;
        this.virtualSize = virtualSize;
        this.residentSetSize = residentSetSize;
        this.kernelTime = kernelTime;
        this.userTime = userTime;
        this.startTime = startTime;
        this.upTime = upTime;
        this.bytesRead = bytesRead;
        this.bytesWritten = bytesWritten;
    }

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

    /**
     * {@inheritDoc}
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(State state) {
        this.state = state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProcessID(int processID) {
        this.processID = processID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParentProcessID(int parentProcessID) {
        this.parentProcessID = parentProcessID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVirtualSize(long virtualSize) {
        this.virtualSize = virtualSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResidentSetSize(long residentSetSize) {
        this.residentSetSize = residentSetSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKernelTime(long kernelTime) {
        this.kernelTime = kernelTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUserTime(long userTime) {
        this.userTime = userTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUpTime(long upTime) {
        this.upTime = upTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBytesRead(long bytesRead) {
        this.bytesRead = bytesRead;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBytesWritten(long bytesWritten) {
        this.bytesWritten = bytesWritten;
    }


}
