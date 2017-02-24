/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
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
package oshi.hardware.platform.unix.solaris;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.hardware.common.AbstractGlobalMemory;
import oshi.jna.platform.unix.solaris.LibKstat.Kstat;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.solaris.KstatUtil;

/**
 * Memory obtained by /proc/meminfo and sysinfo.totalram
 *
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class SolarisGlobalMemory extends AbstractGlobalMemory {

    private static final long serialVersionUID = 1L;

    private static final long PAGESIZE = ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("pagesize"),
            4096L);

    private static final Pattern SWAPINFO = Pattern.compile(".+\\s(\\d+)K\\s+(\\d+)K$");

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateMeminfo() {
        // Get first result
        Kstat ksp = KstatUtil.kstatLookup(null, -1, "system_pages");
        // Set values
        if (ksp != null && KstatUtil.kstatRead(ksp)) {
            this.memAvailable = KstatUtil.kstatDataLookupLong(ksp, "availrmem") * PAGESIZE;
            this.memTotal = KstatUtil.kstatDataLookupLong(ksp, "physmem") * PAGESIZE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateSwap() {
        String swapInfo = ExecutingCommand.getAnswerAt("swap -lk", 1);
        Matcher m = SWAPINFO.matcher(swapInfo);
        if (m.matches()) {
            this.swapTotal = ParseUtil.parseLongOrDefault(m.group(1), 0L) << 10;
            this.swapUsed = this.swapTotal - (ParseUtil.parseLongOrDefault(m.group(2), 0L) << 10);
        }
    }
}
