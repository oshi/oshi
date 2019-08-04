/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.jna.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * CoreFoundation framework for power supply stats. This class should be
 * considered non-API as it may be removed if/when its code is incorporated into
 * the JNA project.
 */
public interface CoreFoundation extends Library {
    /** Constant <code>INSTANCE</code> */
    CoreFoundation INSTANCE = Native.load("CoreFoundation", CoreFoundation.class);

    /** Constant <code>UTF_8=0x08000100</code> */
    int UTF_8 = 0x08000100;

    /**
     * <p>
     * CFArrayGetCount.
     * </p>
     *
     * @param array
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFArrayRef} object.
     * @return a int.
     */
    int CFArrayGetCount(CFArrayRef array);

    /*
     * Decorator class types for PointerType to better match ported code
     */
    class CFTypeRef extends PointerType {
    }

    class CFNumberRef extends PointerType {
    }

    class CFBooleanRef extends PointerType {
    }

    class CFArrayRef extends PointerType {
    }

    class CFDictionaryRef extends PointerType {
    }

    class CFMutableDictionaryRef extends CFDictionaryRef {
    }

    class CFAllocatorRef extends PointerType {
    }

    class CFStringRef extends PointerType {
        /**
         * Creates a new CFString from the given Java string.
         *
         * @param s
         *            A string
         * @return A reference to a CFString representing s
         */
        public static CFStringRef toCFString(String s) {
            final char[] chars = s.toCharArray();
            int length = chars.length;
            return CoreFoundation.INSTANCE.CFStringCreateWithCharacters(null, chars, new NativeLong(length));
        }
    }

    /*
     * References are owned if created by functions including "Create" or "Copy" and
     * must be released with CFRelease to avoid leaking references
     */

    /**
     * <p>
     * CFStringCreateWithCharacters.
     * </p>
     *
     * @param object
     *            a {@link java.lang.Object} object.
     * @param chars
     *            an array of {@link char} objects.
     * @param length
     *            a {@link com.sun.jna.NativeLong} object.
     * @return a {@link oshi.jna.platform.mac.CoreFoundation.CFStringRef} object.
     */
    CFStringRef CFStringCreateWithCharacters(Object object, char[] chars, NativeLong length);

    /**
     * <p>
     * CFDictionaryCreateMutable.
     * </p>
     *
     * @param allocator
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFAllocatorRef}
     *            object.
     * @param capacity
     *            a int.
     * @param keyCallBacks
     *            a {@link com.sun.jna.Pointer} object.
     * @param valueCallBacks
     *            a {@link com.sun.jna.Pointer} object.
     * @return a {@link oshi.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef}
     *         object.
     */
    CFMutableDictionaryRef CFDictionaryCreateMutable(CFAllocatorRef allocator, int capacity, Pointer keyCallBacks,
            Pointer valueCallBacks);

    /**
     * <p>
     * CFRelease.
     * </p>
     *
     * @param blob
     *            a {@link com.sun.jna.PointerType} object.
     */
    void CFRelease(PointerType blob);

    /*
     * References are not owned if created by functions using "Get". Use CFRetain if
     * object retention is required, and then CFRelease later. Do not use CFRelease
     * if you do not own.
     */

    /**
     * <p>
     * CFDictionarySetValue.
     * </p>
     *
     * @param dict
     *            a
     *            {@link oshi.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef}
     *            object.
     * @param key
     *            a {@link com.sun.jna.PointerType} object.
     * @param value
     *            a {@link com.sun.jna.PointerType} object.
     */
    void CFDictionarySetValue(CFMutableDictionaryRef dict, PointerType key, PointerType value);

    /**
     * <p>
     * CFDictionaryGetValue.
     * </p>
     *
     * @param dictionary
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef}
     *            object.
     * @param key
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFStringRef} object.
     * @return a {@link com.sun.jna.Pointer} object.
     */
    Pointer CFDictionaryGetValue(CFDictionaryRef dictionary, CFStringRef key);

    /**
     * <p>
     * CFDictionaryGetValueIfPresent.
     * </p>
     *
     * @param dictionary
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef}
     *            object.
     * @param key
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFStringRef} object.
     * @param value
     *            a {@link com.sun.jna.PointerType} object.
     * @return a boolean.
     */
    boolean CFDictionaryGetValueIfPresent(CFDictionaryRef dictionary, CFStringRef key, PointerType value);

    /**
     * <p>
     * CFStringGetCString.
     * </p>
     *
     * @param cfString
     *            a {@link com.sun.jna.Pointer} object.
     * @param bufferToFill
     *            a {@link com.sun.jna.Pointer} object.
     * @param maxSize
     *            a long.
     * @param encoding
     *            a int.
     * @return a boolean.
     */
    boolean CFStringGetCString(Pointer cfString, Pointer bufferToFill, long maxSize, int encoding);

    /**
     * <p>
     * CFBooleanGetValue.
     * </p>
     *
     * @param booleanRef
     *            a {@link com.sun.jna.Pointer} object.
     * @return a boolean.
     */
    boolean CFBooleanGetValue(Pointer booleanRef);

    /**
     * <p>
     * CFArrayGetValueAtIndex.
     * </p>
     *
     * @param array
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFArrayRef} object.
     * @param index
     *            a int.
     * @return a {@link oshi.jna.platform.mac.CoreFoundation.CFTypeRef} object.
     */
    CFTypeRef CFArrayGetValueAtIndex(CFArrayRef array, int index);

    /**
     * <p>
     * CFNumberGetValue.
     * </p>
     *
     * @param cfNumber
     *            a {@link com.sun.jna.Pointer} object.
     * @param intSize
     *            a int.
     * @param value
     *            a {@link com.sun.jna.ptr.ByReference} object.
     */
    void CFNumberGetValue(Pointer cfNumber, int intSize, ByReference value);

    /**
     * <p>
     * CFStringGetLength.
     * </p>
     *
     * @param str
     *            a {@link com.sun.jna.Pointer} object.
     * @return a long.
     */
    long CFStringGetLength(Pointer str);

    /**
     * <p>
     * CFStringGetMaximumSizeForEncoding.
     * </p>
     *
     * @param length
     *            a long.
     * @param encoding
     *            a int.
     * @return a long.
     */
    long CFStringGetMaximumSizeForEncoding(long length, int encoding);

    /**
     * <p>
     * CFAllocatorGetDefault.
     * </p>
     *
     * @return a {@link oshi.jna.platform.mac.CoreFoundation.CFAllocatorRef} object.
     */
    CFAllocatorRef CFAllocatorGetDefault();

    class CFDataRef extends CFTypeRef {
    }

    /**
     * <p>
     * CFDataGetLength.
     * </p>
     *
     * @param theData
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFTypeRef} object.
     * @return a int.
     */
    int CFDataGetLength(CFTypeRef theData);

    /**
     * <p>
     * CFDataGetBytePtr.
     * </p>
     *
     * @param theData
     *            a {@link oshi.jna.platform.mac.CoreFoundation.CFTypeRef} object.
     * @return a {@link com.sun.jna.ptr.PointerByReference} object.
     */
    PointerByReference CFDataGetBytePtr(CFTypeRef theData);
}
