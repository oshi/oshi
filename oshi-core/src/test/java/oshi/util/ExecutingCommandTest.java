/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.PlatformEnum;
import oshi.SystemInfo;

/**
 * Test command line and returning the result of execution.
 */
class ExecutingCommandTest {

    private static final String ECHO = SystemInfo.getCurrentPlatform().equals(PlatformEnum.WINDOWS)
            ? "cmd.exe /C echo Test"
            : "echo Test";
    private static final String BAD_COMMAND = "noOSshouldHaveACommandNamedThis";

    @Test
    void testRunNative() {
        List<String> test = ExecutingCommand.runNative(ECHO);
        assertThat("echo output", test, hasSize(1));
        assertThat("echo output", test.get(0), is("Test"));
        assertThat("echo first answer", ExecutingCommand.getFirstAnswer(ECHO), is("Test"));

        assertThat("bad command", ExecutingCommand.runNative(BAD_COMMAND), is(empty()));
        assertThat("bad command first answer", ExecutingCommand.getFirstAnswer(BAD_COMMAND), is(emptyString()));
    }
}
