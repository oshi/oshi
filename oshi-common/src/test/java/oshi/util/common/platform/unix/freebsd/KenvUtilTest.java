/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

class KenvUtilTest {

    @Test
    void testParseKenv() {
        // kenv on a DragonFly BSD 6.4 OpenStack guest, which sets no serial, UUID or baseboard strings
        Map<String, String> kenv = KenvUtil.parseKenv("""
                hint.smbios.0.mem="0xeb320"
                smbios.bios.reldate="04/01/2014"
                smbios.bios.vendor="SeaBIOS"
                smbios.bios.version="1.16.3-4.el9.alma.1"
                smbios.chassis.version="RHEL 7.6.0 PC (i440FX + PIIX, 1996)"
                smbios.system.maker="RDO"
                smbios.system.product="OpenStack Compute"
                smbios.system.version="25.3.0-1.el9s"
                """.lines().toList());
        assertThat(kenv, hasEntry("smbios.bios.vendor", "SeaBIOS"));
        assertThat(kenv, hasEntry("smbios.bios.reldate", "04/01/2014"));
        assertThat(kenv, hasEntry("smbios.system.product", "OpenStack Compute"));
        assertThat(kenv, hasEntry("smbios.chassis.version", "RHEL 7.6.0 PC (i440FX + PIIX, 1996)"));
        assertThat(kenv.containsKey("smbios.system.serial"), is(false));
    }

    @Test
    void testParseKenvBlankValue() {
        // Values are trimmed, so a whitespace-only string reads as empty and the callers' fallbacks apply
        Map<String, String> kenv = KenvUtil.parseKenv("""
                smbios.planar.serial="   "
                smbios.system.maker=" RDO "
                """.lines().toList());
        assertThat(kenv, hasEntry("smbios.planar.serial", ""));
        assertThat(kenv, hasEntry("smbios.system.maker", "RDO"));
    }

    @Test
    void testParseKenvEmpty() {
        assertThat(KenvUtil.parseKenv(Collections.emptyList()), is(anEmptyMap()));
    }

    @Test
    void testQueryKenv() {
        // Empty where there is no kenv command; on FreeBSD and DragonFly BSD every value arrives unquoted
        for (String value : KenvUtil.queryKenv().values()) {
            assertThat(value, not(containsString("\"")));
        }
    }
}
