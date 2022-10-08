/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.solaris.kstat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.util.tuples.Pair;

@EnabledOnOs(OS.SOLARIS)
class SystemPagesTest {
    @Test
    void testQueryAvailableTotal() {
        Pair<Long, Long> availTotal = SystemPages.queryAvailableTotal();
        assertThat("Total should be a positive number", availTotal.getB().longValue(), greaterThan(0L));
        assertThat("Available should not exceed total", availTotal.getA().longValue(),
                lessThanOrEqualTo(availTotal.getB().longValue()));
    }
}
