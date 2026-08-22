/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests FileUtil
 */
class FileUtilTest {

    /**
     * Test read file.
     */
    @Test
    void testReadFile(@TempDir Path tempDir) throws IOException {
        Path multilineFile = tempDir.resolve("multiline");
        Files.writeString(multilineFile, """
                Line 1
                Line 2
                The third line
                Line 4
                Line 5
                """);

        List<String> tempFileStrings = FileUtil.readFile(multilineFile.toString());
        assertThat("Temp file line one mismatch", tempFileStrings.get(0), is("Line 1"));
        List<String> matchingLines = tempFileStrings.stream().filter(s -> s.startsWith("Line ")).toList();
        assertThat("Matching lines mismatch", matchingLines.size(), is(4));

        // Deleting is the assertion here, not cleanup: readFile must return empty for a file that is not there
        Files.deleteIfExists(multilineFile);
        assertThat("Deleted file should return empty", FileUtil.readFile(multilineFile.toString()), is(empty()));
    }

    /**
     * Test get*FromFile
     */
    @Test
    void testGetFromFile(@TempDir Path tempDir) throws IOException {
        Path integerFile = tempDir.resolve("int");
        Files.writeString(integerFile, "123\n");
        assertThat("unsigned long from int", FileUtil.getUnsignedLongFromFile(integerFile.toString()), is(123L));
        assertThat("long from int", FileUtil.getLongFromFile(integerFile.toString()), is(123L));
        assertThat("int from int", FileUtil.getIntFromFile(integerFile.toString()), is(123));
        assertThat("string from int", FileUtil.getStringFromFile(integerFile.toString()), is("123"));

        Path stringFile = tempDir.resolve("str");
        Files.writeString(stringFile, "foo bar\n");

        assertThat("unsigned long from string", FileUtil.getUnsignedLongFromFile(stringFile.toString()), is(0L));
        assertThat("long from string", FileUtil.getLongFromFile(stringFile.toString()), is(0L));
        assertThat("int from string", FileUtil.getIntFromFile(stringFile.toString()), is(0));
        assertThat("string from string", FileUtil.getStringFromFile(stringFile.toString()), is("foo bar"));

        // Deleting is the assertion here, not cleanup: every getter must fall back for a file that is not there
        Files.deleteIfExists(stringFile);
        assertThat("unsigned long from invalid", FileUtil.getUnsignedLongFromFile(stringFile.toString()), is(0L));
        assertThat("long from invalid", FileUtil.getLongFromFile(stringFile.toString()), is(0L));
        assertThat("int from invalid", FileUtil.getIntFromFile(stringFile.toString()), is(0));
        assertThat("string from invalid ", FileUtil.getStringFromFile(stringFile.toString()), is(emptyString()));
    }

    /**
     * Test the get*FromFile overloads that take a caller-supplied default
     */
    @Test
    void testGetFromFileWithDefault(@TempDir Path tempDir) throws IOException {
        Path integerFile = tempDir.resolve("int");
        Files.writeString(integerFile, "123\n");
        assertThat("long from int", FileUtil.getLongFromFile(integerFile.toString(), -1L), is(123L));
        assertThat("int from int", FileUtil.getIntFromFile(integerFile.toString(), -1), is(123));

        Path zeroFile = tempDir.resolve("zero");
        Files.writeString(zeroFile, "0\n");
        // A file holding a genuine zero must not be confused with an absent one, which is the whole point of the
        // overload: the no-argument form returns 0 for both.
        assertThat("long from zero", FileUtil.getLongFromFile(zeroFile.toString(), -1L), is(0L));
        assertThat("int from zero", FileUtil.getIntFromFile(zeroFile.toString(), -1), is(0));

        Path stringFile = tempDir.resolve("str");
        Files.writeString(stringFile, "foo bar\n");
        assertThat("long from string", FileUtil.getLongFromFile(stringFile.toString(), -1L), is(-1L));
        assertThat("int from string", FileUtil.getIntFromFile(stringFile.toString(), -1), is(-1));

        Path missingFile = tempDir.resolve("does-not-exist");
        assertThat("long from missing", FileUtil.getLongFromFile(missingFile.toString(), -1L), is(-1L));
        assertThat("int from missing", FileUtil.getIntFromFile(missingFile.toString(), -1), is(-1));
    }

    @Test
    void testReadProcIo(@TempDir Path tempDir) throws IOException {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("rchar", "124788352");
        expected.put("wchar", "124802481");
        expected.put("syscr", "135");
        expected.put("syscw", "1547");
        expected.put("read_bytes", "40304640");
        expected.put("write_bytes", "124780544");
        expected.put("cancelled_write_bytes", "42");
        // Deliberately NOT a text block: the file body is derived from the map asserted against below, so a
        // literal would duplicate the data. Not every fixture wants the new syntax.
        Path procIoFile = Files.createFile(tempDir.resolve("procio"));
        for (Entry<String, String> e : expected.entrySet()) {
            Files.writeString(procIoFile, e.getKey() + ": " + e.getValue() + "\n", StandardOpenOption.APPEND);
        }
        // Read into map
        Map<String, String> actual = FileUtil.getKeyValueMapFromFile(procIoFile.toString(), ":");
        assertThat("procio size", actual, is(aMapWithSize(expected.size())));
        for (Entry<String, String> entry : expected.entrySet()) {
            assertThat("procio entry", actual, hasEntry(entry.getKey(), entry.getValue()));
        }

        // Deleting is the assertion here, not cleanup
        Files.deleteIfExists(procIoFile);
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
    void testReadBytesFromURL(@TempDir Path tempDir) throws IOException {
        Path file1 = tempDir.resolve("file1");
        Path file2 = tempDir.resolve("file2");
        Path file3 = tempDir.resolve("file3");

        Files.writeString(file1, "Same");
        Files.writeString(file2, "Same");
        Files.writeString(file3, "Different");

        byte[] bytes1 = FileUtil.readFileAsBytes(file1.toUri().toURL());
        byte[] bytes2 = FileUtil.readFileAsBytes(file2.toUri().toURL());
        byte[] bytes3 = FileUtil.readFileAsBytes(file3.toUri().toURL());

        assertArrayEquals(bytes1, bytes2, "Byte arrays should match");
        assertFalse(Arrays.equals(bytes1, bytes3), "Byte arrays should not match");
    }

    @Test
    void testReadBinaryFile(@TempDir Path tempDir) throws IOException {
        ByteBuffer buff = ByteBuffer.allocate(18);
        buff.order(ByteOrder.nativeOrder());
        buff.putLong(123L);
        buff.putInt(45);
        buff.putShort((short) 67);
        buff.put((byte) 89);
        byte[] arr = new byte[] { 1, 2, 3 };
        buff.put(arr);

        // Snapshot the backing array since buff is reassigned below
        Path binaryFile = tempDir.resolve("binary");
        Files.write(binaryFile, buff.array());

        // Read from file
        buff = FileUtil.readAllBytesAsBuffer(binaryFile.toString());
        assertThat("Buffer size should match bytes written", buff.limit(), is(18));
        assertThat("Long from buffer should match", FileUtil.readLongFromBuffer(buff), is(123L));
        assertThat("Int from buffer should match", FileUtil.readIntFromBuffer(buff), is(45));
        assertThat("Short from buffer should match", FileUtil.readShortFromBuffer(buff), is((short) 67));
        assertThat("Byte from buffer should match", FileUtil.readByteFromBuffer(buff), is((byte) 89));
        byte[] array = new byte[3];
        FileUtil.readByteArrayFromBuffer(buff, array);
        assertArrayEquals(arr, array, "Byte array from buffer should match");
        // Test reads past end of file
        assertThat("Long from buffer at limit should be 0", FileUtil.readLongFromBuffer(buff), is(0L));
        assertThat("Int from buffer at limit should be 0", FileUtil.readIntFromBuffer(buff), is(0));
        assertThat("Short from buffer at limit should be 0", FileUtil.readShortFromBuffer(buff), is((short) 0));
        assertThat("Byte from buffer at limit should be 0", FileUtil.readByteFromBuffer(buff), is((byte) 0));
        byte[] arr0 = new byte[] { 0, 0, 0 };
        array = new byte[3];
        FileUtil.readByteArrayFromBuffer(buff, array);
        assertArrayEquals(arr0, array, "Byte array from buffer at limit should be all 0s");
    }

    @Test
    void testReadFileNoReportError(@TempDir Path tempDir) {
        String missing = tempDir.resolve("missing.txt").toString();
        assertThat(FileUtil.readFile(missing, false), is(empty()));
    }

    @Test
    void testReadLinesFromFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("lines.txt");
        Files.writeString(file, """
                line1
                line2
                line3
                line4
                line5
                """);

        List<String> lines = FileUtil.readLines(file.toString(), 3);
        assertThat("should read 3 lines", lines, hasSize(3));
        assertThat(lines.get(0), is("line1"));
        assertThat(lines.get(2), is("line3"));

        // Read more lines than exist
        lines = FileUtil.readLines(file.toString(), 100);
        assertThat("should read all 5 lines", lines, hasSize(5));

        Files.deleteIfExists(file);

        // Non-existent file after deletion
        assertThat(FileUtil.readLines(file.toString(), 1), is(empty()));
    }

    @Test
    void testReadLinesNoReportError(@TempDir Path tempDir) {
        String missing = tempDir.resolve("missing.txt").toString();
        assertThat(FileUtil.readLines(missing, 1, false), is(empty()));
    }

    @Test
    void testReadAllBytesNoReportError(@TempDir Path tempDir) {
        String missing = tempDir.resolve("missing.bin").toString();
        byte[] result = FileUtil.readAllBytes(missing, false);
        assertThat(result.length, is(0));
    }

    @Test
    void testGetFileName() {
        assertThat(FileUtil.getFileName("/usr/bin/dmidecode"), is("dmidecode"));
        assertThat(FileUtil.getFileName("dmidecode"), is("dmidecode"));
        assertThat(FileUtil.getFileName(""), is(""));
        assertThat(FileUtil.getFileName(null), is(""));
    }

    @Test
    void testReadAllBytesReportError(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("bytes.bin");
        byte[] expected = { 0x01, 0x02, 0x03, 0x04, 0x05 };
        Files.write(file, expected);

        byte[] actual = FileUtil.readAllBytes(file.toString(), true);
        assertArrayEquals(expected, actual, "readAllBytes should return file content");

        // Non-existent file with reportError=true
        String missing = tempDir.resolve("missing.bin").toString();
        byte[] result = FileUtil.readAllBytes(missing, true);
        assertThat("Missing file should return empty array", result.length, is(0));
    }

    @Test
    void testReadAllBytesAsBufferEmpty(@TempDir Path tempDir) {
        // Non-existent file should return an empty buffer
        String missing = tempDir.resolve("nofile.bin").toString();
        ByteBuffer buff = FileUtil.readAllBytesAsBuffer(missing);
        assertThat("Buffer from missing file should have 0 limit", buff.limit(), is(0));
    }

    @Test
    void testReadSymlinkTargetNonSymlink(@TempDir Path tempDir) throws IOException {
        Path file = Files.createFile(tempDir.resolve("regular.txt"));
        assertThat(FileUtil.readSymlinkTarget(file.toFile()), is(nullValue()));
    }
}
