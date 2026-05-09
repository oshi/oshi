/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

class FileUtilBufferTest {

    @Test
    void testReadByteFromBuffer() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { 0x42, 0x13 });
        assertThat(FileUtil.readByteFromBuffer(buf), is((byte) 0x42));
        assertThat(FileUtil.readByteFromBuffer(buf), is((byte) 0x13));
    }

    @Test
    void testReadByteFromBufferEmpty() {
        ByteBuffer buf = ByteBuffer.allocate(0);
        assertThat(FileUtil.readByteFromBuffer(buf), is((byte) 0));
    }

    @Test
    void testReadShortFromBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(ByteOrder.nativeOrder());
        buf.putShort((short) 1234);
        buf.putShort((short) -1);
        buf.flip();
        assertThat(FileUtil.readShortFromBuffer(buf), is((short) 1234));
        assertThat(FileUtil.readShortFromBuffer(buf), is((short) -1));
    }

    @Test
    void testReadShortFromBufferInsufficient() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { 0x01 });
        assertThat(FileUtil.readShortFromBuffer(buf), is((short) 0));
    }

    @Test
    void testReadIntFromBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(ByteOrder.nativeOrder());
        buf.putInt(305419896);
        buf.flip();
        assertThat(FileUtil.readIntFromBuffer(buf), is(305419896));
    }

    @Test
    void testReadIntFromBufferInsufficient() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { 0x01, 0x02 });
        assertThat(FileUtil.readIntFromBuffer(buf), is(0));
    }

    @Test
    void testReadLongFromBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(ByteOrder.nativeOrder());
        buf.putLong(123456789012345L);
        buf.flip();
        assertThat(FileUtil.readLongFromBuffer(buf), is(123456789012345L));
    }

    @Test
    void testReadLongFromBufferInsufficient() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { 0x01, 0x02, 0x03, 0x04 });
        assertThat(FileUtil.readLongFromBuffer(buf), is(0L));
    }

    @Test
    void testReadByteArrayFromBuffer() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        byte[] arr = new byte[3];
        FileUtil.readByteArrayFromBuffer(buf, arr);
        assertThat(arr[0], is((byte) 1));
        assertThat(arr[1], is((byte) 2));
        assertThat(arr[2], is((byte) 3));
    }

    @Test
    void testReadByteArrayFromBufferInsufficient() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { 1, 2 });
        byte[] arr = new byte[5];
        FileUtil.readByteArrayFromBuffer(buf, arr);
        // Array should remain zeroed since buffer doesn't have enough
        assertThat(arr[0], is((byte) 0));
    }

    @Test
    void testReadAllBytesAsBuffer() {
        // This tests the readAllBytesAsBuffer method with a non-existent file
        ByteBuffer buf = FileUtil.readAllBytesAsBuffer("/nonexistent/file/path");
        assertThat(buf.remaining(), is(0));
    }
}
