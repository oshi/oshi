/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.TestConstants;
import oshi.util.tuples.Pair;

@EnabledOnOs(OS.LINUX)
class DmidecodeTest {

    @Test
    void testQueries() {
        assertDoesNotThrow(Dmidecode::querySerialNumber);
        assertDoesNotThrow(Dmidecode::queryBiosNameRev);

        final String uuid = Dmidecode.queryUUID();
        if (uuid != null) {
            assertThat("Test Lshal queryUUID format", uuid, matchesRegex(TestConstants.UUID_REGEX));
        }
    }

    @Test
    void testQueryBiosNameRevReturnsPair() {
        Pair<String, String> result = Dmidecode.queryBiosNameRev();
        assertThat(result, notNullValue());
        // Both fields may be null if dmidecode is not available or not root,
        // but the pair itself should never be null
    }
}
