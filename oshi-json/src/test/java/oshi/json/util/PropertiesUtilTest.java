/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors:
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
package oshi.json.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

import oshi.software.os.OperatingSystem.ProcessSort;

/**
 * Test properties loader
 */
public class PropertiesUtilTest {
    private static final String PROPS_FILE = "oshi.json.properties";
    private static String NO_FILE = "does.not.exist";

    /**
     * Test load properties
     */
    @Test
    public void testLoadProperties() {
        Properties props;

        props = PropertiesUtil.loadProperties(NO_FILE);
        assertEquals(0, props.size());

        props = PropertiesUtil.loadProperties(PROPS_FILE);
        assertEquals(5, PropertiesUtil.getIntOrDefault(props, "operatingSystem.processes.limit", 0));
        assertEquals(3, PropertiesUtil.getIntOrDefault(props, "operatingSystem.processes.nokey", 3));

        assertTrue(PropertiesUtil.getBoolean(props, "hardware.usbDevices.tree"));
        assertTrue(PropertiesUtil.getBoolean(props, "hardware.usbDevices.nokey"));

        assertEquals("5", PropertiesUtil.getString(props, "operatingSystem.processes.limit"));
        assertEquals("null", PropertiesUtil.getString(props, "operatingSystem.processes.nokey"));

        assertEquals(ProcessSort.CPU,
                PropertiesUtil.getEnum(props, "operatingSystem.processes.sort", ProcessSort.class));
        assertEquals(null, PropertiesUtil.getEnum(props, "operatingSystem.processes.limit", ProcessSort.class));

    }
}
