/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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
        assertEquals(get("oshi.test.property", null), "test");
        set("oshi.test.property", 123);
        assertEquals(get("oshi.test.property", null), "123");
    }

    @Test
    public void testGetInteger() {
        assertEquals(get("oshi.test.property", 0), 0);
        set("oshi.test.property", 123);
        assertEquals(get("oshi.test.property", 0), 123);
        assertEquals(get("oshi.test.property", null), "123");

        // Invalid integer
        set("oshi.test.property", "1.23");
        assertEquals(get("oshi.test.property", 0), 0);
    }

    @Test
    public void testGetDouble() {
        assertTrue(get("oshi.test.property", 0.0) == 0.0);
        set("oshi.test.property", 1.23d);
        assertTrue(get("oshi.test.property", 0.0) == 1.23);
        assertEquals(get("oshi.test.property", null), "1.23");

        // Invalid double
        set("oshi.test.property", "1.2.3");
        assertTrue(get("oshi.test.property", 0.0) == 0.0);
    }

    @Test
    public void testGetBoolean() {
        assertEquals(get("oshi.test.property", false), false);
        set("oshi.test.property", true);
        assertEquals(get("oshi.test.property", false), true);
        assertEquals(get("oshi.test.property", null), "true");
    }

    @Test
    public void testSetNull() {
        set("oshi.test.property", "test");
        set("oshi.test.property", null);
        assertEquals(get("oshi.test.property", "123"), "123");
    }

    @Test
    public void testRemove() {
        set("oshi.test.property", "test");
        remove("oshi.test.property");
        assertEquals(get("oshi.test.property", "123"), "123");
    }

    @Test
    public void testLoad() {
        Properties updates = new Properties();
        updates.setProperty("oshi.test.property", "321");

        load(updates);
        assertEquals(get("oshi.test.property", null), "321");
    }

    @Test
    public void testPropertyExceptionMessage() {
        set("oshi.test.property", "test");
        assertEquals(new GlobalConfig.PropertyException("oshi.test.property").getMessage(),
                "Invalid property: \"oshi.test.property\" = test");
    }
}
