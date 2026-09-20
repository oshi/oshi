/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.netbsd;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class NetBsdOSProcessTest {

    @Test
    void testParseEnvironment() {
        byte[] env = "HOME=/root\0PATH=/bin:/usr/bin\0EMPTY=\0NOEQUALS\0=novalue\0EQ=a=b\0\0".getBytes(UTF_8);
        Map<String, String> parsed = NetBsdOSProcess.parseEnvironment(env);
        assertThat(parsed.keySet(), contains("HOME", "PATH", "EMPTY", "EQ"));
        assertThat(parsed, hasEntry("HOME", "/root"));
        assertThat(parsed, hasEntry("PATH", "/bin:/usr/bin"));
        assertThat(parsed, hasEntry("EMPTY", ""));
        assertThat(parsed, hasEntry("EQ", "a=b"));
    }

    @Test
    void testParseEnvironmentMultibyte() {
        byte[] env = "GREETING=héllo wörld\0NEXT=ok\0".getBytes(UTF_8);
        Map<String, String> parsed = NetBsdOSProcess.parseEnvironment(env);
        assertThat(parsed, hasEntry("GREETING", "héllo wörld"));
        assertThat(parsed, hasEntry("NEXT", "ok"));
    }

    @Test
    void testParseEnvironmentEmpty() {
        assertThat(NetBsdOSProcess.parseEnvironment(new byte[0]), is(anEmptyMap()));
        assertThat(NetBsdOSProcess.parseEnvironment(new byte[] { 0, 0 }), is(anEmptyMap()));
    }

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

    @Test
    void testParseCommandLine() {
        assertThat(NetBsdOSProcess.parseCommandLine("/opt/bootstrap/bin/java -Xmx64m Main"),
                is("/opt/bootstrap/bin/java -Xmx64m Main"));
        assertThat(NetBsdOSProcess.parseCommandLine("java /tmp/S.java"), is("java /tmp/S.java"));
        // ps's placeholder is not a command line, so nothing is known
        assertThat(NetBsdOSProcess.parseCommandLine("(java)"), is(""));
        assertThat(NetBsdOSProcess.parseCommandLine("(sshd)"), is(""));
        assertThat(NetBsdOSProcess.parseCommandLine(null), is(""));
    }

    @Test
    void testIsPlaceholder() {
        // ps substitutes the command in parentheses where the kernel would not release the arguments
        assertTrue(NetBsdOSProcess.isPlaceholder("(java)"));
        assertTrue(NetBsdOSProcess.isPlaceholder("(sshd)"));
        assertFalse(NetBsdOSProcess.isPlaceholder("/opt/bootstrap/bin/java"));
        assertFalse(NetBsdOSProcess.isPlaceholder("java"));
        assertFalse(NetBsdOSProcess.isPlaceholder("()"));
        assertFalse(NetBsdOSProcess.isPlaceholder(""));
    }
}
