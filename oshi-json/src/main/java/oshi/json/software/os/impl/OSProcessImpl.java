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

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.software.os.OSProcess;
import oshi.json.util.PropertiesUtil;
import oshi.software.os.OSProcess.State;

/**
 * Wrapper class to implement OSProcess interface with platform-specific objects
 */
public class OSProcessImpl extends AbstractOshiJsonObject implements OSProcess {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.software.os.OSProcess osProcess;

    /**
     * Creates a new platform-specific OSProcessor object wrapping the provided
     * argument
     *
     * @param osProcess
     *            a platform-specific OSProcessor object
     */
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
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.name")) {
            json.add("name", getName());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.path")) {
            json.add("path", getPath());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.state")) {
            json.add("state", getState().name());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.processID")) {
            json.add("processID", getProcessID());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.parentProcessID")) {
            json.add("parentProcessID", getParentProcessID());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.threadCount")) {
            json.add("threadCount", getThreadCount());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.priority")) {
            json.add("priority", getPriority());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.virtualSize")) {
            json.add("virtualSize", getVirtualSize());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.residentSetSize")) {
            json.add("residentSetSize", getResidentSetSize());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.kernelTime")) {
            json.add("kernelTime", getKernelTime());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.userTime")) {
            json.add("userTime", getUserTime());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.upTime")) {
            json.add("upTime", getUpTime());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes.startTime")) {
            json.add("startTime", getStartTime());
        }
        return json.build();
    }

}
