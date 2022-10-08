/*
 * Copyright 2019-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.windows;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

import oshi.util.Util;

public interface WinNT extends com.sun.jna.platform.win32.WinNT {

    @FieldOrder({ "TokenIsElevated" })
    class TOKEN_ELEVATION extends Structure implements AutoCloseable {
        public int TokenIsElevated;

        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }
}
