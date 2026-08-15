/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.mac;

import java.lang.foreign.MemorySegment;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.platform.mac.CoreFoundation.CFNumberRef;
import oshi.ffm.platform.mac.CoreFoundation.CFStringRef;
import oshi.util.Constants;

/**
 * CoreFoundation utility methods
 */
@ThreadSafe
public final class CFUtilFFM {

    private CFUtilFFM() {
    }

    /**
     * Convert a MemorySegment pointing to a CFString into a Java String.
     *
     * @param segment MemorySegment to the CFString
     * @return a CFString or "unknown" if it has no value
     */
    public static String cfPointerToString(MemorySegment segment) {
        return cfPointerToString(segment, true);
    }

    /**
     * Convert a MemorySegment pointing to a CFString into a Java String.
     *
     * @param segment       MemorySegment to the CFString
     * @param returnUnknown Whether to return the "unknown" string
     * @return a CFString including a possible empty one if {@code returnUnknown} is false, or "unknown" if it is true
     */
    public static String cfPointerToString(MemorySegment segment, boolean returnUnknown) {
        String s = "";
        if (segment != null && !segment.equals(MemorySegment.NULL)) {
            s = CFStringRef.stringValue(segment);
        }
        if (returnUnknown && s.isEmpty()) {
            return Constants.UNKNOWN;
        }
        return s;
    }

    /**
     * Creates a CoreFoundation string from a Java string
     *
     * @param str The Java string to convert
     * @return A CFStringRef that must be released by the caller
     */
    public static CFStringRef stringToCFString(String str) {
        return CFStringRef.createCFString(str);
    }

    /**
     * Reads a nested dictionary value from a CFDictionary by key.
     * <p>
     * {@code CFDictionaryGetValue} returns a borrowed reference, so the returned dictionary must <em>not</em> be
     * released.
     *
     * @param dict the dictionary to read from
     * @param key  the string key to look up
     * @return the value as a {@link CFDictionaryRef}, or {@code null} if the key is absent
     */
    public static @Nullable CFDictionaryRef getDictionary(CFDictionaryRef dict, String key) {
        try (CFStringRef k = CFStringRef.createCFString(key)) {
            MemorySegment v = dict.getValue(k);
            return v == null || v.equals(MemorySegment.NULL) ? null : new CFDictionaryRef(v);
        }
    }

    /**
     * Reads a string value from a CFDictionary by key.
     * <p>
     * {@code CFDictionaryGetValue} returns a borrowed reference, so the underlying value must <em>not</em> be released.
     *
     * @param dict the dictionary to read from
     * @param key  the string key to look up
     * @return the value as a {@link String}, or {@code null} if the key is absent
     */
    public static @Nullable String getString(CFDictionaryRef dict, String key) {
        try (CFStringRef k = CFStringRef.createCFString(key)) {
            MemorySegment v = dict.getValue(k);
            return v == null || v.equals(MemorySegment.NULL) ? null : CFStringRef.stringValue(v);
        }
    }

    /**
     * Reads a long value from a CFDictionary by key.
     * <p>
     * {@code CFDictionaryGetValue} returns a borrowed reference, so the underlying value must <em>not</em> be released.
     * The value is read as a 64-bit integer.
     *
     * @param dict the dictionary to read from
     * @param key  the string key to look up
     * @return the value as a {@link Long}, or {@code null} if the key is absent
     */
    public static @Nullable Long getLong(CFDictionaryRef dict, String key) {
        try (CFStringRef k = CFStringRef.createCFString(key)) {
            MemorySegment v = dict.getValue(k);
            return v == null || v.equals(MemorySegment.NULL) ? null : CFNumberRef.longValue(v);
        }
    }
}
