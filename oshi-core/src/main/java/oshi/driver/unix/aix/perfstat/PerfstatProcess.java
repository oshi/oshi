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
package oshi.driver.unix.aix.perfstat;

import java.util.Arrays;

import com.sun.jna.platform.unix.aix.Perfstat; // NOSONAR squid:S1191
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_id_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_process_t;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility to query performance stats for processes
 */
@ThreadSafe
public final class PerfstatProcess {

    private static final Perfstat PERF = Perfstat.INSTANCE;

    private PerfstatProcess() {
    }

    /**
     * Queries perfstat_process for per-process usage statistics
     *
     * @return an array of usage statistics
     */
    public static perfstat_process_t[] queryProcesses() {
        perfstat_process_t process = new perfstat_process_t();
        // With null, null, ..., 0, returns total # of elements
        int procCount = PERF.perfstat_process(null, null, process.size(), 0);
        if (procCount > 0) {
            perfstat_process_t[] proct = (perfstat_process_t[]) process.toArray(procCount);
            perfstat_id_t firstprocess = new perfstat_id_t(); // name is ""
            int ret = PERF.perfstat_process(firstprocess, proct, process.size(), procCount);
            if (ret > 0) {
                return Arrays.copyOf(proct, ret);
            }
        }
        return new perfstat_process_t[0];
    }
}
