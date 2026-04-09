/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import com.sun.jna.Native;

/**
 * Tests JNA-specific buffer read methods in {@link FileUtilJNA}.
 */
class FileUtilJNATest {

    @Test
    void testReadNativeTypesFromBuffer() {
        ByteBuffer buff = ByteBuffer.allocate(Native.LONG_SIZE + Native.SIZE_T_SIZE);
        buff.order(ByteOrder.nativeOrder());
        if (Native.LONG_SIZE > 4) {
            buff.putLong(10L);
        } else {
            buff.putInt(10);
        }
        if (Native.SIZE_T_SIZE > 4) {
            buff.putLong(11L);
        } else {
            buff.putInt(11);
        }
        buff.flip();

        assertThat("NativeLong from buffer should match", FileUtilJNA.readNativeLongFromBuffer(buff).longValue(),
                is(10L));
        assertThat("SizeT from buffer should match", FileUtilJNA.readSizeTFromBuffer(buff).longValue(), is(11L));

        // Test reads past end of buffer
        assertThat("NativeLong from buffer at limit should be 0",
                FileUtilJNA.readNativeLongFromBuffer(buff).longValue(), is(0L));
        assertThat("SizeT from buffer at limit should be 0", FileUtilJNA.readSizeTFromBuffer(buff).longValue(), is(0L));
    }
}
