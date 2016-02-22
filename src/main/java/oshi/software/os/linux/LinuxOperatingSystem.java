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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemVersion;

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

    @Override
    public String getFamily() {
        if (this._family == null) {
            try (final Scanner in = new Scanner(new FileReader("/etc/os-release"))) {
                in.useDelimiter("\n");
                while (in.hasNext()) {
                    String[] splittedLine = in.next().split("=");
                    if (splittedLine[0].equals("NAME")) {
                        // remove beginning and ending '"' characters, etc from
                        // NAME="Ubuntu"
                        this._family = splittedLine[1].replaceAll("^\"|\"$", "");
                        break;
                    }
                }
            } catch (FileNotFoundException e) {
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
            this._version = new LinuxOSVersionInfoEx();
        }
        return this._version;
    }

    @Override
    public JsonObject toJSON() {
        return jsonFactory.createObjectBuilder().add("manufacturer", getManufacturer()).add("family", getFamily())
                .add("version", getVersion().toJSON()).build();
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
