/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.mac;

import org.jspecify.annotations.Nullable;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFNumberRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.Constants;

/**
 * CoreFoundation utility methods
 */
@ThreadSafe
public final class CFUtil {

    private static final CoreFoundation CF = CoreFoundation.INSTANCE;

    private CFUtil() {
    }

    /**
     * /** Convert a pointer to a CFString into a String.
     *
     * @param result Pointer to the CFString
     * @return a CFString or "unknown" if it has no value
     */
    public static String cfPointerToString(@Nullable Pointer result) {
        return cfPointerToString(result, true);
    }

    /**
     * Convert a pointer to a CFString into a String.
     *
     * @param result        Pointer to the CFString
     * @param returnUnknown Whether to return the "unknown" string
     * @return a CFString including a possible empty one if {@code returnUnknown} is false, or "unknown" if it is true
     */
    public static String cfPointerToString(@Nullable Pointer result, boolean returnUnknown) {
        String s = "";
        if (result != null) {
            CFStringRef cfs = new CFStringRef(result);
            s = cfs.stringValue();
        }
        if (returnUnknown && s.isEmpty()) {
            return Constants.UNKNOWN;
        }
        return s;
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
        CFStringRef k = CFStringRef.createCFString(key);
        try {
            Pointer v = CF.CFDictionaryGetValue(dict, k);
            return v == null ? null : new CFDictionaryRef(v);
        } finally {
            k.release();
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
        CFStringRef k = CFStringRef.createCFString(key);
        try {
            Pointer v = CF.CFDictionaryGetValue(dict, k);
            return v == null ? null : new CFStringRef(v).stringValue();
        } finally {
            k.release();
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
        CFStringRef k = CFStringRef.createCFString(key);
        try {
            Pointer v = CF.CFDictionaryGetValue(dict, k);
            return v == null ? null : new CFNumberRef(v).longValue();
        } finally {
            k.release();
        }
    }
}
