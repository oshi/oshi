/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.unix.solaris;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FFM probe for {@code libkstat2.so.1}, available on Solaris 11.4 and later but absent on illumos and earlier Solaris
 * releases. {@link #HAS_KSTAT2} reflects whether the library was loadable at class-init time; consumers should branch
 * to a {@link LibKstatFunctions} path on {@code false}.
 * <p>
 * Function bindings for the {@code kstat2_*} API will be added when a downstream consumer requires them. On
 * illumos/OpenIndiana — the FFM CI target — only the legacy {@code libkstat} path is exercised, so this class is
 * intentionally a probe-only stub today.
 */
public final class Kstat2Functions {

    private static final Logger LOG = LoggerFactory.getLogger(Kstat2Functions.class);

    /**
     * {@code true} if {@code libkstat2.so.1} was loadable at JVM startup. Mirrors
     * {@code SolarisOperatingSystem.HAS_KSTAT2} on the JNA side.
     */
    public static final boolean HAS_KSTAT2;

    static {
        boolean available = false;
        try {
            SymbolLookup.libraryLookup("libkstat2.so.1", Arena.global());
            available = true;
        } catch (Throwable t) {
            // illumos and Solaris < 11.4 have no libkstat2; this is the expected path on OpenIndiana CI.
            LOG.debug("libkstat2.so.1 not available; falling back to legacy libkstat", t);
        }
        HAS_KSTAT2 = available;
    }

    private Kstat2Functions() {
    }
}
