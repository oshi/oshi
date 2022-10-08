/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import oshi.TestConstants;

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
}
