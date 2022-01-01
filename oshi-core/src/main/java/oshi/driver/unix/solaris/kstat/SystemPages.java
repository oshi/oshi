/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.unix.solaris.kstat;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; // NOSONAR squid:s1191

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;
import oshi.util.tuples.Pair;

/**
 * Utility to query geom part list
 */
@ThreadSafe
public final class SystemPages {

    private SystemPages() {
    }

    /**
     * Queries the {@code system_pages} kstat and returns available and physical
     * memory
     *
     * @return A pair with the available and total memory, in pages. Mutiply by page
     *         size for bytes.
     */
    public static Pair<Long, Long> queryAvailableTotal() {
        long memAvailable = 0;
        long memTotal = 0;
        // Get first result
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = KstatChain.lookup(null, -1, "system_pages");
            // Set values
            if (ksp != null && KstatChain.read(ksp)) {
                memAvailable = KstatUtil.dataLookupLong(ksp, "availrmem"); // not a typo
                memTotal = KstatUtil.dataLookupLong(ksp, "physmem");
            }
        }
        return new Pair<>(memAvailable, memTotal);
    }
}
