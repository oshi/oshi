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

import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemVersion;
import oshi.util.ParseUtil;

/**
 * Microsoft Windows is a family of proprietary operating systems most commonly
 * used on personal computers.
 * 
 * @author dblock[at]dblock[dot]org
 */
public class WindowsOperatingSystem implements OperatingSystem {

    private OperatingSystemVersion _version;

    @Override
    public OperatingSystemVersion getVersion() {
        if (this._version == null) {
            this._version = new WindowsOSVersionInfoEx();
        }
        return this._version;
    }

    @Override
    public String getFamily() {
        return "Windows";
    }

    @Override
    public String getManufacturer() {
        return "Microsoft";
    }

    @Override
    public String toJSON() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"manufacturer\":").append(ParseUtil.jsonQuote(getManufacturer())).append(",");
        sb.append("\"family\":").append(ParseUtil.jsonQuote(getFamily())).append(",");
        sb.append("\"version\":").append(getVersion().toJSON());
        return sb.append("}").toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getManufacturer());
        sb.append(" ");
        sb.append(getFamily());
        sb.append(" ");
        sb.append(getVersion().toString());
        return sb.toString();
    }
}
