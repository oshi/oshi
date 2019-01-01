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
 *
 * @author widdis[at]gmail[dot]com
 */
public interface CoreFoundation extends Library {
    CoreFoundation INSTANCE = Native.load("CoreFoundation", CoreFoundation.class);

    int UTF_8 = 0x08000100;

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
     * References are owned if created by functions including "Create" or "Copy"
     * and must be released with CFRelease to avoid leaking references
     */

    CFStringRef CFStringCreateWithCharacters(Object object, char[] chars, NativeLong length);

    CFMutableDictionaryRef CFDictionaryCreateMutable(CFAllocatorRef allocator, int capacity, Pointer keyCallBacks,
            Pointer valueCallBacks);

    void CFRelease(PointerType blob);

    /*
     * References are not owned if created by functions using "Get". Use
     * CFRetain if object retention is required, and then CFRelease later. Do
     * not use CFRelease if you do not own.
     */

    void CFDictionarySetValue(CFMutableDictionaryRef dict, PointerType key, PointerType value);

    Pointer CFDictionaryGetValue(CFDictionaryRef dictionary, CFStringRef key);

    boolean CFDictionaryGetValueIfPresent(CFDictionaryRef dictionary, CFStringRef key, PointerType value);

    boolean CFStringGetCString(Pointer cfString, Pointer bufferToFill, long maxSize, int encoding);

    boolean CFBooleanGetValue(Pointer booleanRef);

    CFTypeRef CFArrayGetValueAtIndex(CFArrayRef array, int index);

    void CFNumberGetValue(Pointer cfNumber, int intSize, ByReference value);

    long CFStringGetLength(Pointer str);

    long CFStringGetMaximumSizeForEncoding(long length, int encoding);

    CFAllocatorRef CFAllocatorGetDefault();

    class CFDataRef extends CFTypeRef {
    }

    int CFDataGetLength(CFTypeRef theData);

    PointerByReference CFDataGetBytePtr(CFTypeRef theData);
}