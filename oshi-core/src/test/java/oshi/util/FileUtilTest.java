/*
 * MIT License
 *
 * Copyright (c) 2020-2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

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
}
