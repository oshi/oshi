/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.bsd;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BsdOperatingSystemTest {

    @Test
    void testIsRcService() {
        // Services, from NetBSD 10.1's /etc/rc.d, including one with a suffix and one with a digit
        assertTrue(BsdOperatingSystem.isRcService("sshd", true));
        assertTrue(BsdOperatingSystem.isRcService("bootconf.sh", true));
        assertTrue(BsdOperatingSystem.isRcService("dhcpd6", true));
        // rcorder placeholders, from NetBSD and FreeBSD
        for (String placeholder : new String[] { "CRITLOCALMOUNTED", "DAEMON", "DISKS", "LOGIN", "NETWORKING",
                "SERVERS", "FILESYSTEMS" }) {
            assertFalse(BsdOperatingSystem.isRcService(placeholder, true), placeholder);
        }
        // OpenBSD's rc.subr, a library the scripts source
        assertFalse(BsdOperatingSystem.isRcService("rc.subr", false));
    }
}
