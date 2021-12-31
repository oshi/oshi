/*
 * MIT License
 *
 * Copyright (c) 2019-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * Tests FileUtil
 */
class FileUtilTest {

    /*
     * File sources
     */
    private static final String PROJECTROOT;
    static {
        String root;
        try {
            File core = new File("oshi-core");
            if (core.exists()) {
                // If we're in main project directory get path to oshi-core
                root = core.getCanonicalPath();
            } else {
                // Assume we must be in oshi-core
                root = new File(".").getCanonicalPath();
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e.getMessage());
        }
        PROJECTROOT = root;
    }
    private static final String THISCLASS = PROJECTROOT + "/src/test/java/oshi/util/FileUtilTest.java";
    private static final String INT_FILE = PROJECTROOT + "/src/test/resources/test.integer.txt";
    private static final String STRING_FILE = PROJECTROOT + "/src/test/resources/test.string.txt";
    private static final String PROCIO_FILE = PROJECTROOT + "/src/test/resources/test.procio.txt";
    private static final String NO_FILE = PROJECTROOT + "/does/not/exist";

    /**
     * Test read file.
     */
    @Test
    void testReadFile() {
        // Try file not found
        assertThat("no file", FileUtil.readFile(NO_FILE), is(empty()));
        // Try this file
        List<String> thisFile = FileUtil.readFile(THISCLASS);
        // Comment ONE line
        int lineOne = 0;
        // Comment TWO line
        int lineTwo = 0;
        for (int i = 0; i < thisFile.size(); i++) {
            String line = thisFile.get(i);
            if (lineOne == 0 && line.contains("Comment ONE line")) {
                lineOne = i;
            } else if (lineTwo == 0 && line.contains("Comment TWO line")) {
                lineTwo = i;
                break;
            }
        }
        assertThat("Comment line difference", lineTwo - lineOne, is(2));
    }

    /**
     * Test get*FromFile
     */
    @Test
    void testGetFromFile() {
        assertThat("unsigned long from int", FileUtil.getUnsignedLongFromFile(INT_FILE), is(123L));
        assertThat("unsigned long from string", FileUtil.getUnsignedLongFromFile(STRING_FILE), is(0L));
        assertThat("unsigned long from invalid", FileUtil.getUnsignedLongFromFile(NO_FILE), is(0L));

        assertThat("long from int", FileUtil.getLongFromFile(INT_FILE), is(123L));
        assertThat("long from string", FileUtil.getLongFromFile(STRING_FILE), is(0L));
        assertThat("long from invalid", FileUtil.getLongFromFile(NO_FILE), is(0L));

        assertThat("int from int", FileUtil.getIntFromFile(INT_FILE), is(123));
        assertThat("int from string", FileUtil.getIntFromFile(STRING_FILE), is(0));
        assertThat("int from invalid", FileUtil.getIntFromFile(NO_FILE), is(0));

        assertThat("string from int", FileUtil.getStringFromFile(INT_FILE), is("123"));
        assertThat("string from invalid ", FileUtil.getStringFromFile(NO_FILE), is(emptyString()));
    }

    @Test
    void testReadProcIo() {
        Map<String, String> expected = new HashMap<>();
        expected.put("rchar", "124788352");
        expected.put("wchar", "124802481");
        expected.put("syscr", "135");
        expected.put("syscw", "1547");
        expected.put("read_bytes", "40304640");
        expected.put("write_bytes", "124780544");
        expected.put("cancelled_write_bytes", "42");
        Map<String, String> actual = FileUtil.getKeyValueMapFromFile(PROCIO_FILE, ":");
        assertThat("procio size", actual, is(aMapWithSize(expected.size())));
        for (Entry<String, String> entry : expected.entrySet()) {
            assertThat("procio entry", actual, hasEntry(entry.getKey(), entry.getValue()));
        }
    }

    @Test
    void testReadProperties() {
        Properties props = FileUtil.readPropertiesFromFilename("simplelogger.properties");
        assertThat("simplelogger properties", props.getProperty("org.slf4j.simpleLogger.defaultLogLevel"), is("INFO"));
        props = FileUtil.readPropertiesFromFilename("this.file.does.not.exist");
        assertThat("invalid file", props.stringPropertyNames(), is(empty()));
    }
}
