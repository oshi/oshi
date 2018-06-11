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
package oshi.hardware.platform.unix.solaris;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; // NOSONAR

import oshi.hardware.common.AbstractGlobalMemory;
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

    private static final Pattern SWAPINFO = Pattern.compile(".+\\s(\\d+)K\\s+(\\d+)K$");

    public SolarisGlobalMemory() {
        this.pageSize = ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("pagesize"), 4096L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateMeminfo() {
        // Get first result
        Kstat ksp = KstatUtil.kstatLookup(null, -1, "system_pages");
        // Set values
        if (ksp != null && KstatUtil.kstatRead(ksp)) {
            this.memAvailable = KstatUtil.kstatDataLookupLong(ksp, "availrmem") * this.pageSize;
            this.memTotal = KstatUtil.kstatDataLookupLong(ksp, "physmem") * this.pageSize;
        }

        this.swapPagesIn = 0L;
        List<String> kstat = ExecutingCommand.runNative("kstat -p cpu_stat:::pgpgin");
        for (String s : kstat) {
            this.swapPagesIn += ParseUtil.parseLastLong(s, 0L);
        }

        this.swapPagesOut = 0L;
        kstat = ExecutingCommand.runNative("kstat -p cpu_stat:::pgpgout");
        for (String s : kstat) {
            this.swapPagesOut += ParseUtil.parseLastLong(s, 0L);
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
