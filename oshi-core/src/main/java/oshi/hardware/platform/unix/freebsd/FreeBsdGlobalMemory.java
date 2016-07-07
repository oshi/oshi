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
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.unix.freebsd;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Memory obtained by /proc/meminfo and sysinfo.totalram
 *
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class FreeBsdGlobalMemory extends AbstractGlobalMemory {

    private static final long serialVersionUID = 1L;

    private static final long PAGESIZE = ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("pagesize"),
            4096L);

    private static final Pattern SWAPINFO = Pattern.compile(".+\\s(\\d+)K\\s+(\\d+)K$");

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateMeminfo() {
        // TODO: Replace kstat command line with native kstat()
        List<String> memInfo = ExecutingCommand.runNative("kstat -n system_pages");
        if (memInfo.isEmpty()) {
            return;
        }
        for (String line : memInfo) {
            String[] splitLine = line.trim().split("\\s+");
            if (splitLine.length < 2) {
                break;
            }
            switch (splitLine[0]) {
            case "availrmem":
                this.memAvailable = ParseUtil.parseLongOrDefault(splitLine[1], 0L) * PAGESIZE;
                break;
            case "physmem":
                this.memTotal = ParseUtil.parseLongOrDefault(splitLine[1], 0L) * PAGESIZE;
                break;
            default:
                // Do nothing
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateSwap() {
        List<String> swapInfo = ExecutingCommand.runNative("swap -lk");
        if (swapInfo.isEmpty()) {
            return;
        }
        for (String line : swapInfo) {
            Matcher m = SWAPINFO.matcher(line);
            if (m.matches()) {
                this.swapTotal = ParseUtil.parseLongOrDefault(m.group(1), 0L) << 10;
                this.swapUsed = swapTotal - (ParseUtil.parseLongOrDefault(m.group(2), 0L) << 10);
                break;
            }
        }
    }
}
