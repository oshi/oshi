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
package oshi.json.software.os.impl;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.software.os.OSProcess;

public class OSProcessImpl implements OSProcess {

    private static final long serialVersionUID = 1L;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.software.os.OSProcess osProcess;

    public OSProcessImpl(oshi.software.os.OSProcess osProcess) {
        this.osProcess = osProcess;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.osProcess.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() {
        return this.osProcess.getPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public State getState() {
        return this.osProcess.getState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessID() {
        return this.osProcess.getProcessID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getParentProcessID() {
        return this.osProcess.getParentProcessID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThreadCount() {
        return this.osProcess.getThreadCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        return this.osProcess.getPriority();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getVirtualSize() {
        return this.osProcess.getVirtualSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getResidentSetSize() {
        return this.osProcess.getResidentSetSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getKernelTime() {
        return this.osProcess.getKernelTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUserTime() {
        return this.osProcess.getUserTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUpTime() {
        return this.osProcess.getUpTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getStartTime() {
        return this.osProcess.getStartTime();
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
                .add("userTime", getUserTime()).add("upTime", getUpTime()).add("startTime", getStartTime()).build();
    }

}
