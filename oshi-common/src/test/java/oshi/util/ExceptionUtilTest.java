/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Tests for {@link ExceptionUtil}.
 */
class ExceptionUtilTest {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionUtilTest.class);

    // -- getOrDefault --

    @Test
    void testGetOrDefaultReturnsValue() {
        String result = ExceptionUtil.getOrDefault(() -> "hello", "default");
        assertThat(result, is(equalTo("hello")));
    }

    @Test
    void testGetOrDefaultReturnsDefaultOnException() {
        String result = ExceptionUtil.getOrDefault(() -> {
            throw new RuntimeException("fail");
        }, "default");
        assertThat(result, is(equalTo("default")));
    }

    @Test
    void testGetOrDefaultReturnsDefaultOnError() {
        String result = ExceptionUtil.getOrDefault(() -> {
            throw new OutOfMemoryError("simulated");
        }, "default");
        assertThat(result, is(equalTo("default")));
    }

    @Test
    void testGetOrDefaultWithNullDefault() {
        String result = ExceptionUtil.getOrDefault(() -> {
            throw new RuntimeException("fail");
        }, null);
        assertThat(result, is(nullValue()));
    }

    @Test
    void testGetOrDefaultWithLogging() {
        String result = ExceptionUtil.getOrDefault(() -> {
            throw new RuntimeException("test error");
        }, "fallback", LOG, "Operation failed: {}");
        assertThat(result, is(equalTo("fallback")));
    }

    @Test
    void testGetOrDefaultWithLoggingSuccess() {
        String result = ExceptionUtil.getOrDefault(() -> "success", "fallback", LOG, "Should not log");
        assertThat(result, is(equalTo("success")));
    }

    // -- getIntOrDefault --

    @Test
    void testGetIntOrDefaultReturnsValue() {
        int result = ExceptionUtil.getIntOrDefault(() -> 42, -1);
        assertThat(result, is(42));
    }

    @Test
    void testGetIntOrDefaultReturnsDefaultOnException() {
        int result = ExceptionUtil.getIntOrDefault(() -> {
            throw new RuntimeException("fail");
        }, -1);
        assertThat(result, is(-1));
    }

    @Test
    void testGetIntOrDefaultWithLogging() {
        int result = ExceptionUtil.getIntOrDefault(() -> {
            throw new RuntimeException("fail");
        }, 0, LOG, "Int op failed: {}");
        assertThat(result, is(0));
    }

    @Test
    void testGetIntOrDefaultWithLoggingSuccess() {
        int result = ExceptionUtil.getIntOrDefault(() -> 42, -1, LOG, "Should not log");
        assertThat(result, is(42));
    }

    // -- getLongOrDefault --

    @Test
    void testGetLongOrDefaultReturnsValue() {
        long result = ExceptionUtil.getLongOrDefault(() -> 100L, -1L);
        assertThat(result, is(100L));
    }

    @Test
    void testGetLongOrDefaultReturnsDefaultOnException() {
        long result = ExceptionUtil.getLongOrDefault(() -> {
            throw new RuntimeException("fail");
        }, -1L);
        assertThat(result, is(-1L));
    }

    @Test
    void testGetLongOrDefaultWithLogging() {
        long result = ExceptionUtil.getLongOrDefault(() -> {
            throw new RuntimeException("fail");
        }, 0L, LOG, "Long op failed: {}");
        assertThat(result, is(0L));
    }

    @Test
    void testGetLongOrDefaultWithLoggingSuccess() {
        long result = ExceptionUtil.getLongOrDefault(() -> 100L, -1L, LOG, "Should not log");
        assertThat(result, is(100L));
    }

    // -- getBooleanOrDefault --

    @Test
    void testGetBooleanOrDefaultReturnsValue() {
        boolean result = ExceptionUtil.getBooleanOrDefault(() -> true, false);
        assertThat(result, is(true));
    }

    @Test
    void testGetBooleanOrDefaultReturnsDefaultOnException() {
        boolean result = ExceptionUtil.getBooleanOrDefault(() -> {
            throw new RuntimeException("fail");
        }, false);
        assertThat(result, is(false));
    }

    @Test
    void testGetBooleanOrDefaultWithLogging() {
        boolean result = ExceptionUtil.getBooleanOrDefault(() -> {
            throw new RuntimeException("fail");
        }, true, LOG, "Bool op failed: {}");
        assertThat(result, is(true));
    }

    @Test
    void testGetBooleanOrDefaultWithLoggingSuccess() {
        boolean result = ExceptionUtil.getBooleanOrDefault(() -> true, false, LOG, "Should not log");
        assertThat(result, is(true));
    }

    // -- getDoubleOrDefault --

    @Test
    void testGetDoubleOrDefaultReturnsValue() {
        double result = ExceptionUtil.getDoubleOrDefault(() -> 3.14, -1d);
        assertThat(result, is(3.14));
    }

    @Test
    void testGetDoubleOrDefaultReturnsDefaultOnException() {
        double result = ExceptionUtil.getDoubleOrDefault(() -> {
            throw new RuntimeException("fail");
        }, -1d);
        assertThat(result, is(-1d));
    }

    @Test
    void testGetDoubleOrDefaultWithLogging() {
        double result = ExceptionUtil.getDoubleOrDefault(() -> {
            throw new RuntimeException("fail");
        }, -1d, LOG, "Double op failed: {}");
        assertThat(result, is(-1d));
    }

    @Test
    void testGetDoubleOrDefaultWithLoggingSuccess() {
        double result = ExceptionUtil.getDoubleOrDefault(() -> 3.14, -1d, LOG, "Should not log");
        assertThat(result, is(3.14));
    }

    // -- getOptional --

    @Test
    void testGetOptionalReturnsValue() {
        Optional<String> result = ExceptionUtil.getOptional(() -> "value", LOG, "msg");
        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(equalTo("value")));
    }

    @Test
    void testGetOptionalReturnsEmptyOnException() {
        Optional<String> result = ExceptionUtil.getOptional(() -> {
            throw new RuntimeException("fail");
        }, LOG, "Optional op failed: {}");
        assertThat(result.isPresent(), is(false));
    }

    @Test
    void testGetOptionalReturnsEmptyOnNull() {
        Optional<String> result = ExceptionUtil.getOptional(() -> null, LOG, "msg");
        assertThat(result.isPresent(), is(false));
    }

    // -- getOptionalInt --

    @Test
    void testGetOptionalIntReturnsValue() {
        OptionalInt result = ExceptionUtil.getOptionalInt(() -> 7, LOG, "msg");
        assertThat(result.isPresent(), is(true));
        assertThat(result.getAsInt(), is(7));
    }

    @Test
    void testGetOptionalIntReturnsEmptyOnException() {
        OptionalInt result = ExceptionUtil.getOptionalInt(() -> {
            throw new RuntimeException("fail");
        }, LOG, "OptionalInt op failed: {}");
        assertThat(result.isPresent(), is(false));
    }

    // -- getOptionalLong --

    @Test
    void testGetOptionalLongReturnsValue() {
        OptionalLong result = ExceptionUtil.getOptionalLong(() -> 99L, LOG, "msg");
        assertThat(result.isPresent(), is(true));
        assertThat(result.getAsLong(), is(99L));
    }

    @Test
    void testGetOptionalLongReturnsEmptyOnException() {
        OptionalLong result = ExceptionUtil.getOptionalLong(() -> {
            throw new RuntimeException("fail");
        }, LOG, "OptionalLong op failed: {}");
        assertThat(result.isPresent(), is(false));
    }

    // -- runSilently --

    @Test
    void testRunSilentlyExecutes() {
        List<String> evidence = new ArrayList<>();
        ExceptionUtil.runSilently(() -> evidence.add("executed"));
        assertThat(evidence.size(), is(1));
    }

    @Test
    void testRunSilentlySwallowsException() {
        // Should not throw
        ExceptionUtil.runSilently(() -> {
            throw new RuntimeException("fail");
        });
    }

    @Test
    void testRunSilentlySwallowsError() {
        // Should not throw
        ExceptionUtil.runSilently(() -> {
            throw new StackOverflowError("simulated");
        });
    }

    // -- runOrLog --

    @Test
    void testRunOrLogExecutes() {
        List<String> evidence = new ArrayList<>();
        ExceptionUtil.runOrLog(() -> evidence.add("executed"), LOG, "msg");
        assertThat(evidence.size(), is(1));
    }

    @Test
    void testRunOrLogHandlesException() {
        // Should not throw
        ExceptionUtil.runOrLog(() -> {
            throw new RuntimeException("test error");
        }, LOG, "RunOrLog failed: {}");
    }

    // -- Edge cases --

    @Test
    void testGetOrDefaultWithCollectionDefault() {
        List<String> result = ExceptionUtil.getOrDefault(() -> {
            throw new RuntimeException("fail");
        }, Collections.emptyList());
        assertThat(result, is(empty()));
    }

    // -- Level-taking overloads --

    @Test
    void testGetOrDefaultWithTraceLevel() {
        String result = ExceptionUtil.getOrDefault(() -> {
            throw new RuntimeException("trace test");
        }, "fallback", LOG, Level.TRACE, "Trace-level failure: {}");
        assertThat(result, is(equalTo("fallback")));
    }

    @Test
    void testGetIntOrDefaultWithTraceLevel() {
        int result = ExceptionUtil.getIntOrDefault(() -> {
            throw new RuntimeException("fail");
        }, -1, LOG, Level.TRACE, "Int trace failure");
        assertThat(result, is(-1));
    }

    @Test
    void testGetLongOrDefaultWithTraceLevel() {
        long result = ExceptionUtil.getLongOrDefault(() -> {
            throw new RuntimeException("fail");
        }, -1L, LOG, Level.TRACE, "Long trace failure");
        assertThat(result, is(-1L));
    }

    @Test
    void testGetBooleanOrDefaultWithTraceLevel() {
        boolean result = ExceptionUtil.getBooleanOrDefault(() -> {
            throw new RuntimeException("fail");
        }, true, LOG, Level.TRACE, "Bool trace failure");
        assertThat(result, is(true));
    }

    @Test
    void testGetDoubleOrDefaultWithTraceLevel() {
        double result = ExceptionUtil.getDoubleOrDefault(() -> {
            throw new RuntimeException("fail");
        }, -1d, LOG, Level.TRACE, "Double trace failure");
        assertThat(result, is(-1d));
    }

    @Test
    void testGetOptionalWithTraceLevel() {
        Optional<String> result = ExceptionUtil.getOptional(() -> {
            throw new RuntimeException("fail");
        }, LOG, Level.TRACE, "Optional trace failure");
        assertThat(result.isPresent(), is(false));
    }

    @Test
    void testGetOptionalIntWithTraceLevel() {
        OptionalInt result = ExceptionUtil.getOptionalInt(() -> {
            throw new RuntimeException("fail");
        }, LOG, Level.TRACE, "OptionalInt trace failure");
        assertThat(result.isPresent(), is(false));
    }

    @Test
    void testGetOptionalLongWithTraceLevel() {
        OptionalLong result = ExceptionUtil.getOptionalLong(() -> {
            throw new RuntimeException("fail");
        }, LOG, Level.TRACE, "OptionalLong trace failure");
        assertThat(result.isPresent(), is(false));
    }

    @Test
    void testRunOrLogWithTraceLevel() {
        // Should not throw
        ExceptionUtil.runOrLog(() -> {
            throw new RuntimeException("fail");
        }, LOG, Level.TRACE, "Runnable trace failure");
    }

    @Test
    void testGetOrDefaultWithWarnLevelSuccessPath() {
        // Non-throwing path with an exotic level — verifies the level argument doesn't disturb success behavior.
        String result = ExceptionUtil.getOrDefault(() -> "ok", "fallback", LOG, Level.WARN, "Should not log");
        assertThat(result, is(equalTo("ok")));
    }
}
