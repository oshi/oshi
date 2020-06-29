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
package oshi.jna.platform.unix.aix;

import com.sun.jna.Native;

import oshi.jna.platform.unix.aix.Perfstat.perfstat_cpu_t;
import oshi.jna.platform.unix.aix.Perfstat.perfstat_id_t;

/**
 * This is temporary to allow testing of the methods without doing the whole
 * SystemInfoTest route
 */
public class PerfstatTest {

    private static final Perfstat PERF = Perfstat.INSTANCE;

    public static void main(String[] args) {
        perfstat_cpu_t cpu = new perfstat_cpu_t();
        // With null, null, ..., 0, returns total # of elements
        int cputotal = PERF.perfstat_cpu(null, null, cpu.size(), 0);
        System.out.println(cputotal + " cpu(s)");
        if (cputotal > 0) {
            perfstat_cpu_t[] statp = (perfstat_cpu_t[]) cpu.toArray(cputotal);
            perfstat_id_t firstcpu = new perfstat_id_t(); // name is ""
            int ret = PERF.perfstat_cpu(firstcpu, statp, cpu.size(), 1);
            for (int i = 0; i < ret; i++) {
                System.out.format("%s: U=%d, S=%d, I=%d%n", Native.toString(statp[i].name), statp[i].user, statp[i].sys,
                        statp[i].idle);
            }
        }
    }
}
