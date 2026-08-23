/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.unix;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;

/**
 * C library. This class should be considered non-API as it may be removed if/when its code is incorporated into the JNA
 * project.
 */
public interface AixLibc extends CLibrary {

    AixLibc INSTANCE = Native.load("c", AixLibc.class);

    /**
     * Returns the caller's kernel thread ID.
     * <p>
     * AIX declares this as {@code tid_t}, whose width follows {@code long}: measured at 4 bytes compiling with
     * {@code -maix32} and 8 with {@code -maix64}. {@link NativeLong} is therefore the mapping that stays correct in
     * either data model, as it is for FreeBSD's {@code thr_self}.
     *
     * @return the caller's kernel thread ID.
     */
    NativeLong thread_self();
}
