/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasEntry;

import java.util.Map;

import org.junit.jupiter.api.Test;

class NetBsdComputerSystemTest {

    @Test
    void testParseDmiArmVm() {
        // NetBSD 10.1 evbarm, a QEMU/KVM guest: no BIOS or baseboard table
        Map<String, String> dmi = NetBsdComputerSystem.parseDmi("""
                machdep.dmi.system-vendor = QEMU
                machdep.dmi.system-product = KVM Virtual Machine
                machdep.dmi.system-version = virt-9.2
                machdep.dmi.chassis-vendor = QEMU
                machdep.dmi.chassis-type = QEMU
                machdep.dmi.chassis-version = virt-9.2
                machdep.dmi.processor-vendor = QEMU
                machdep.dmi.processor-version = virt-9.2
                machdep.dmi.processor-frequency = 2000 MHz
                """.lines().toList());
        assertThat(dmi, aMapWithSize(9));
        assertThat(dmi, hasEntry("system-vendor", "QEMU"));
        assertThat(dmi, hasEntry("system-product", "KVM Virtual Machine"));
        assertThat(dmi, hasEntry("processor-frequency", "2000 MHz"));
    }

    @Test
    void testParseDmiIgnoresOtherLines() {
        Map<String, String> dmi = NetBsdComputerSystem.parseDmi("""
                machdep.dmi.bios-vendor = SeaBIOS
                machdep.dmi.bios-date = 20140401
                machdep.dmi.board-product = 440FX - 82441FX PMC [Natoma]
                sysctl: unrelated warning
                kern.hostname = example
                """.lines().toList());
        assertThat(dmi, aMapWithSize(3));
        assertThat(dmi, hasEntry("bios-date", "20140401"));
        assertThat(dmi, hasEntry("board-product", "440FX - 82441FX PMC [Natoma]"));
    }
}
