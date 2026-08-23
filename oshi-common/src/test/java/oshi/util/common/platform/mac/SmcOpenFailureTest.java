/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

/**
 * Tests that each reason an SMC connection could not be opened is reported once and then demoted to debug, so that
 * polling sensors on a machine with no SMC does not repeat the message on every reading.
 */
class SmcOpenFailureTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\}");

    /**
     * Records the name and formatted text of every logging call. The {@code isXxxEnabled} guards answer with whatever
     * the recorder was told to report, so a test can check what is skipped when a level is off.
     */
    private static final class RecordingLogger implements InvocationHandler {
        private final List<String> calls = new ArrayList<>();
        private final List<String> messages = new ArrayList<>();
        private final boolean levelsEnabled;

        private RecordingLogger(boolean levelsEnabled) {
            this.levelsEnabled = levelsEnabled;
        }

        @Override
        public @Nullable Object invoke(Object proxy, Method method, Object @Nullable [] args) {
            String name = method.getName();
            if (name.startsWith("is") && name.endsWith("Enabled")) {
                return levelsEnabled;
            }
            calls.add(name);
            messages.add(format(args));
            return null;
        }

        /**
         * Substitutes the arguments into the format string the way SLF4J would. The varargs overloads arrive through
         * the proxy as {@code {format, Object[]}}, so the substitutions have to be unwrapped from the second argument.
         *
         * @param args the arguments the proxy was invoked with
         * @return the formatted message
         */
        private static String format(Object @Nullable [] args) {
            if (args == null || args.length == 0) {
                return "";
            }
            String message = String.valueOf(args[0]);
            if (args.length > 1 && args[1] instanceof Object[]) {
                for (Object substitution : (Object[]) args[1]) {
                    message = PLACEHOLDER.matcher(message)
                            .replaceFirst(Matcher.quoteReplacement(String.valueOf(substitution)));
                }
            }
            return message;
        }

        private Logger logger() {
            return (Logger) Proxy.newProxyInstance(Logger.class.getClassLoader(), new Class<?>[] { Logger.class },
                    this);
        }
    }

    private static RecordingLogger recorder() {
        return new RecordingLogger(true);
    }

    @Test
    void testServiceNotFoundWarnsOnceThenDebugs() {
        RecordingLogger recorder = recorder();
        SmcOpenFailure failure = new SmcOpenFailure(recorder.logger());
        failure.serviceNotFound();
        failure.serviceNotFound();
        failure.serviceNotFound();
        assertThat(recorder.calls, contains("warn", "debug", "debug"));
    }

    @Test
    void testOpenFailedErrorsOnceThenDebugs() {
        RecordingLogger recorder = recorder();
        SmcOpenFailure failure = new SmcOpenFailure(recorder.logger());
        failure.openFailed(0xe00002c2);
        failure.openFailed(0xe00002c2);
        assertThat(recorder.calls, contains("error", "debug"));
    }

    @Test
    void testNullConnectionErrorsOnceThenDebugs() {
        RecordingLogger recorder = recorder();
        SmcOpenFailure failure = new SmcOpenFailure(recorder.logger());
        failure.nullConnection();
        failure.nullConnection();
        assertThat(recorder.calls, contains("error", "debug"));
    }

    @Test
    void testConditionsLatchIndependently() {
        RecordingLogger recorder = recorder();
        SmcOpenFailure failure = new SmcOpenFailure(recorder.logger());
        failure.serviceNotFound();
        failure.openFailed(1);
        failure.nullConnection();
        assertThat("Each condition gets its own first report", recorder.calls, contains("warn", "error", "error"));
    }

    @Test
    void testEachInstanceLatchesSeparately() {
        RecordingLogger first = recorder();
        RecordingLogger second = recorder();
        new SmcOpenFailure(first.logger()).serviceNotFound();
        new SmcOpenFailure(second.logger()).serviceNotFound();
        assertThat(first.calls, contains("warn"));
        assertThat("A second backend reports under its own logger", second.calls, contains("warn"));
    }

    @Test
    void testServiceNotFoundExplainsTheCommonCause() {
        RecordingLogger recorder = recorder();
        new SmcOpenFailure(recorder.logger()).serviceNotFound();
        assertThat(recorder.messages.get(0), containsString("virtual machine"));
    }

    @Test
    void testOpenFailedReportsTheErrorInHex() {
        RecordingLogger recorder = recorder();
        new SmcOpenFailure(recorder.logger()).openFailed(0xe00002c2);
        assertThat(recorder.messages.get(0), containsString("0xe00002c2"));
    }

    @Test
    void testHexIsNotFormattedWhenTheLevelIsOff() {
        RecordingLogger recorder = new RecordingLogger(false);
        new SmcOpenFailure(recorder.logger()).openFailed(1);
        assertThat("The guard skips the logging call entirely", recorder.calls, is(empty()));
    }
}
