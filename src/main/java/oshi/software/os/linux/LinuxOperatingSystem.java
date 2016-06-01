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
package oshi.software.os.linux;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.software.common.AbstractOperatingSystem;
import oshi.util.FileUtil;

/**
 * Linux is a family of free operating systems most commonly used on personal
 * computers.
 *
 * @author alessandro[at]perucchi[dot]org
 */
public class LinuxOperatingSystem extends AbstractOperatingSystem {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LinuxOperatingSystem.class);

    private List<String> osRelease;

    public LinuxOperatingSystem() {
        this.manufacturer = "GNU/Linux";
        this.family = parseFamily(); // populates osRelease
        this.version = (osRelease != null) ? new LinuxOSVersionInfoEx(osRelease) : new LinuxOSVersionInfoEx();
    }

    private String parseFamily() {
        if (this.family == null) {
            String etcOsRelease = getReleaseFilename();
            this.osRelease = FileUtil.readFile(etcOsRelease);
            for (String line : this.osRelease) {
                String[] splittedLine = line.split("=");
                if ((splittedLine[0].equals("NAME") || splittedLine[0].equals("DISTRIB_ID"))
                        && splittedLine.length > 1) {
                    LOG.debug("OS Name/Distribution: {}", line);
                    // remove beginning and ending '"' characters, etc from
                    // NAME="Ubuntu"
                    this.family = splittedLine[1].replaceAll("^\"|\"$", "");
                    break;
                }
            }
            // If we couldn't parse the os-release or lsb-release formats,
            // see if we can parse first line of /etc/*-release
            if (this.family == null && this.osRelease.size() > 0) {
                // Get everything before " release" or " VERSION"
                String[] split = this.osRelease.get(0).split("release");
                if (split.length > 1) {
                    this.family = split[0].trim();
                } else {
                    split = this.osRelease.get(0).split("VERSION");
                    if (split.length > 1) {
                        this.family = split[0].trim();
                    }
                }
            }
            // If we've gotten to the end without matching, use the filename
            if (this.family == null) {
                this.family = filenameToFamily(etcOsRelease.replace("/etc/", "").replace("release", "")
                        .replace("version", "").replace("-", "").replace("_", ""));
            }
        }
        return this.family;

    }

    protected static String getReleaseFilename() {
        // Check for existence of primary sources of info:
        if (new File("/etc/os-release").exists()) {
            return "/etc/os-release";
        }
        if (new File("/etc/lsb-release").exists()) {
            return "/etc/lsb-release";
        }
        // Look for any /etc/*-release, *-version, and variants
        File etc = new File("/etc");
        File[] files = etc.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("-release") || name.endsWith("-version") || name.endsWith("_release")
                        || name.endsWith("_version");
            }
        });
        if (files != null && files.length > 0) {
            return files[0].getPath();
        }
        if (new File("/etc/release").exists()) {
            return "/etc/release";
        }
        // If all else fails, try this
        return "/etc/issue";
    }

    /**
     * Converts a portion of a filename (e.g. the 'redhat' in
     * /etc/redhat-release) to a mixed case string representing the family
     * (e.g., Red Hat)
     * 
     * @param name
     *            Stripped version of filename after removing /etc and -release
     * @return Mixed case family
     */
    private static String filenameToFamily(String name) {
        switch (name) {
        // Handle known special cases
        case "":
            return "Solaris";
        case "blackcat":
            return "Black Cat";
        case "e-smith":
            return "SME Server";
        case "eos":
            return "FreeEOS";
        case "hlfs":
            return "HLFS";
        case "linuxppc":
            return "Linux-PPC";
        case "mklinux":
            return "MkLinux";
        case "nld":
            return "Novell Linux Desktop";
        case "pld":
            return "PLD";
        case "redhat":
            return "Red Hat";
        case "novell":
        case "sles":
            return "SuSE";
        case "synoinfo":
            return "Synology";
        case "tinysofa":
            return "Tiny Sofa";
        case "turbolinux":
            return "TurboLinux";
        case "ultrapenguin":
            return "UltraPenguin";
        case "va":
            return "VA-Linux";
        case "yellowdog":
            return "Yellow Dog";
        // /etc/issue will end up here:
        case "issue":
            return "Unknown";
        // If not a special case just capitalize first letter
        default:
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }
}
