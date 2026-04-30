/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared assertion helpers for JNA vs FFM comparison tests.
 */
final class ComparisonAssertions {

    private ComparisonAssertions() {
    }

    /**
     * Asserts that two {@code long} values are within the given ratio of each other.
     *
     * @param actual      the actual value
     * @param expected    the expected (baseline) value
     * @param ratio       the maximum allowed deviation (0.0–1.0)
     * @param description a label for assertion failure messages
     */
    static void assertWithinRatio(long actual, long expected, double ratio, String description) {
        assertWithinRatio((double) actual, (double) expected, ratio, description);
    }

    /**
     * Asserts that two {@code double} values are within the given ratio of each other.
     *
     * @param actual      the actual value
     * @param expected    the expected (baseline) value
     * @param ratio       the maximum allowed deviation (0.0–1.0)
     * @param description a label for assertion failure messages
     */
    static void assertWithinRatio(double actual, double expected, double ratio, String description) {
        if (expected == 0 && actual == 0) {
            return;
        }
        if (expected == 0 || actual == 0) {
            double nonZero = Math.max(Math.abs(expected), Math.abs(actual));
            assertThat(nonZero).as("%s: one value is 0, other is %.2f", description, nonZero)
                    .isLessThanOrEqualTo(ratio);
            return;
        }
        double min = Math.min(Math.abs(actual), Math.abs(expected));
        double max = Math.max(Math.abs(actual), Math.abs(expected));
        assertThat(min / max).as("%s: expected=%f, actual=%f", description, expected, actual)
                .isGreaterThanOrEqualTo(1.0 - ratio);
    }
}
