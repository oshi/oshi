/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import java.util.List;

import java.lang.foreign.SymbolLookup;

import oshi.ffm.ForeignFunctions;

/**
 * Base class for macOS FFM function bindings, adding framework lookup support on top of {@link ForeignFunctions}.
 */
public abstract class MacForeignFunctions extends ForeignFunctions {

    /**
     * Search paths for macOS frameworks, in priority order.
     * <ul>
     * <li>{@code /System/Library/Frameworks} - Reserved strictly for Apple's core OS frameworks (e.g. IOKit,
     * CoreFoundation, AppKit).</li>
     * <li>{@code /Library/Frameworks} - Standard location for third-party frameworks available to all users (e.g. SDL2,
     * GStreamer, or custom drivers).</li>
     * </ul>
     */
    protected static final List<String> FRAMEWORK_SEARCH_PATHS = List.of("/System/Library/Frameworks",
            "/Library/Frameworks");

    /** Not intended for instantiation. */
    protected MacForeignFunctions() {
    }

    /**
     * Lookup a macOS framework by simple name, e.g. {@code "IOKit"}. Searches {@link #FRAMEWORK_SEARCH_PATHS} in order.
     *
     * @param frameworkName the framework name without path or extension
     * @return the symbol lookup for the framework
     * @throws IllegalArgumentException if the framework is not found in any search path
     */
    protected static SymbolLookup frameworkLookup(String frameworkName) {
        for (String base : FRAMEWORK_SEARCH_PATHS) {
            try {
                return SymbolLookup.libraryLookup(base + "/" + frameworkName + ".framework/" + frameworkName,
                        LIBRARY_ARENA);
            } catch (IllegalArgumentException ignored) {
                // Not found in this directory, try the next one
            }
        }
        throw new IllegalArgumentException("Framework not found: " + frameworkName);
    }
}
