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
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
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
}
