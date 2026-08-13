/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import oshi.ffm.ForeignFunctions;

class FileUtilFFMTest {

    @Test
    void testNativeSizeConstants() {
        assertThat("NATIVE_LONG_SIZE should be 4 or 8", ForeignFunctions.NATIVE_LONG_SIZE, is(oneOf(4L, 8L)));
        assertThat("NATIVE_SIZE_T_SIZE should be 4 or 8", ForeignFunctions.NATIVE_SIZE_T_SIZE, is(oneOf(4L, 8L)));
        assertThat("NATIVE_POINTER_SIZE should be 4 or 8", ForeignFunctions.NATIVE_POINTER_SIZE, is(oneOf(4L, 8L)));
    }

    @Test
    void testReadNativeLongFromBuffer() {
        ByteBuffer buff = ByteBuffer.allocate(8).order(ByteOrder.nativeOrder());
        buff.putLong(0x0000_0000_0000_002AL);
        buff.flip();
        long value = FileUtilFFM.readNativeLongFromBuffer(buff);
        assertEquals(42L, value);
    }

    @Test
    void testReadSizeTFromBuffer() {
        ByteBuffer buff = ByteBuffer.allocate(8).order(ByteOrder.nativeOrder());
        buff.putLong(0x0000_0000_0000_0007L);
        buff.flip();
        long value = FileUtilFFM.readSizeTFromBuffer(buff);
        assertEquals(7L, value);
    }

    @Test
    void testReadPointerFromBuffer() {
        ByteBuffer buff = ByteBuffer.allocate(8).order(ByteOrder.nativeOrder());
        // Non-zero high 32 bits so the 32-bit and 64-bit read paths produce distinguishable values
        long written = 0x1234_5678_DEAD_BEEFL;
        buff.putLong(written);
        buff.flip();
        // Read the first 4 bytes the same way the 32-bit path does, so the expectation is endian-agnostic
        long expected32Bit = Integer.toUnsignedLong(buff.duplicate().getInt());
        long value = FileUtilFFM.readPointerFromBuffer(buff);
        if (ForeignFunctions.NATIVE_POINTER_SIZE == 4) {
            // 32-bit: reads only the first 4 bytes, as unsigned
            assertEquals(expected32Bit, value);
        } else {
            assertEquals(written, value);
        }
    }

    @Test
    void testReadPointerFromEmptyBuffer() {
        ByteBuffer buff = ByteBuffer.allocate(0);
        assertEquals(0L, FileUtilFFM.readPointerFromBuffer(buff));
    }
}
