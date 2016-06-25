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

import java.util.ArrayList;
import java.util.List;

import oshi.software.os.FileSystem;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemVersion;

public abstract class AbstractOperatingSystem implements OperatingSystem {

    private static final long serialVersionUID = 1L;

    protected String manufacturer;
    protected String family;
    protected OperatingSystemVersion version;

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
    public String getFamily() {
        return this.family;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        return this.manufacturer;
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract FileSystem getFileSystem();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract int getProcessCount();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract int getThreadCount();

    /**
     * Sorts an array of processes using the specified sorting, returning an
     * array with the top limit results if positive.
     * 
     * @param processes
     *            The array to sort
     * @param limit
     *            The number of results to return if positive; if zero returns
     *            all results
     * @param sort
     *            The sorting to use
     * @return An array of size limit (if positive) or of all processes, sorted
     *         as specified
     */
    protected List<OSProcess> processSort(List<OSProcess> processes, int limit, ProcessSort sort) {
        // TODO Sort processes here.
        // Nonpositive limit means return all
        if (limit <= 0) {
            limit = Integer.MAX_VALUE;
        }
        List<OSProcess> procs = new ArrayList<>();
        for (int i = 0; i < limit && i < processes.size(); i++) {
            procs.add(processes.get(i));
        }
        return procs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getManufacturer());
        sb.append(" ");
        sb.append(getFamily());
        sb.append(" ");
        sb.append(getVersion().toString());
        return sb.toString();
    }
}
