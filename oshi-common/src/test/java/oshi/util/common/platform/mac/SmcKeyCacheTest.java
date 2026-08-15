/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.util.GlobalConfig;

/**
 * Tests the resolve-once-and-cache behavior the SMC sensors share, without a Mac.
 * <p>
 * Each test uses a property name of its own rather than a real OSHI property, because the suite runs in parallel and a
 * shared property would race.
 */
class SmcKeyCacheTest {

    private static final List<String> FALLBACK = Collections.unmodifiableList(Arrays.asList("Tg05", "Tg0D"));

    /** A discovery function that records how many times it ran. */
    private static final class CountingDiscovery implements Supplier<@Nullable List<String>> {
        private final AtomicInteger calls = new AtomicInteger();
        private final @Nullable List<String> result;

        private CountingDiscovery(@Nullable List<String> result) {
            this.result = result;
        }

        @Override
        public @Nullable List<String> get() {
            calls.incrementAndGet();
            return result;
        }
    }

    private static SmcKeyCache cache(String property) {
        return new SmcKeyCache(property, "test", FALLBACK);
    }

    @Test
    void testCompletedDiscoveryIsCached() {
        CountingDiscovery discovery = new CountingDiscovery(Arrays.asList("Tg0f", "Tg0j"));
        SmcKeyCache keys = cache("oshi.test.smckeycache.completed");
        assertThat(keys.get(discovery), contains("Tg0f", "Tg0j"));
        assertThat(keys.get(discovery), contains("Tg0f", "Tg0j"));
        assertThat("Discovery must run only once", discovery.calls.get(), is(1));
    }

    @Test
    void testIncompleteDiscoveryFallsBackWithoutCaching() {
        // Null means "could not read". Caching it would disable the sensor for the lifetime of the JVM, so the next
        // call must try again.
        CountingDiscovery discovery = new CountingDiscovery(null);
        SmcKeyCache keys = cache("oshi.test.smckeycache.incomplete");
        assertThat(keys.get(discovery), is(sameInstance(FALLBACK)));
        assertThat(keys.get(discovery), is(sameInstance(FALLBACK)));
        assertThat("A failed discovery must be retried, not cached", discovery.calls.get(), is(2));
    }

    @Test
    void testEmptyDiscoveryIsCached() {
        // Empty is a real answer from a completed run: this machine has no such sensor. That is cacheable, unlike null.
        CountingDiscovery discovery = new CountingDiscovery(Collections.emptyList());
        SmcKeyCache keys = cache("oshi.test.smckeycache.empty");
        assertThat(keys.get(discovery), is(empty()));
        assertThat(keys.get(discovery), is(empty()));
        assertThat("An empty completed discovery must be cached", discovery.calls.get(), is(1));
    }

    @Test
    void testConfiguredKeysBypassDiscovery() {
        String property = "oshi.test.smckeycache.configured";
        GlobalConfig.set(property, "Tg1k,Tg1l");
        try {
            CountingDiscovery discovery = new CountingDiscovery(Arrays.asList("Tg0f"));
            SmcKeyCache keys = cache(property);
            assertThat(keys.get(discovery), contains("Tg1k", "Tg1l"));
            assertThat(keys.get(discovery), contains("Tg1k", "Tg1l"));
            assertThat("Configured keys must bypass discovery entirely", discovery.calls.get(), is(0));
        } finally {
            GlobalConfig.remove(property);
        }
    }

    @Test
    void testUnusableConfiguredKeysFallThroughToDiscovery() {
        // Every token is the wrong length, so parseConfiguredKeys drops them all and the result is empty. That must be
        // treated as "nothing configured" rather than as an empty key list.
        String property = "oshi.test.smckeycache.malformed";
        GlobalConfig.set(property, "TOOLONG,ab");
        try {
            CountingDiscovery discovery = new CountingDiscovery(Arrays.asList("Tg0f"));
            assertThat(cache(property).get(discovery), contains("Tg0f"));
            assertThat(discovery.calls.get(), is(1));
        } finally {
            GlobalConfig.remove(property);
        }
    }

    @Test
    void testFallbackIsReportedByIdentity() {
        // Callers distinguish "fell back" from "discovered" by identity, so the fallback must be handed back as-is
        // rather than copied.
        SmcKeyCache keys = cache("oshi.test.smckeycache.identity");
        assertThat(keys.fallback(), is(sameInstance(FALLBACK)));
        assertThat(keys.get(new CountingDiscovery(null)), is(sameInstance(keys.fallback())));
    }
}
