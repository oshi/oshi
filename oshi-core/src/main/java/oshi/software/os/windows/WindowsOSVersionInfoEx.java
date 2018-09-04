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
package oshi.software.os.windows;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;

import oshi.jna.platform.windows.WbemcliUtil.WmiQuery;
import oshi.jna.platform.windows.WbemcliUtil.WmiResult;
import oshi.software.common.AbstractOSVersionInfoEx;
import oshi.util.ParseUtil;
import oshi.util.StringUtil;
import oshi.util.platform.windows.WmiUtil;

public class WindowsOSVersionInfoEx extends AbstractOSVersionInfoEx {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOSVersionInfoEx.class);

    enum OSVersionProperty {
        VERSION, PRODUCTTYPE, BUILDNUMBER, CSDVERSION, SUITEMASK;
    }

    public WindowsOSVersionInfoEx() {
        init();
    }

    private void init() {
        // Populate a key-value map from WMI
        WmiQuery<OSVersionProperty> osVersionQuery = new WmiQuery<>("Win32_OperatingSystem", OSVersionProperty.class);
        WmiResult<OSVersionProperty> versionInfo = WmiUtil.queryWMI(osVersionQuery);
        if (versionInfo.getResultCount() < 1) {
            LOG.warn("No version data available.");
            setVersion(System.getProperty("os.version"));
            setCodeName("");
            setBuildNumber("");
        } else {
            // Guaranteed that versionInfo is not null and lists non-empty
            // before calling the parse*() methods
            int suiteMask = WmiUtil.getUint32(versionInfo, OSVersionProperty.SUITEMASK, 0);
            setVersion(parseVersion(versionInfo, suiteMask));
            setCodeName(parseCodeName(suiteMask));
            setBuildNumber(WmiUtil.getString(versionInfo, OSVersionProperty.BUILDNUMBER, 0));
            LOG.debug("Initialized OSVersionInfoEx");
        }
    }

    /**
     * Gets the operating system version
     * 
     * @param suiteMask
     *
     * @return Version
     */
    private String parseVersion(WmiResult<OSVersionProperty> versionInfo, int suiteMask) {

        // Initialize a default, sane value
        String version = System.getProperty("os.version");

        // Version is major.minor.build. Parse the version string for
        // major/minor and get the build number separately
        String[] verSplit = (WmiUtil.getString(versionInfo, OSVersionProperty.VERSION, 0)).split("\\D");
        int major = verSplit.length > 0 ? ParseUtil.parseIntOrDefault(verSplit[0], 0) : 0;
        int minor = verSplit.length > 1 ? ParseUtil.parseIntOrDefault(verSplit[1], 0) : 0;

        // see
        // http://msdn.microsoft.com/en-us/library/windows/desktop/ms724833%28v=vs.85%29.aspx
        boolean ntWorkstation = WmiUtil.getUint32(versionInfo, OSVersionProperty.PRODUCTTYPE,
                0) == WinNT.VER_NT_WORKSTATION;
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
                if ((suiteMask & 0x00008000) != 0) {// VER_SUITE_WH_SERVER
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

        String sp = WmiUtil.getString(versionInfo, OSVersionProperty.CSDVERSION, 0);
        if (!sp.isEmpty() && !"unknown".equals(sp)) {
            version = version + " " + sp.replace("Service Pack ", "SP");
        }

        return version;
    }

    /**
     * Gets suites available on the system and return as a codename
     * 
     * @param suiteMask
     *
     * @return Suites
     */
    private String parseCodeName(int suiteMask) {
        List<String> suites = new ArrayList<>();
        if ((suiteMask & 0x00000002) != 0) {
            suites.add("Enterprise");
        }
        if ((suiteMask & 0x00000004) != 0) {
            suites.add("BackOffice");
        }
        if ((suiteMask & 0x00000008) != 0) {
            suites.add("Communication Server");
        }
        if ((suiteMask & 0x00000080) != 0) {
            suites.add("Datacenter");
        }
        if ((suiteMask & 0x00000200) != 0) {
            suites.add("Home");
        }
        if ((suiteMask & 0x00000400) != 0) {
            suites.add("Web Server");
        }
        if ((suiteMask & 0x00002000) != 0) {
            suites.add("Storage Server");
        }
        if ((suiteMask & 0x00004000) != 0) {
            suites.add("Compute Cluster");
        }
        // 0x8000, Home Server, is included in main version name
        return StringUtil.join(",", suites);
    }
}
