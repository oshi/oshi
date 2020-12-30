/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.CTL_VM;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.VM_UVMEXP;
import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import com.sun.jna.Memory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractVirtualMemory;
import oshi.jna.platform.unix.openbsd.OpenBsdLibc.Uvmexp;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;
import oshi.util.tuples.Quartet;

/**
 * Memory obtained by swapinfo
 */
@ThreadSafe
final class OpenBsdVirtualMemory extends AbstractVirtualMemory {

    OpenBsdGlobalMemory global;

    private final Supplier<Quartet<Integer, Integer, Integer, Integer>> usedTotalPginPgout = memoize(
            OpenBsdVirtualMemory::querySwap, defaultExpiration());

    OpenBsdVirtualMemory(OpenBsdGlobalMemory freeBsdGlobalMemory) {
        this.global = freeBsdGlobalMemory;
    }

    @Override
    public long getSwapUsed() {
        return usedTotalPginPgout.get().getA() * global.getPageSize();
    }

    @Override
    public long getSwapTotal() {
        return usedTotalPginPgout.get().getB() * global.getPageSize();
    }

    @Override
    public long getVirtualMax() {
        return this.global.getTotal() + getSwapTotal();
    }

    @Override
    public long getVirtualInUse() {
        return this.global.getTotal() - this.global.getAvailable() + getSwapUsed();
    }

    @Override
    public long getSwapPagesIn() {
        return usedTotalPginPgout.get().getC() * global.getPageSize();
    }

    @Override
    public long getSwapPagesOut() {
        return usedTotalPginPgout.get().getD() * global.getPageSize();
    }

    private static Quartet<Integer, Integer, Integer, Integer> querySwap() {
        int[] mib = new int[2];
        mib[0] = CTL_VM;
        mib[1] = VM_UVMEXP;
        Memory m = OpenBsdSysctlUtil.sysctl(mib);
        Uvmexp uvm = new Uvmexp(m);

        return new Quartet<>(uvm.swpginuse, uvm.swpages, uvm.pgswapin, uvm.pgswapout);
    }
}
