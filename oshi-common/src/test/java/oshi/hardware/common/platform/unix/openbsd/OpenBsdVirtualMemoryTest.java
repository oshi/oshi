/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.openbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Triplet;

class OpenBsdVirtualMemoryTest {

    // Excerpt of vmstat -s; the format is "%11u <label>"
    private static final List<String> VMSTAT = """
                   4096 bytes per page
                 503785 pages managed
                1056291 swap pages
                   1234 swap pages in use
             3000000000 pagein operations
                  12345 forks
            """.lines().toList();

    // Excerpt of systat -ab -w 200 uvm: values are blank when zero, names are upper case when nonzero
    private static final List<String> UVM = """
             ===== Page Counters      ===== Stats Counters     ===== Fault Counters
            503785 NPAGES           2345678 FAULTS                   fltnoram
              4321 PERCPUCACHES        7890 PGSWAPIN               3 FLTANGET
                                       6543 PGSWAPOUT                fltanretry
             ===== Constants         987654 PDPAGEOUTS          1234 SWPGINUSE
              4096 PAGESIZE                 pdpending           1234 SWPGONLY
            """.lines().toList();

    @Test
    void testParseVmstat() {
        Triplet<Long, Long, Long> swap = OpenBsdVirtualMemory.parseVmstat(VMSTAT);
        assertThat(swap.getA(), is(1234L));
        assertThat(swap.getB(), is(1056291L));
        // Unsigned 32-bit counter above Integer.MAX_VALUE, and pages rather than bytes
        assertThat(swap.getC(), is(3_000_000_000L));
    }

    @Test
    void testParseUvmCounter() {
        assertThat(OpenBsdVirtualMemory.parseUvmCounter(UVM, "pdpageouts"), is(987654L));
        assertThat(OpenBsdVirtualMemory.parseUvmCounter(UVM, "pgswapout"), is(6543L));
        assertThat(OpenBsdVirtualMemory.parseUvmCounter(UVM, "faults"), is(2345678L));
        // Blank (zero) values, preceded by another counter's name
        assertThat(OpenBsdVirtualMemory.parseUvmCounter(UVM, "pdpending"), is(0L));
        assertThat(OpenBsdVirtualMemory.parseUvmCounter(UVM, "fltanretry"), is(0L));
        // Blank value at the start of a row
        assertThat(OpenBsdVirtualMemory.parseUvmCounter(List.of("pdpageouts swpginuse"), "pdpageouts"), is(0L));
        assertThat(OpenBsdVirtualMemory.parseUvmCounter(UVM, "missing"), is(0L));
    }
}
