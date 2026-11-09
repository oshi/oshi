/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class DevicetreeTest {

    @Test
    @EnabledOnOs(OS.LINUX)
    void testQueryLiveDevicetree() {
        // Most hosts have no devicetree; on any host each value is absent or a non-blank string
        for (String value : new String[] { Devicetree.queryModel(), Devicetree.queryManufacturer(),
                Devicetree.querySerialNumber() }) {
            assertThat(value == null || !value.isBlank(), is(true));
        }
    }

    @Test
    void testRaspberryPi(@TempDir Path base) throws IOException {
        // A Raspberry Pi 4's devicetree: each property is NUL-terminated, and compatible lists board then SoC
        Files.writeString(base.resolve("model"), "Raspberry Pi 4 Model B Rev 1.4\0");
        Files.writeString(base.resolve("compatible"), "raspberrypi,4-model-b\0brcm,bcm2711\0");
        Files.writeString(base.resolve("serial-number"), "10000000abcdef12\0");
        String dir = base + "/";
        assertThat("The terminating NUL is not part of the model", Devicetree.queryModel(dir),
                is("Raspberry Pi 4 Model B Rev 1.4"));
        assertThat(Devicetree.queryManufacturer(dir), is("Raspberry Pi"));
        assertThat(Devicetree.querySerialNumber(dir), is("10000000abcdef12"));
    }

    @Test
    void testManufacturerPrefixes(@TempDir Path base) throws IOException {
        Path compatible = base.resolve("compatible");
        String dir = base + "/";
        Files.writeString(compatible, "radxa,rock-5b\0rockchip,rk3588\0");
        assertThat(Devicetree.queryManufacturer(dir), is("Radxa"));
        Files.writeString(compatible, "acme,widget-board\0");
        assertThat("An unlisted prefix is reported as it stands", Devicetree.queryManufacturer(dir), is("acme"));
        Files.writeString(compatible, "linux,dummy-virt\0");
        assertThat("QEMU's virt machine names no vendor", Devicetree.queryManufacturer(dir), is(nullValue()));
        Files.writeString(compatible, "novendor\0");
        assertThat(Devicetree.queryManufacturer(dir), is(nullValue()));
    }

    @Test
    void testMissingProperties(@TempDir Path base) {
        String dir = base.resolve("absent") + "/";
        assertThat(Devicetree.queryModel(dir), is(nullValue()));
        assertThat(Devicetree.queryManufacturer(dir), is(nullValue()));
        assertThat(Devicetree.querySerialNumber(dir), is(nullValue()));
    }
}
