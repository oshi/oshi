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
package oshi.hardware.platform.unix.freebsd;

import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * Memory obtained by /proc/meminfo and sysinfo.totalram
 *
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class FreeBsdGlobalMemory extends AbstractGlobalMemory {

    private static final long serialVersionUID = 1L;

    private static final long PAGESIZE = BsdSysctlUtil.sysctl("hw.pagesize", 4096);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateMeminfo() {
        if (this.memTotal == 0L) {
            this.memTotal = BsdSysctlUtil.sysctl("hw.physmem", 0L);
        }
        // get pages of available memory
        long inactive = BsdSysctlUtil.sysctl("vm.stats.vm.v_inactive_count", 0L);
        long cache = BsdSysctlUtil.sysctl("vm.stats.vm.v_cache_count", 0L);
        long free = BsdSysctlUtil.sysctl("vm.stats.vm.v_free_count", 0L);
        this.memAvailable = (inactive + cache + free) * PAGESIZE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateSwap() {
        this.swapTotal = BsdSysctlUtil.sysctl("vm.swap_total", 0L);
        String swapInfo = ExecutingCommand.getAnswerAt("swapinfo -k", 1);
        String[] split = ParseUtil.whitespaces.split(swapInfo);
        if (split.length < 5) {
            return;
        }
        this.swapUsed = ParseUtil.parseLongOrDefault(split[2], 0L) << 10;
    }
}
