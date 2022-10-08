/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.MAC)
class FsstatTest {
    @Test
    void testQueryPartitionToMountMap() {
        assertThat("Partition to mount map should not be empty.", Fsstat.queryPartitionToMountMap(),
                is(not(anEmptyMap())));
    }
}
