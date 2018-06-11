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
package oshi.hardware.platform.linux;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native; // NOSONAR

import oshi.hardware.common.AbstractGlobalMemory;
import oshi.jna.platform.linux.Libc;
import oshi.jna.platform.linux.Libc.Sysinfo;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * Memory obtained by /proc/meminfo and sysinfo.totalram
 *
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class LinuxGlobalMemory extends AbstractGlobalMemory {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LinuxGlobalMemory.class);

    // Values read from /proc/meminfo used for other calculations
    private long memFree = 0;
    private long activeFile = 0;
    private long inactiveFile = 0;
    private long sReclaimable = 0;
    private long swapFree = 0;

    private long lastUpdate = 0;

    public LinuxGlobalMemory() {
        try {
            Sysinfo info = new Sysinfo();
            if (0 == Libc.INSTANCE.sysinfo(info)) {
                this.pageSize = info.mem_unit;
            } else {
                LOG.error("Failed to get sysinfo. Error code: {}", Native.getLastError());
            }
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            LOG.error("Failed to get mem_unit from sysinfo. {}", e);
        }
    }

    /**
     * Updates instance variables from reading /proc/meminfo no more frequently
     * than every 100ms. While most of the information is available in the
     * sysinfo structure, the most accurate calculation of MemAvailable is only
     * available from reading this pseudo-file. The maintainers of the Linux
     * Kernel have indicated this location will be kept up to date if the
     * calculation changes: see
     * https://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?
     * id=34e431b0ae398fc54ea69ff85ec700722c9da773
     *
     * Internally, reading /proc/meminfo is faster than sysinfo because it only
     * spends time populating the memory components of the sysinfo structure.
     */
    @Override
    protected void updateMeminfo() {
        long now = System.currentTimeMillis();
        if (now - this.lastUpdate > 100) {
            List<String> memInfo = FileUtil.readFile("/proc/meminfo");
            boolean found = false;
            for (String checkLine : memInfo) {
                String[] memorySplit = ParseUtil.whitespaces.split(checkLine);
                if (memorySplit.length > 1) {
                    switch (memorySplit[0]) {
                    case "MemTotal:":
                        this.memTotal = parseMeminfo(memorySplit);
                        break;
                    case "MemFree:":
                        this.memFree = parseMeminfo(memorySplit);
                        break;
                    case "MemAvailable:":
                        this.memAvailable = parseMeminfo(memorySplit);
                        found = true;
                        break;
                    case "Active(file):":
                        this.activeFile = parseMeminfo(memorySplit);
                        break;
                    case "Inactive(file):":
                        this.inactiveFile = parseMeminfo(memorySplit);
                        break;
                    case "SReclaimable:":
                        this.sReclaimable = parseMeminfo(memorySplit);
                        break;
                    case "SwapTotal:":
                        this.swapTotal = parseMeminfo(memorySplit);
                        break;
                    case "SwapFree:":
                        this.swapFree = parseMeminfo(memorySplit);
                        break;
                    default:
                        // do nothing with other lines
                        break;
                    }
                }
            }
            this.swapUsed = this.swapTotal - this.swapFree;
            // If no MemAvailable, calculate from other fields
            if (!found) {
                this.memAvailable = this.memFree + this.activeFile + this.inactiveFile + this.sReclaimable;
            }

            memInfo = FileUtil.readFile("/proc/vmstat");
            for (String checkLine : memInfo) {
                String[] memorySplit = ParseUtil.whitespaces.split(checkLine);
                if (memorySplit.length > 1) {
                    switch (memorySplit[0]) {
                    case "pgpgin":
                        swapPagesIn = ParseUtil.parseLongOrDefault(memorySplit[1], 0L);
                        break;
                    case "pgpgout":
                        swapPagesOut = ParseUtil.parseLongOrDefault(memorySplit[1], 0L);
                        break;
                    default:
                        // do nothing with other lines
                        break;
                    }
                }
            }

            this.lastUpdate = now;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateSwap() {
        updateMeminfo();
    }

    /**
     * Parses lines from the display of /proc/meminfo
     *
     * @param memorySplit
     *            Array of Strings representing the 3 columns of /proc/meminfo
     * @return value, multiplied by 1024 if kB is specified
     */
    private long parseMeminfo(String[] memorySplit) {
        if (memorySplit.length < 2) {
            return 0l;
        }
        long memory = ParseUtil.parseLongOrDefault(memorySplit[1], 0L);
        if (memorySplit.length > 2 && "kB".equals(memorySplit[2])) {
            memory *= 1024;
        }
        return memory;
    }
}
