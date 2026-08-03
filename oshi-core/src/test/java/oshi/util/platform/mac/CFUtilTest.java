/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFIndex;
import com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFNumberRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.ptr.LongByReference;

import oshi.util.Constants;

/**
 * Tests for {@link CFUtil} using real native CoreFoundation calls.
 */
@EnabledOnOs(OS.MAC)
@DisabledIfSystemProperty(named = "os.name", matches = "(?i).*netbsd.*")
class CFUtilTest {

    private static final CoreFoundation CF = CoreFoundation.INSTANCE;

    // CFEqual-based key/value callbacks so lookups match by string content, not pointer identity, as real IOKit
    // dictionaries do. Reuse the NativeLibrary already backing CoreFoundation.INSTANCE rather than loading a second.
    private static final Pointer KEY_CALLBACKS = Native.getNativeLibrary(CF)
            .getGlobalVariableAddress("kCFTypeDictionaryKeyCallBacks");
    private static final Pointer VALUE_CALLBACKS = Native.getNativeLibrary(CF)
            .getGlobalVariableAddress("kCFTypeDictionaryValueCallBacks");

    @Test
    void testCfPointerToString() {
        CFStringRef s = CFStringRef.createCFString("hello");
        try {
            assertThat(CFUtil.cfPointerToString(s.getPointer()), is("hello"));
        } finally {
            s.release();
        }
        // Null pointer yields the "unknown" sentinel, or empty when returnUnknown is false
        assertThat(CFUtil.cfPointerToString(null), is(Constants.UNKNOWN));
        assertThat(CFUtil.cfPointerToString(null, false), is(""));
    }

    @Test
    void testGetString() {
        CFMutableDictionaryRef dict = newDict();
        CFStringRef key = CFStringRef.createCFString("model");
        CFStringRef value = CFStringRef.createCFString("Studio Display");
        try {
            dict.setValue(key, value);
            assertThat(CFUtil.getString(dict, "model"), is("Studio Display"));
            // Absent key returns null
            assertThat(CFUtil.getString(dict, "missing"), is(nullValue()));
        } finally {
            value.release();
            key.release();
            dict.release();
        }
    }

    @Test
    void testGetLong() {
        CFMutableDictionaryRef dict = newDict();
        CFStringRef key = CFStringRef.createCFString("year");
        LongByReference valuePtr = new LongByReference(2026L);
        CFNumberRef value = CF.CFNumberCreate(null, CoreFoundation.CFNumberType.kCFNumberLongLongType.typeIndex(),
                valuePtr);
        try {
            dict.setValue(key, value);
            assertThat(CFUtil.getLong(dict, "year"), is(2026L));
            // Absent key returns null
            assertThat(CFUtil.getLong(dict, "missing"), is(nullValue()));
        } finally {
            value.release();
            key.release();
            dict.release();
        }
    }

    @Test
    void testGetDictionary() {
        CFMutableDictionaryRef outer = newDict();
        CFMutableDictionaryRef inner = newDict();
        CFStringRef outerKey = CFStringRef.createCFString("ProductAttributes");
        CFStringRef innerKey = CFStringRef.createCFString("ProductName");
        CFStringRef innerValue = CFStringRef.createCFString("Pro Display XDR");
        try {
            inner.setValue(innerKey, innerValue);
            outer.setValue(outerKey, inner);

            CFDictionaryRef nested = CFUtil.getDictionary(outer, "ProductAttributes");
            assertThat(nested, is(notNullValue()));
            assertThat(CFUtil.getString(nested, "ProductName"), is("Pro Display XDR"));

            // Absent key returns null
            assertThat(CFUtil.getDictionary(outer, "missing"), is(nullValue()));
        } finally {
            innerValue.release();
            innerKey.release();
            outerKey.release();
            inner.release();
            outer.release();
        }
    }

    private static CFMutableDictionaryRef newDict() {
        return CF.CFDictionaryCreateMutable(CF.CFAllocatorGetDefault(), new CFIndex(0), KEY_CALLBACKS, VALUE_CALLBACKS);
    }
}
