/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.TestConstants;

@EnabledOnOs(OS.LINUX)
class LshwTest {

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
