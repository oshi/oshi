/*
 * MIT License
 *
 * Copyright (c) 2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_cpu_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_cpu_total_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_disk_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_memory_total_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_netinterface_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_partition_config_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_process_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_protocol_t;

@EnabledOnOs(OS.AIX)
class PerfstatTest {
    @Test
    void testQueryConfig() {
        perfstat_partition_config_t config = PerfstatConfig.queryConfig();
        assertThat("Should have at least one logical CPU", config.lcpus, greaterThan(0));
    }

    @Test
    void testQueryCpu() {
        perfstat_cpu_t[] cpus = PerfstatCpu.queryCpu();
        assertThat("Should have at least one CPU", cpus.length, greaterThan(0));
        for (perfstat_cpu_t cpu : cpus) {
            assertThat("Should have at least one idle tick", cpu.idle, greaterThan(0L));
        }

        perfstat_cpu_total_t total = PerfstatCpu.queryCpuTotal();
        assertThat("Should have at least one idle tick", total.idle, greaterThan(0L));

        long affinity = PerfstatCpu.queryCpuAffinityMask();
        assertThat("Should have at least one CPU", affinity, greaterThan(0L));
    }

    @Test
    void testQueryDiskStats() {
        perfstat_disk_t[] disks = PerfstatDisk.queryDiskStats();
        assertThat("Should have at least one disk", disks.length, greaterThan(0));
        for (perfstat_disk_t disk : disks) {
            assertThat("Should have a nonzero disk capacity", disk.size, greaterThan(0L));
        }
    }

    @Test
    void testQueryMemory() {
        perfstat_memory_total_t mem = PerfstatMemory.queryMemoryTotal();
        assertThat("Should have nonzero memory", mem.real_total, greaterThan(0L));
    }

    @Test
    void testQueryNetInterface() {
        perfstat_netinterface_t[] nets = PerfstatNetInterface.queryNetInterfaces();
        assertThat("Should have at least one interface", nets.length, greaterThan(0));
        for (perfstat_netinterface_t net : nets) {
            assertThat("Should have a nonempty name", Native.toString(net.name), not(emptyString()));
        }
    }

    @Test
    void testQueryProcesses() {
        perfstat_process_t[] procs = PerfstatProcess.queryProcesses();
        assertThat("Should have at least one process", procs.length, greaterThan(0));
        for (perfstat_process_t proc : procs) {
            assertThat("Should have at least one thread", proc.num_threads, greaterThan(0L));
        }
    }

    @Test
    void testQueryProtocol() {
        perfstat_protocol_t[] protos = PerfstatProtocol.queryProtocols();
        assertThat("Should have at least one protocol", protos.length, greaterThan(0));
        List<String> validProtos = Arrays.asList("ip", "ipv6", "icmp", "icmpv6", "udp", "tcp", "rpc", "nfs", "nfsv2",
                "nfsv3", "nfsv4");
        for (perfstat_protocol_t proto : protos) {
            String protoName = Native.toString(proto.name);
            assertTrue(validProtos.contains(protoName), "Protocol must be in defined list of names");
        }
    }
}
