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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static oshi.util.GlobalConfig.clear;
import static oshi.util.GlobalConfig.get;
import static oshi.util.GlobalConfig.load;
import static oshi.util.GlobalConfig.remove;
import static oshi.util.GlobalConfig.set;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class GlobalConfigTest {

    @Before
    public void setUp() {
        clear();
    }

    @Test
    public void testGetString() {
        assertNull(get("oshi.test.property", null));
        set("oshi.test.property", "test");
        assertEquals("test", get("oshi.test.property", null));
        set("oshi.test.property", 123);
        assertEquals("123", get("oshi.test.property", null));
    }

    @Test
    public void testGetInteger() {
        assertEquals(0, get("oshi.test.property", 0));
        set("oshi.test.property", 123);
        assertEquals(123, get("oshi.test.property", 0));
        assertEquals("123", get("oshi.test.property", null));

        // Invalid integer
        set("oshi.test.property", "1.23");
        assertEquals(0, get("oshi.test.property", 0));
    }

    @Test
    public void testGetDouble() {
        assertEquals(0.0, get("oshi.test.property", 0.0), Double.MIN_VALUE);
        set("oshi.test.property", 1.23d);
        assertEquals(1.23, get("oshi.test.property", 0.0), Double.MIN_VALUE);
        assertEquals("1.23", get("oshi.test.property", null));

        // Invalid double
        set("oshi.test.property", "1.2.3");
        assertEquals(0.0, get("oshi.test.property", 0.0), Double.MIN_VALUE);
    }

    @Test
    public void testGetBoolean() {
        assertFalse(get("oshi.test.property", false));
        set("oshi.test.property", true);
        assertTrue(get("oshi.test.property", false));
        assertEquals("true", get("oshi.test.property", null));
    }

    @Test
    public void testSetNull() {
        set("oshi.test.property", "test");
        set("oshi.test.property", null);
        assertEquals("123", get("oshi.test.property", "123"));
    }

    @Test
    public void testRemove() {
        set("oshi.test.property", "test");
        remove("oshi.test.property");
        assertEquals("123", get("oshi.test.property", "123"));
    }

    @Test
    public void testLoad() {
        Properties updates = new Properties();
        updates.setProperty("oshi.test.property", "321");

        load(updates);
        assertEquals("321", get("oshi.test.property", null));
    }

    @Test
    public void testPropertyExceptionMessage() {
        set("oshi.test.property", "test");
        assertEquals("Invalid property: \"oshi.test.property\" = test",
                new GlobalConfig.PropertyException("oshi.test.property").getMessage());
    }
}
