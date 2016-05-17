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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util.platform.linux;

import java.util.List;

import oshi.util.FileUtil;

/**
 * Provides access to some /proc filesystem info on Linux
 * 
 * @author widdis[at]gmail[dot]com
 */
public class ProcUtil {
    /**
     * Parses the first value in /proc/uptime for seconds since boot
     * 
     * @return Seconds since boot
     */
    public static float getSystemUptimeFromProc() {
        List<String> procUptime = FileUtil.readFile("/proc/uptime");
        if (procUptime.size() > 0) {
            String[] split = procUptime.get(0).split("\\s+");
            if (split.length > 0) {
                try {
                    return Float.parseFloat(split[0]);
                } catch (NumberFormatException nfe) {
                    return 0f;
                }
            }
        }
        return 0f;
    }
}