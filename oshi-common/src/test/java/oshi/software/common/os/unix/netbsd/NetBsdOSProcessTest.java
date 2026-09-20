/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class NetBsdOSProcessTest {

    @Test
    void testParseArgv0() {
        // The comm column would have clipped this to "/opt/bo", the width of ps's COMMAND header
        assertThat(NetBsdOSProcess.parseArgv0("/opt/bootstrap/bin/java -Xmx64m -cp /tmp/a.jar Main"),
                is("/opt/bootstrap/bin/java"));
        assertThat(NetBsdOSProcess.parseArgv0("/usr/sbin/sshd"), is("/usr/sbin/sshd"));
        // Started through PATH, so argv[0] is a bare name rather than a path
        assertThat(NetBsdOSProcess.parseArgv0("java /tmp/S.java"), is("java"));
        assertThat(NetBsdOSProcess.parseArgv0("  /bin/sh -c true  "), is("/bin/sh"));
        assertThat(NetBsdOSProcess.parseArgv0(null), is(""));
        assertThat(NetBsdOSProcess.parseArgv0(""), is(""));
    }

}
