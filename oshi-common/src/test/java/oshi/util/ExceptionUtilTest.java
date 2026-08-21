/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.WrongMethodTypeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        }, "fallback", LOG, "Operation failed");
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
        }, 0, LOG, "Int op failed");
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
        }, 0L, LOG, "Long op failed");
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
        }, true, LOG, "Bool op failed");
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
        }, -1d, LOG, "Double op failed");
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
        }, LOG, "Optional op failed");
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
        }, LOG, "OptionalInt op failed");
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
        }, LOG, "OptionalLong op failed");
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
        assertDoesNotThrow(() -> ExceptionUtil.runSilently(() -> {
            throw new RuntimeException("fail");
        }));
    }

    @Test
    void testRunSilentlySwallowsError() {
        assertDoesNotThrow(() -> ExceptionUtil.runSilently(() -> {
            throw new StackOverflowError("simulated");
        }));
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
        assertDoesNotThrow(() -> ExceptionUtil.runOrLog(() -> {
            throw new RuntimeException("test error");
        }, LOG, "RunOrLog failed"));
    }

    // -- Edge cases --

    @Test
    void testGetOrDefaultWithCollectionDefault() {
        List<String> result = ExceptionUtil.getOrDefault(() -> {
            throw new RuntimeException("fail");
        }, Collections.emptyList());
        assertThat(result, is(empty()));
    }

    // -- LogLevel-taking overloads --

    @Test
    void testGetOrDefaultWithTraceLevel() {
        String result = ExceptionUtil.getOrDefault(() -> {
            throw new RuntimeException("trace test");
        }, "fallback", LOG, LogLevel.TRACE, "Trace-level failure");
        assertThat(result, is(equalTo("fallback")));
    }

    @Test
    void testGetIntOrDefaultWithTraceLevel() {
        int result = ExceptionUtil.getIntOrDefault(() -> {
            throw new RuntimeException("fail");
        }, -1, LOG, LogLevel.TRACE, "Int trace failure");
        assertThat(result, is(-1));
    }

    @Test
    void testGetLongOrDefaultWithTraceLevel() {
        long result = ExceptionUtil.getLongOrDefault(() -> {
            throw new RuntimeException("fail");
        }, -1L, LOG, LogLevel.TRACE, "Long trace failure");
        assertThat(result, is(-1L));
    }

    @Test
    void testGetBooleanOrDefaultWithTraceLevel() {
        boolean result = ExceptionUtil.getBooleanOrDefault(() -> {
            throw new RuntimeException("fail");
        }, true, LOG, LogLevel.TRACE, "Bool trace failure");
        assertThat(result, is(true));
    }

    @Test
    void testGetDoubleOrDefaultWithTraceLevel() {
        double result = ExceptionUtil.getDoubleOrDefault(() -> {
            throw new RuntimeException("fail");
        }, -1d, LOG, LogLevel.TRACE, "Double trace failure");
        assertThat(result, is(-1d));
    }

    @Test
    void testGetOptionalWithTraceLevel() {
        Optional<String> result = ExceptionUtil.getOptional(() -> {
            throw new RuntimeException("fail");
        }, LOG, LogLevel.TRACE, "Optional trace failure");
        assertThat(result.isPresent(), is(false));
    }

    @Test
    void testGetOptionalIntWithTraceLevel() {
        OptionalInt result = ExceptionUtil.getOptionalInt(() -> {
            throw new RuntimeException("fail");
        }, LOG, LogLevel.TRACE, "OptionalInt trace failure");
        assertThat(result.isPresent(), is(false));
    }

    @Test
    void testGetOptionalLongWithTraceLevel() {
        OptionalLong result = ExceptionUtil.getOptionalLong(() -> {
            throw new RuntimeException("fail");
        }, LOG, LogLevel.TRACE, "OptionalLong trace failure");
        assertThat(result.isPresent(), is(false));
    }

    @Test
    void testRunOrLogWithTraceLevel() {
        assertDoesNotThrow(() -> ExceptionUtil.runOrLog(() -> {
            throw new RuntimeException("fail");
        }, LOG, LogLevel.TRACE, "Runnable trace failure"));
    }

    @Test
    void testGetOrDefaultWithWarnLevelSuccessPath() {
        // Non-throwing path with an exotic level — verifies the level argument doesn't disturb success behavior.
        String result = ExceptionUtil.getOrDefault(() -> "ok", "fallback", LOG, LogLevel.WARN, "Should not log");
        assertThat(result, is(equalTo("ok")));
    }

    @Test
    void testLogsAndReturnsDefaultAtEveryLevel() {
        for (LogLevel level : LogLevel.values()) {
            String result = ExceptionUtil.getOrDefault(() -> {
                // Intentional: exercises the exception-logging path at each level. The stack trace this logs is
                // expected test output, not a real failure.
                throw new IllegalStateException("expected test exception, stack trace is intentional");
            }, "no exception", LOG, level, "failed");
            assertThat(result, is("no exception"));
        }
    }

    @Test
    void testLogAtLevelPassesOnlyTheThrowable() {
        LogUtilTest.RecordingLogger recorder = LogUtilTest.RecordingLogger.create();
        Throwable t = new IllegalStateException("boom");
        ExceptionUtil.logAtLevel(recorder.logger(), LogLevel.WARN, "Failed", t);
        assertThat("Dispatched to the method matching the level", recorder.onlyCall(), is("warn"));
        // Only the throwable is appended, for SLF4J to pull out as the cause. Its message is not passed
        // separately: doing so would print it inline and again in the stack trace the backend renders.
        assertThat("Throwable only", recorder.arguments(), arrayContaining(t));
    }

    @Test
    void testLogAtLevelPutsCallerArgumentsBeforeTheThrowable() {
        LogUtilTest.RecordingLogger recorder = LogUtilTest.RecordingLogger.create();
        Throwable t = new IllegalStateException("boom");
        ExceptionUtil.logAtLevel(recorder.logger(), LogLevel.DEBUG, "Failed to read {} of {}", t, "key", 42);
        assertThat("Dispatched to the method matching the level", recorder.onlyCall(), is("debug"));
        // Caller arguments fill the placeholders in order, then the throwable as the cause
        assertThat("Caller args then throwable", recorder.arguments(), arrayContaining("key", 42, t));
    }

    @Test
    void testLogAtLevelAcceptsATypedArgumentArray() {
        LogUtilTest.RecordingLogger recorder = LogUtilTest.RecordingLogger.create();
        Throwable t = new IllegalStateException("boom");
        // Passing a typed array for the varargs parameter is legal Java, so the copy must widen to Object[]:
        // a copy keeping the String[] component type would throw ArrayStoreException on appending the throwable.
        // The cast only tells javac the array is meant to be spread; at runtime this is still a String[].
        String[] args = new String[] { "key", "value" };
        ExceptionUtil.logAtLevel(recorder.logger(), LogLevel.WARN, "Failed to read {} of {}", t, (Object[]) args);
        assertThat("Caller args then throwable", recorder.arguments(), arrayContaining("key", "value", t));
    }

    @Test
    void testGetOrDefaultForwardsArgumentsOnFailure() {
        LogUtilTest.RecordingLogger recorder = LogUtilTest.RecordingLogger.create();
        String result = ExceptionUtil.getOrDefault(() -> {
            throw new IllegalStateException("boom");
        }, "fallback", recorder.logger(), "Lookup of {} failed", "someKey");
        assertThat("Returns the default", result, is("fallback"));
        assertThat("Defaults to debug", recorder.onlyCall(), is("debug"));
        assertThat("Argument precedes the throwable", recorder.arguments(),
                arrayContaining(is("someKey"), instanceOf(IllegalStateException.class)));
    }

    @Test
    void testGetLongOrDefaultForwardsArgumentsAtExplicitLevel() {
        LogUtilTest.RecordingLogger recorder = LogUtilTest.RecordingLogger.create();
        long result = ExceptionUtil.getLongOrDefault(() -> {
            throw new IllegalStateException("boom");
        }, -1L, recorder.logger(), LogLevel.WARN, "Read of {}/{} failed", "path", "value");
        assertThat("Returns the default", result, is(-1L));
        assertThat("Honors the explicit level", recorder.onlyCall(), is("warn"));
        assertThat("Both arguments precede the throwable", recorder.arguments(),
                arrayContaining(is("path"), is("value"), instanceOf(IllegalStateException.class)));
    }

    @Test
    void testNoArgumentCallsAreUnchanged() {
        LogUtilTest.RecordingLogger recorder = LogUtilTest.RecordingLogger.create();
        String result = ExceptionUtil.getOrDefault(() -> {
            throw new IllegalStateException("boom");
        }, "fallback", recorder.logger(), "Failed");
        assertThat("Returns the default", result, is("fallback"));
        // Adding the varargs parameter must not change what an existing zero-argument call logs
        assertThat("Throwable only", recorder.arguments(), arrayContaining(instanceOf(IllegalStateException.class)));
    }

    @Test
    void testSuccessfulCallLogsNothing() {
        LogUtilTest.RecordingLogger recorder = LogUtilTest.RecordingLogger.create();
        String result = ExceptionUtil.getOrDefault(() -> "ok", "fallback", recorder.logger(), "Failed for {}", "key");
        assertThat("Returns the supplied value", result, is("ok"));
        assertThat("Nothing logged on the success path", recorder.calls(), is(empty()));
    }

    // -- WrongMethodTypeException is a binding defect, not missing data --

    @Test
    void testSignatureMismatchIsLoggedAtErrorAndRethrown() {
        LogUtilTest.RecordingLogger recorder = LogUtilTest.RecordingLogger.create();
        WrongMethodTypeException thrown = assertThrows(WrongMethodTypeException.class,
                () -> ExceptionUtil.getIntOrDefault(() -> {
                    throw new WrongMethodTypeException("handle's method type (int)int but found (int)void");
                }, 0, recorder.logger(), "Sizing call failed"));
        assertThat("Rethrown as-is, so the trace still points at the call site", thrown.getMessage(),
                containsString("(int)void"));
        assertThat("Escalated past the caller's debug level", recorder.onlyCall(), is("error"));
        assertThat("Keeps the caller's message", recorder.message(), startsWith("Sizing call failed"));
        assertThat("Names it as an OSHI defect", recorder.message(), containsString("defect in OSHI"));
        assertThat("Throwable still attached", recorder.arguments(),
                arrayContaining(instanceOf(WrongMethodTypeException.class)));
    }

    @Test
    void testSignatureMismatchOutranksAnExplicitLevel() {
        LogUtilTest.RecordingLogger recorder = LogUtilTest.RecordingLogger.create();
        assertThrows(WrongMethodTypeException.class, () -> ExceptionUtil.runOrLog(() -> {
            throw new WrongMethodTypeException("mismatch");
        }, recorder.logger(), LogLevel.TRACE, "Release failed"));
        assertThat("A defect outranks the level the caller asked for", recorder.onlyCall(), is("error"));
    }

    @Test
    void testOrdinaryFailureKeepsCallersLevelAndMessage() {
        LogUtilTest.RecordingLogger recorder = LogUtilTest.RecordingLogger.create();
        int result = ExceptionUtil.getIntOrDefault(() -> {
            throw new IllegalStateException("boom");
        }, 0, recorder.logger(), "Sizing call failed");
        assertThat("Still returns the default", result, is(0));
        assertThat("A failed system call stays at debug", recorder.onlyCall(), is("debug"));
        assertThat("Message is untouched", recorder.message(), is("Sizing call failed"));
    }

    @Test
    void testSilentOverloadsAlsoRethrowASignatureMismatch() {
        // The wrappers with no logger of their own are silent about a failed system call, but not about a defect
        assertThrows(WrongMethodTypeException.class, () -> ExceptionUtil.getOrDefault(() -> {
            throw new WrongMethodTypeException("mismatch");
        }, "fallback"));
        assertThrows(WrongMethodTypeException.class, () -> ExceptionUtil.runSilently(() -> {
            throw new WrongMethodTypeException("mismatch");
        }));
    }

    @Test
    void testNestedWrappersPropagateTheSameSignatureMismatch() {
        // The inner wrapper's rethrow is caught by the outer one, which must not turn it back into a default value
        WrongMethodTypeException thrown = assertThrows(WrongMethodTypeException.class,
                () -> ExceptionUtil.getOrDefault(() -> ExceptionUtil.getIntOrDefault(() -> {
                    throw new WrongMethodTypeException("mismatch");
                }, 0, LOG, "inner"), -1, LOG, "outer"));
        assertThat("The original exception, not a wrapper", thrown.getMessage(), is("mismatch"));
    }
}
