/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.gpu;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Caches the PCI bus IDs of the NVML devices present on this machine.
 * <p>
 * Enumeration is retried on every call until it succeeds and is then never repeated: the set of installed devices does
 * not change for the lifetime of the JVM, but a transient NVML failure must not be cached as "no devices". That is why
 * the enumerator distinguishes a failure ({@code null}) from a machine with no NVIDIA hardware (an empty set).
 */
@ThreadSafe
public final class NvmlDeviceCache {

    private static final Logger LOG = LoggerFactory.getLogger(NvmlDeviceCache.class);

    private final AtomicReference<Set<String>> busIds = new AtomicReference<>(Collections.emptySet());
    private final String backend;
    private volatile boolean enumerated;

    /**
     * Creates an empty cache, to be populated on the first successful enumeration.
     *
     * @param backend names the binding in log messages, e.g. {@code JNA} or {@code FFM}
     */
    public NvmlDeviceCache(String backend) {
        this.backend = backend;
    }

    /**
     * Returns the cached bus IDs, enumerating them first if that has not yet succeeded. Must be called while NVML is
     * initialized.
     *
     * @param enumerator supplies the bus IDs, returning {@code null} if NVML could not be queried
     * @return the enumerated bus IDs, empty if enumeration has never succeeded
     */
    public Set<String> get(Supplier<Set<String>> enumerator) {
        if (!enumerated) {
            Set<String> ids = enumerator.get();
            if (ids != null) {
                busIds.set(ids);
                enumerated = true;
                LOG.debug("NVML ({}) enumerated {} device(s)", backend, ids.size());
            }
        }
        return busIds.get();
    }
}
