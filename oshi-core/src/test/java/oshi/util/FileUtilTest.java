/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

/**
 * Tests FileUtil
 */
public class FileUtilTest {

    /*
     * File sources
     */
    private static String THISCLASS = "src/test/java/oshi/util/FileUtilTest.java";
    private static String INT_FILE = "src/test/resources/test.integer.txt";
    private static String STRING_FILE = "src/test/resources/test.string.txt";
    private static String PROCIO_FILE = "src/test/resources/test.procio.txt";
    private static String NO_FILE = "does/not/exist";

    /**
     * Test read file.
     */
    @Test
    public void testReadFile() {
        List<String> thisFile = null;
        // Try file not found
        thisFile = FileUtil.readFile(NO_FILE);
        assertEquals(0, thisFile.size());
        // Try this file
        thisFile = FileUtil.readFile(THISCLASS);
        // Comment ONE line
        int lineOne = 0;
        // Comment TWO line
        int lineTwo = 0;
        for (int i = 0; i < thisFile.size(); i++) {
            String line = thisFile.get(i);
            if (lineOne == 0 && line.contains("Comment ONE line")) {
                lineOne = i;
                continue;
            }
            if (lineTwo == 0 && line.contains("Comment TWO line")) {
                lineTwo = i;
                break;
            }
        }
        assertEquals(2, lineTwo - lineOne);
    }

    /**
     * Test get*FromFile
     */
    @Test
    public void testGetFromFile() {
        assertEquals(123L, FileUtil.getUnsignedLongFromFile(INT_FILE));
        assertEquals(0L, FileUtil.getUnsignedLongFromFile(STRING_FILE));
        assertEquals(0L, FileUtil.getUnsignedLongFromFile(NO_FILE));

        assertEquals(123L, FileUtil.getLongFromFile(INT_FILE));
        assertEquals(0L, FileUtil.getLongFromFile(STRING_FILE));
        assertEquals(0L, FileUtil.getLongFromFile(NO_FILE));

        assertEquals(123, FileUtil.getIntFromFile(INT_FILE));
        assertEquals(0, FileUtil.getIntFromFile(STRING_FILE));
        assertEquals(0, FileUtil.getIntFromFile(NO_FILE));

        assertEquals("123", FileUtil.getStringFromFile(INT_FILE));
        assertEquals("", FileUtil.getStringFromFile(NO_FILE));
    }

    @Test
    public void testReadProcIo() {
        Map<String, String> expected = new HashMap<>();
        expected.put("rchar", "124788352");
        expected.put("wchar", "124802481");
        expected.put("syscr", "135");
        expected.put("syscw", "1547");
        expected.put("read_bytes", "40304640");
        expected.put("write_bytes", "124780544");
        expected.put("cancelled_write_bytes", "42");
        Map<String, String> actual = FileUtil.getKeyValueMapFromFile(PROCIO_FILE, ":");
        assertEquals(expected.size(), actual.size());
        for (String key : expected.keySet()) {
            assertEquals(expected.get(key), actual.get(key));
        }
    }

    @Test
    public void testReadProperties() {
        Properties props = FileUtil.readPropertiesFromFilename("simplelogger.properties");
        assertEquals("INFO", props.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));
        props = FileUtil.readPropertiesFromFilename("this.file.does.not.exist");
        assertFalse(props.elements().hasMoreElements());
    }
}
