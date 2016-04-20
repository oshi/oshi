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
 * enrico[dot]bianchi[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.linux;

import java.io.IOException;
import java.util.List;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.GlobalMemory;
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.util.FileUtil;

/**
 * Memory obtained by /proc/meminfo and sysinfo.totalram
 *
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class LinuxGlobalMemory implements GlobalMemory {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxGlobalMemory.class);

    // Values read from /proc/meminfo used for other calculations
    private long memTotal = 0;
    private long memFree = 0;
    private long memAvailable = 0;
    private long activeFile = 0;
    private long inactiveFile = 0;
    private long sReclaimable = 0;
    private long swapTotal = 0;
    private long swapFree = 0;

    private long lastUpdate = 0;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

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
    private void updateMeminfo() {
        long now = System.currentTimeMillis();
        if (now - this.lastUpdate > 100) {
            List<String> memInfo = null;
            try {
                memInfo = FileUtil.readFile("/proc/meminfo");
            } catch (IOException e) {
                LOG.error("Problem with /proc/meminfo: {}", e.getMessage());
                return;
            }
            for (String checkLine : memInfo) {
                String[] memorySplit = checkLine.split("\\s+");
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
            this.lastUpdate = now;
        }
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
        long memory = Long.valueOf(memorySplit[1]);
        if (memorySplit.length > 2 && memorySplit[2].equals("kB")) {
            memory *= 1024;
        }
        return memory;
    }

    @Override
    public long getAvailable() {
        updateMeminfo();
        // If we have MemAvailable, it trumps all. Otherwise we combine MemFree,
        // Active(file), Inactive(file), and Reclaimable.
        return this.memAvailable > 0 ? this.memAvailable
                : this.memFree + this.activeFile + this.inactiveFile + this.sReclaimable;
    }

    @Override
    public long getTotal() {
        if (this.memTotal == 0) {
            updateMeminfo();
        }
        return this.memTotal;
    }

    @Override
    public long getSwapUsed() {
        updateMeminfo();
        return this.swapTotal - this.swapFree;
    }

    @Override
    public long getSwapTotal() {
        updateMeminfo();
        return this.swapTotal;
    }

    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("available", getAvailable())
                .add("total", getTotal()).add("swapTotal", getSwapTotal()).add("swapUsed", getSwapUsed()).build();
    }
}
