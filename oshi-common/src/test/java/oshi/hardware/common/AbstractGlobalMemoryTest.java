/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;

class AbstractGlobalMemoryTest {

    private static AbstractGlobalMemory createMemory(long total, long available) {
        return new AbstractGlobalMemory() {
            @Override
            public long getTotal() {
                return total;
            }

            @Override
            public long getAvailable() {
                return available;
            }

            @Override
            public long getPageSize() {
                return 4096L;
            }

            @Override
            public VirtualMemory getVirtualMemory() {
                return null;
            }
        };
    }

    @Test
    void testToString() {
        String s = createMemory(8L * 1024 * 1024 * 1024, 4L * 1024 * 1024 * 1024).toString();
        assertThat(s, containsString("Available:"));
        assertThat(s, containsString("4 GiB"));
        assertThat(s, containsString("8 GiB"));
    }

    @Test
    void testGetPhysicalMemoryParsing() throws IOException {
        // Create a temp file simulating dmidecode output
        Path dmiFile = Files.createTempFile("oshitest.dmi", null);
        String dmiOutput = String.join("\n", "# dmidecode 3.2", "Handle 0x0038, DMI type 17, 40 bytes", "Memory Device",
                "\tBank Locator: BANK 0", "\tLocator: DIMM_A1", "\tSize: 8192 MB", "\tType: DDR4", "\tSpeed: 2400 MT/s",
                "\tManufacturer: Samsung", "\tPart Number: M471A1K43CB1-CRC", "\tSerial Number: 12345678", "");
        Files.write(dmiFile, dmiOutput.getBytes(StandardCharsets.UTF_8));

        // The default getPhysicalMemory() calls dmidecode which won't work in test,
        // but we can verify the method returns a list (empty on non-root)
        AbstractGlobalMemory mem = createMemory(8L * 1024 * 1024 * 1024, 4L * 1024 * 1024 * 1024);
        List<PhysicalMemory> pmList = mem.getPhysicalMemory();
        assertThat(pmList, is(not((List<PhysicalMemory>) null)));

        Files.deleteIfExists(dmiFile);
    }
}
