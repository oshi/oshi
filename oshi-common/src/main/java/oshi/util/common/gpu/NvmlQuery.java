/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.gpu;

import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Backend-independent logic shared by the JNA and FFM bindings to the NVIDIA Management Library (NVML).
 * <p>
 * Every NVML metric has the same shape: reject a blank device identifier, pair {@code nvmlInit_v2} with
 * {@code nvmlShutdown} around the query, acquire a fresh device handle inside that scope, and fall back to a sentinel
 * whenever any step fails. Only the native read differs between the backends, so the skeleton lives here and each
 * backend supplies a one-line reader.
 * <p>
 * Device handles are valid only within a single init/shutdown scope, which is why the backends exchange stable PCI bus
 * ID strings with their callers and re-acquire a handle for each query.
 */
@ThreadSafe
public final class NvmlQuery {

    private NvmlQuery() {
    }

    /**
     * A backend's NVML lifecycle and device-handle acquisition, implemented once per binding.
     *
     * @param <H> the backend's device handle type: a JNA {@code Pointer}, or an FFM handle bundled with the arena its
     *            out-parameters are allocated from
     */
    public interface NvmlScope<H> {

        /**
         * Calls {@code nvmlInit_v2}, incrementing NVML's internal reference count.
         *
         * @return true if NVML was initialized, in which case {@link #uninit()} must be called exactly once
         */
        boolean init();

        /**
         * Calls {@code nvmlShutdown}, decrementing the reference count {@link #init()} incremented.
         */
        void uninit();

        /**
         * Acquires a device handle matching the given identifier and applies {@code body} to it. Must be called while
         * NVML is initialized. Any scope the handle depends on, such as an FFM arena, is opened and closed around
         * {@code body} by the implementation, so no native resource escapes the backend.
         *
         * @param <R>      the reader's result type
         * @param deviceId the stable device identifier to match
         * @param body     the reader to apply to the acquired handle
         * @param sentinel the value to return if no matching device could be acquired
         * @return the reader's result, or {@code sentinel} if no device matched
         */
        <R> R withDevice(String deviceId, Function<H, R> body, R sentinel);
    }

    /**
     * Runs an NVML query against a device, returning the sentinel if the identifier is blank, NVML cannot be
     * initialized, no matching device can be acquired, or the read itself fails.
     *
     * @param <H>      the backend's device handle type
     * @param <R>      the query's result type
     * @param deviceId the stable device identifier
     * @param scope    the backend's NVML lifecycle
     * @param reader   reads the metric from a device handle, itself returning {@code sentinel} if the read fails
     * @param sentinel the value denoting an unavailable metric
     * @return the metric, or {@code sentinel}
     */
    public static <H, R> R query(@Nullable String deviceId, NvmlScope<H> scope, Function<H, R> reader, R sentinel) {
        if (deviceId == null || deviceId.isEmpty()) {
            return sentinel;
        }
        if (!scope.init()) {
            return sentinel;
        }
        try {
            return scope.withDevice(deviceId, reader, sentinel);
        } finally {
            scope.uninit();
        }
    }

    /**
     * Tests whether an NVML identifier matches a sought fragment, in either direction, so that a domain-qualified form
     * matches a bare one: {@code 00000000:01:00.0}, {@code 0000:01:00.0} and {@code 01:00.0} all match each other.
     * <p>
     * An empty candidate never matches. Bidirectional containment would otherwise make it match everything, because
     * every string contains the empty string, letting a device whose PCI info read back blank answer to any query.
     *
     * @param candidate the identifier read from a device, lowercased
     * @param needle    the sought fragment, lowercased
     * @return true if the two identify the same device
     */
    public static boolean matches(String candidate, String needle) {
        return !candidate.isEmpty() && (candidate.contains(needle) || needle.contains(candidate));
    }

    /**
     * Returns the enumerated bus ID matching the given fragment, preferring the most qualified form.
     * <p>
     * Both of a device's bus ID forms are enumerated, so a bare fragment such as {@code 01:00.0} matches the modern and
     * legacy entries alike. The modern form carries the full eight-digit domain and is therefore the longer of the two,
     * so returning the longest match yields it consistently, and matches what the name lookup returns. Returning the
     * first match instead would let set iteration order decide, which can differ between runs and between the two
     * bindings.
     *
     * A blank fragment matches nothing, rather than matching every enumerated ID as bidirectional containment would
     * otherwise have it do. This is checked here rather than left to {@link #matches}, whose guard covers a blank
     * candidate read from a device and not a blank fragment supplied by a caller.
     *
     * @param busIds   the enumerated PCI bus IDs, already lowercased
     * @param fragment the fragment to match
     * @return the matching bus ID, or {@code null} if none matched or the fragment was blank
     */
    public static @Nullable String matchBusId(Set<String> busIds, @Nullable String fragment) {
        if (fragment == null || fragment.isEmpty()) {
            return null;
        }
        String needle = fragment.toLowerCase(Locale.ROOT);
        String best = null;
        for (String id : busIds) {
            if (matches(id, needle) && (best == null || id.length() > best.length())) {
                best = id;
            }
        }
        return best;
    }
}
