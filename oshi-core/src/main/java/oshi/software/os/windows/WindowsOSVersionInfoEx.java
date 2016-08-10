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
package oshi.software.os.windows;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.OSVERSIONINFOEX;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.WinUser;

import oshi.software.common.AbstractOSVersionInfoEx;

public class WindowsOSVersionInfoEx extends AbstractOSVersionInfoEx {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOSVersionInfoEx.class);

    private transient OSVERSIONINFOEX versionInfo = null;

    public WindowsOSVersionInfoEx() {
        this.versionInfo = new OSVERSIONINFOEX();
        if (!Kernel32.INSTANCE.GetVersionEx(this.versionInfo)) {
            LOG.error("Failed to Initialize OSVersionInfoEx. Error code: {}", Kernel32.INSTANCE.GetLastError());
        }
        init();
        LOG.debug("Initialized OSVersionInfoEx");

    }

    public WindowsOSVersionInfoEx(OSVERSIONINFOEX versionInfo) {
        this.versionInfo = versionInfo;
        init();
    }

    /**
     * Initialize class variables
     */
    private void init() {
        setVersion(parseVersion());
        setCodeName(parseCodeName());
        if (this.versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No build number available");
            setBuildNumber("");
            return;
        }
        setBuildNumber(this.versionInfo.dwBuildNumber.toString());
    }

    /**
     * Gets the operating system version
     *
     * @return Version
     */
    private String parseVersion() {
        if (this.versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No version data available");
            return System.getProperty("os.version");
        }

        String version;

        // see
        // http://msdn.microsoft.com/en-us/library/windows/desktop/ms724833%28v=vs.85%29.aspx
        if (getPlatformId() == WinNT.VER_PLATFORM_WIN32_NT) {
            boolean ntWorkstation = getProductType() == WinNT.VER_NT_WORKSTATION;
            if (getMajor() == 10 && getMinor() == 0 && ntWorkstation) {
                version = "10";
            } else if (getMajor() == 10 && getMinor() == 0 && !ntWorkstation) {
                version = "Server 2016";
            } else if (getMajor() == 6 && getMinor() == 3 && ntWorkstation) {
                version = "8.1";
            } else if (getMajor() == 6 && getMinor() == 3 && !ntWorkstation) {
                version = "Server 2012 R2";
            } else if (getMajor() == 6 && getMinor() == 2 && ntWorkstation) {
                version = "8";
            } else if (getMajor() == 6 && getMinor() == 2 && !ntWorkstation) {
                version = "Server 2012";
            } else if (getMajor() == 6 && getMinor() == 1 && ntWorkstation) {
                version = "7";
            } else if (getMajor() == 6 && getMinor() == 1 && !ntWorkstation) {
                version = "Server 2008 R2";
            } else if (getMajor() == 6 && getMinor() == 0 && !ntWorkstation) {
                version = "Server 2008";
            } else if (getMajor() == 6 && getMinor() == 0 && ntWorkstation) {
                version = "Vista";
            } else if (getMajor() == 5 && getMinor() == 2 && !ntWorkstation) {
                version = User32.INSTANCE.GetSystemMetrics(WinUser.SM_SERVERR2) != 0 ? "Server 2003" : "Server 2003 R2";
            } else if (getMajor() == 5 && getMinor() == 2 && ntWorkstation) {
                version = "XP"; // 64 bits
            } else if (getMajor() == 5 && getMinor() == 1) {
                version = "XP"; // 32 bits
            } else if (getMajor() == 5 && getMinor() == 0) {
                version = "2000";
            } else if (getMajor() == 4) {
                version = "NT 4";
                if ("Service Pack 6".equals(getServicePack())
                        && Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE,
                                "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Hotfix\\Q246009")) {
                    return "NT4 SP6a";
                }
            } else {
                throw new UnsupportedOperationException(
                        "Unsupported Windows NT version: " + this.versionInfo.toString());
            }

            if (this.versionInfo.wServicePackMajor.intValue() > 0) {
                version = version + " SP" + this.versionInfo.wServicePackMajor.intValue();
            }

        } else if (getPlatformId() == WinNT.VER_PLATFORM_WIN32_WINDOWS) {
            if (getMajor() == 4 && getMinor() == 90) {
                version = "ME";
            } else if (getMajor() == 4 && getMinor() == 10) {
                if (this.versionInfo.szCSDVersion[1] == 'A') {
                    version = "98 SE";
                } else {
                    version = "98";
                }
            } else if (getMajor() == 4 && getMinor() == 0) {
                if (this.versionInfo.szCSDVersion[1] == 'C' || this.versionInfo.szCSDVersion[1] == 'B') {
                    version = "95 OSR2";
                } else {
                    version = "95";
                }
            } else {
                throw new UnsupportedOperationException(
                        "Unsupported Windows 9x version: " + this.versionInfo.toString());
            }
        } else {
            throw new UnsupportedOperationException("Unsupported Windows platform: " + this.versionInfo.toString());
        }

        return version;
    }

    /**
     * The operating system platform. This member can be VER_PLATFORM_WIN32_NT.
     *
     * @return Platform ID.
     */
    private int getPlatformId() {
        if (this.versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No platform id available");
            return 0;
        }
        return this.versionInfo.dwPlatformId.intValue();
    }

    /**
     * Any additional information about the system.
     *
     * @return Product type.
     */
    private byte getProductType() {
        if (this.versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No product type available");
            return 0;
        }
        return this.versionInfo.wProductType;
    }

    /**
     * The major version number of the operating system.
     *
     * @return The major version within the following supported operating
     *         systems. Windows 8: 6.2 Windows Server 2012: 6.2 Windows 7: 6.1
     *         Windows Server 2008 R2: 6.1 Windows Server 2008: 6.0 Windows
     *         Vista: 6.0 Windows Server 2003 R2: 5.2 Windows Home Server: 5.2
     *         Windows Server 2003: 5.2 Windows XP Professional x64 Edition: 5.2
     *         Windows XP: 5.1 Windows 2000: 5.0
     */
    private int getMajor() {
        if (this.versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No version data available");
            return 0;
        }
        return this.versionInfo.dwMajorVersion.intValue();
    }

    /**
     * The minor version number of the operating system.
     *
     * @return The minor version within the following supported operating
     *         systems. Windows 8: 6.2 Windows Server 2012: 6.2 Windows 7: 6.1
     *         Windows Server 2008 R2: 6.1 Windows Server 2008: 6.0 Windows
     *         Vista: 6.0 Windows Server 2003 R2: 5.2 Windows Home Server: 5.2
     *         Windows Server 2003: 5.2 Windows XP Professional x64 Edition: 5.2
     *         Windows XP: 5.1 Windows 2000: 5.0
     */
    private int getMinor() {
        if (this.versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No version data available");
            return 0;
        }
        return this.versionInfo.dwMinorVersion.intValue();
    }

    /**
     * String, such as "Service Pack 3", that indicates the latest Service Pack
     * installed on the system. If no Service Pack has been installed, the
     * string is empty.
     *
     * @return Service pack.
     */
    private String getServicePack() {
        if (this.versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No service pack data available");
            return "";
        }
        return Native.toString(this.versionInfo.szCSDVersion);
    }

    /**
     * Gets suites available on the system and return as a codename
     *
     * @return Suites
     */
    private String parseCodeName() {
        List<String> suites = new ArrayList<>();
        int bitmask = getSuiteMask();
        if ((bitmask & 0x00000004) != 0) {
            suites.add("BackOffice");
        }
        if ((bitmask & 0x00000400) != 0) {
            suites.add("Web Edition");
        }
        if ((bitmask & 0x00004000) != 0) {
            suites.add("Compute Cluster");
        }
        if ((bitmask & 0x00000080) != 0) {
            suites.add("Datacenter");
        }
        if ((bitmask & 0x00000002) != 0) {
            suites.add("Enterprise");
        }
        if ((bitmask & 0x00000200) != 0) {
            suites.add("Home");
        }
        if ((bitmask & 0x00008000) != 0) {
            suites.add("Home Server");
        }
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
        if (this.versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No suite mask data available");
            return 0;
        }

        return this.versionInfo.wSuiteMask.intValue();
    }
}
