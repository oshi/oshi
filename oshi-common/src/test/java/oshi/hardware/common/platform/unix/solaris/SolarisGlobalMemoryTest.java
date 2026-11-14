/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.PhysicalMemory;

class SolarisGlobalMemoryTest {

    @Test
    void testParseSmbiosMemoryDevicesQemu() {
        // Captured unprivileged on a vmactions OpenIndiana VM
        List<String> smbios = """
                ID    SIZE TYPE
                4352  51   SMB_TYPE_MEMDEVICE (type 17) (memory device)

                  Manufacturer: QEMU
                  Location Tag: DIMM 0

                  Physical Memory Array: 4096
                  Memory Error Data: Not Supported
                  Total Width: Unknown
                  Data Width: Unknown
                  Size: 6442450944 bytes
                  Form Factor: 9 (DIMM)
                  Set: None
                  Rank: Unknown
                  Memory Type: 7 (RAM)
                  Flags: 0x2
                \tSMB_MDF_OTHER (other)
                  Speed: Unknown
                  Configured Speed: Unknown
                  Device Locator: DIMM 0
                  Bank Locator:\s
                  Minimum Voltage: Unknown
                  Maximum Voltage: Unknown
                  Configured Voltage: Unknown
                """.lines().toList();
        List<PhysicalMemory> modules = SolarisGlobalMemory.parseSmbiosMemoryDevices(smbios);
        assertThat(modules, hasSize(1));
        PhysicalMemory dimm = modules.get(0);
        assertThat(dimm.getBankLabel(), is("DIMM 0"));
        assertThat(dimm.getCapacity(), is(6_442_450_944L));
        assertThat(dimm.getManufacturer(), is("QEMU"));
        assertThat(dimm.getMemoryType(), is("RAM"));
        assertThat(dimm.getClockSpeed(), is(-1L));
        assertThat(dimm.getPartNumber(), is("unknown"));
        assertThat(dimm.getSerialNumber(), is("unknown"));
    }

    @Test
    void testParseSmbiosMemoryDevicesPopulatedAndEmptySlot() {
        // Constructed in the same layout, with the fields real modules carry and an empty slot
        List<String> smbios = """
                ID    SIZE TYPE
                40    92   SMB_TYPE_MEMDEVICE (type 17) (memory device)

                  Manufacturer: Samsung
                  Serial Number: 4A1B2C3D
                  Part Number: M393A2K43DB3-CWE
                  Size: 17179869184 bytes
                  Memory Type: 26 (DDR4)
                  Speed: 3200 MT/s
                  Device Locator: P1-DIMMA1
                  Bank Locator: P0_Node0_Channel0_Dimm0

                ID    SIZE TYPE
                41    40   SMB_TYPE_MEMDEVICE (type 17) (memory device)

                  Manufacturer: NO DIMM
                  Size: Not Installed
                  Device Locator: P1-DIMMA2
                """.lines().toList();
        List<PhysicalMemory> modules = SolarisGlobalMemory.parseSmbiosMemoryDevices(smbios);
        assertThat(modules, hasSize(1));
        PhysicalMemory dimm = modules.get(0);
        assertThat(dimm.getBankLabel(), is("P0_Node0_Channel0_Dimm0/P1-DIMMA1"));
        assertThat(dimm.getCapacity(), is(17_179_869_184L));
        assertThat(dimm.getMemoryType(), is("DDR4"));
        assertThat(dimm.getClockSpeed(), is(3_200_000_000L));
        assertThat(dimm.getPartNumber(), is("M393A2K43DB3-CWE"));
        assertThat(dimm.getSerialNumber(), is("4A1B2C3D"));
    }

    @Test
    void testParseSmbiosMemoryDevicesNone() {
        // SPARC has no SMBIOS table
        assertThat(SolarisGlobalMemory.parseSmbiosMemoryDevices(Collections.emptyList()), is(empty()));
    }
}
