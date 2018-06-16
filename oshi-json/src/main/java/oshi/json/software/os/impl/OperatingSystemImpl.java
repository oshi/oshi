/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.json.software.os.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.software.os.FileSystem;
import oshi.json.software.os.NetworkParams;
import oshi.json.software.os.OSProcess;
import oshi.json.software.os.OperatingSystem;
import oshi.json.software.os.OperatingSystemVersion;
import oshi.json.util.PropertiesUtil;
import oshi.software.os.OperatingSystem.ProcessSort;

/**
 * Wrapper class to implement OperatingSystem interface with platform-specific
 * objects
 */
public class OperatingSystemImpl extends AbstractOshiJsonObject implements OperatingSystem {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.software.os.OperatingSystem os;

    private OperatingSystemVersion version;

    /**
     * Creates a new platform-specific OperatingSystem object wrapping the
     * provided argument
     *
     * @param operatingSystem
     *            a platform-specific OperatingSystem object
     */
    public OperatingSystemImpl(oshi.software.os.OperatingSystem operatingSystem) {
        this.os = operatingSystem;
        this.version = new OperatingSystemVersionImpl(this.os.getVersion());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFamily() {
        return this.os.getFamily();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        return this.os.getManufacturer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperatingSystemVersion getVersion() {
        return this.version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem() {
        return new FileSystemImpl(this.os.getFileSystem());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess[] getProcesses(int limit, ProcessSort sort) {
        oshi.software.os.OSProcess[] procs = this.os.getProcesses(limit, sort);
        OSProcess[] processes = new OSProcess[procs.length];
        for (int i = 0; i < procs.length; i++) {
            processes[i] = procs[i] == null ? null : new OSProcess(procs[i]);
        }
        return processes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess getProcess(int pid) {
        oshi.software.os.OSProcess proc = this.os.getProcess(pid);
        return proc == null ? null : new OSProcess(proc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OSProcess> getProcesses(Collection<Integer> pids) {
        List<oshi.software.os.OSProcess> procs = this.os.getProcesses(pids);
        List<OSProcess> processes = new ArrayList<>();
        for (oshi.software.os.OSProcess proc : procs) {
            processes.add(new OSProcess(proc));
        }
        return processes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess[] getChildProcesses(int parentPid, int limit, ProcessSort sort) {
        oshi.software.os.OSProcess[] procs = this.os.getChildProcesses(parentPid, limit, sort);
        OSProcess[] processes = new OSProcess[procs.length];
        for (int i = 0; i < procs.length; i++) {
            processes[i] = procs[i] == null ? null : new OSProcess(procs[i]);
        }
        return processes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessId() {
        return this.os.getProcessId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessCount() {
        return this.os.getProcessCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThreadCount() {
        return this.os.getThreadCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBitness() {
        return this.os.getBitness();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkParams getNetworkParams() {
        return new NetworkParamsImpl(this.os.getNetworkParams());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.manufacturer")) {
            json.add("manufacturer", getManufacturer());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.family")) {
            json.add("family", getFamily());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.version")) {
            json.add("version", getVersion().toJSON(properties));
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.fileSystem")) {
            json.add("fileSystem", getFileSystem().toJSON(properties));
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processID")) {
            json.add("processID", getProcessId());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processCount")) {
            json.add("processCount", getProcessCount());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.threadCount")) {
            json.add("threadCount", getThreadCount());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.bitness")) {
            json.add("bitness", getBitness());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.processes")) {
            JsonArrayBuilder processArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (OSProcess proc : getProcesses(
                    PropertiesUtil.getIntOrDefault(properties, "operatingSystem.processes.limit", 0),
                    PropertiesUtil.getEnum(properties, "operatingSystem.processes.sort", ProcessSort.class))) {
                processArrayBuilder.add(proc.toJSON(properties));
            }
            json.add("processes", processArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.networkParams")) {
            json.add("networkParams", getNetworkParams().toJSON(properties));
        }
        return json.build();
    }

    @Override
    public String toString() {
        return this.os.toString();
    }
}
