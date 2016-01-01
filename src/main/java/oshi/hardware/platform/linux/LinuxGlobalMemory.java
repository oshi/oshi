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
package oshi.hardware.platform.linux;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;

import oshi.hardware.GlobalMemory;
import oshi.jna.platform.linux.Libc;
import oshi.jna.platform.linux.Libc.Sysinfo;
import oshi.util.FileUtil;

/**
 * Memory obtained by /proc/meminfo and sysinfo.totalram
 * 
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class LinuxGlobalMemory implements GlobalMemory {
    private static final Logger LOG = LoggerFactory.getLogger(LinuxGlobalMemory.class);

    private long totalMemory = 0;

    @Override
    public long getAvailable() {
        long availableMemory = 0;
        List<String> memInfo = null;
        try {
            memInfo = FileUtil.readFile("/proc/meminfo");
        } catch (IOException e) {
            LOG.error("Problem with /proc/meminfo: {}", e.getMessage());
            return availableMemory;
        }
        for (String checkLine : memInfo) {
            // If we have MemAvailable, it trumps all. See code in
            // https://git.kernel.org/cgit/linux/kernel/git/torvalds/
            // linux.git/commit/?id=34e431b0ae398fc54ea69ff85ec700722c9da773
            if (checkLine.startsWith("MemAvailable:")) {
                String[] memorySplit = checkLine.split("\\s+");
                availableMemory = parseMeminfo(memorySplit);
                break;
            } else
            // Otherwise we combine MemFree + Active(file), Inactive(file), and
            // Reclaimable. Free+cached is no longer appropriate. MemAvailable
            // reduces these values using watermarks to estimate when swapping
            // is prevented, omitted here for simplicity (assuming 0 swap).
            if (checkLine.startsWith("MemFree:")) {
                String[] memorySplit = checkLine.split("\\s+");
                availableMemory += parseMeminfo(memorySplit);
            } else if (checkLine.startsWith("Active(file):")) {
                String[] memorySplit = checkLine.split("\\s+");
                availableMemory += parseMeminfo(memorySplit);
            } else if (checkLine.startsWith("Inactive(file):")) {
                String[] memorySplit = checkLine.split("\\s+");
                availableMemory += parseMeminfo(memorySplit);
            } else if (checkLine.startsWith("SReclaimable:")) {
                String[] memorySplit = checkLine.split("\\s+");
                availableMemory += parseMeminfo(memorySplit);
            }
        }
        return availableMemory;
    }

    @Override
    public long getTotal() {
        if (this.totalMemory == 0) {
            // Try to get it from the libc sysinfo call
            try {
                Sysinfo info = new Sysinfo();
                if (0 != Libc.INSTANCE.sysinfo(info)) {
                    LOG.error("Failed to get total memory. Error code: " + Native.getLastError());
                } else {
                    this.totalMemory = info.totalram.longValue() * info.mem_unit;
                    return this.totalMemory;
                }
            } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
                LOG.warn("Failed to get total memory from sysinfo. Falling back /proc/meminfo MemTotal. {}",
                        e.getMessage());
            }
            // If still no success, populate from /proc/meminfo
            List<String> memInfo = null;
            try {
                memInfo = FileUtil.readFile("/proc/meminfo");
            } catch (IOException e) {
                LOG.error("Problem with /proc/meminfo: {}", e.getMessage());
                return 0;
            }
            for (String checkLine : memInfo) {
                if (checkLine.startsWith("MemTotal:")) {
                    String[] memorySplit = checkLine.split("\\s+");
                    this.totalMemory = parseMeminfo(memorySplit);
                    break;
                }
            }
        }
        return this.totalMemory;
    }

    private long parseMeminfo(String[] memorySplit) {
        if (memorySplit.length < 2) {
            return 0l;
        }
        long memory = Long.valueOf(memorySplit[1]);
        if (memorySplit.length > 2 && memorySplit[2].equals("kB")) {
            memory *= 1024;
        }
        return memory;
    }
}
