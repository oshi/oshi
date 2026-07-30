/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

import org.junit.jupiter.api.Test;

import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractFirmware;
import oshi.hardware.common.platform.unix.netbsd.NetBsdComputerSystem;
import oshi.hardware.common.platform.unix.openbsd.OpenBsdComputerSystem;

/**
 * Tests the structure NetBSD and OpenBSD share. The attribute values come from the {@code sysctl} command, so they
 * cannot be asserted off a BSD; what is portable is that both platforms inherit the shared implementation, that only
 * the firmware type differs, and that the memoized attributes are computed once.
 */
class BsdComputerSystemTest {

    /** Records how many times the platform query ran, without invoking sysctl. */
    private static final class CountingComputerSystem extends BsdComputerSystem {
        private int firmwareCalls;

        @Override
        protected Firmware createFirmware() {
            firmwareCalls++;
            return new AbstractFirmware() {
                @Override
                public String getManufacturer() {
                    return "test";
                }

                @Override
                public String getVersion() {
                    return "1";
                }
            };
        }
    }

    @Test
    void testBothPlatformsShareTheImplementation() {
        assertThat("NetBSD must inherit the shared BSD implementation", new NetBsdComputerSystem(), is(notNullValue()));
        assertThat(new NetBsdComputerSystem(), is(instanceOf(BsdComputerSystem.class)));
        assertThat(new OpenBsdComputerSystem(), is(instanceOf(BsdComputerSystem.class)));
    }

    /**
     * The only thing the two platforms vary. Guards against the subclasses being reduced to nothing, which would leave
     * both reporting the same firmware.
     */
    @Test
    void testFirmwareTypeIsThePlatformDifference() {
        assertThat(new NetBsdComputerSystem().getFirmware().getClass().getSimpleName(), is("NetBsdFirmware"));
        assertThat(new OpenBsdComputerSystem().getFirmware().getClass().getSimpleName(), is("OpenBsdFirmware"));
    }

    @Test
    void testFirmwareAndBaseboardAreMemoized() {
        CountingComputerSystem cs = new CountingComputerSystem();
        Firmware first = cs.getFirmware();
        assertThat("Firmware is created once and cached", cs.getFirmware(), is(sameInstance(first)));
        assertThat(cs.firmwareCalls, is(1));
        Baseboard board = cs.getBaseboard();
        assertThat(cs.getBaseboard(), is(sameInstance(board)));
    }
}
