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
    CoreFoundation INSTANCE = Native.loadLibrary("CoreFoundation", CoreFoundation.class);

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