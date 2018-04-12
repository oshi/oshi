/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.util.platform.mac;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import oshi.jna.platform.mac.CoreFoundation;
import oshi.jna.platform.mac.CoreFoundation.CFAllocatorRef;
import oshi.jna.platform.mac.CoreFoundation.CFStringRef;

/**
 * Provides utilities for Core Foundations
 *
 * @author widdis[at]gmail[dot]com
 */
public class CfUtil {
    public static final CFAllocatorRef ALLOCATOR = CoreFoundation.INSTANCE.CFAllocatorGetDefault();

    /**
     * Cache cfStrings
     */
    private static Map<String, CFStringRef> cfStringMap = new ConcurrentHashMap<>();

    /**
     * Return a CFStringRef representing a string, caching the result
     *
     * @param key
     *            The string, usually a registry key
     * @return the corresponding CFString
     */
    public static CFStringRef getCFString(String key) {
        synchronized (cfStringMap) {
            CFStringRef value = cfStringMap.get(key);
            if (value != null) {
                return value;
            }
            value = CFStringRef.toCFString(key);
            cfStringMap.put(key, value);
            return value;
        }
    }

    /**
     * Enum values used for number type in CFNumberGetValue(). Use ordinal() to
     * fetch the corresponding constant.
     */
    public enum CFNumberType {
        unusedZero, kCFNumberSInt8Type, kCFNumberSInt16Type, kCFNumberSInt32Type, kCFNumberSInt64Type, kCFNumberFloat32Type, kCFNumberFloat64Type, kCFNumberCharType, kCFNumberShortType, kCFNumberIntType, kCFNumberLongType, kCFNumberLongLongType, kCFNumberFloatType, kCFNumberDoubleType, kCFNumberCFIndexType, kCFNumberNSIntegerType, kCFNumberCGFloatType, kCFNumberMaxType
    }

    /**
     * Convert a pointer representing a Core Foundations LongLong into its long
     *
     * @param p
     *            The pointer to a 64-bit integer
     * @return The corresponding long
     */
    public static long cfPointerToLong(Pointer p) {
        LongByReference lbr = new LongByReference();
        CoreFoundation.INSTANCE.CFNumberGetValue(p, CFNumberType.kCFNumberLongLongType.ordinal(), lbr);
        return lbr.getValue();
    }

    /**
     * Convert a pointer representing a Core Foundations LongLong into its long
     *
     * @param p
     *            The pointer to an integer
     * @return The corresponding int
     */
    public static int cfPointerToInt(Pointer p) {
        IntByReference ibr = new IntByReference();
        CoreFoundation.INSTANCE.CFNumberGetValue(p, CFNumberType.kCFNumberIntType.ordinal(), ibr);
        return ibr.getValue();
    }

    /**
     * Convert a pointer representing a Core Foundations Boolean into its
     * boolean
     *
     * @param p
     *            The pointer to a boolean
     * @return The corresponding boolean
     */
    public static boolean cfPointerToBoolean(Pointer p) {
        return CoreFoundation.INSTANCE.CFBooleanGetValue(p);
    }

    /**
     * Convert a pointer representing a Core Foundations String into its string
     *
     * @param p
     *            The pointer to a CFString
     * @return The corresponding string
     */
    public static String cfPointerToString(Pointer p) {
        if (p == null) {
            return "null";
        }
        long length = CoreFoundation.INSTANCE.CFStringGetLength(p);
        long maxSize = CoreFoundation.INSTANCE.CFStringGetMaximumSizeForEncoding(length, CoreFoundation.UTF_8);
        if (maxSize == 0) {
            maxSize = 1;
        }
        Pointer buf = new Memory(maxSize);
        CoreFoundation.INSTANCE.CFStringGetCString(p, buf, maxSize, CoreFoundation.UTF_8);
        return buf.getString(0);
    }

    /**
     * Releases a CF reference. Mandatory when an object is owned (using
     * 'create' or 'copy' methods).
     *
     * @param ref
     *            The reference to release
     */
    public static void release(PointerType ref) {
        if (ref != null) {
            CoreFoundation.INSTANCE.CFRelease(ref);
        }
    }

}