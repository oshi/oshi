/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.unix;

import com.sun.jna.Native;

/**
 * C library. This class should be considered non-API as it may be removed if/when its code is incorporated into the JNA
 * project.
 */
public interface AixLibc extends CLibrary {

    AixLibc INSTANCE = Native.load("c", AixLibc.class);

    /**
     * Returns the caller's kernel thread ID.
     *
     * @return the caller's kernel thread ID.
     */
    int thread_self();
}
