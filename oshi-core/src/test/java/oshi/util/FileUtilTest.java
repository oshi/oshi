/*
 * Copyright 2016-2024 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sun.jna.Native;

/**
 * Tests FileUtil
 */
class FileUtilTest {

    /**
     * Test read file.
     */
    @Test
    void testReadFile() {
        // Write to a temp file
        Path multilineFile = null;
        try {
            multilineFile = Files.createTempFile("oshitest.multiline", null);
            String s = "Line 1\nLine 2\nThe third line\nLine 4\nLine 5\n";
            Files.write(multilineFile, s.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            fail("IO Exception creating or writing to temporary multiline file.");
        }

        // Try the new temp file
        List<String> tempFileStrings = FileUtil.readFile(multilineFile.toString());
        assertThat("Temp file line one mismatch", tempFileStrings.get(0), is("Line 1"));
        List<String> matchingLines = tempFileStrings.stream().filter(s -> s.startsWith("Line "))
                .collect(Collectors.toList());
        assertThat("Matching lines mismatch", matchingLines.size(), is(4));

        // Delete the temp file
        try {
            Files.deleteIfExists(multilineFile);
        } catch (IOException e) {
            fail("IO Exception deleting temporary multiline file.");
        }

        // Try file not found on deleted file
        assertThat("Deleted file should return empty", FileUtil.readFile(multilineFile.toString()), is(empty()));
    }

    /**
     * Test get*FromFile
     */
    @Test
    void testGetFromFile() {
        // Write to temp file
        Path integerFile = null;
        try {
            integerFile = Files.createTempFile("oshitest.int", null);
            Files.write(integerFile, "123\n".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            fail("IO Exception creating or writing to temporary integer file.");
        }
        assertThat("unsigned long from int", FileUtil.getUnsignedLongFromFile(integerFile.toString()), is(123L));
        assertThat("long from int", FileUtil.getLongFromFile(integerFile.toString()), is(123L));
        assertThat("int from int", FileUtil.getIntFromFile(integerFile.toString()), is(123));
        assertThat("string from int", FileUtil.getStringFromFile(integerFile.toString()), is("123"));

        // Delete the temp file
        try {
            Files.deleteIfExists(integerFile);
        } catch (IOException e) {
            fail("IO Exception deleting temporary integer file.");
        }

        // Write to temp file
        Path stringFile = null;
        try {
            stringFile = Files.createTempFile("oshitest.str", null);
            Files.write(stringFile, "foo bar\n".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            fail("IO Exception creating or writing to temporary string file.");
        }

        assertThat("unsigned long from string", FileUtil.getUnsignedLongFromFile(stringFile.toString()), is(0L));
        assertThat("long from string", FileUtil.getLongFromFile(stringFile.toString()), is(0L));
        assertThat("int from string", FileUtil.getIntFromFile(stringFile.toString()), is(0));
        assertThat("string from string", FileUtil.getStringFromFile(stringFile.toString()), is("foo bar"));
        // Delete the temp file
        try {
            Files.deleteIfExists(stringFile);
        } catch (IOException e) {
            fail("IO Exception deleting temporary string file.");
        }

        // Try file not found on deleted file
        assertThat("unsigned long from invalid", FileUtil.getUnsignedLongFromFile(stringFile.toString()), is(0L));
        assertThat("long from invalid", FileUtil.getLongFromFile(stringFile.toString()), is(0L));
        assertThat("int from invalid", FileUtil.getIntFromFile(stringFile.toString()), is(0));
        assertThat("string from invalid ", FileUtil.getStringFromFile(stringFile.toString()), is(emptyString()));
    }

    @Test
    void testReadProcIo() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("rchar", "124788352");
        expected.put("wchar", "124802481");
        expected.put("syscr", "135");
        expected.put("syscw", "1547");
        expected.put("read_bytes", "40304640");
        expected.put("write_bytes", "124780544");
        expected.put("cancelled_write_bytes", "42");
        // Write this to a temp file
        Path procIoFile = null;
        try {
            procIoFile = Files.createTempFile("oshitest.procio", null);
            for (Entry<String, String> e : expected.entrySet()) {
                String s = e.getKey() + ": " + e.getValue() + "\n";
                Files.write(procIoFile, s.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            fail("IO Exception creating or writing to temporary procIo file.");
        }
        // Read into map
        Map<String, String> actual = FileUtil.getKeyValueMapFromFile(procIoFile.toString(), ":");
        assertThat("procio size", actual, is(aMapWithSize(expected.size())));
        for (Entry<String, String> entry : expected.entrySet()) {
            assertThat("procio entry", actual, hasEntry(entry.getKey(), entry.getValue()));
        }

        // Cleanup
        try {
            Files.deleteIfExists(procIoFile);
        } catch (IOException e) {
            fail("IO Exception deleting temporary procIo file.");
        }

        // Test deleted file
        actual = FileUtil.getKeyValueMapFromFile(procIoFile.toString(), ":");
        assertThat("procio size", actual, anEmptyMap());
    }

    @Test
    void testReadProperties() {
        Properties props = FileUtil.readPropertiesFromFilename("simplelogger.properties");
        assertThat("simplelogger properties", props.getProperty("org.slf4j.simpleLogger.defaultLogLevel"), is("INFO"));
        props = FileUtil.readPropertiesFromFilename("this.file.does.not.exist");
        assertThat("invalid file", props.stringPropertyNames(), is(empty()));
    }

    @Test
    void testReadBytesFromURL() throws IOException {
        // Create temporary files
        Path file1 = Files.createTempFile("oshitest.file1", null);
        Path file2 = Files.createTempFile("oshitest.file2", null);
        Path file3 = Files.createTempFile("oshitest.file3", null);

        Files.write(file1, "Same".getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
        Files.write(file2, "Same".getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
        Files.write(file3, "Different".getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);

        byte[] bytes1 = FileUtil.readFileAsBytes(file1.toUri().toURL());
        byte[] bytes2 = FileUtil.readFileAsBytes(file2.toUri().toURL());
        byte[] bytes3 = FileUtil.readFileAsBytes(file3.toUri().toURL());

        assertArrayEquals(bytes1, bytes2, "Byte arrays should match");
        assertFalse(Arrays.equals(bytes1, bytes3), "Byte arrays should not match");
    }

    @Test
    void testReadBinaryFile() {
        ByteBuffer buff = ByteBuffer.allocate(18 + Native.LONG_SIZE + Native.SIZE_T_SIZE);
        buff.order(ByteOrder.nativeOrder());
        buff.putLong(123L);
        buff.putInt(45);
        buff.putShort((short) 67);
        buff.put((byte) 89);
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
        byte[] arr = new byte[] { 1, 2, 3 };
        buff.put(arr);

        // Write to temp file
        Path binaryFile = null;
        try {
            binaryFile = Files.createTempFile("oshitest.binary", null);
            Files.write(binaryFile, buff.array());
        } catch (IOException e) {
            fail("IO Exception creating or writing to temporary binary file.");
        }

        // Read from file
        buff = FileUtil.readAllBytesAsBuffer(binaryFile.toString());
        assertThat("Buffer size should match bytes written", buff.limit(),
                is(18 + Native.LONG_SIZE + Native.SIZE_T_SIZE));
        assertThat("Long from buffer should match", FileUtil.readLongFromBuffer(buff), is(123L));
        assertThat("Int from buffer should match", FileUtil.readIntFromBuffer(buff), is(45));
        assertThat("Short from buffer should match", FileUtil.readShortFromBuffer(buff), is((short) 67));
        assertThat("Byte from buffer should match", FileUtil.readByteFromBuffer(buff), is((byte) 89));
        assertThat("NativeLong from buffer should match", FileUtil.readNativeLongFromBuffer(buff).longValue(), is(10L));
        assertThat("SizeT from buffer should match", FileUtil.readSizeTFromBuffer(buff).longValue(), is(11L));
        byte[] array = new byte[3];
        FileUtil.readByteArrayFromBuffer(buff, array);
        assertArrayEquals(arr, array, "Byte array from buffer should match");
        // Test reads past end of file
        assertThat("Long from buffer at limit should be 0", FileUtil.readLongFromBuffer(buff), is(0L));
        assertThat("Int from buffer at limit should be 0", FileUtil.readIntFromBuffer(buff), is(0));
        assertThat("Short from buffer at limit should be 0", FileUtil.readShortFromBuffer(buff), is((short) 0));
        assertThat("Byte from buffer at limit should be 0", FileUtil.readByteFromBuffer(buff), is((byte) 0));
        assertThat("NativeLong from buffer at limit should be 0", FileUtil.readNativeLongFromBuffer(buff).longValue(),
                is(0L));
        assertThat("SizeT from buffer at limit should be 0", FileUtil.readSizeTFromBuffer(buff).longValue(), is(0L));
        byte[] arr0 = new byte[] { 0, 0, 0 };
        array = new byte[3];
        FileUtil.readByteArrayFromBuffer(buff, array);
        assertArrayEquals(arr0, array, "Byte array from buffer at limit should be all 0s");

        // Cleanup
        try {
            Files.deleteIfExists(binaryFile);
        } catch (IOException e) {
            fail("IO Exception deleting temporary procIo file.");
        }
    }
}
