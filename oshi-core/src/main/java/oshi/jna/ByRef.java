/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna;

import com.sun.jna.NativeLong;
import com.sun.jna.platform.unix.LibCAPI.size_t;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTRByReference;
import com.sun.jna.platform.win32.Tlhelp32.PROCESSENTRY32;
import com.sun.jna.platform.win32.WinDef.LONGLONGByReference;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.util.Util;

/**
 * Wrapper classes for JNA clases which extend {@link com.sun.jna.ptr.ByReference} intended for use in
 * try-with-resources blocks.
 */
public interface ByRef {

    class CloseableIntByReference extends IntByReference implements AutoCloseable {
        public CloseableIntByReference() {
            super();
        }

        public CloseableIntByReference(int value) {
            super(value);
        }

        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableLongByReference extends LongByReference implements AutoCloseable {
        public CloseableLongByReference() {
            super();
        }

        public CloseableLongByReference(long value) {
            super(value);
        }

        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableNativeLongByReference extends NativeLongByReference implements AutoCloseable {
        public CloseableNativeLongByReference() {
            super();
        }

        public CloseableNativeLongByReference(NativeLong nativeLong) {
            super(nativeLong);
        }

        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseablePointerByReference extends PointerByReference implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableLONGLONGByReference extends LONGLONGByReference implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableULONGptrByReference extends ULONG_PTRByReference implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableHANDLEByReference extends HANDLEByReference implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseableSizeTByReference extends size_t.ByReference implements AutoCloseable {
        public CloseableSizeTByReference() {
            super();
        }

        public CloseableSizeTByReference(long value) {
            super(value);
        }

        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    class CloseablePROCESSENTRY32ByReference extends PROCESSENTRY32.ByReference implements AutoCloseable {
        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }
}
