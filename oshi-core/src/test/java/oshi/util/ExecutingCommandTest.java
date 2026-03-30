/*
 * Copyright 2016-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

import oshi.PlatformEnum;
import oshi.SystemInfo;

/**
 * Test command line and returning the result of execution.
 */
@Execution(SAME_THREAD)
class ExecutingCommandTest {

    private static final String ECHO = SystemInfo.getCurrentPlatform().equals(PlatformEnum.WINDOWS)
            ? "cmd.exe /C echo Test"
            : "echo Test";
    private static final String BAD_COMMAND = "noOSshouldHaveACommandNamedThis";

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
    void testRunPrivilegedNativeNoConfig() {
        // Without config, runPrivilegedNative should return empty list when not elevated
        // (unless already running as root, which we can't control in tests)
        if (!UserGroupInfo.isElevated()) {
            List<String> result = ExecutingCommand.runPrivilegedNative("dmidecode -t system");
            assertThat("no config should return empty", result, is(empty()));

            String answer = ExecutingCommand.getFirstPrivilegedAnswer("dmidecode -t system");
            assertThat("no config first answer should be empty", answer, is(emptyString()));
        }
    }

    @Test
    void testRunPrivilegedNativeNotInAllowlist() {
        // Configure prefix but don't include command in allowlist
        GlobalConfig.set(GlobalConfig.OSHI_SUDOCOMMAND_PREFIX, "sudo -n");
        GlobalConfig.set(GlobalConfig.OSHI_SUDOCOMMAND_ALLOWLIST, "lshw");

        if (!UserGroupInfo.isElevated()) {
            // dmidecode is not in allowlist, should return empty
            List<String> result = ExecutingCommand.runPrivilegedNative("dmidecode -t system");
            assertThat("command not in allowlist should return empty", result, is(empty()));
        }
    }

    @Test
    void testRunPrivilegedNativeInAllowlist() {
        // Configure prefix and include command in allowlist
        GlobalConfig.set(GlobalConfig.OSHI_SUDOCOMMAND_PREFIX, "echo");
        GlobalConfig.set(GlobalConfig.OSHI_SUDOCOMMAND_ALLOWLIST, "test-command");

        if (!UserGroupInfo.isElevated()) {
            // This will execute "echo test-command arg" which should work
            List<String> result = ExecutingCommand.runPrivilegedNative("test-command arg");
            // The command "echo test-command arg" should output "test-command arg"
            assertThat("command in allowlist with echo prefix", result, hasSize(1));
            assertThat("output should be the command with args", result.get(0), is("test-command arg"));
        }
    }

    @Test
    void testGetFirstPrivilegedAnswerInAllowlist() {
        GlobalConfig.set(GlobalConfig.OSHI_SUDOCOMMAND_PREFIX, "echo");
        GlobalConfig.set(GlobalConfig.OSHI_SUDOCOMMAND_ALLOWLIST, "hello");

        if (!UserGroupInfo.isElevated()) {
            String answer = ExecutingCommand.getFirstPrivilegedAnswer("hello world");
            assertThat("first privileged answer", answer, is("hello world"));
        }
    }
}
