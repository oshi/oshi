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
package oshi.software.common;

import oshi.software.os.OperatingSystemVersion;

/**
 * Contains operating system version information. The information includes major
 * and minor version numbers, a build number, a platform identifier, and
 * descriptive text about the operating system.
 */
public class AbstractOSVersionInfoEx implements OperatingSystemVersion {

    private static final long serialVersionUID = 1L;

    protected String version;

    protected String codeName;

    protected String versionStr;

    protected String buildNumber;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        return this.version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCodeName() {
        return this.codeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCodeName(String codeName) {
        this.codeName = codeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBuildNumber() {
        return this.buildNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    @Override
    public String toString() {
        if (this.versionStr == null) {
            StringBuilder sb = new StringBuilder(getVersion() != null ? getVersion() : "Unknown");
            if (getCodeName().length() > 0) {
                sb.append(" (").append(getCodeName()).append(')');
            }
            if (getBuildNumber().length() > 0) {
                sb.append(" build ").append(getBuildNumber());
            }
            this.versionStr = sb.toString();
        }
        return this.versionStr;
    }
}
