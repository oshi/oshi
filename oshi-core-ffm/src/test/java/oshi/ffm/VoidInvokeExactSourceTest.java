/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Source-policy guard for a footgun that has already regressed twice (issues #3301 / #3422): a <em>void</em>
 * {@link java.lang.invoke.MethodHandle#invokeExact} invoked from an expression-bodied lambda passed to a void-returning
 * helper such as {@code ExceptionUtil.runOrLog}.
 * <p>
 * {@code invokeExact} is signature-polymorphic: its return type is inferred from the call site. In an expression lambda
 * targeting a void functional interface, javac infers {@code Object} instead of {@code void}, so the call site
 * ({@code (X)Object}) does not match the handle ({@code (X)void}) and throws {@code WrongMethodTypeException} at
 * runtime. The failure only surfaces on the native platform and is swallowed by the logging helper, so a normal unit
 * test can't see it. A block lambda ({@code () -> { handle.invokeExact(...); }}) keeps the call in a statement context
 * where the return type is correctly {@code void}.
 * <p>
 * Scanning the sources catches the regression on every build (not only on the platform where the call runs), which
 * matters because the fix keeps getting lost whenever the FFM code is moved between packages.
 */
class VoidInvokeExactSourceTest {

    // run(OrLog|Silently)(() -> ... invokeExact with no '{' before invokeExact is an expression lambda -- the bug.
    private static final Pattern EXPRESSION_LAMBDA_INVOKE_EXACT = Pattern
            .compile("run(?:OrLog|Silently)\\(\\(\\)\\s*->[^{]*invokeExact");

    @Test
    void noVoidInvokeExactInExpressionLambda() throws IOException {
        Path srcRoot = Path.of("src", "main", "java");
        assumeTrue(Files.isDirectory(srcRoot), "FFM source tree not on the working directory; skipping source scan");

        List<String> offenders = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(srcRoot)) {
            paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                    for (int i = 0; i < lines.size(); i++) {
                        if (EXPRESSION_LAMBDA_INVOKE_EXACT.matcher(lines.get(i)).find()) {
                            offenders.add(p + ":" + (i + 1) + "  " + lines.get(i).strip());
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        assertThat("void invokeExact in an expression lambda infers Object and throws WrongMethodTypeException at "
                + "runtime; use a block lambda instead: () -> { handle.invokeExact(...); }. Offenders: " + offenders,
                offenders, is(empty()));
    }
}
