/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.unix.openbsd;

import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.CTL_VFS;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.VFS_BCACHESTAT;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.VFS_GENERIC;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import com.sun.jna.Memory; // NOSONAR squid:S1191

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.jna.platform.unix.openbsd.OpenBsdLibc.Bcachestats;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;

/**
 * Memory obtained by sysctl vm.stats
 */
@ThreadSafe
final class OpenBsdGlobalMemory extends AbstractGlobalMemory {

    private final Supplier<Long> available = memoize(OpenBsdGlobalMemory::queryAvailable, defaultExpiration());

    private final Supplier<Long> total = memoize(OpenBsdGlobalMemory::queryPhysMem);

    private final Supplier<Long> pageSize = memoize(OpenBsdGlobalMemory::queryPageSize);

    private final Supplier<VirtualMemory> vm = memoize(this::createVirtualMemory);

    @Override
    public long getAvailable() {
        return available.get() * getPageSize();
    }

    @Override
    public long getTotal() {
        return total.get();
    }

    @Override
    public long getPageSize() {
        return pageSize.get();
    }

    @Override
    public VirtualMemory getVirtualMemory() {
        return vm.get();
    }

    private static long queryAvailable() {
        long free = 0L;
        long inactive = 0L;
        for (String line : ExecutingCommand.runNative("vmstat -s")) {
            if (line.endsWith("pages free")) {
                free = ParseUtil.getFirstIntValue(line);
            } else if (line.endsWith("pages inactive")) {
                inactive = ParseUtil.getFirstIntValue(line);
            }
        }
        int[] mib = new int[3];
        mib[0] = CTL_VFS;
        mib[1] = VFS_GENERIC;
        mib[2] = VFS_BCACHESTAT;
        Memory m = OpenBsdSysctlUtil.sysctl(mib);
        Bcachestats cache = new Bcachestats(m);
        return (cache.numbufpages + free + inactive);
    }

    private static long queryPhysMem() {
        return OpenBsdSysctlUtil.sysctl("hw.physmem", 0L);
    }

    private static long queryPageSize() {
        return OpenBsdSysctlUtil.sysctl("hw.pagesize", 4096L);
    }

    private VirtualMemory createVirtualMemory() {
        return new OpenBsdVirtualMemory(this);
    }
}
