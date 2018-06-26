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
package oshi.util.platform.linux;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

import oshi.util.FileUtil;

/**
 * Provides access to some /proc filesystem info on Linux
 *
 * @author widdis[at]gmail[dot]com
 */
public class ProcUtil {
    private static final Pattern DIGITS = Pattern.compile("\\d+"); // NOSONAR-squid:S1068

    private ProcUtil() {
    }

    /**
     * Parses the first value in /proc/uptime for seconds since boot
     *
     * @return Seconds since boot
     */
    public static float getSystemUptimeFromProc() {
        String uptime = FileUtil.getStringFromFile("/proc/uptime");
        int spaceIndex = uptime.indexOf(' ');
        try {
            if (spaceIndex < 0) {
                // No space, just parse
                return Float.parseFloat(uptime);
            } else {
                return Float.parseFloat(uptime.substring(0, spaceIndex));
            }
        } catch (NumberFormatException nfe) {
            return 0f;
        }
    }

    /**
     * Gets an array of files in the /proc directory with only numeric digit
     * filenames, corresponding to processes
     *
     * @return An array of File objects for the process files
     */
    public static File[] getPidFiles() {
        File procdir = new File("/proc");
        File[] pids = procdir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return DIGITS.matcher(file.getName()).matches();
            }
        });
        return pids != null ? pids : new File[0];
    }
}