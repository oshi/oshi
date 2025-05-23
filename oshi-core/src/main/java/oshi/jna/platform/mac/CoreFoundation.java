/*
 * Copyright 2021-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.mac;

import com.sun.jna.Native;

/**
 * CoreFoundation class. This class should be considered non-API as it may be removed if/when its code is incorporated into the
 * JNA project.
 */
public interface CoreFoundation extends com.sun.jna.platform.mac.CoreFoundation {

    CoreFoundation INSTANCE = Native.load("CoreFoundation", CoreFoundation.class);

    int kCFDateFormatterNoStyle = 0;
    int kCFDateFormatterShortStyle = 1;
    int kCFDateFormatterMediumStyle = 2;
    int kCFDateFormatterLongStyle = 3;
    int kCFDateFormatterFullStyle = 4;

    CFTypeRef CFLocaleCopyCurrent();

    CFTypeRef CFDateFormatterCreate(CFAllocatorRef allocator, CFTypeRef locale, long dateStyle, long timeStyle);

    CFStringRef CFDateFormatterGetFormat(CFTypeRef formatter);
}