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
