/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.openbsd;

import static oshi.ffm.unix.openbsd.OpenBsdLibcFunctions.CTL_VFS;
import static oshi.ffm.unix.openbsd.OpenBsdLibcFunctions.VFS_BCACHESTAT;
import static oshi.ffm.unix.openbsd.OpenBsdLibcFunctions.VFS_GENERIC;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.unix.openbsd.OpenBsdLibcFunctions;
import oshi.ffm.util.platform.unix.openbsd.OpenBsdSysctlUtilFFM;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.platform.unix.openbsd.OpenBsdVirtualMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
final class OpenBsdGlobalMemoryFFM extends oshi.hardware.common.platform.unix.openbsd.OpenBsdGlobalMemory {

    @Override
    protected long queryAvailable() {
        long free = 0L;
        long inactive = 0L;
        for (String line : ExecutingCommand.runNative("vmstat -s")) {
            if (line.endsWith("pages free")) {
                free = ParseUtil.getFirstIntValue(line);
            } else if (line.endsWith("pages inactive")) {
                inactive = ParseUtil.getFirstIntValue(line);
            }
        }
        long bufPages = 0L;
        int[] mib = { CTL_VFS, VFS_GENERIC, VFS_BCACHESTAT };
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(OpenBsdLibcFunctions.BCACHESTATS_LAYOUT);
            if (OpenBsdSysctlUtilFFM.sysctl(mib, seg)) {
                bufPages = OpenBsdLibcFunctions.bcachestatsNumbufpages(seg);
            }
        }
        return (bufPages + free + inactive) * getPageSize();
    }

    @Override
    protected long queryPhysMem() {
        return OpenBsdSysctlUtilFFM.sysctl("hw.physmem", 0L);
    }

    @Override
    protected long queryPageSize() {
        return OpenBsdSysctlUtilFFM.sysctl("hw.pagesize", 4096L);
    }

    @Override
    protected VirtualMemory createVirtualMemory() {
        return new OpenBsdVirtualMemory(this);
    }
}
