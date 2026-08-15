/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.gpu;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.util.common.gpu.NvmlQuery.NvmlScope;

/**
 * Tests the NVML logic shared by the JNA and FFM bindings, without an NVIDIA GPU or either native library. This is why
 * the logic lives here rather than in the bindings, whose native library loading makes them unusable in a unit test.
 */
class NvmlQueryTest {

    /** A scope that records its lifecycle calls and hands out a fixed handle. */
    private static final class FakeScope implements NvmlScope<String> {
        private final @Nullable String handle;
        private final boolean initSucceeds;
        private int inits;
        private int uninits;

        FakeScope(@Nullable String handle, boolean initSucceeds) {
            this.handle = handle;
            this.initSucceeds = initSucceeds;
        }

        @Override
        public boolean init() {
            inits++;
            return initSucceeds;
        }

        @Override
        public void uninit() {
            uninits++;
        }

        @Override
        public <R> R withDevice(String deviceId, Function<String, R> body, R sentinel) {
            return handle == null ? sentinel : body.apply(handle);
        }
    }

    @Test
    void testQueryReadsThroughAndBalancesInit() {
        FakeScope scope = new FakeScope("device", true);
        double value = NvmlQuery.query("0000:01:00.0", scope, handle -> 42d, -1d);
        assertThat("reader result is returned", value, is(42d));
        assertThat("init called once", scope.inits, is(1));
        assertThat("uninit paired with init", scope.uninits, is(1));
    }

    @Test
    void testQueryRejectsBlankDeviceIdWithoutInitializing() {
        for (String blank : new String[] { null, "" }) {
            FakeScope scope = new FakeScope("device", true);
            assertThat("blank id yields the sentinel", NvmlQuery.query(blank, scope, handle -> 42d, -1d), is(-1d));
            assertThat("NVML is never initialized for a blank id", scope.inits, is(0));
            assertThat("nothing to shut down", scope.uninits, is(0));
        }
    }

    @Test
    void testQueryReturnsSentinelWhenInitFails() {
        FakeScope scope = new FakeScope("device", false);
        assertThat("failed init yields the sentinel", NvmlQuery.query("id", scope, handle -> 42d, -1d), is(-1d));
        // A failed nvmlInit_v2 did not increment NVML's reference count, so it must not be decremented.
        assertThat("uninit is not called after a failed init", scope.uninits, is(0));
    }

    @Test
    void testQueryReturnsSentinelWhenDeviceNotFound() {
        FakeScope scope = new FakeScope(null, true);
        assertThat("missing device yields the sentinel", NvmlQuery.query("id", scope, handle -> 42d, -1d), is(-1d));
        assertThat("init is still balanced", scope.uninits, is(1));
    }

    @Test
    void testQueryUninitsWhenTheReaderThrows() {
        FakeScope scope = new FakeScope("device", true);
        try {
            NvmlQuery.query("id", scope, handle -> {
                throw new IllegalStateException("native read blew up");
            }, -1d);
        } catch (IllegalStateException expected) {
            // The reference count must still be released on the exceptional path.
        }
        assertThat("uninit runs even when the reader throws", scope.uninits, is(1));
    }

    @Test
    void testMatchesIsBidirectional() {
        assertThat("qualified matches bare", NvmlQuery.matches("00000000:01:00.0", "01:00.0"), is(true));
        assertThat("bare matches qualified", NvmlQuery.matches("01:00.0", "00000000:01:00.0"), is(true));
        assertThat("legacy matches modern", NvmlQuery.matches("0000:01:00.0", "00000000:01:00.0"), is(true));
        assertThat("identical matches", NvmlQuery.matches("0000:01:00.0", "0000:01:00.0"), is(true));
        assertThat("different devices do not match", NvmlQuery.matches("0000:01:00.0", "0000:02:00.0"), is(false));
    }

    @Test
    void testEmptyCandidateNeverMatches() {
        // Bidirectional containment would otherwise make a blank PCI read answer to every query, since every string
        // contains the empty string.
        assertThat("blank candidate does not match", NvmlQuery.matches("", "0000:01:00.0"), is(false));
    }

    @Test
    void testMatchBusIdReturnsTheEnumeratedForm() {
        Set<String> busIds = new LinkedHashSet<>(Arrays.asList("00000000:0a:00.0", "00000000:02:00.0"));
        assertThat("returns the canonical enumerated id", NvmlQuery.matchBusId(busIds, "02:00.0"),
                is("00000000:02:00.0"));
        assertThat("is case-insensitive", NvmlQuery.matchBusId(busIds, "0A:00.0"), is("00000000:0a:00.0"));
        assertThat("no match yields null", NvmlQuery.matchBusId(busIds, "09:00.0"), is(nullValue()));
        assertThat("empty set yields null", NvmlQuery.matchBusId(Collections.emptySet(), "01:00.0"), is(nullValue()));
    }

    @Test
    void testMatchBusIdRejectsABlankFragment() {
        // Every string contains the empty string, so without an explicit guard a blank fragment would match every
        // enumerated id and resolve to the longest of them.
        Set<String> busIds = new LinkedHashSet<>(Arrays.asList("0000:01:00.0", "00000000:01:00.0"));
        assertThat("an empty fragment matches nothing", NvmlQuery.matchBusId(busIds, ""), is(nullValue()));
        assertThat("a null fragment matches nothing", NvmlQuery.matchBusId(busIds, null), is(nullValue()));
    }

    @Test
    void testMatchBusIdPrefersTheQualifiedForm() {
        // Both of a device's forms are enumerated and a bare fragment matches both, so the answer must not depend on
        // which one iteration happens to reach first.
        Set<String> legacyFirst = new LinkedHashSet<>(Arrays.asList("0000:01:00.0", "00000000:01:00.0"));
        Set<String> modernFirst = new LinkedHashSet<>(Arrays.asList("00000000:01:00.0", "0000:01:00.0"));
        assertThat("the domain-qualified form wins", NvmlQuery.matchBusId(legacyFirst, "01:00.0"),
                is("00000000:01:00.0"));
        assertThat("regardless of iteration order", NvmlQuery.matchBusId(modernFirst, "01:00.0"),
                is("00000000:01:00.0"));
    }

    @Test
    void testDeviceCacheRetriesUntilEnumerationSucceeds() {
        NvmlDeviceCache cache = new NvmlDeviceCache("test");
        int[] calls = new int[1];
        Set<String> ids = new HashSet<>(Collections.singletonList("00000000:01:00.0"));

        calls[0] = 0;
        assertThat("a failed enumeration is not cached", cache.get(() -> {
            calls[0]++;
            return null;
        }), is(Collections.<String>emptySet()));

        assertThat("the failure is retried", cache.get(() -> {
            calls[0]++;
            return ids;
        }), is(ids));
        assertThat("both attempts ran", calls[0], is(2));

        assertThat("success is cached", cache.get(() -> {
            calls[0]++;
            return Collections.<String>emptySet();
        }), is(ids));
        assertThat("the enumerator is not called again", calls[0], is(2));
    }

    @Test
    void testDeviceCacheCachesAGenuinelyEmptyResult() {
        // An empty set means "no NVIDIA hardware", which is a real answer and must not be retried forever.
        NvmlDeviceCache cache = new NvmlDeviceCache("test");
        int[] calls = new int[1];
        for (int i = 0; i < 2; i++) {
            cache.get(() -> {
                calls[0]++;
                return Collections.<String>emptySet();
            });
        }
        assertThat("an empty result is cached", calls[0], is(1));
    }
}
