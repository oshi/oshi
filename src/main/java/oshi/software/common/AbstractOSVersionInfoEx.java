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
import oshi.software.os.OperatingSystemVersion;

/**
 * Contains operating system version information. The information includes major
 * and minor version numbers, a build number, a platform identifier, and
 * descriptive text about the operating system.
 */
public class AbstractOSVersionInfoEx implements OperatingSystemVersion {

    protected String version;

    protected String codeName;

    protected String versionStr;

    protected String buildNumber;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

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

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("version", getVersion())
                .add("codeName", getCodeName()).add("build", getBuildNumber()).build();
    }

    @Override
    public String toString() {
        if (this.versionStr == null) {
            StringBuilder sb = new StringBuilder(getVersion());
            if (getCodeName().length() > 0) {
                sb.append(" (").append(getCodeName()).append(")");
            }
            if (getBuildNumber().length() > 0) {
                sb.append(" build ").append(getBuildNumber());
            }
            this.versionStr = sb.toString();
        }
        return this.versionStr;
    }
}
