/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;

/**
 * Test command line and returning the result of execution.
 */
@Execution(SAME_THREAD)
class ExecutingCommandTest {

    private static final String ECHO = PlatformEnum.getCurrentPlatform().equals(PlatformEnum.WINDOWS)
            ? "cmd.exe /C echo Test"
            : "echo Test";
    private static final String BAD_COMMAND = "noOSshouldHaveACommandNamedThis";

    static boolean isElevated() {
        return UserGroupInfo.isElevated();
    }

    @BeforeEach
    void setUp() {
        GlobalConfig.clear();
    }

    @Test
    void testRunNative() {
        List<String> test = ExecutingCommand.runNative(ECHO);
        assertThat("echo output", test, hasSize(1));
        assertThat("echo output", test.get(0), is("Test"));
        assertThat("echo first answer", ExecutingCommand.getFirstAnswer(ECHO), is("Test"));

        assertThat("bad command", ExecutingCommand.runNative(BAD_COMMAND), is(empty()));
        assertThat("bad command first answer", ExecutingCommand.getFirstAnswer(BAD_COMMAND), is(emptyString()));
    }

    @Test
    @DisabledIf("isElevated")
    void testRunPrivilegedNativeNoConfig() {
        // Without config, runPrivilegedNative runs command directly
        List<String> result = ExecutingCommand.runPrivilegedNative(ECHO);
        assertThat("no config should run directly", result, hasSize(1));
    }

    @Test
    @DisabledIf("isElevated")
    @EnabledOnOs(OS.LINUX)
    void testRunPrivilegedNativeNotInAllowlist() {
        // Configure prefix but don't include command in allowlist
        // Command should still run without prefix
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_PREFIX, "sudo -n");
        GlobalConfig.set(GlobalConfig.OSHI_OS_LINUX_PRIVILEGED_ALLOWLIST, "lshw");

        // echo is not in allowlist, should run without prefix
        List<String> result = ExecutingCommand.runPrivilegedNative(ECHO);
        assertThat("command not in allowlist should run without prefix", result, hasSize(1));
    }

    @Test
    @DisabledIf("isElevated")
    void testRunPrivilegedNativeRunsCommand() {
        // Without allowlist config, command runs directly without prefix
        // Test that runPrivilegedNative actually executes the command
        List<String> result = ExecutingCommand.runPrivilegedNative(ECHO);
        assertThat("command should execute", result, hasSize(1));
        assertThat("output should be Test", result.get(0), is("Test"));
    }

    @Test
    @DisabledIf("isElevated")
    void testGetFirstPrivilegedAnswer() {
        // Test getFirstPrivilegedAnswer returns first line
        String answer = ExecutingCommand.getFirstPrivilegedAnswer(ECHO);
        assertThat("first privileged answer", answer, is("Test"));
    }

    @Test
    void testGetAnswerAtOutOfRange() {
        assertThat("negative index", ExecutingCommand.getAnswerAt(ECHO, -1), is(emptyString()));
        assertThat("index past end", ExecutingCommand.getAnswerAt(ECHO, 999), is(emptyString()));
    }

    @Test
    void testGetAnswerAtValidIndex() {
        assertThat("index 0", ExecutingCommand.getAnswerAt(ECHO, 0), is("Test"));
    }

    @Test
    void testRunNativeWithArray() {
        String[] cmd = PlatformEnum.getCurrentPlatform().equals(PlatformEnum.WINDOWS)
                ? new String[] { "cmd.exe", "/C", "echo", "Hello" }
                : new String[] { "echo", "Hello" };
        List<String> result = ExecutingCommand.runNative(cmd);
        assertThat("array command", result, hasSize(1));
        assertThat("array command output", result.get(0), is("Hello"));
    }

    @Test
    void testFindExecutableSearchesDirectoriesInOrder(@TempDir Path tmp) throws IOException {
        Path first = Files.createDirectory(tmp.resolve("sbin"));
        Path second = Files.createDirectory(tmp.resolve("usr-sbin"));
        Path tool = Files.createFile(second.resolve("sysctl"));
        assertThat(tool.toFile().setExecutable(true), is(true));
        List<String> dirs = List.of(first.toString(), second.toString());
        assertThat(ExecutingCommand.findExecutable(dirs, "sysctl"), is(tool.toString()));
        assertThat(ExecutingCommand.findExecutable(dirs, BAD_COMMAND), is(nullValue()));
        // A name with a path is the caller's to resolve, and an empty one names nothing
        assertThat(ExecutingCommand.findExecutable(dirs, "../usr-sbin/sysctl"), is(nullValue()));
        assertThat(ExecutingCommand.findExecutable(dirs, ""), is(nullValue()));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testFindExecutableSkipsFilesThatCannotRun(@TempDir Path tmp) throws IOException {
        Files.createDirectory(tmp.resolve("route"));
        assertThat(ExecutingCommand.findExecutable(List.of(tmp.toString()), "route"), is(nullValue()));
        // access(X_OK) lets root execute a file with no execute bit, so only an unprivileged run can see it skipped
        assumeFalse(UserGroupInfo.isElevated(), "root may execute any file");
        Path tool = Files.createFile(tmp.resolve("sysctl"));
        assertThat(tool.toFile().setExecutable(false, false), is(true));
        assertThat(ExecutingCommand.findExecutable(List.of(tmp.toString()), "sysctl"), is(nullValue()));
    }

    private static String firstLine(Process p) throws IOException, InterruptedException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            p.waitFor();
            return line == null ? "" : line;
        }
    }

    private static boolean isRunnable(Path script) throws InterruptedException {
        try {
            return "found".equals(firstLine(Runtime.getRuntime().exec(new String[] { script.toString() })));
        } catch (IOException e) {
            return false;
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testExecFindsAndRefindsACommandOffThePath(@TempDir Path tmp) throws IOException, InterruptedException {
        // A name unique to this test, so that no other test shares its remembered path
        String name = "oshi-exec-test-" + System.nanoTime();
        Path first = Files.createDirectory(tmp.resolve("first"));
        Path second = Files.createDirectory(tmp.resolve("second"));
        Path tool = first.resolve(name);
        Files.writeString(tool, "#!/bin/sh\necho found\n");
        assertThat(tool.toFile().setExecutable(true), is(true));
        // A temporary directory on a noexec mount cannot run the script at all
        assumeTrue(isRunnable(tool), "temporary directory does not allow execution");
        List<String> dirs = List.of(first.toString(), second.toString());
        assertThat(firstLine(ExecutingCommand.exec(new String[] { name }, null, dirs)), is("found"));
        // Moved after it was remembered: the stale path is dropped and the command found again
        Files.move(tool, second.resolve(name), StandardCopyOption.ATOMIC_MOVE);
        assertThat(firstLine(ExecutingCommand.exec(new String[] { name }, null, dirs)), is("found"));
        // Gone entirely: starting it fails as it would have without the search
        Files.delete(second.resolve(name));
        assertThrows(IOException.class, () -> ExecutingCommand.exec(new String[] { name }, null, dirs));
    }
}
