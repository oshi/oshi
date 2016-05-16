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
package oshi.software.common;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.json.NullAwareJsonObjectBuilder;
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

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

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

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String getPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     */
    public State getState() {
        return state;
    }

    /**
     * {@inheritDoc}
     */
    public int getProcessID() {
        return processID;
    }

    /**
     * {@inheritDoc}
     */
    public int getParentProcessID() {
        return parentProcessID;
    }

    /**
     * {@inheritDoc}
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * {@inheritDoc}
     */
    public int getPriority() {
        return priority;
    }

    /**
     * {@inheritDoc}
     */
    public long getVirtualSize() {
        return virtualSize;
    }

    /**
     * {@inheritDoc}
     */
    public long getResidentSetSize() {
        return residentSetSize;
    }

    /**
     * {@inheritDoc}
     */
    public long getKernelTime() {
        return kernelTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getUserTime() {
        return userTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("name", getName())
                .add("path", getPath()).add("state", getState().name()).add("processID", getProcessID())
                .add("parentProcessID", getParentProcessID()).add("threadCount", getThreadCount())
                .add("priority", getPriority()).add("virtualSize", getVirtualSize())
                .add("residentSetSize", getResidentSetSize()).add("kernelTime", getKernelTime())
                .add("userTime", getUserTime()).add("startTime", getStartTime()).build();
    }
}
