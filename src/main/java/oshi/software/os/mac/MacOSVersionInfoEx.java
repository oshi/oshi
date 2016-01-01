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
package oshi.software.os.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.ptr.IntByReference;

import oshi.software.os.OperatingSystemVersion;

/**
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */

public class MacOSVersionInfoEx implements OperatingSystemVersion {
    private static final Logger LOG = LoggerFactory.getLogger(MacOSVersionInfoEx.class);

    private String _version;

    private String _codeName;

    private String _versionStr;

    private String _buildNumber;

    public MacOSVersionInfoEx() {
    }

    /**
     * @return the _version
     */
    public String getVersion() {
        if (this._version == null) {
            this._version = System.getProperty("os.version");
        }
        return this._version;
    }

    /**
     * @param version
     *            the version to set
     */
    public void setVersion(String version) {
        this._version = version;
    }

    /**
     * @return the _codeName
     */
    public String getCodeName() {
        if (this._codeName == null) {
            if (getVersion() != null) {
                String[] versionSplit = getVersion().split("\\.");
                if (versionSplit.length > 1 && versionSplit[0].equals("10")) {
                    switch (Integer.parseInt(versionSplit[1])) {
                    case 0:
                        this._codeName = "Cheetah";
                        break;
                    case 1:
                        this._codeName = "Puma";
                        break;
                    case 2:
                        this._codeName = "Jaguar";
                        break;
                    case 3:
                        this._codeName = "Panther";
                        break;
                    case 4:
                        this._codeName = "Tiger";
                        break;
                    case 5:
                        this._codeName = "Leopard";
                        break;
                    case 6:
                        this._codeName = "Snow Leopard";
                        break;
                    case 7:
                        this._codeName = "Lion";
                        break;
                    case 8:
                        this._codeName = "Mountain Lion";
                        break;
                    case 9:
                        this._codeName = "Mavericks";
                        break;
                    case 10:
                        this._codeName = "Yosemite";
                        break;
                    case 11:
                        this._codeName = "El Capitan";
                        break;
                    default:
                        this._codeName = "";
                    }

                } else
                    this._codeName = "";
            }
        }
        return this._codeName;
    }

    /**
     * @param codeName
     *            the codeName to set
     */
    public void setCodeName(String codeName) {
        this._codeName = codeName;
    }

    public String getBuildNumber() {
        if (this._buildNumber == null) {
            IntByReference size = new IntByReference();
            if (0 != SystemB.INSTANCE.sysctlbyname("kern.osversion", null, size, null, 0)) {
                LOG.error("Failed to get OS Version. Error code: " + Native.getLastError());
                return "";
            }
            Pointer p = new Memory(size.getValue() + 1);
            if (0 != SystemB.INSTANCE.sysctlbyname("kern.osversion", p, size, null, 0)) {
                LOG.error("Failed to get OS Version. Error code: " + Native.getLastError());
                return "";
            }
            this._buildNumber = p.getString(0);
        }
        return this._buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this._buildNumber = buildNumber;
    }

    @Override
    public String toString() {
        if (this._versionStr == null) {
            StringBuilder sb = new StringBuilder(getVersion());
            if (getCodeName().length() > 0) {
                sb.append(" (").append(getCodeName()).append(")");
            }
            sb.append(" build ").append(getBuildNumber());
            this._versionStr = sb.toString();
        }
        return this._versionStr;
    }
}
