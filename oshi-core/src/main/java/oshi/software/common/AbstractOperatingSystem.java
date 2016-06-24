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

import oshi.software.os.FileSystem;
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
