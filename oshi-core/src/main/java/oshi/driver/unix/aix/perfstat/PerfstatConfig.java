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
package oshi.driver.unix.aix.perfstat;

import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.aix.Perfstat;
import oshi.jna.platform.unix.aix.Perfstat.perfstat_partition_config_t;

/**
 * Utility to query partition config
 */
@ThreadSafe
public final class PerfstatConfig {

    private static final Perfstat PERF = Perfstat.INSTANCE;

    private PerfstatConfig() {
    }

    /**
     * Queries perfstat_partition_config for config
     *
     * @return usage statistics
     */
    public static perfstat_partition_config_t queryConfig() {
        perfstat_partition_config_t config = new perfstat_partition_config_t();
        int ret = PERF.perfstat_partition_config(null, config, config.size(), 1);
        if (ret > 0) {
            return config;
        }
        return new perfstat_partition_config_t();
    }

    public static void main(String[] args) {
        perfstat_partition_config_t config = queryConfig();
        System.out.println("prt:" + Native.toString(config.partitionname));
        System.out.println("nod:" + Native.toString(config.nodename));
        System.out.println("Fam:" + Native.toString(config.processorFamily));
        System.out.println("Mod:" + Native.toString(config.processorModel));
        System.out.println("Mid:" + Native.toString(config.machineID));
        System.out.println(" OS:" + Native.toString(config.OSName));
        System.out.println("ver:" + Native.toString(config.OSVersion));
        System.out.println("bld:" + Native.toString(config.OSBuild));
        System.out.println(config.toString());
    }
}
