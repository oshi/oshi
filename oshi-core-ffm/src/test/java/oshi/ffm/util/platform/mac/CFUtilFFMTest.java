/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.mac;

import static java.lang.foreign.MemorySegment.NULL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;

import oshi.ffm.platform.mac.CoreFoundation;
import oshi.ffm.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.ffm.platform.mac.CoreFoundation.CFNumberRef;
import oshi.ffm.platform.mac.CoreFoundation.CFStringRef;
import oshi.ffm.platform.mac.CoreFoundationFunctions;
import oshi.util.Constants;

/**
 * Tests for {@link CFUtilFFM} using real native CoreFoundation calls.
 */
@EnabledOnOs(OS.MAC)
@EnabledForJreRange(min = JRE.JAVA_25)
class CFUtilFFMTest {

    private static final SymbolLookup CF_LOOKUP = SymbolLookup
            .libraryLookup("/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation", Arena.global());

    @Test
    void testCfPointerToString() {
        try (CFStringRef s = CFStringRef.createCFString("hello")) {
            assertThat(CFUtilFFM.cfPointerToString(s.segment()), is("hello"));
        }
        // Null segment yields the "unknown" sentinel, or empty when returnUnknown is false
        assertThat(CFUtilFFM.cfPointerToString(NULL), is(Constants.UNKNOWN));
        assertThat(CFUtilFFM.cfPointerToString(NULL, false), is(""));
    }

    @Test
    void testStringToCFString() {
        try (CFStringRef s = CFUtilFFM.stringToCFString("round-trip")) {
            assertThat(s.isNull(), is(false));
            assertThat(s.stringValue(), is("round-trip"));
        }
    }

    @Test
    void testGetString() throws Throwable {
        try (CFMutableDictionaryRef dict = newDict();
                CFStringRef key = CFStringRef.createCFString("model");
                CFStringRef value = CFStringRef.createCFString("Studio Display")) {
            dict.setValue(key, value);
            assertThat(CFUtilFFM.getString(dict, "model"), is("Studio Display"));
            // Absent key returns null
            assertThat(CFUtilFFM.getString(dict, "missing"), is(nullValue()));
        }
    }

    @Test
    void testGetLong() throws Throwable {
        try (Arena arena = Arena.ofConfined();
                CFMutableDictionaryRef dict = newDict();
                CFStringRef key = CFStringRef.createCFString("year")) {
            MemorySegment allocator = CoreFoundationFunctions.CFAllocatorGetDefault();
            MemorySegment valuePtr = arena.allocate(ValueLayout.JAVA_LONG);
            valuePtr.set(ValueLayout.JAVA_LONG, 0, 2026L);
            MemorySegment numSeg = CoreFoundationFunctions.CFNumberCreate(allocator,
                    CoreFoundation.kCFNumberLongLongType, valuePtr);
            try (CFNumberRef value = new CFNumberRef(numSeg)) {
                dict.setValue(key, value);
                assertThat(CFUtilFFM.getLong(dict, "year"), is(2026L));
                // Absent key returns null
                assertThat(CFUtilFFM.getLong(dict, "missing"), is(nullValue()));
            }
        }
    }

    @Test
    void testGetDictionary() throws Throwable {
        try (CFMutableDictionaryRef outer = newDict();
                CFMutableDictionaryRef inner = newDict();
                CFStringRef outerKey = CFStringRef.createCFString("ProductAttributes");
                CFStringRef innerKey = CFStringRef.createCFString("ProductName");
                CFStringRef innerValue = CFStringRef.createCFString("Pro Display XDR")) {
            inner.setValue(innerKey, innerValue);
            outer.setValue(outerKey, inner);

            CFDictionaryRef nested = CFUtilFFM.getDictionary(outer, "ProductAttributes");
            assertThat(nested, is(notNullValue()));
            assertThat(CFUtilFFM.getString(nested, "ProductName"), is("Pro Display XDR"));

            // Absent key returns null
            assertThat(CFUtilFFM.getDictionary(outer, "missing"), is(nullValue()));
        }
    }

    private static CFMutableDictionaryRef newDict() throws Throwable {
        MemorySegment allocator = CoreFoundationFunctions.CFAllocatorGetDefault();
        MemorySegment keyCallbacks = CF_LOOKUP.findOrThrow("kCFTypeDictionaryKeyCallBacks");
        MemorySegment valCallbacks = CF_LOOKUP.findOrThrow("kCFTypeDictionaryValueCallBacks");
        return new CFMutableDictionaryRef(
                CoreFoundationFunctions.CFDictionaryCreateMutable(allocator, 0, keyCallbacks, valCallbacks));
    }
}
