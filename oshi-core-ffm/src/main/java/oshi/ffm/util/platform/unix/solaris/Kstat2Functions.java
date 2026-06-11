/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.unix.solaris;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.platform.unix.solaris.LibKstatFunctions;

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
        // Check the library file's existence on disk before letting SymbolLookup.libraryLookup
        // attempt a dlopen — on illumos / Solaris < 11.4 the file simply doesn't exist, and
        // avoiding the failed native load sidesteps a SIGSEGV pattern seen in JNA's
        // Native.load on the same library / same JDK 25 + illumos combination.
        if (libkstat2Present()) {
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

    /**
     * Returns {@code true} if any {@code libkstat2.so*} file is present on the standard Solaris/illumos library search
     * paths. Solaris 11.4+ ships it (currently as {@code libkstat2.so.1}); illumos and Solaris &lt; 11.4 do not.
     * Matches any suffix to survive future SONAME bumps.
     *
     * @return whether a libkstat2 shared object exists on disk
     */
    private static boolean libkstat2Present() {
        for (String dir : new String[] { "/lib/64", "/usr/lib/64", "/lib", "/usr/lib" }) {
            String[] hits = new File(dir).list((d, name) -> name.startsWith("libkstat2.so"));
            if (hits != null && hits.length > 0) {
                return true;
            }
        }
        return false;
    }
}
