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
import java.util.List;
import java.util.regex.Pattern;

import oshi.hardware.CentralProcessor.TickType;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

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
    public static double getSystemUptimeSeconds() {
        String uptime = FileUtil.getStringFromFile("/proc/uptime");
        int spaceIndex = uptime.indexOf(' ');
        try {
            if (spaceIndex < 0) {
                // No space, error
                return 0d;
            } else {
                return Double.parseDouble(uptime.substring(0, spaceIndex));
            }
        } catch (NumberFormatException nfe) {
            return 0d;
        }
    }

    /**
     * Gets the CPU ticks array from /proc/stat
     *
     * @return Array of CPU ticks
     */
    public static long[] getSystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        // /proc/stat expected format
        // first line is overall user,nice,system,idle,iowait,irq, etc.
        // cpu 3357 0 4313 1362393 ...
        String tickStr;
        List<String> procStat = FileUtil.readFile("/proc/stat");
        if (!procStat.isEmpty()) {
            tickStr = procStat.get(0);
        } else {
            return ticks;
        }
        // Split the line. Note the first (0) element is "cpu" so remaining
        // elements are offset by 1 from the enum index
        String[] tickArr = ParseUtil.whitespaces.split(tickStr);
        if (tickArr.length <= TickType.IDLE.getIndex()) {
            // If ticks don't at least go user/nice/system/idle, abort
            return ticks;
        }
        // Note tickArr is offset by 1 because first element is "cpu"
        for (int i = 0; i < TickType.values().length; i++) {
            ticks[i] = ParseUtil.parseLongOrDefault(tickArr[i + 1], 0L);
        }
        // Ignore guest or guest_nice, they are included in user/nice
        return ticks;
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