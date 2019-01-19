/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os.windows;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.software.common.AbstractOSVersionInfoEx;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiQueryHandler;
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
        WmiResult<OSVersionProperty> versionInfo = WmiQueryHandler.createInstance().queryWMI(osVersionQuery);
        if (versionInfo.getResultCount() < 1) {
            handleNoVersionInfo();
        } else {
            // Guaranteed that versionInfo is not null and lists non-empty
            // before calling the parse*() methods
            int suiteMask = WmiUtil.getUint32(versionInfo, OSVersionProperty.SUITEMASK, 0);
            setBuildNumber(WmiUtil.getString(versionInfo, OSVersionProperty.BUILDNUMBER, 0));
            setVersion(parseVersion(versionInfo, suiteMask));
            setCodeName(parseCodeName(suiteMask));
            LOG.debug("Initialized OSVersionInfoEx");
        }
    }

    protected void handleNoVersionInfo() {
        LOG.warn("No version data available.");
        setVersion(System.getProperty("os.version"));
        setCodeName("");
        setBuildNumber("");
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
        String[] verSplit = WmiUtil.getString(versionInfo, OSVersionProperty.VERSION, 0).split("\\D");
        int major = verSplit.length > 0 ? ParseUtil.parseIntOrDefault(verSplit[0], 0) : 0;
        int minor = verSplit.length > 1 ? ParseUtil.parseIntOrDefault(verSplit[1], 0) : 0;

        // see
        // http://msdn.microsoft.com/en-us/library/windows/desktop/ms724833%28v=vs.85%29.aspx
        boolean ntWorkstation = WmiUtil.getUint32(versionInfo, OSVersionProperty.PRODUCTTYPE,
                0) == WinNT.VER_NT_WORKSTATION;
        switch (major) {
        case 10:
            if (minor == 0) {
                if (ntWorkstation) {
                    version = "10";
                } else {
                    // Build numbers greater than 17762 is Server 2019 for OS
                    // Version 10.0
                    version = (ParseUtil.parseLongOrDefault(getBuildNumber(), 0L) > 17762) ? "Server 2019"
                            : "Server 2016";
                }
            }
            break;
        case 6:
            if (minor == 3) {
                version = ntWorkstation ? "8.1" : "Server 2012 R2";
            } else if (minor == 2) {
                version = ntWorkstation ? "8" : "Server 2012";
            } else if (minor == 1) {
                version = ntWorkstation ? "7" : "Server 2008 R2";
            } else if (minor == 0) {
                version = ntWorkstation ? "Vista" : "Server 2008";
            }
            break;
        case 5:
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
            break;
        default:
            break;
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
        return String.join(",", suites);
    }
}
