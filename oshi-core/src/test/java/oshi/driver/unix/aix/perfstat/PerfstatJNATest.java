/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix.perfstat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.sun.jna.Native;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_cpu_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_cpu_total_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_disk_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_memory_total_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_netinterface_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_partition_config_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_protocol_t;

import oshi.driver.common.unix.aix.AixPerfstatProcess;

@EnabledOnOs(OS.AIX)
class PerfstatJNATest {
    @Test
    void testQueryConfig() {
        perfstat_partition_config_t config = PerfstatConfigJNA.queryConfig();
        assertThat("Should have at least one logical CPU", config.lcpus, greaterThan(0));
    }

    @Test
    void testQueryCpu() {
        perfstat_cpu_t[] cpus = PerfstatCpuJNA.queryCpu();
        assertThat("Should have at least one CPU", cpus.length, greaterThan(0));
        for (perfstat_cpu_t cpu : cpus) {
            assertThat("Should have at least one idle tick", cpu.idle, greaterThan(0L));
        }

        perfstat_cpu_total_t total = PerfstatCpuJNA.queryCpuTotal();
        assertThat("Should have at least one idle tick", total.idle, greaterThan(0L));

        long affinity = PerfstatCpuJNA.queryCpuAffinityMask();
        assertThat("Should have at least one CPU", affinity, greaterThan(0L));
    }

    @Test
    void testQueryDiskStats() {
        perfstat_disk_t[] disks = PerfstatDiskJNA.queryDiskStats();
        assertThat("Should have at least one disk", disks.length, greaterThan(0));
        for (perfstat_disk_t disk : disks) {
            // Virtual disks may give 0 capacity but also have 0 time
            if (disk.time == 0) {
                assertThat("Should have a nonnegative disk capacity", disk.size, greaterThanOrEqualTo(0L));
            } else {
                assertThat("Should have a nonzero disk capacity", disk.size, greaterThan(0L));
            }
        }
    }

    @Test
    void testQueryMemory() {
        perfstat_memory_total_t mem = PerfstatMemoryJNA.queryMemoryTotal();
        assertThat("Should have nonzero memory", mem.real_total, greaterThan(0L));
    }

    @Test
    void testQueryNetInterface() {
        perfstat_netinterface_t[] nets = PerfstatNetInterfaceJNA.queryNetInterfaces();
        assertThat("Should have at least one interface", nets.length, greaterThan(0));
        for (perfstat_netinterface_t net : nets) {
            assertThat("Should have a nonempty name", Native.toString(net.name), not(emptyString()));
        }
    }

    @Test
    void testQueryProcesses() {
        AixPerfstatProcess[] procs = PerfstatProcessJNA.queryProcesses();
        assertThat("Should have at least one process", procs.length, greaterThan(0));
        for (AixPerfstatProcess proc : procs) {
            assertThat("Should have at least one thread", proc.num_threads, greaterThan(0L));
        }
    }

    @Test
    void testQueryProtocol() {
        perfstat_protocol_t[] protos = PerfstatProtocolJNA.queryProtocols();
        assertThat("Should have at least one protocol", protos.length, greaterThan(0));
        List<String> validProtos = Arrays.asList("ip", "ipv6", "icmp", "icmpv6", "udp", "tcp", "rpc", "nfs", "nfsv2",
                "nfsv3", "nfsv4");
        for (perfstat_protocol_t proto : protos) {
            String protoName = Native.toString(proto.name);
            assertTrue(validProtos.contains(protoName), "Protocol must be in defined list of names");
        }
    }
}
