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
import static oshi.util.ConfigUtil.get;
import static oshi.util.ConfigUtil.getBoolean;
import static oshi.util.ConfigUtil.getDouble;
import static oshi.util.ConfigUtil.getInteger;
import static oshi.util.ConfigUtil.set;

import java.util.Properties;

import org.junit.Test;

public class ConfigUtilTest {

    @Test
    public void testGet() {
        assertNull(get("string"));
        set("string", "test");
        assertEquals(get("string"), "test");
        set("string", 123);
        assertEquals(get("string"), "123");
    }

    @Test
    public void testGetInteger() {
        assertNull(get("integer"));
        set("integer", 123);
        assertEquals(getInteger("integer"), 123);
        assertEquals(get("integer"), "123");
    }

    @Test
    public void testGetDouble() {
        assertNull(get("double"));
        set("double", 1.23d);
        assertEquals(getDouble("double"), Double.valueOf(1.23d));
        assertEquals(get("double"), "1.23");
    }

    @Test
    public void testGetBoolean() {
        assertNull(get("boolean"));
        set("boolean", true);
        assertEquals(getBoolean("boolean"), true);
        assertEquals(get("boolean"), "true");
    }

    @Test
    public void testLoad() {
        Properties updates = new Properties();
        updates.setProperty("test", "321");

        ConfigUtil.load(updates);
        assertEquals(get("test"), "321");
    }

}
