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
package oshi.software.os.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.software.common.AbstractOSVersionInfoEx;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtil;

public class MacOSVersionInfoEx extends AbstractOSVersionInfoEx {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MacOSVersionInfoEx.class);

    private int osxVersionNumber = -1;

    public MacOSVersionInfoEx() {
        setVersion(System.getProperty("os.version"));
        setCodeName(parseCodeName());
        setBuildNumber(SysctlUtil.sysctl("kern.osversion", ""));
    }

    public int getOsxVersionNumber() {
        return this.osxVersionNumber;
    }

    private String parseCodeName() {
        if (ParseUtil.getFirstIntValue(getVersion()) == 10) {
            this.osxVersionNumber = ParseUtil.getNthIntValue(getVersion(), 2);
            switch (this.osxVersionNumber) {
            // MacOS
            case 14:
                return "Mojave";
            case 13:
                return "High Sierra";
            case 12:
                return "Sierra";
            // OS X
            case 11:
                return "El Capitan";
            case 10:
                return "Yosemite";
            case 9:
                return "Mavericks";
            case 8:
                return "Mountain Lion";
            case 7:
                return "Lion";
            case 6:
                return "Snow Leopard";
            case 5:
                return "Leopard";
            case 4:
                return "Tiger";
            case 3:
                return "Panther";
            case 2:
                return "Jaguar";
            case 1:
                return "Puma";
            case 0:
                return "Cheetah";
            // Not OS X
            default:
            }
        }
        LOG.warn("Unable to parse version {} to a codename.", getVersion());
        return "";
    }
}
