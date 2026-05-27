/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.unix;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Helper to load the BSD C library from the current process. Since the JVM already links against libc, loading with
 * {@code null} retrieves it from the running process without needing platform-specific library name resolution.
 */
final class BsdLibcLoader {

    private BsdLibcLoader() {
    }

    /**
     * Loads the BSD C library.
     *
     * @param <T>            the library interface type
     * @param interfaceClass the JNA library interface class
     * @return the loaded library instance
     */
    static <T extends Library> T loadLibc(Class<T> interfaceClass) {
        return Native.load(null, interfaceClass);
    }
}
