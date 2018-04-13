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
package oshi.software.os.unix.solaris;

import oshi.software.common.AbstractOSVersionInfoEx;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

public class SolarisOSVersionInfoEx extends AbstractOSVersionInfoEx {

    private static final long serialVersionUID = 1L;

    public SolarisOSVersionInfoEx() {
        // TODO use sysinfo() instead of commandline
        String versionInfo = ExecutingCommand.getFirstAnswer("uname -rv");
        String[] split = ParseUtil.whitespaces.split(versionInfo);
        setVersion(split[0]);
        if (split.length > 1) {
            setBuildNumber(split[1]);
        }
        setCodeName("Solaris");
    }
}
