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
package oshi.software.os.windows;

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

import oshi.software.os.OperatingSystemVersion;

/**
 * Contains operating system version information. The information includes major
 * and minor version numbers, a build number, a platform identifier, and
 * descriptive text about the operating system.
 * 
 * @author dblock[at]dblock[dot]org
 */
public class WindowsOSVersionInfoEx implements OperatingSystemVersion {
    private static final Logger LOG = LoggerFactory.getLogger(WindowsOSVersionInfoEx.class);

    private OSVERSIONINFOEX _versionInfo;

    public WindowsOSVersionInfoEx() {
        this._versionInfo = new OSVERSIONINFOEX();
        if (!Kernel32.INSTANCE.GetVersionEx(this._versionInfo)) {
            LOG.error("Failed to Initialize OSVersionInfoEx. Error code: {}", Kernel32.INSTANCE.GetLastError());
            this._versionInfo = null;
        } else {
            LOG.debug("Initialized OSVersionInfoEx");
        }
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
    public int getMajor() {
        if (this._versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No version data available");
            return 0;
        }
        return this._versionInfo.dwMajorVersion.intValue();
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
    public int getMinor() {
        if (this._versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No version data available");
            return 0;
        }
        return this._versionInfo.dwMinorVersion.intValue();
    }

    /**
     * The build number of the operating system.
     * 
     * @return Build number.
     */
    public int getBuildNumber() {
        if (this._versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No build number available");
            return 0;
        }
        return this._versionInfo.dwBuildNumber.intValue();
    }

    /**
     * The operating system platform. This member can be VER_PLATFORM_WIN32_NT.
     * 
     * @return Platform ID.
     */
    public int getPlatformId() {
        if (this._versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No platform id available");
            return 0;
        }
        return this._versionInfo.dwPlatformId.intValue();
    }

    /**
     * String, such as "Service Pack 3", that indicates the latest Service Pack
     * installed on the system. If no Service Pack has been installed, the
     * string is empty.
     * 
     * @return Service pack.
     */
    public String getServicePack() {
        if (this._versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No service pack data available");
            return "";
        }
        return Native.toString(this._versionInfo.szCSDVersion);
    }

    /**
     * A bit mask that identifies the product suites available on the system.
     * 
     * @return Suite mask.
     */
    public int getSuiteMask() {
        if (this._versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No suite mask data available");
            return 0;
        }

        return this._versionInfo.wSuiteMask.intValue();
    }

    /**
     * Any additional information about the system.
     * 
     * @return Product type.
     */
    public byte getProductType() {
        if (this._versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No product type available");
            return 0;
        }
        return this._versionInfo.wProductType;
    }

    @Override
    public String toString() {
        if (this._versionInfo == null) {
            LOG.warn("OSVersionInfoEx not initialized. No version data available");
            return "Unknown";
        }

        String version = null;

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
                if ("Service Pack 6".equals(getServicePack())) {
                    if (Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE,
                            "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Hotfix\\Q246009")) {
                        return "NT4 SP6a";
                    }
                }

            } else {
                throw new RuntimeException("Unsupported Windows NT version: " + this._versionInfo.toString());
            }

            if (this._versionInfo.wServicePackMajor.intValue() > 0) {
                version = version + " SP" + this._versionInfo.wServicePackMajor.intValue();
            }

        } else if (getPlatformId() == WinNT.VER_PLATFORM_WIN32_WINDOWS) {
            if (getMajor() == 4 && getMinor() == 90) {
                version = "ME";
            } else if (getMajor() == 4 && getMinor() == 10) {
                if (this._versionInfo.szCSDVersion[1] == 'A') {
                    version = "98 SE";
                } else {
                    version = "98";
                }
            } else if (getMajor() == 4 && getMinor() == 0) {
                if (this._versionInfo.szCSDVersion[1] == 'C' || this._versionInfo.szCSDVersion[1] == 'B') {
                    version = "95 OSR2";
                } else {
                    version = "95";
                }
            } else {
                throw new RuntimeException("Unsupported Windows 9x version: " + this._versionInfo.toString());
            }
        } else {
            throw new RuntimeException("Unsupported Windows platform: " + this._versionInfo.toString());
        }

        return version;
    }

    public WindowsOSVersionInfoEx(OSVERSIONINFOEX versionInfo) {
        this._versionInfo = versionInfo;
    }
}
