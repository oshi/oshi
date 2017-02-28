/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
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
package oshi.software.os.windows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;

import oshi.software.common.AbstractOSVersionInfoEx;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;

public class WindowsOSVersionInfoEx extends AbstractOSVersionInfoEx {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOSVersionInfoEx.class);

    private static final ValueType[] queryTypes = { ValueType.STRING, ValueType.UINT32, ValueType.STRING,
            ValueType.STRING, ValueType.UINT32 };

    private transient Map<String, List<Object>> versionInfo = new HashMap<>();

    public WindowsOSVersionInfoEx() {
        // Populate a key-value map from WMI
        this.versionInfo = WmiUtil.selectObjectsFrom(null, "Win32_OperatingSystem",
                "Version,ProductType,BuildNumber,CSDVersion,SuiteMask", null, queryTypes);
        if (this.versionInfo.get("Version").isEmpty()) {
            LOG.warn("No version data available.");
            setVersion(System.getProperty("os.version"));
            setCodeName("");
            setBuildNumber("");
        } else {
            // Guaranteed that versionInfo is not null and lists non-empty
            // before calling the parse*() methods
            setVersion(parseVersion());
            setCodeName(parseCodeName());
            setBuildNumber(parseBuildNumber());
            LOG.debug("Initialized OSVersionInfoEx");
        }
    }

    /**
     * Gets the operating system version
     *
     * @return Version
     */
    private String parseVersion() {

        // Initialize a default, sane value
        String version = System.getProperty("os.version");

        // Version is major.minor.build. Parse the version string for
        // major/minor and get the build number separately
        String[] verSplit = ((String) this.versionInfo.get("Version").get(0)).split("\\D");
        int major = verSplit.length > 0 ? ParseUtil.parseIntOrDefault(verSplit[0], 0) : 0;
        int minor = verSplit.length > 1 ? ParseUtil.parseIntOrDefault(verSplit[1], 0) : 0;

        // see
        // http://msdn.microsoft.com/en-us/library/windows/desktop/ms724833%28v=vs.85%29.aspx
        boolean ntWorkstation = (long) this.versionInfo.get("ProductType").get(0) == WinNT.VER_NT_WORKSTATION;
        if (major == 10) {
            if (minor == 0) {
                version = ntWorkstation ? "10" : "Server 2016";
            }
        } else if (major == 6) {
            if (minor == 3) {
                version = ntWorkstation ? "8.1" : "Server 2012 R2";
            } else if (minor == 2) {
                version = ntWorkstation ? "8" : "Server 2012";
            } else if (minor == 1) {
                version = ntWorkstation ? "7" : "Server 2008 R2";
            } else if (minor == 0) {
                version = ntWorkstation ? "Vista" : "Server 2008";
            }
        } else if (major == 5) {
            if (minor == 2) {
                if ((getSuiteMask() & 0x00008000) != 0) {// VER_SUITE_WH_SERVER
                    version = "Home Server";
                } else if (ntWorkstation) {
                    version = "XP"; // 64 bits
                } else {
                    version = User32.INSTANCE.GetSystemMetrics(WinUser.SM_SERVERR2) != 0 ? "Server 2003"
                            : "Server 2003 R2";
                }
            } else if (minor == 1) {
                version = "XP"; // 32 bits
            } else if (minor == 0) {
                version = "2000";
            }
        }

        String sp = (String) this.versionInfo.get("CSDVersion").get(0);
        if (!sp.isEmpty() && !"unknown".equals(sp)) {
            version = version + " " + sp.replace("Service Pack ", "SP");
        }

        return version;
    }

    /**
     * Gets suites available on the system and return as a codename
     *
     * @return Suites
     */
    private String parseCodeName() {
        List<String> suites = new ArrayList<>();
        int bitmask = getSuiteMask();
        if ((bitmask & 0x00000002) != 0) {
            suites.add("Enterprise");
        }
        if ((bitmask & 0x00000004) != 0) {
            suites.add("BackOffice");
        }
        if ((bitmask & 0x00000008) != 0) {
            suites.add("Communication Server");
        }
        if ((bitmask & 0x00000080) != 0) {
            suites.add("Datacenter");
        }
        if ((bitmask & 0x00000200) != 0) {
            suites.add("Home");
        }
        if ((bitmask & 0x00000400) != 0) {
            suites.add("Web Server");
        }
        if ((bitmask & 0x00002000) != 0) {
            suites.add("Storage Server");
        }
        if ((bitmask & 0x00004000) != 0) {
            suites.add("Compute Cluster");
        }
        // 0x8000, Home Server, is included in main version name
        String separator = "";
        StringBuilder sb = new StringBuilder();
        for (String s : suites) {
            sb.append(separator).append(s);
            separator = ",";
        }
        return sb.toString();
    }

    /**
     * A bit mask that identifies the product suites available on the system.
     *
     * @return Suite mask.
     */
    private int getSuiteMask() {
        // Although this object is 64 bits, it originates from
        // a UINT32 bit mask and can safely be cast directly
        // to int, which preserves the low 32 bits
        return (int) ((Long) this.versionInfo.get("SuiteMask").get(0)).longValue();
    }

    /**
     * Gets the build number
     *
     * @return A string representing the Build Number
     */
    private String parseBuildNumber() {
        return (String) this.versionInfo.get("BuildNumber").get(0);
    }

}
