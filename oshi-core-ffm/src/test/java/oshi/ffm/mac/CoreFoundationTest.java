/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.MemorySegment.NULL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;

import oshi.ffm.mac.CoreFoundation.CFAllocatorRef;
import oshi.ffm.mac.CoreFoundation.CFArrayRef;
import oshi.ffm.mac.CoreFoundation.CFBooleanRef;
import oshi.ffm.mac.CoreFoundation.CFDataRef;
import oshi.ffm.mac.CoreFoundation.CFDateFormatter;
import oshi.ffm.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.mac.CoreFoundation.CFLocale;
import oshi.ffm.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.ffm.mac.CoreFoundation.CFNumberRef;
import oshi.ffm.mac.CoreFoundation.CFStringRef;
import oshi.ffm.mac.CoreFoundation.CFTypeRef;

/**
 * Tests for {@link CoreFoundation} wrapper types using real native CoreFoundation calls.
 */
@EnabledOnOs(OS.MAC)
@EnabledForJreRange(min = JRE.JAVA_25)
class CoreFoundationTest {

    private static final SymbolLookup CF_LOOKUP = SymbolLookup
            .libraryLookup("/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation", Arena.global());

    // -------------------------------------------------------------------------
    // CFTypeRef — null handling, type ID, equals, retain/release
    // -------------------------------------------------------------------------

    @Test
    void testCFTypeRefNullSegment() {
        var ref = new CFTypeRef(NULL);
        assertThat(ref.isNull(), is(true));
        assertThat(ref.getTypeID(), is(0L));
        assertThat(ref.isTypeID(CoreFoundationFunctions.STRING_TYPE_ID), is(false));
        // retain/release on null should not throw
        ref.retain();
        ref.release();
    }

    @Test
    void testCFTypeRefJavaNullSegment() {
        var ref = new CFTypeRef(null);
        assertThat(ref.isNull(), is(true));
    }

    @Test
    void testCFTypeRefEqualsAndHashCode() {
        var str1 = CFStringRef.createCFString("test");
        var str2 = CFStringRef.createCFString("test");
        var str3 = CFStringRef.createCFString("other");
        try {
            assertThat(str1.equals(str2), is(true));
            assertThat(str1.hashCode(), is(str2.hashCode()));
            assertThat(str1.equals(str3), is(false));
            assertThat(str1.equals(null), is(false));
            assertThat(str1.equals(str1), is(true));
            // Null refs are equal to each other
            var nullRef1 = new CFTypeRef(NULL);
            var nullRef2 = new CFTypeRef(NULL);
            assertThat(nullRef1.equals(nullRef2), is(true));
            assertThat(nullRef1.hashCode(), is(nullRef2.hashCode()));
            assertThat(nullRef1.equals(str1), is(false));
        } finally {
            str1.release();
            str2.release();
            str3.release();
        }
    }

    @Test
    void testCFTypeRefRetainRelease() throws Throwable {
        var str = CFStringRef.createCFString("retain-test");
        try {
            long countBefore = CoreFoundationFunctions.CFGetRetainCount(str.segment());
            str.retain();
            long countAfter = CoreFoundationFunctions.CFGetRetainCount(str.segment());
            assertThat(countAfter, is(countBefore + 1));
            // Release the extra retain
            str.release();
            assertThat(CoreFoundationFunctions.CFGetRetainCount(str.segment()), is(countBefore));
        } finally {
            str.release();
        }
    }

    // -------------------------------------------------------------------------
    // CFStringRef
    // -------------------------------------------------------------------------

    @Test
    void testCFStringRoundtrip() {
        var cfStr = CFStringRef.createCFString("Hello, CoreFoundation!");
        try {
            assertThat(cfStr.isNull(), is(false));
            assertThat(cfStr.isTypeID(CoreFoundationFunctions.STRING_TYPE_ID), is(true));
            assertThat(cfStr.stringValue(), is("Hello, CoreFoundation!"));
        } finally {
            cfStr.release();
        }
    }

    @Test
    void testCFStringEmpty() {
        var cfStr = CFStringRef.createCFString("");
        try {
            assertThat(cfStr.stringValue(), is(""));
        } finally {
            cfStr.release();
        }
    }

    @Test
    void testCFStringNullReturnsEmpty() {
        var cfStr = new CFStringRef(NULL);
        assertThat(cfStr.stringValue(), is(""));
    }

    @Test
    void testCFStringUnicode() {
        var cfStr = CFStringRef.createCFString("日本語テスト");
        try {
            assertThat(cfStr.stringValue(), is("日本語テスト"));
        } finally {
            cfStr.release();
        }
    }

    @Test
    void testCFStringCastCheckRejectsNonString() throws Throwable {
        try (var arena = Arena.ofConfined()) {
            var allocator = CoreFoundationFunctions.CFAllocatorGetDefault();
            var valuePtr = arena.allocate(ValueLayout.JAVA_INT);
            valuePtr.set(ValueLayout.JAVA_INT, 0, 42);
            var numSeg = CoreFoundationFunctions.CFNumberCreate(allocator, CoreFoundation.kCFNumberIntType, valuePtr);
            try {
                assertThrows(ClassCastException.class, () -> new CFStringRef(numSeg));
            } finally {
                CoreFoundationFunctions.CFRelease(numSeg);
            }
        }
    }

    // -------------------------------------------------------------------------
    // CFNumberRef
    // -------------------------------------------------------------------------

    @Test
    void testCFNumberLong() throws Throwable {
        try (var arena = Arena.ofConfined()) {
            var allocator = CoreFoundationFunctions.CFAllocatorGetDefault();
            var valuePtr = arena.allocate(ValueLayout.JAVA_LONG);
            valuePtr.set(ValueLayout.JAVA_LONG, 0, 123456789L);
            var numSeg = CoreFoundationFunctions.CFNumberCreate(allocator, CoreFoundation.kCFNumberLongLongType,
                    valuePtr);
            var cfNum = new CFNumberRef(numSeg);
            try {
                assertThat(cfNum.longValue(), is(123456789L));
                assertThat(cfNum.isTypeID(CoreFoundationFunctions.NUMBER_TYPE_ID), is(true));
            } finally {
                cfNum.release();
            }
        }
    }

    @Test
    void testCFNumberInt() throws Throwable {
        try (var arena = Arena.ofConfined()) {
            var allocator = CoreFoundationFunctions.CFAllocatorGetDefault();
            var valuePtr = arena.allocate(ValueLayout.JAVA_INT);
            valuePtr.set(ValueLayout.JAVA_INT, 0, 42);
            var numSeg = CoreFoundationFunctions.CFNumberCreate(allocator, CoreFoundation.kCFNumberIntType, valuePtr);
            var cfNum = new CFNumberRef(numSeg);
            try {
                assertThat(cfNum.intValue(), is(42));
            } finally {
                cfNum.release();
            }
        }
    }

    @Test
    void testCFNumberShort() throws Throwable {
        try (var arena = Arena.ofConfined()) {
            var allocator = CoreFoundationFunctions.CFAllocatorGetDefault();
            var valuePtr = arena.allocate(ValueLayout.JAVA_SHORT);
            valuePtr.set(ValueLayout.JAVA_SHORT, 0, (short) 7);
            var numSeg = CoreFoundationFunctions.CFNumberCreate(allocator, CoreFoundation.kCFNumberShortType, valuePtr);
            var cfNum = new CFNumberRef(numSeg);
            try {
                assertThat(cfNum.shortValue(), is((short) 7));
            } finally {
                cfNum.release();
            }
        }
    }

    @Test
    void testCFNumberByte() throws Throwable {
        try (var arena = Arena.ofConfined()) {
            var allocator = CoreFoundationFunctions.CFAllocatorGetDefault();
            var valuePtr = arena.allocate(ValueLayout.JAVA_BYTE);
            valuePtr.set(ValueLayout.JAVA_BYTE, 0, (byte) 3);
            var numSeg = CoreFoundationFunctions.CFNumberCreate(allocator, CoreFoundation.kCFNumberCharType, valuePtr);
            var cfNum = new CFNumberRef(numSeg);
            try {
                assertThat(cfNum.byteValue(), is((byte) 3));
            } finally {
                cfNum.release();
            }
        }
    }

    @Test
    void testCFNumberDouble() throws Throwable {
        try (var arena = Arena.ofConfined()) {
            var allocator = CoreFoundationFunctions.CFAllocatorGetDefault();
            var valuePtr = arena.allocate(ValueLayout.JAVA_DOUBLE);
            valuePtr.set(ValueLayout.JAVA_DOUBLE, 0, 3.14);
            var numSeg = CoreFoundationFunctions.CFNumberCreate(allocator, CoreFoundation.kCFNumberDoubleType,
                    valuePtr);
            var cfNum = new CFNumberRef(numSeg);
            try {
                assertThat(cfNum.doubleValue(), is(closeTo(3.14, 0.001)));
            } finally {
                cfNum.release();
            }
        }
    }

    @Test
    void testCFNumberFloat() throws Throwable {
        try (var arena = Arena.ofConfined()) {
            var allocator = CoreFoundationFunctions.CFAllocatorGetDefault();
            var valuePtr = arena.allocate(ValueLayout.JAVA_FLOAT);
            valuePtr.set(ValueLayout.JAVA_FLOAT, 0, 2.5f);
            var numSeg = CoreFoundationFunctions.CFNumberCreate(allocator, CoreFoundation.kCFNumberFloatType, valuePtr);
            var cfNum = new CFNumberRef(numSeg);
            try {
                assertThat(cfNum.floatValue(), is(2.5f));
            } finally {
                cfNum.release();
            }
        }
    }

    @Test
    void testCFNumberNullReturnsZero() {
        var cfNum = new CFNumberRef(NULL);
        assertThat(cfNum.longValue(), is(0L));
        assertThat(cfNum.intValue(), is(0));
        assertThat(cfNum.shortValue(), is((short) 0));
        assertThat(cfNum.byteValue(), is((byte) 0));
        assertThat(cfNum.doubleValue(), is(0.0));
        assertThat(cfNum.floatValue(), is(0.0f));
    }

    // -------------------------------------------------------------------------
    // CFBooleanRef
    // -------------------------------------------------------------------------

    @Test
    void testCFBooleanTrue() {
        var seg = CF_LOOKUP.findOrThrow("kCFBooleanTrue").reinterpret(ValueLayout.ADDRESS.byteSize())
                .get(ValueLayout.ADDRESS, 0);
        var cfBool = new CFBooleanRef(seg);
        assertThat(cfBool.booleanValue(), is(true));
        assertThat(cfBool.isTypeID(CoreFoundationFunctions.BOOLEAN_TYPE_ID), is(true));
    }

    @Test
    void testCFBooleanFalse() {
        var seg = CF_LOOKUP.findOrThrow("kCFBooleanFalse").reinterpret(ValueLayout.ADDRESS.byteSize())
                .get(ValueLayout.ADDRESS, 0);
        var cfBool = new CFBooleanRef(seg);
        assertThat(cfBool.booleanValue(), is(false));
    }

    @Test
    void testCFBooleanNullReturnsFalse() {
        var cfBool = new CFBooleanRef(NULL);
        assertThat(cfBool.booleanValue(), is(false));
    }

    // -------------------------------------------------------------------------
    // CFDataRef
    // -------------------------------------------------------------------------

    @Test
    void testCFDataRoundtrip() throws Throwable {
        try (var arena = Arena.ofConfined()) {
            byte[] input = { 1, 2, 3, 4, 5 };
            var bytesSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, input);
            var allocator = CoreFoundationFunctions.CFAllocatorGetDefault();
            var dataSeg = CoreFoundationFunctions.CFDataCreate(allocator, bytesSeg, input.length);
            var cfData = new CFDataRef(dataSeg);
            try {
                assertThat(cfData.isNull(), is(false));
                assertThat(cfData.isTypeID(CoreFoundationFunctions.DATA_TYPE_ID), is(true));
                assertThat(cfData.getLength(), is(5));
                assertThat(cfData.getBytePtr(), is(not(NULL)));
                assertThat(cfData.getBytes(), is(input));
            } finally {
                cfData.release();
            }
        }
    }

    @Test
    void testCFDataNullReturnsDefaults() {
        var cfData = new CFDataRef(NULL);
        assertThat(cfData.getLength(), is(0));
        assertThat(cfData.getBytePtr(), is(NULL));
        assertThat(cfData.getBytes(), is(new byte[0]));
    }

    // -------------------------------------------------------------------------
    // CFArrayRef
    // -------------------------------------------------------------------------

    @Test
    void testCFArrayWithStrings() throws Throwable {
        var str1 = CFStringRef.createCFString("a");
        var str2 = CFStringRef.createCFString("b");
        try (var arena = Arena.ofConfined()) {
            var values = arena.allocate(ValueLayout.ADDRESS, 2);
            values.setAtIndex(ValueLayout.ADDRESS, 0, str1.segment());
            values.setAtIndex(ValueLayout.ADDRESS, 1, str2.segment());
            var allocator = CoreFoundationFunctions.CFAllocatorGetDefault();
            var callbacks = CF_LOOKUP.findOrThrow("kCFTypeArrayCallBacks");
            var arraySeg = CoreFoundationFunctions.CFArrayCreate(allocator, values, 2, callbacks);
            var cfArray = new CFArrayRef(arraySeg);
            try {
                assertThat(cfArray.isTypeID(CoreFoundationFunctions.ARRAY_TYPE_ID), is(true));
                assertThat(cfArray.getCount(), is(2));
                var elem0 = new CFStringRef(cfArray.getValueAtIndex(0));
                assertThat(elem0.stringValue(), is("a"));
            } finally {
                cfArray.release();
            }
        } finally {
            str1.release();
            str2.release();
        }
    }

    @Test
    void testCFArrayNullReturnsDefaults() {
        var cfArray = new CFArrayRef(NULL);
        assertThat(cfArray.getCount(), is(0));
        assertThat(cfArray.getValueAtIndex(0), is(NULL));
    }

    // -------------------------------------------------------------------------
    // CFDictionaryRef / CFMutableDictionaryRef
    // -------------------------------------------------------------------------

    @Test
    void testCFMutableDictionarySetAndGet() throws Throwable {
        try (var arena = Arena.ofConfined()) {
            var allocator = CoreFoundationFunctions.CFAllocatorGetDefault();
            var keyCallbacks = CF_LOOKUP.findOrThrow("kCFTypeDictionaryKeyCallBacks");
            var valCallbacks = CF_LOOKUP.findOrThrow("kCFTypeDictionaryValueCallBacks");
            var dictSeg = CoreFoundationFunctions.CFDictionaryCreateMutable(allocator, 0, keyCallbacks, valCallbacks);
            var dict = new CFMutableDictionaryRef(dictSeg);
            try {
                var key = CFStringRef.createCFString("myKey");
                var val = CFStringRef.createCFString("myValue");
                try {
                    dict.setValue(key, val);
                    assertThat(dict.getCount(), is(1L));

                    var result = dict.getValue(key);
                    assertThat(result, is(not(NULL)));
                    assertThat(new CFStringRef(result).stringValue(), is("myValue"));

                    // getValueIfPresent
                    var outPtr = arena.allocate(ValueLayout.ADDRESS);
                    assertThat(dict.getValueIfPresent(key, outPtr), is(true));

                    // Missing key
                    var missingKey = CFStringRef.createCFString("noSuchKey");
                    try {
                        assertThat(dict.getValue(missingKey), is(NULL));
                        assertThat(dict.getValueIfPresent(missingKey, outPtr), is(false));
                    } finally {
                        missingKey.release();
                    }
                } finally {
                    key.release();
                    val.release();
                }
            } finally {
                dict.release();
            }
        }
    }

    @Test
    void testCFDictionaryNullReturnsDefaults() {
        var dict = new CFDictionaryRef(NULL);
        assertThat(dict.getCount(), is(0L));
        assertThat(dict.getValue(new CFStringRef(NULL)), is(NULL));
        assertThat(dict.getValueIfPresent(new CFStringRef(NULL), NULL), is(false));
    }

    @Test
    void testCFMutableDictionarySetValueNullKeyIgnored() throws Throwable {
        try (var arena = Arena.ofConfined()) {
            var allocator = CoreFoundationFunctions.CFAllocatorGetDefault();
            var keyCallbacks = CF_LOOKUP.findOrThrow("kCFTypeDictionaryKeyCallBacks");
            var valCallbacks = CF_LOOKUP.findOrThrow("kCFTypeDictionaryValueCallBacks");
            var dictSeg = CoreFoundationFunctions.CFDictionaryCreateMutable(allocator, 0, keyCallbacks, valCallbacks);
            var dict = new CFMutableDictionaryRef(dictSeg);
            try {
                // setValue with null key should not throw
                var val1 = CFStringRef.createCFString("val");
                var val2 = CFStringRef.createCFString("val");
                try {
                    dict.setValue(null, val1);
                    dict.setValue(new CFStringRef(NULL), val2);
                    assertThat(dict.getCount(), is(0L));
                } finally {
                    val1.release();
                    val2.release();
                }
            } finally {
                dict.release();
            }
        }
    }

    // -------------------------------------------------------------------------
    // CFAllocatorRef
    // -------------------------------------------------------------------------

    @Test
    void testCFAllocatorRef() throws Throwable {
        var allocator = new CFAllocatorRef(CoreFoundationFunctions.CFAllocatorGetDefault());
        assertThat(allocator.isNull(), is(false));
        assertThat(allocator.segment(), is(notNullValue()));
    }

    // -------------------------------------------------------------------------
    // CFLocale
    // -------------------------------------------------------------------------

    @Test
    void testCFLocaleCopyCurrent() {
        var locale = CFLocale.copyCurrent();
        try {
            assertThat(locale.isNull(), is(false));
        } finally {
            locale.release();
        }
    }

    // -------------------------------------------------------------------------
    // CFDateFormatter
    // -------------------------------------------------------------------------

    @Test
    void testCFDateFormatterGetFormat() {
        var locale = CFLocale.copyCurrent();
        try {
            var formatter = CFDateFormatter.create(null, locale, CoreFoundation.kCFDateFormatterShortStyle,
                    CoreFoundation.kCFDateFormatterNoStyle);
            try {
                assertThat(formatter.isNull(), is(false));
                var format = formatter.getFormat();
                assertThat(format.isNull(), is(false));
                assertThat(format.stringValue().length(), is(greaterThan(0)));
            } finally {
                formatter.release();
            }
        } finally {
            locale.release();
        }
    }

    @Test
    void testCFDateFormatterNullReturnsNullFormat() {
        var formatter = new CFDateFormatter(NULL);
        var format = formatter.getFormat();
        assertThat(format.isNull(), is(true));
    }

    // -------------------------------------------------------------------------
    // ClassCastException paths
    // -------------------------------------------------------------------------

    @Test
    void testCFNumberRefRejectsString() {
        var cfStr = CFStringRef.createCFString("notANumber");
        try {
            assertThrows(ClassCastException.class, () -> new CFNumberRef(cfStr.segment()));
        } finally {
            cfStr.release();
        }
    }

    @Test
    void testCFBooleanRefRejectsString() {
        var cfStr = CFStringRef.createCFString("notABool");
        try {
            assertThrows(ClassCastException.class, () -> new CFBooleanRef(cfStr.segment()));
        } finally {
            cfStr.release();
        }
    }

    @Test
    void testCFArrayRefRejectsString() {
        var cfStr = CFStringRef.createCFString("notAnArray");
        try {
            assertThrows(ClassCastException.class, () -> new CFArrayRef(cfStr.segment()));
        } finally {
            cfStr.release();
        }
    }

    @Test
    void testCFDataRefRejectsString() {
        var cfStr = CFStringRef.createCFString("notData");
        try {
            assertThrows(ClassCastException.class, () -> new CFDataRef(cfStr.segment()));
        } finally {
            cfStr.release();
        }
    }

    @Test
    void testCFDictionaryRefRejectsString() {
        var cfStr = CFStringRef.createCFString("notADict");
        try {
            assertThrows(ClassCastException.class, () -> new CFDictionaryRef(cfStr.segment()));
        } finally {
            cfStr.release();
        }
    }
}
