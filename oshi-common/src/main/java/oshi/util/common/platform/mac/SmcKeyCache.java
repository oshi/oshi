/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.mac;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.GlobalConfig;

/**
 * Holds the SMC keys a sensor reads, resolved once and then cached.
 * <p>
 * Every sensor whose keys are discovered rather than fixed needs the same three-step resolution: a configuration
 * property naming the keys outright, then discovery from the SMC key index, then a fallback for when discovery cannot
 * complete. This holds that once so each backend supplies only the discovery itself.
 * <p>
 * Deliberately not an {@link oshi.util.Memoizer}: that caches whatever the supplier returned, including a failure, so a
 * transient inability to open the SMC would disable the sensor for the lifetime of the JVM. A completed discovery is
 * cached, including one that legitimately found nothing; an incomplete one is not, so a later call retries.
 */
@ThreadSafe
public final class SmcKeyCache {

    private static final Logger LOG = LoggerFactory.getLogger(SmcKeyCache.class);

    private final String configProperty;
    private final String description;
    private final List<String> fallback;

    private final Object lock = new Object();

    /** Only ever assigned an unmodifiable list, so the volatile write safely publishes an immutable value. */
    private volatile @Nullable List<String> keys; // NOSONAR java:S3077 - published value is immutable

    /**
     * Creates a key cache.
     *
     * @param configProperty the {@link GlobalConfig} property that names the keys outright, bypassing discovery
     * @param description    what these keys read, for log messages, e.g. {@code "GPU temperature"}
     * @param fallback       the keys to report when discovery cannot complete. Not cached, so a later call retries.
     */
    public SmcKeyCache(String configProperty, String description, List<String> fallback) {
        this.configProperty = configProperty;
        this.description = description;
        this.fallback = fallback;
    }

    /**
     * Returns the keys to read, resolving them on first use.
     *
     * @param discovery discovers the keys from the SMC, returning null if discovery could not complete. Called at most
     *                  once per completed resolution.
     * @return the keys, never null
     */
    public List<String> get(Supplier<@Nullable List<String>> discovery) {
        List<String> resolved = keys;
        if (resolved != null) {
            return resolved;
        }
        synchronized (lock) {
            if (keys != null) {
                return keys;
            }
            // Read the config lazily rather than at class initialization, so GlobalConfig.set() still takes effect.
            List<String> configured = SmcKeyIndex.parseConfiguredKeys(GlobalConfig.get(configProperty, ""));
            if (!configured.isEmpty()) {
                LOG.debug("Using configured {} keys {}", description, configured);
                keys = configured;
                return configured;
            }
            List<String> discovered = discovery.get();
            if (discovered == null) {
                LOG.debug("{} key discovery did not complete; using {} this time.", description,
                        fallback.isEmpty() ? "no keys" : "the fallback list");
                return fallback;
            }
            LOG.debug("Using {} {} keys: {}", discovered.size(), description, discovered);
            keys = discovered;
            return discovered;
        }
    }

    /**
     * The keys reported when discovery cannot complete.
     *
     * @return the fallback keys
     */
    public List<String> fallback() {
        return fallback;
    }
}
