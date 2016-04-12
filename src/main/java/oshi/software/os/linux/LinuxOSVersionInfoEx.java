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
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.linux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.json.NullAwareJsonObjectBuilder;
import oshi.software.os.OperatingSystemVersion;
import oshi.util.FileUtil;

/**
 * Contains operating system version information. The information includes major
 * and minor version numbers, a build number, a platform identifier, and
 * descriptive text about the operating system.
 *
 * @author alessandro[at]perucchi[dot]org
 */
public class LinuxOSVersionInfoEx implements OperatingSystemVersion {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxOSVersionInfoEx.class);

    private String _version;

    private String _codeName;

    private String version;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    public LinuxOSVersionInfoEx() {
        String etcOsRelease = LinuxOperatingSystem.getReleaseFilename();
        List<String> osRelease;
        try {
            osRelease = FileUtil.readFile(etcOsRelease);
        } catch (IOException e) {
            LOG.trace("", e);
            osRelease = new ArrayList<String>();
        }
        init(osRelease);
    }

    protected LinuxOSVersionInfoEx(List<String> osRelease) {
        init(osRelease);
    }

    private void init(List<String> osRelease) {
        // For multiline files (/etc/lsb-release or /etc/os-release):
        for (String line : osRelease) {
            String[] splittedLine = line.split("=");
            if (splittedLine.length < 2) {
                continue;
            }
            if (splittedLine[0].equals("VERSION_ID") || splittedLine[0].equals("DISTRIB_RELEASE")) {
                // remove beginning and ending '"' characters, etc from
                // VERSION_ID="14.04"
                setVersion(splittedLine[1].replaceAll("^\"|\"$", ""));
            }
            if (splittedLine[0].equals("DISTRIB_CODENAME")) {
                setCodeName(splittedLine[1].replaceAll("^\"|\"$", ""));
            } else if (splittedLine[0].equals("VERSION")) {
                // remove beginning and ending '"' characters
                splittedLine[1] = splittedLine[1].replaceAll("^\"|\"$", "");
                // Check basically if the code is between parenthesis or
                // after the comma-space
                String[] split = splittedLine[1].split("[()]");
                if (split.length <= 1) {
                    // We are probably with Ubuntu, so need to get that part
                    // correctly.
                    split = splittedLine[1].split(", ");
                }
                if (split.length > 1) {
                    setCodeName(split[1]);
                } else {
                    setCodeName(splittedLine[1]);
                }
            }
        }
        // If we haven't found version or codename, parse first line of the file
        // e.g. "Red Hat Enterprise Linux AS release 3 (Taroon)"
        if (getVersion() == null && osRelease.size() > 0) {
            // After word "release" or "VERSION" but before parenthesis
            String vers = null;
            String[] split = osRelease.get(0).split("release");
            if (split.length > 1) {
                vers = split[1].trim();
            } else {
                split = osRelease.get(0).split("VERSION");
                if (split.length > 1) {
                    vers = split[1].trim();
                }
            }
            if (vers != null) {
                // Get part before parenthesis
                split = vers.split("\\(");
                if (split.length > 1) {
                    vers = split[0];
                }
                setVersion(vers.trim());
            }
        }
        if (getCodeName() == null && osRelease.size() > 0) {
            // Between parenthesis
            String name = osRelease.get(0).replaceAll(".*\\(|\\).*", "");
            if (name.length() > 0) {
                setCodeName(name);
            }
        }
    }

    /**
     * @return the _codeName
     */
    public String getCodeName() {
        return this._codeName;
    }

    /**
     * @return the _version
     */
    public String getVersion() {
        return this._version;
    }

    /**
     * @param value
     *            the _codeName to set
     */
    public void setCodeName(String value) {
        this._codeName = value;
    }

    /**
     * @param value
     *            the _version to set
     */
    public void setVersion(String value) {
        this._version = value;
    }

    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("version", getVersion())
                .add("codeName", getCodeName()).add("build", "").build();
    }

    @Override
    public String toString() {
        if (this.version == null) {
            this.version = getVersion() + " (" + getCodeName() + ")";
        }
        return this.version;
    }

}
