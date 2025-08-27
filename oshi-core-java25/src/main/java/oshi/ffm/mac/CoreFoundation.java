/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static oshi.ffm.ForeignFunctions.getByteArrayFromNativePointer;
import static oshi.ffm.mac.CoreFoundationFunctions.ARRAY_TYPE_ID;
import static oshi.ffm.mac.CoreFoundationFunctions.BOOLEAN_TYPE_ID;
import static oshi.ffm.mac.CoreFoundationFunctions.CFAllocatorGetDefault;
import static oshi.ffm.mac.CoreFoundationFunctions.CFArrayGetCount;
import static oshi.ffm.mac.CoreFoundationFunctions.CFArrayGetValueAtIndex;
import static oshi.ffm.mac.CoreFoundationFunctions.CFBooleanGetValue;
import static oshi.ffm.mac.CoreFoundationFunctions.CFDataGetBytePtr;
import static oshi.ffm.mac.CoreFoundationFunctions.CFDataGetLength;
import static oshi.ffm.mac.CoreFoundationFunctions.CFDateFormatterCreate;
import static oshi.ffm.mac.CoreFoundationFunctions.CFDateFormatterGetFormat;
import static oshi.ffm.mac.CoreFoundationFunctions.CFDictionaryGetCount;
import static oshi.ffm.mac.CoreFoundationFunctions.CFDictionaryGetValue;
import static oshi.ffm.mac.CoreFoundationFunctions.CFDictionaryGetValueIfPresent;
import static oshi.ffm.mac.CoreFoundationFunctions.CFDictionarySetValue;
import static oshi.ffm.mac.CoreFoundationFunctions.CFEqual;
import static oshi.ffm.mac.CoreFoundationFunctions.CFGetTypeID;
import static oshi.ffm.mac.CoreFoundationFunctions.CFLocaleCopyCurrent;
import static oshi.ffm.mac.CoreFoundationFunctions.CFNumberGetValue;
import static oshi.ffm.mac.CoreFoundationFunctions.CFRelease;
import static oshi.ffm.mac.CoreFoundationFunctions.CFRetain;
import static oshi.ffm.mac.CoreFoundationFunctions.CFStringCreateWithCharacters;
import static oshi.ffm.mac.CoreFoundationFunctions.CFStringGetCString;
import static oshi.ffm.mac.CoreFoundationFunctions.CFStringGetLength;
import static oshi.ffm.mac.CoreFoundationFunctions.CFStringGetMaximumSizeForEncoding;
import static oshi.ffm.mac.CoreFoundationFunctions.DATA_TYPE_ID;
import static oshi.ffm.mac.CoreFoundationFunctions.DICTIONARY_TYPE_ID;
import static oshi.ffm.mac.CoreFoundationFunctions.NUMBER_TYPE_ID;
import static oshi.ffm.mac.CoreFoundationFunctions.STRING_TYPE_ID;
import static oshi.ffm.mac.CoreFoundationHeaders.kCFNotFound;
import static oshi.ffm.mac.CoreFoundationHeaders.kCFNumberCharType;
import static oshi.ffm.mac.CoreFoundationHeaders.kCFNumberDoubleType;
import static oshi.ffm.mac.CoreFoundationHeaders.kCFNumberFloatType;
import static oshi.ffm.mac.CoreFoundationHeaders.kCFNumberIntType;
import static oshi.ffm.mac.CoreFoundationHeaders.kCFNumberLongLongType;
import static oshi.ffm.mac.CoreFoundationHeaders.kCFNumberShortType;
import static oshi.ffm.mac.CoreFoundationHeaders.kCFStringEncodingUTF8;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/**
 * Core Foundation is a framework that provides fundamental software services useful to application services,
 * application environments, and to applications themselves. Core Foundation also provides abstractions for common data
 * types.
 * <p>
 * This is the FFM implementation of the CoreFoundation framework.
 */
public interface CoreFoundation {

    /**
     * Base class for all CoreFoundation objects
     */
    class CFTypeRef {
        private final MemorySegment segment;

        public CFTypeRef(MemorySegment segment) {
            this.segment = segment;
        }

        public MemorySegment segment() {
            return segment;
        }

        public boolean isNull() {
            return segment == null || segment.equals(MemorySegment.NULL);
        }

        /**
         * Gets the type ID of this CF object
         *
         * @return The type ID
         */
        public long getTypeID() {
            if (isNull()) {
                return 0;
            }
            try {
                return CFGetTypeID(segment());
            } catch (Throwable e) {
                return 0;
            }
        }

        /**
         * Tests if this object has the specified type ID
         *
         * @param typeID The type ID to test against
         * @return True if the types match
         */
        public boolean isTypeID(long typeID) {
            return getTypeID() == typeID;
        }

        /**
         * Retains this object (increments reference count)
         */
        public void retain() {
            if (!isNull()) {
                try {
                    CFRetain(segment());
                } catch (Throwable e) {
                    // Ignore
                }
            }
        }

        /**
         * Releases this object (decrements reference count)
         */
        public void release() {
            if (!isNull()) {
                try {
                    CFRelease(segment());
                } catch (Throwable e) {
                    // Ignore
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CFTypeRef cfTypeRef = (CFTypeRef) o;
            if (isNull() || cfTypeRef.isNull()) {
                return isNull() == cfTypeRef.isNull();
            }
            try {
                return CFEqual(segment(), cfTypeRef.segment);
            } catch (Throwable e) {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(segment());
        }
    }

    /**
     * A reference to a CFAllocator object
     */
    class CFAllocatorRef extends CFTypeRef {
        public CFAllocatorRef(MemorySegment segment) {
            super(segment);
        }
    }

    /**
     * A reference to a CFNumber object
     */
    class CFNumberRef extends CFTypeRef {
        public CFNumberRef(MemorySegment segment) {
            super(segment);
            if (!isNull() && !isTypeID(NUMBER_TYPE_ID)) {
                throw new ClassCastException("Unable to cast to CFNumber. Type ID: " + getTypeID());
            }
        }

        /**
         * Convert this CFNumber to a long
         *
         * @return The long value
         */
        public long longValue() {
            if (isNull()) {
                return 0;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment valuePtr = arena.allocate(ValueLayout.JAVA_LONG);
                if (CFNumberGetValue(segment(), kCFNumberLongLongType, valuePtr)) {
                    return valuePtr.get(ValueLayout.JAVA_LONG, 0);
                }
                return 0;
            } catch (Throwable e) {
                return 0;
            }
        }

        /**
         * Convert this CFNumber to an int
         *
         * @return The int value
         */
        public int intValue() {
            if (isNull()) {
                return 0;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment valuePtr = arena.allocate(ValueLayout.JAVA_INT);
                if (CFNumberGetValue(segment(), kCFNumberIntType, valuePtr)) {
                    return valuePtr.get(ValueLayout.JAVA_INT, 0);
                }
                return 0;
            } catch (Throwable e) {
                return 0;
            }
        }

        /**
         * Convert this CFNumber to a short
         *
         * @return The short value
         */
        public short shortValue() {
            if (isNull()) {
                return 0;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment valuePtr = arena.allocate(ValueLayout.JAVA_SHORT);
                if (CFNumberGetValue(segment(), kCFNumberShortType, valuePtr)) {
                    return valuePtr.get(ValueLayout.JAVA_SHORT, 0);
                }
                return 0;
            } catch (Throwable e) {
                return 0;
            }
        }

        /**
         * Convert this CFNumber to a byte
         *
         * @return The byte value
         */
        public byte byteValue() {
            if (isNull()) {
                return 0;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment valuePtr = arena.allocate(ValueLayout.JAVA_BYTE);
                if (CFNumberGetValue(segment(), kCFNumberCharType, valuePtr)) {
                    return valuePtr.get(ValueLayout.JAVA_BYTE, 0);
                }
                return 0;
            } catch (Throwable e) {
                return 0;
            }
        }

        /**
         * Convert this CFNumber to a double
         *
         * @return The double value
         */
        public double doubleValue() {
            if (isNull()) {
                return 0;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment valuePtr = arena.allocate(ValueLayout.JAVA_DOUBLE);
                if (CFNumberGetValue(segment(), kCFNumberDoubleType, valuePtr)) {
                    return valuePtr.get(ValueLayout.JAVA_DOUBLE, 0);
                }
                return 0;
            } catch (Throwable e) {
                return 0;
            }
        }

        /**
         * Convert this CFNumber to a float
         *
         * @return The float value
         */
        public float floatValue() {
            if (isNull()) {
                return 0;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment valuePtr = arena.allocate(ValueLayout.JAVA_FLOAT);
                if (CFNumberGetValue(segment(), kCFNumberFloatType, valuePtr)) {
                    return valuePtr.get(ValueLayout.JAVA_FLOAT, 0);
                }
                return 0;
            } catch (Throwable e) {
                return 0;
            }
        }
    }

    /**
     * A reference to a CFBoolean object
     */
    class CFBooleanRef extends CFTypeRef {
        public CFBooleanRef(MemorySegment segment) {
            super(segment);
            if (!isNull() && !isTypeID(BOOLEAN_TYPE_ID)) {
                throw new ClassCastException("Unable to cast to CFBoolean. Type ID: " + getTypeID());
            }
        }

        /**
         * Get the boolean value
         *
         * @return The boolean value
         */
        public boolean booleanValue() {
            if (isNull()) {
                return false;
            }
            try {
                return CFBooleanGetValue(segment()) != 0;
            } catch (Throwable e) {
                return false;
            }
        }
    }

    /**
     * A reference to a CFArray object
     */
    class CFArrayRef extends CFTypeRef {
        public CFArrayRef(MemorySegment segment) {
            super(segment);
            if (!isNull() && !isTypeID(ARRAY_TYPE_ID)) {
                throw new ClassCastException("Unable to cast to CFArray. Type ID: " + getTypeID());
            }
        }

        /**
         * Get the count of items in the array
         *
         * @return Number of items
         */
        public int getCount() {
            if (isNull()) {
                return 0;
            }
            try {
                return (int) CFArrayGetCount(segment());
            } catch (Throwable e) {
                return 0;
            }
        }

        /**
         * Get a value at the specified index
         *
         * @param idx The index
         * @return The value at that index
         */
        public MemorySegment getValueAtIndex(int idx) {
            if (isNull()) {
                return MemorySegment.NULL;
            }
            try {
                return CFArrayGetValueAtIndex(segment(), idx);
            } catch (Throwable e) {
                return MemorySegment.NULL;
            }
        }
    }

    /**
     * A reference to a CFData object
     */
    class CFDataRef extends CFTypeRef {
        public CFDataRef(MemorySegment segment) {
            super(segment);
            if (!isNull() && !isTypeID(DATA_TYPE_ID)) {
                throw new ClassCastException("Unable to cast to CFData. Type ID: " + getTypeID());
            }
        }

        /**
         * Get the length of the data in bytes
         *
         * @return Length in bytes
         */
        public int getLength() {
            if (isNull()) {
                return 0;
            }
            try {
                return (int) CFDataGetLength(segment());
            } catch (Throwable e) {
                return 0;
            }
        }

        /**
         * Get a pointer to the bytes in the data
         *
         * @return Pointer to bytes
         */
        public MemorySegment getBytePtr() {
            if (isNull()) {
                return MemorySegment.NULL;
            }
            try {
                return CFDataGetBytePtr(segment());
            } catch (Throwable e) {
                return MemorySegment.NULL;
            }
        }

        /**
         * Get the data as a byte array
         *
         * @return Byte array containing the data
         */
        public byte[] getBytes() {
            if (isNull()) {
                return new byte[0];
            }
            try (Arena arena = Arena.ofConfined()) {
                int length = getLength();
                MemorySegment bytePtr = getBytePtr();
                return getByteArrayFromNativePointer(bytePtr, length, arena);
            } catch (Throwable e) {
                return new byte[0];
            }
        }
    }

    /**
     * A reference to a CFDictionary object
     */
    class CFDictionaryRef extends CFTypeRef {
        public CFDictionaryRef(MemorySegment segment) {
            super(segment);
            if (!isNull() && !isTypeID(DICTIONARY_TYPE_ID)) {
                throw new ClassCastException("Unable to cast to CFDictionary. Type ID: " + getTypeID());
            }
        }

        /**
         * Get a value from the dictionary
         *
         * @param key The key
         * @return The value associated with the key, or null if not found
         */
        public MemorySegment getValue(CFTypeRef key) {
            if (isNull() || key == null || key.isNull()) {
                return MemorySegment.NULL;
            }
            try {
                return CFDictionaryGetValue(segment(), key.segment);
            } catch (Throwable e) {
                return MemorySegment.NULL;
            }
        }

        /**
         * Get the count of key-value pairs in the dictionary
         *
         * @return Number of pairs
         */
        public long getCount() {
            if (isNull()) {
                return 0;
            }
            try {
                return CFDictionaryGetCount(segment());
            } catch (Throwable e) {
                return 0;
            }
        }

        /**
         * Get a value if present
         *
         * @param key   The key
         * @param value Pointer to store the value
         * @return True if the key exists
         */
        public boolean getValueIfPresent(CFTypeRef key, MemorySegment value) {
            if (isNull() || key == null || key.isNull()) {
                return false;
            }
            try {
                return CFDictionaryGetValueIfPresent(segment(), key.segment, value) != 0;
            } catch (Throwable e) {
                return false;
            }
        }
    }

    /**
     * A reference to a mutable CFDictionary object
     */
    class CFMutableDictionaryRef extends CFDictionaryRef {
        public CFMutableDictionaryRef(MemorySegment segment) {
            super(segment);
        }

        /**
         * Set a value in the dictionary
         *
         * @param key   The key
         * @param value The value
         */
        public void setValue(CFTypeRef key, CFTypeRef value) {
            if (isNull() || key == null || key.isNull() || value == null || value.isNull()) {
                return;
            }
            try {
                CFDictionarySetValue(segment(), key.segment, value.segment);
            } catch (Throwable e) {
                // Ignore
            }
        }
    }

    /**
     * A reference to a CFString object
     */
    class CFStringRef extends CFTypeRef {
        public CFStringRef(MemorySegment segment) {
            super(segment);
            if (!isNull() && !isTypeID(STRING_TYPE_ID)) {
                throw new ClassCastException("Unable to cast to CFString. Type ID: " + getTypeID());
            }
        }

        /**
         * Create a CFString from a Java String
         *
         * @param s The Java string
         * @return The CFString
         */
        public static CFStringRef createCFString(String s) {
            try (Arena arena = Arena.ofConfined()) {
                char[] chars = s.toCharArray();
                MemorySegment charsSeg = arena.allocateFrom(ValueLayout.JAVA_CHAR, chars);

                MemorySegment allocator = CFAllocatorGetDefault();
                MemorySegment stringRef = CFStringCreateWithCharacters(allocator, charsSeg, chars.length);

                return new CFStringRef(stringRef);
            } catch (Throwable e) {
                return new CFStringRef(MemorySegment.NULL);
            }
        }

        /**
         * Convert this CFString to a Java String
         *
         * @return The Java String value
         */
        public String stringValue() {
            if (isNull()) {
                return "";
            }

            try (Arena arena = Arena.ofConfined()) {
                // Get length and calculate buffer size
                long length = CFStringGetLength(segment());
                if (length == 0) {
                    return "";
                }

                long maxSize = CFStringGetMaximumSizeForEncoding(length, kCFStringEncodingUTF8);
                if (maxSize == kCFNotFound) {
                    throw new StringIndexOutOfBoundsException("CFString maximum number of bytes exceeds LONG_MAX.");
                }

                // Allocate buffer (add 1 for null terminator)
                MemorySegment buf = arena.allocate(maxSize + 1);

                if (CFStringGetCString(segment(), buf, maxSize + 1, kCFStringEncodingUTF8)) {
                    return buf.getString(0);
                }

                throw new IllegalArgumentException("CFString conversion failed or buffer too small.");
            } catch (Throwable e) {
                return "";
            }
        }
    }

    /**
     * A reference to a CFLocale object
     */
    class CFLocale extends CFTypeRef {
        public CFLocale(MemorySegment segment) {
            super(segment);
        }

        /**
         * Get the current locale
         *
         * @return The current locale
         */
        public static CFLocale copyCurrent() {
            try {
                return new CFLocale(CFLocaleCopyCurrent());
            } catch (Throwable e) {
                return new CFLocale(MemorySegment.NULL);
            }
        }
    }

    /**
     * A reference to a CFDateFormatter object
     */
    class CFDateFormatter extends CFTypeRef {
        public CFDateFormatter(MemorySegment segment) {
            super(segment);
        }

        /**
         * Create a new date formatter
         *
         * @param allocator The allocator (can be null)
         * @param locale    The locale to use (can be null)
         * @param dateStyle The date style
         * @param timeStyle The time style
         * @return A new date formatter
         */
        public static CFDateFormatter create(CFAllocatorRef allocator, CFLocale locale, int dateStyle, int timeStyle) {
            try {
                MemorySegment allocSeg = allocator != null ? allocator.segment() : MemorySegment.NULL;
                MemorySegment localeSeg = locale != null ? locale.segment() : MemorySegment.NULL;

                MemorySegment formatter = CFDateFormatterCreate(allocSeg, localeSeg, dateStyle, timeStyle);
                return new CFDateFormatter(formatter);
            } catch (Throwable e) {
                return new CFDateFormatter(MemorySegment.NULL);
            }
        }

        /**
         * Get the format string
         *
         * @return The format string
         */
        public CFStringRef getFormat() {
            if (isNull()) {
                return new CFStringRef(MemorySegment.NULL);
            }
            try {
                return new CFStringRef(CFDateFormatterGetFormat(segment()));
            } catch (Throwable e) {
                return new CFStringRef(MemorySegment.NULL);
            }
        }
    }
}
