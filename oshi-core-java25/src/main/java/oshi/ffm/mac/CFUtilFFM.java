/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import java.lang.foreign.MemorySegment;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.mac.CoreFoundation.CFStringRef;
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
            CFStringRef cfString = null;
            cfString = new CFStringRef(segment);
            s = cfString.stringValue();
            // No need to release the string since we didn't create it,
            // just accessed an existing one
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
}
