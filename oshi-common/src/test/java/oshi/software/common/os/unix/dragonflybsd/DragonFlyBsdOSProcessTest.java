/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.dragonflybsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import oshi.software.common.os.unix.bsd.BsdPsKeyword;
import oshi.software.os.OperatingSystem;

/**
 * Tests that {@link DragonFlyBsdOSProcess} asks the kernel for an executable path only for the current process, and how
 * it falls back for the others.
 */
class DragonFlyBsdOSProcessTest {

    private static final int OWN_PID = 42;

    // Only getProcessId() is reached by the code under test
    private static final OperatingSystem OS = (OperatingSystem) Proxy.newProxyInstance(
            OperatingSystem.class.getClassLoader(), new Class<?>[] { OperatingSystem.class }, (proxy, method, args) -> {
                if ("getProcessId".equals(method.getName())) {
                    return OWN_PID;
                }
                throw new UnsupportedOperationException(method.getName());
            });

    private static DragonFlyBsdOSProcess stubProcess(int pid, AtomicInteger kernelQueries) {
        return new DragonFlyBsdOSProcess(pid, OS) {
            @Override
            protected String queryOwnExecutablePath() {
                kernelQueries.incrementAndGet();
                return "/usr/local/bin/java";
            }

            @Override
            protected long queryAffinityMask() {
                return 0L;
            }

            @Override
            protected List<String> queryArguments() {
                return Collections.emptyList();
            }

            @Override
            protected Map<String, String> queryEnvironmentVariables() {
                return Collections.emptyMap();
            }
        };
    }

    @Test
    void testCurrentProcessPathFromKernel() {
        AtomicInteger kernelQueries = new AtomicInteger();
        assertThat(stubProcess(OWN_PID, kernelQueries).queryExecutablePath(), is("/usr/local/bin/java"));
        assertThat(kernelQueries.get(), is(1));
    }

    @Test
    void testOtherProcessNeverQueriesKernel() {
        AtomicInteger kernelQueries = new AtomicInteger();
        assertThat(stubProcess(OWN_PID + 1, kernelQueries).queryExecutablePath(), is(""));
        assertThat(kernelQueries.get(), is(0));
    }

    private static String otherProcessPath(String ucomm, String command) {
        Map<BsdPsKeyword, String> psMap = new EnumMap<>(BsdPsKeyword.class);
        psMap.put(BsdPsKeyword.UCOMM, ucomm);
        psMap.put(BsdPsKeyword.COMMAND, command);
        AtomicInteger kernelQueries = new AtomicInteger();
        String path = stubProcess(OWN_PID + 1, kernelQueries).queryPath(psMap);
        assertThat(kernelQueries.get(), is(0));
        return path;
    }

    @Test
    void testCurrentProcessPathPrefersKernel() {
        Map<BsdPsKeyword, String> psMap = new EnumMap<>(BsdPsKeyword.class);
        psMap.put(BsdPsKeyword.UCOMM, "java");
        psMap.put(BsdPsKeyword.COMMAND, "java -jar oshi.jar");
        assertThat(stubProcess(OWN_PID, new AtomicInteger()).queryPath(psMap), is("/usr/local/bin/java"));
    }

    @Test
    void testOtherProcessPathFromArgv0() {
        assertThat(otherProcessPath("sshd", "/usr/sbin/sshd -D"), is("/usr/sbin/sshd"));
        // Started through PATH, so argv[0] is a bare name
        assertThat(otherProcessPath("gmake", "gmake -j8 all"), is("gmake"));
        // ucomm is cut to MAXCOMLEN; argv[0] keeps the whole name
        assertThat(otherProcessPath("postgres_exporte", "/usr/local/bin/postgres_exporter --log.level=info"),
                is("/usr/local/bin/postgres_exporter"));
    }

    @Test
    void testOtherProcessPathIgnoresRewrittenArgv0() {
        // A process title, a login shell and a different program sharing a prefix all fall back to ucomm
        assertThat(otherProcessPath("sshd", "sshd: oshi@notty (sshd)"), is("sshd"));
        assertThat(otherProcessPath("sh", "-sh"), is("sh"));
        assertThat(otherProcessPath("java", "javac Main.java"), is("java"));
        assertThat(otherProcessPath("dhclient", ""), is("dhclient"));
        assertThat(otherProcessPath("", "/usr/sbin/cron"), is(""));
    }
}
