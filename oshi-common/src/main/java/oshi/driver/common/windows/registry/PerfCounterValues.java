/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.registry;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Typed lookups into the property maps produced for a performance counter object.
 * <p>
 * Every property of the counter enum is present or none of them are: {@link HkeyPerformanceDataUtil#getCounterIndices}
 * returns {@code null} unless it resolved an index for each one, and the perf-counter path fills its value map from the
 * same all-or-nothing query. A missing key therefore means the data was assembled inconsistently rather than that a
 * counter is optional, so these throw rather than substitute a value, naming the property instead of failing later with
 * a bare {@code NullPointerException}.
 */
@ThreadSafe
final class PerfCounterValues {

    private PerfCounterValues() {
    }

    private static <T extends Enum<T>> Object require(Map<T, ?> map, T key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalStateException("Missing performance counter value for " + key.name());
        }
        return value;
    }

    /**
     * Fetches a property whose value is stored as an {@code Integer}.
     *
     * @param <T> the counter property enum type
     * @param map the property map for one instance
     * @param key the property to fetch
     * @return the value as an {@code int}
     */
    static <T extends Enum<T>> int intValue(Map<T, Object> map, T key) {
        return ((Integer) require(map, key)).intValue();
    }

    /**
     * Fetches a property whose value is stored as a {@code Long}.
     *
     * @param <T> the counter property enum type
     * @param map the property map for one instance
     * @param key the property to fetch
     * @return the value as a {@code long}
     */
    static <T extends Enum<T>> long longValue(Map<T, Object> map, T key) {
        return ((Long) require(map, key)).longValue();
    }

    /**
     * Fetches a property whose value is stored as a {@code String}.
     *
     * @param <T> the counter property enum type
     * @param map the property map for one instance
     * @param key the property to fetch
     * @return the value as a {@code String}
     */
    static <T extends Enum<T>> String stringValue(Map<T, Object> map, T key) {
        return (String) require(map, key);
    }

    /**
     * Fetches a pointer-sized property, which the registry reports as an {@code Integer} on a 32-bit system and a
     * {@code Long} on a 64-bit one.
     *
     * @param <T> the counter property enum type
     * @param map the property map for one instance
     * @param key the property to fetch
     * @return the value widened to a {@code long}, without sign extension
     */
    static <T extends Enum<T>> long pointerValue(Map<T, Object> map, T key) {
        Object value = require(map, key);
        return value instanceof Long ? (Long) value : Integer.toUnsignedLong((Integer) value);
    }

    /**
     * Fetches the per-instance values of one counter from a performance counter query result.
     *
     * @param <T>      the counter property enum type
     * @param valueMap the query's value map
     * @param key      the property to fetch
     * @return the list of values, one per instance
     */
    static <T extends Enum<T>> List<Long> counterList(Map<T, List<Long>> valueMap, T key) {
        @SuppressWarnings("unchecked")
        List<Long> values = (List<Long>) require(valueMap, key);
        return values;
    }
}
