/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux.proc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
class AuxvTest {
    @Test
    void testQueryAuxv() {
        Map<Integer, Long> auxv = Auxv.queryAuxv();
        assertNotNull("Aux vector should not be null");
        assertThat("Clock Ticks should be positive", auxv.getOrDefault(Auxv.AT_CLKTCK, 0L), greaterThan(0L));
        assertThat("Page Size should be positive", auxv.getOrDefault(Auxv.AT_PAGESZ, 0L), greaterThan(0L));
    }
}
