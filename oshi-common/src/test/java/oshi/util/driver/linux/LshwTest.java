/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.TestConstants;
import oshi.util.tuples.Triplet;

class LshwTest {

    // Fixture: lshw -C system output
    private static final List<String> SYSTEM_OUTPUT = Arrays.asList("  *-system", "       description: Computer",
            "       product: PowerEdge R720", "       vendor: Dell Inc.", "       serial: ABC1234",
            "       width: 64 bits", "       uuid: 4C4C4544-0044-4810-8031-B4C04F333132");

    // Fixture: lshw -class processor output
    private static final List<String> PROCESSOR_OUTPUT = Arrays.asList("  *-cpu",
            "       product: Intel(R) Xeon(R) CPU E5-2670 v2 @ 2.50GHz", "       vendor: Intel Corp.",
            "       capacity: 3300MHz", "       width: 64 bits");

    @Test
    void testParseSystemInfo() {
        Triplet<String, String, String> result = Lshw.parseSystemInfo(SYSTEM_OUTPUT);
        assertThat(result.getA(), is("PowerEdge R720"));
        assertThat(result.getB(), is("ABC1234"));
        assertThat(result.getC(), is("4C4C4544-0044-4810-8031-B4C04F333132"));
    }

    @Test
    void testParseSystemInfoEmpty() {
        Triplet<String, String, String> result = Lshw.parseSystemInfo(Collections.emptyList());
        assertThat(result.getA(), is(nullValue()));
        assertThat(result.getB(), is(nullValue()));
        assertThat(result.getC(), is(nullValue()));
    }

    @Test
    void testParseSystemInfoPartial() {
        List<String> partial = Arrays.asList("       product: MyServer");
        Triplet<String, String, String> result = Lshw.parseSystemInfo(partial);
        assertThat(result.getA(), is("MyServer"));
        assertThat(result.getB(), is(nullValue()));
        assertThat(result.getC(), is(nullValue()));
    }

    @Test
    void testQueryCpuCapacityParsing() {
        assertThat(Lshw.queryCpuCapacity(PROCESSOR_OUTPUT), is(3_300_000_000L));
    }

    @Test
    void testQueryCpuCapacityEmpty() {
        assertThat(Lshw.queryCpuCapacity(Collections.emptyList()), is(-1L));
    }

    @Test
    void testQueryCpuCapacityNoCapacityLine() {
        List<String> noCapacity = Arrays.asList("  *-cpu", "       product: ARM Cortex-A72");
        assertThat(Lshw.queryCpuCapacity(noCapacity), is(-1L));
    }

    @Nested
    @EnabledOnOs(OS.LINUX)
    class LiveTests {
        @Test
        void testQueries() {
            assertDoesNotThrow(Lshw::queryModel);
            assertDoesNotThrow(Lshw::querySerialNumber);
            String uuid = Lshw.queryUUID();
            if (uuid != null) {
                assertThat("Test Lshw queryUUID", uuid, matchesRegex(TestConstants.UUID_REGEX));
            }
            assertThat("Test Lshw queryCpuCapacity", Lshw.queryCpuCapacity(), anyOf(greaterThan(0L), equalTo(-1L)));
        }
    }
}
