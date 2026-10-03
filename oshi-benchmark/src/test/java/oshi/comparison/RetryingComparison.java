/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junitpioneer.jupiter.RetryingTest;

/**
 * Runs a comparison test up to three times, 50 ms apart, retrying only on a failed assertion.
 * <p>
 * The comparisons take one snapshot from each implementation in sequence, so a value that moves between the two reads
 * can push an otherwise correct pair past its bound. Retrying lets those bounds sit close to the deviation actually
 * measured rather than far enough out to absorb a rare race: a check that fails one run in twenty fails about one in
 * eight thousand with three attempts, as long as each attempt fails independently. A wrong-field or wrong-unit bug
 * fails every attempt, so it is still caught. An exception other than an assertion failure is not retried.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@RetryingTest(maxAttempts = 3, suspendForMs = 50, onExceptions = AssertionError.class)
public @interface RetryingComparison {
}
