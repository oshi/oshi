/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.dragonflybsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import oshi.hardware.platform.unix.freebsd.FreeBsdHardwareAbstractionLayerJNA;

/**
 * DragonFly BSD wires every hardware component the way FreeBSD does except the processor, so its hardware abstraction
 * layer inherits the FreeBSD one. These guard that relationship, which no runtime test can: the components themselves
 * need a BSD to construct.
 */
class DragonFlyBsdHardwareAbstractionLayerJNATest {

    @Test
    void testInheritsTheFreeBsdWiring() {
        assertThat(new DragonFlyBsdHardwareAbstractionLayerJNA(),
                is(instanceOf(FreeBsdHardwareAbstractionLayerJNA.class)));
    }

    /**
     * The one component DragonFly must not inherit. Its processor reads tick counters from {@code kern.cputime} where
     * FreeBSD reads {@code kern.cp_time}, so dropping this override would silently report FreeBSD's counters.
     */
    @Test
    void testProcessorIsOverridden() {
        assertDoesNotThrow(() -> DragonFlyBsdHardwareAbstractionLayerJNA.class.getDeclaredMethod("createProcessor"),
                "DragonFly must declare its own createProcessor, not inherit FreeBSD's");
    }

    /**
     * The point of inheriting: these must not be copied back in. A re-declared method here would silently diverge from
     * the FreeBSD one it was copied from.
     */
    @Test
    void testEverythingElseIsInherited() {
        for (String method : new String[] { "createComputerSystem", "createMemory", "createSensors", "createDisplays",
                "createUsbDevices", "createSoundCards", "createGraphicsCards" }) {
            assertThrows(NoSuchMethodException.class,
                    () -> DragonFlyBsdHardwareAbstractionLayerJNA.class.getDeclaredMethod(method),
                    method + " must be inherited from FreeBSD, not redeclared");
        }
    }
}
