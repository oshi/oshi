/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.mac;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.Constants;

/**
 * CF String retrieving
 */
@ThreadSafe
public final class CFUtil {

    private CFUtil() {
    }

    /**
     * /** Convert a pointer to a CFString into a String.
     *
     * @param result Pointer to the CFString
     * @return a CFString or "unknown" if it has no value
     */
    public static String cfPointerToString(Pointer result) {
        return cfPointerToString(result, true);
    }

    /**
     * Convert a pointer to a CFString into a String.
     *
     * @param result        Pointer to the CFString
     * @param returnUnknown Whether to return the "unknown" string
     * @return a CFString including a possible empty one if {@code returnUnknown} is false, or "unknown" if it is true
     */
    public static String cfPointerToString(Pointer result, boolean returnUnknown) {
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

}
