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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.software.os.OperatingSystemVersion;

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

    public LinuxOSVersionInfoEx() {
        try (Scanner in = new Scanner(new FileReader("/etc/os-release"))) {
            in.useDelimiter("\n");
            while (in.hasNext()) {
                String[] splittedLine = in.next().split("=");
                if (splittedLine[0].equals("VERSION_ID")) {
                    // remove beginning and ending '"' characters, etc from
                    // VERSION_ID="14.04"
                    setVersion(splittedLine[1].replaceAll("^\"|\"$", ""));
                }
                if (splittedLine[0].equals("VERSION")) {
                    // remove beginning and ending '"' characters
                    splittedLine[1] = splittedLine[1].replaceAll("^\"|\"$", "");

                    // Check basically if the code is between parenthesis or
                    // after
                    // the comma-space

                    // Basically, until now, that seems to be the standard to
                    // use
                    // parenthesis for the codename.
                    String[] split = splittedLine[1].split("[()]");
                    if (split.length <= 1)
                        // We are probably with Ubuntu, so need to get that part
                        // correctly.
                        split = splittedLine[1].split(", ");

                    if (split.length > 1) {
                        setCodeName(split[1]);
                    } else {
                        setCodeName(splittedLine[1]);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            LOG.trace("", e);
            return;
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
    public String toString() {
        if (this.version == null) {
            this.version = getVersion() + " (" + getCodeName() + ")";
        }
        return this.version;
    }

}
