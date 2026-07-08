/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Objective-C runtime bindings for dispatching messages to AppKit classes via {@code objc_msgSend}. This class should
 * be considered non-API as it may be removed if/when its code is incorporated into the JNA project.
 * <p>
 * On ARM64, {@code objc_msgSend} must be called with a typed signature matching the selector's actual parameter types.
 * Variadic {@code Object...} dispatch does not work reliably. Each overload below matches a specific argument shape.
 */
public interface ObjCRuntime extends Library {

    ObjCRuntime INSTANCE = Native.load("objc", ObjCRuntime.class);

    Pointer objc_getClass(String className);

    Pointer sel_registerName(String selectorName);

    Pointer objc_msgSend(Pointer receiver, Pointer selector);

    Pointer objc_msgSend(Pointer receiver, Pointer selector, long index);
}
