/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.dragonflybsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        return stubProcess(pid, kernelQueries, Collections.emptyList());
    }

    private static DragonFlyBsdOSProcess stubProcess(int pid, AtomicInteger kernelQueries, List<String> args) {
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
                return args;
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

    private static String otherProcessPath(String ucomm, String... args) {
        Map<BsdPsKeyword, String> psMap = new EnumMap<>(BsdPsKeyword.class);
        psMap.put(BsdPsKeyword.UCOMM, ucomm);
        AtomicInteger kernelQueries = new AtomicInteger();
        String path = stubProcess(OWN_PID + 1, kernelQueries, List.of(args)).queryPath(psMap);
        assertThat(kernelQueries.get(), is(0));
        return path;
    }

    @Test
    void testCurrentProcessPathPrefersKernel() {
        Map<BsdPsKeyword, String> psMap = new EnumMap<>(BsdPsKeyword.class);
        psMap.put(BsdPsKeyword.UCOMM, "java");
        assertThat(stubProcess(OWN_PID, new AtomicInteger(), List.of("java", "-jar", "oshi.jar")).queryPath(psMap),
                is("/usr/local/bin/java"));
    }

    @Test
    void testOtherProcessPathFromArgv0() {
        assertThat(otherProcessPath("sshd", "/usr/sbin/sshd", "-D"), is("/usr/sbin/sshd"));
        // Started through PATH, so argv[0] is a bare name
        assertThat(otherProcessPath("gmake", "gmake", "-j8", "all"), is("gmake"));
        // ucomm is cut to MAXCOMLEN; argv[0] keeps the whole name
        assertThat(otherProcessPath("postgres_exporte", "/usr/local/bin/postgres_exporter", "--log.level=info"),
                is("/usr/local/bin/postgres_exporter"));
        // The arguments are NUL-separated, so a space in the path does not split it
        assertThat(otherProcessPath("java", "/opt/java tools/java", "-version"), is("/opt/java tools/java"));
    }

    @Test
    void testOtherProcessPathIgnoresRewrittenArgv0() {
        // Process titles, a login shell and a different program sharing a prefix all fall back to ucomm
        assertThat(otherProcessPath("sshd", "sshd: oshi@notty (sshd)"), is("sshd"));
        assertThat(otherProcessPath("postgres_exporte", "postgres_exporter: running"), is("postgres_exporte"));
        assertThat(otherProcessPath("postgres_exporte", "postgres_exporter:"), is("postgres_exporte"));
        assertThat(otherProcessPath("sh", "-sh"), is("sh"));
        assertThat(otherProcessPath("java", "javac", "Main.java"), is("java"));
        assertThat(otherProcessPath("", "/usr/sbin/cron"), is(""));
    }

    @Test
    void testOtherProcessPathWithoutProcfs() {
        assertThat(otherProcessPath("dhclient"), is("dhclient"));
    }

    private static byte[] cmdline(String... args) {
        return (String.join("\0", args) + "\0").getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void testArgumentsAgreeingWithPsKeepTheirBoundaries() {
        assertThat(DragonFlyBsdOSProcess.reconcileArguments(cmdline("/opt/java tools/java", "-version"),
                "/opt/java tools/java -version", "java"), is(List.of("/opt/java tools/java", "-version")));
        assertThat(DragonFlyBsdOSProcess.reconcileArguments(cmdline("sshd", "-D"), "", "sshd"),
                is(List.of("sshd", "-D")));
    }

    @Test
    void testCommandNameBufferFallsBackToPs() {
        // Another process past kern.ps_arg_cache_limit: the file holds the 16-byte p_comm buffer, stale bytes and all
        byte[] comm = "java\0er\0\0\0\0\0\0\0\0\0".getBytes(StandardCharsets.UTF_8);
        assertThat(comm.length, is(16));
        // The kernel drops the cache only past kern.ps_arg_cache_limit (256 bytes), so ps prints a long command line
        String classpath = IntStream.rangeClosed(1, 12).mapToObj(i -> "/home/oshi/some/long/path/lib" + i + ".jar")
                .collect(Collectors.joining(":"));
        assertThat(DragonFlyBsdOSProcess.reconcileArguments(comm, "/usr/local/bin/java -cp " + classpath + " S arg1",
                "java"), is(List.of("/usr/local/bin/java", "-cp", classpath, "S", "arg1")));
        // The buffer holds ucomm, not argv[0]: started as "worker", ps follows the line with " (java)", not an argument
        assertThat(DragonFlyBsdOSProcess.reconcileArguments(comm, "worker -cp " + classpath + " S (java)", "java"),
                is(List.of("worker", "-cp", classpath, "S")));
        // When ps cannot read the arguments either, it prints "(ucomm)", and neither source has them
        assertThat(DragonFlyBsdOSProcess.reconcileArguments(comm, "(java)", "java"), is(empty()));
    }

    @Test
    void testSixteenByteArgumentsThatPsEscapesKeepTheFile() {
        // A complete argument list can be MAXCOMLEN bytes too; ps prints the tab escaped, so the two disagree
        byte[] args = cmdline("sh", "-c", "\tread foo");
        assertThat(args.length, is(16));
        assertThat(DragonFlyBsdOSProcess.reconcileArguments(args, "sh -c \\tread foo", "sh"),
                is(List.of("sh", "-c", "\tread foo")));
    }

    @Test
    void testCurrentProcessTruncatedAtLimitFallsBackToPs() {
        // The current process reads its own arguments up to the limit, losing the long classpath and what follows
        assertThat(
                DragonFlyBsdOSProcess.reconcileArguments(cmdline("/usr/local/bin/java", "-Dfoo=bar", "-cp"),
                        "/usr/local/bin/java -Dfoo=bar -cp .:a.jar:b.jar S", "java"),
                is(List.of("/usr/local/bin/java", "-Dfoo=bar", "-cp", ".:a.jar:b.jar", "S")));
    }

    @Test
    void testProcessTitleAndKernelThreadKeepTheFile() {
        // setproctitle(): ps follows the title with "(ucomm)"
        assertThat(DragonFlyBsdOSProcess.reconcileArguments(cmdline("dhclient: vtnet0"), "dhclient: vtnet0 (dhclient)",
                "dhclient"), is(List.of("dhclient: vtnet0")));
        // Sixteen bytes of real arguments that ps prints exactly, parentheses and all
        byte[] parenthesized = cmdline("(sd-pam-worker)");
        assertThat(parenthesized.length, is(16));
        assertThat(DragonFlyBsdOSProcess.reconcileArguments(parenthesized, "(sd-pam-worker)", "sd-pam-worker"),
                is(List.of("(sd-pam-worker)")));
        // A kernel thread has no arguments, and ps prints its name in parentheses
        assertThat(DragonFlyBsdOSProcess.reconcileArguments(new byte[0], "(idle_5)", "idle_5"), is(empty()));
    }

    @Test
    void testEmptyFileKeepsNoArguments() {
        // Without the file a title cannot be told from arguments, so neither is split into them
        assertThat(DragonFlyBsdOSProcess.reconcileArguments(new byte[0], "dhclient: vtnet0 (dhclient)", "dhclient"),
                is(empty()));
        assertThat(DragonFlyBsdOSProcess.reconcileArguments(new byte[0], "/sbin/init --", "init"), is(empty()));
    }

    @Test
    void testCommandNameSuffixIsNotAnArgument() {
        // ps appends " (ucomm)" to a line that does not start with the command name; it is never an argument
        assertThat(DragonFlyBsdOSProcess.reconcileArguments(cmdline("worker", "-q"), "worker -q --long (svc)", "svc"),
                is(List.of("worker", "-q", "--long")));
    }

    @Test
    void testTrailingCommandNameArgumentIsKept() {
        // argv[0] names the command, so ps adds no suffix and a trailing "(java)" is a real argument
        assertThat(
                DragonFlyBsdOSProcess.reconcileArguments(cmdline("/usr/local/bin/java", "-cp"),
                        "/usr/local/bin/java -cp a.jar Main (java)", "java"),
                is(List.of("/usr/local/bin/java", "-cp", "a.jar", "Main", "(java)")));
        // A name longer than MAXCOMLEN still names the command it was cut to
        assertThat(
                DragonFlyBsdOSProcess.reconcileArguments(cmdline("/opt/a_very_long_program_name", "5"),
                        "/opt/a_very_long_program_name 5", "a_very_long_prog"),
                is(List.of("/opt/a_very_long_program_name", "5")));
    }

    @Test
    void testEscapedCharacterIsNotTruncation() {
        // ps prints a backslash doubled, so the file's line is a prefix of ps's without being cut short
        assertThat(DragonFlyBsdOSProcess.reconcileArguments(cmdline("echo", "\\"), "echo \\\\", "echo"),
                is(List.of("echo", "\\")));
    }
}
