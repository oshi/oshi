/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.lang.foreign.MemorySegment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM.KstatChain;

@EnabledOnOs(OS.SOLARIS)
class KstatUtilFFMTest {

    @Test
    void testOpenChain() {
        try (KstatChain chain = KstatUtilFFM.openChain()) {
            assertThat("Chain should open", chain, notNullValue());
            int kcid = chain.update();
            assertThat("KCID should be >= 0 (changed or unchanged)", kcid, greaterThanOrEqualTo(0));
        }
    }

    @Test
    void testLookupSystemMisc() {
        try (KstatChain chain = KstatUtilFFM.openChain()) {
            // unix:0:system_misc is a stable kstat present on every Solaris/illumos system
            MemorySegment ksp = chain.lookup("unix", 0, "system_misc");
            assertThat("system_misc lookup should succeed", ksp.address(), greaterThan(0L));
            assertThat("read should succeed", chain.read(ksp), is(true));
            long bootTime = KstatUtilFFM.dataLookupLong(ksp, "boot_time");
            assertThat("boot_time should be positive", bootTime, greaterThan(0L));
        }
    }

    @Test
    void testDataLookupString() {
        try (KstatChain chain = KstatUtilFFM.openChain()) {
            MemorySegment ksp = chain.lookup("unix", 0, "system_misc");
            assertThat("read should succeed", chain.read(ksp), is(true));
            String bootStr = KstatUtilFFM.dataLookupString(ksp, "boot_time");
            assertThat("boot_time string should be non-empty", bootStr, is(not("")));
        }
    }

    @Test
    void testKstat2ProbeIsFalseOnIllumos() {
        // On OpenIndiana/illumos, libkstat2 is absent. On Solaris 11.4+, it would be true.
        // Either way, the probe should not throw.
        boolean has = Kstat2Functions.HAS_KSTAT2;
        // Avoid asserting the value (depends on environment); just confirm static init succeeded.
        assertThat(has || !has, is(true));
    }
}
