/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.platform.unix.aix;

import java.lang.foreign.SymbolLookup;
import java.util.List;

import oshi.ffm.ForeignFunctions;

/**
 * AIX-specific shared-object loading via direct {@code dlopen}. Mirrors JNA's
 * {@code com.sun.jna.platform.unix.aix.SharedObjectLoader}: AIX's {@code dlopen} needs the {@code RTLD_MEMBER} flag to
 * accept archive-member path syntax like {@code libperfstat.a(shr_64.o)}, and FFM's
 * {@link SymbolLookup#libraryLookup(java.nio.file.Path, java.lang.foreign.Arena) libraryLookup} can't pass it. The
 * generic dlopen scaffolding lives on {@link ForeignFunctions#dlopenFirstAvailable(List, int)}; this class supplies the
 * AIX constants and the library paths.
 */
public final class SharedObjectLoader {

    /** AIX {@code RTLD_LAZY} ({@code 0x4}) — defer symbol resolution until first reference. */
    public static final int RTLD_LAZY = 0x4;
    /** AIX {@code RTLD_GLOBAL} ({@code 0x10000}) — make symbols visible to later loads. */
    public static final int RTLD_GLOBAL = 0x10000;
    /** AIX {@code RTLD_MEMBER} ({@code 0x40000}) — required to accept the {@code lib.a(member.o)} syntax. */
    public static final int RTLD_MEMBER = 0x40000;

    private SharedObjectLoader() {
    }

    /**
     * Loads {@code libperfstat} as a {@link SymbolLookup}. Tries the 64-bit archive member first, falls back to the
     * 32-bit member, mirroring JNA's {@code SharedObjectLoader.getPerfstatInstance}.
     *
     * @return a {@link SymbolLookup} backed by the loaded library
     * @throws UnsatisfiedLinkError if neither member can be loaded
     */
    public static SymbolLookup loadPerfstat() {
        return ForeignFunctions.dlopenFirstAvailable(
                List.of("/usr/lib/libperfstat.a(shr_64.o)", "/usr/lib/libperfstat.a(shr.o)"),
                RTLD_MEMBER | RTLD_GLOBAL | RTLD_LAZY);
    }
}
