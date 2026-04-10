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
        buff.putLong(0x0000_0000_DEAD_BEEFL);
        buff.flip();
        long value = FileUtilFFM.readPointerFromBuffer(buff);
        if (ForeignFunctions.NATIVE_POINTER_SIZE == 4) {
            // 32-bit: reads 4 bytes as unsigned
            assertEquals(0xDEAD_BEEFL, value);
        } else {
            assertEquals(0x0000_0000_DEAD_BEEFL, value);
        }
    }

    @Test
    void testReadPointerFromEmptyBuffer() {
        ByteBuffer buff = ByteBuffer.allocate(0);
        assertEquals(0L, FileUtilFFM.readPointerFromBuffer(buff));
    }
}
