/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link LogUtil}.
 */
class LogUtilTest {

    private static final Logger LOG = LoggerFactory.getLogger(LogUtilTest.class);

    /**
     * A {@link Logger} proxy which records the name and arguments of each logging method invoked on it. Used in place
     * of SLF4J's own test doubles, which expose {@code org.slf4j.event.Level} and are therefore off limits here.
     */
    static final class RecordingLogger implements InvocationHandler {
        private final List<String> calls = new ArrayList<>();
        private Object @Nullable [] lastArgs;
        private boolean enabled = true;

        static RecordingLogger create() {
            return new RecordingLogger();
        }

        /**
         * Makes every {@code isXEnabled()} check answer false, as a logger with the level turned off would.
         *
         * @return this recorder
         */
        RecordingLogger disabled() {
            this.enabled = false;
            return this;
        }

        Logger logger() {
            return (Logger) Proxy.newProxyInstance(Logger.class.getClassLoader(), new Class<?>[] { Logger.class },
                    this);
        }

        @Override
        public @Nullable Object invoke(Object proxy, Method method, Object @Nullable [] args) {
            calls.add(method.getName());
            if (boolean.class.equals(method.getReturnType())) {
                return enabled;
            }
            lastArgs = args;
            return null;
        }

        List<String> calls() {
            return calls;
        }

        String onlyCall() {
            assertThat("Expected exactly one logging call", calls.size(), is(1));
            return calls.get(0);
        }

        String message() {
            assertNotNull(lastArgs, "no logging call was recorded");
            return (String) lastArgs[0];
        }

        Object[] arguments() {
            assertNotNull(lastArgs, "no logging call was recorded");
            return (Object[]) lastArgs[1];
        }
    }

    /**
     * Maps a level to the {@link Logger} enabled-check expected to answer for it, e.g. WARN to isWarnEnabled.
     *
     * @param level the level
     * @return the expected method name
     */
    private static String enabledCheckFor(LogLevel level) {
        String name = level.name();
        return "is" + name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT) + "Enabled";
    }

    @Test
    void testIsEnabledQueriesMatchingCheck() {
        for (LogLevel level : LogLevel.values()) {
            RecordingLogger recorder = RecordingLogger.create();
            assertThat("Reports the logger's answer", LogUtil.isEnabled(recorder.logger(), level), is(true));
            assertThat("Queried the check matching the level", recorder.onlyCall(), is(enabledCheckFor(level)));
        }
    }

    @Test
    void testIsEnabledReportsDisabledLevel() {
        for (LogLevel level : LogLevel.values()) {
            RecordingLogger recorder = RecordingLogger.create().disabled();
            assertThat("Reports the logger's answer", LogUtil.isEnabled(recorder.logger(), level), is(false));
            assertThat("Queried the check matching the level", recorder.onlyCall(), is(enabledCheckFor(level)));
        }
    }

    @Test
    void testLogAtEveryLevelWithRealLogger() {
        for (LogLevel level : LogLevel.values()) {
            assertDoesNotThrow(() -> LogUtil.logAtLevel(LOG, level, "Testing {} at {}", "message", level));
        }
    }

    @Test
    void testLogAtLevelDispatchesToMatchingMethod() {
        for (LogLevel level : LogLevel.values()) {
            RecordingLogger recorder = RecordingLogger.create();
            LogUtil.logAtLevel(recorder.logger(), level, "Value is {}", 42);
            assertThat("Dispatched to the method matching the level", recorder.onlyCall(),
                    is(level.name().toLowerCase(Locale.ROOT)));
            assertThat("Message left unformatted for the logging backend", recorder.message(), is("Value is {}"));
            assertThat("Argument passed through for deferred substitution", recorder.arguments(),
                    arrayContaining((Object) 42));
        }
    }

    @Test
    void testLogAtLevelWithNoArguments() {
        RecordingLogger recorder = RecordingLogger.create();
        LogUtil.logAtLevel(recorder.logger(), LogLevel.INFO, "No placeholders here");
        assertThat("Dispatched to info", recorder.onlyCall(), is("info"));
        assertThat("Message passed through", recorder.message(), is("No placeholders here"));
        assertThat("No arguments passed", recorder.arguments(), is(emptyArray()));
    }

    @Test
    void testLogAtLevelPassesMultipleArgumentsInOrder() {
        RecordingLogger recorder = RecordingLogger.create();
        LogUtil.logAtLevel(recorder.logger(), LogLevel.ERROR, "{} then {}", "first", "second");
        assertThat("Dispatched to error", recorder.onlyCall(), is("error"));
        assertThat("Arguments passed through in order", recorder.arguments(),
                arrayContaining((Object) "first", "second"));
    }
}
