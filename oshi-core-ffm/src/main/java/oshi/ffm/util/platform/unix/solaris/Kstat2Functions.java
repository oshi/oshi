/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.unix.solaris;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.platform.unix.solaris.LibKstatFunctions;
import oshi.util.NativeLibraryUtil;

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
     * {@code SolarisOperatingSystemJNA.HAS_KSTAT2} on the JNA side.
     */
    public static final boolean HAS_KSTAT2;

    static {
        boolean available = false;
        // illumos and Solaris < 11.4 have no libkstat2, and a failed load there can crash the JVM; see
        // NativeLibraryUtil.
        if (NativeLibraryUtil.isSafeToLoad("libkstat2.so.1")) {
            try {
                SymbolLookup.libraryLookup("libkstat2.so.1", Arena.global());
                available = true;
            } catch (Throwable t) {
                LOG.debug("libkstat2.so.1 present on disk but not loadable", t);
            }
        }
        HAS_KSTAT2 = available;
    }

    private Kstat2Functions() {
    }
}
