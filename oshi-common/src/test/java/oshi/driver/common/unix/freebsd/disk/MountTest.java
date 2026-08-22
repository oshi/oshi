/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.freebsd.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MountTest {

    @Test
    void testParseMountOutputTypicalOutput() {
        List<String> mountOutput = """
                /dev/ada0p2 on / (ufs, local, journaled, soft-updates)
                devfs on /dev (devfs)
                /dev/ada0p3 on /usr (ufs, local, journaled, soft-updates)
                /dev/da0p1 on /mnt/usb (msdosfs, local)
                """.lines().toList();

        Map<String, String> result = Mount.parseMountOutput(mountOutput);

        assertThat(result.size(), is(3));
        assertThat(result.get("ada0p2"), is("/"));
        assertThat(result.get("ada0p3"), is("/usr"));
        assertThat(result.get("da0p1"), is("/mnt/usb"));
    }

    @Test
    void testParseMountOutputEmptyInput() {
        Map<String, String> result = Mount.parseMountOutput(Collections.emptyList());
        assertThat(result, is(anEmptyMap()));
    }

    @Test
    void testParseMountOutputNonMatchingLinesIgnored() {
        List<String> mountOutput = """
                devfs on /dev (devfs)
                procfs on /proc (procfs, local)
                tmpfs on /tmp (tmpfs, local)
                """.lines().toList();

        Map<String, String> result = Mount.parseMountOutput(mountOutput);
        assertThat(result, is(anEmptyMap()));
    }
}
