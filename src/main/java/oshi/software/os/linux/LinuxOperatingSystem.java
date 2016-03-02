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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.json.NullAwareJsonObjectBuilder;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemVersion;
import oshi.util.FileUtil;

/**
 * Linux is a family of free operating systems most commonly used on personal
 * computers.
 *
 * @author alessandro[at]perucchi[dot]org
 */
public class LinuxOperatingSystem implements OperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxOperatingSystem.class);

    private OperatingSystemVersion _version;

    private String _family;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private List<String> osRelease;

    protected static String getReleaseFilename() {
        // Check for existence of primary sources of info:
        if ((new File("/etc/os-release")).exists()) {
            return "/etc/os-release";
        }
        if ((new File("/etc/lsb-release")).exists()) {
            return "/etc/lsb-release";
        }
        // Look for any /etc/*-release, *-version, and variants
        File etc = new File("/etc");
        File[] files = etc.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.endsWith("-release") || name.endsWith("-version") || name.endsWith("_version"));
            }
        });
        if (files.length > 0) {
            return files[0].getPath();
        }
        return "/etc/release";
    }

    @Override
    public String getFamily() {
        if (this._family == null) {
            String etcOsRelease = getReleaseFilename();
            try {
                this.osRelease = FileUtil.readFile(etcOsRelease);
                for (String line : this.osRelease) {
                    String[] splittedLine = line.split("=");
                    if ((splittedLine[0].equals("NAME") || splittedLine[0].equals("DISTRIB_ID"))
                            && splittedLine.length > 1) {
                        // remove beginning and ending '"' characters, etc from
                        // NAME="Ubuntu"
                        this._family = splittedLine[1].replaceAll("^\"|\"$", "");
                        break;
                    }
                }
                // If we've gotten to the end without matching, use the filename
                if (this._family == null) {
                    this._family = etcOsRelease.replace("/etc/", "").replace("release", "").replace("version", "")
                            .replace("-", "");
                }
            } catch (IOException e) {
                LOG.trace("", e);
                return "";
            }
        }
        return this._family;
    }

    @Override
    public String getManufacturer() {
        return "GNU/Linux";
    }

    @Override
    public OperatingSystemVersion getVersion() {
        if (this._version == null) {
            this._version = (osRelease != null) ? new LinuxOSVersionInfoEx(osRelease) : new LinuxOSVersionInfoEx();
        }
        return this._version;
    }

    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("manufacturer", getManufacturer())
                .add("family", getFamily()).add("version", getVersion().toJSON()).build();
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
