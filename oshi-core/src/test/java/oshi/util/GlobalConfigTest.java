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

import org.junit.Before;
import org.junit.Test;
import oshi.util.GlobalConfig.PropertyException;

import java.util.Properties;

import static java.lang.Double.MIN_VALUE;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static oshi.util.GlobalConfig.clear;
import static oshi.util.GlobalConfig.get;
import static oshi.util.GlobalConfig.load;
import static oshi.util.GlobalConfig.remove;
import static oshi.util.GlobalConfig.set;
import static oshi.util.GlobalConfigTest.GlobalConfigAsserter.asserter;

public class GlobalConfigTest {
    private static final String PROPERTY = "oshi.test.property";

    @Before
    public void setUp() {
        clear();
    }

    @Test
    public void testGetString() {
        asserter(PROPERTY).assertDefaultThat(null, null);
        set(PROPERTY, "test");
        asserter(PROPERTY).assertThat("test", null);
        set(PROPERTY, 123);

        asserter(PROPERTY).assertThat("123", null);
    }

    @Test
    public void testGetInteger() {
        asserter(PROPERTY).assertDefaultThat(0, 0);
        set(PROPERTY, 123);
        asserter(PROPERTY).assertThat(123, 0).assertThat("123", null);

        // Invalid integer
        set(PROPERTY, "1.23");
        asserter(PROPERTY).assertThat(0, 0);
    }

    @Test
    public void testGetDouble() {
        asserter(PROPERTY).assertDefaultThat(0.0, 0.0);
        set(PROPERTY, 1.23d);
        asserter(PROPERTY).assertThat(1.23, 0.0).assertThat("1.23", null);

        // Invalid double
        set(PROPERTY, "1.2.3");
        asserter(PROPERTY).assertThat(0.0, 0.0);
    }

    @Test
    public void testGetBoolean() {
        asserter(PROPERTY).assertDefaultThat(false, false);
        set(PROPERTY, true);
        asserter(PROPERTY).assertThat(true, false).assertThat("true", null);
    }

    @Test
    public void testSetNull() {
        set(PROPERTY, "test");
        set(PROPERTY, null);
        asserter(PROPERTY).assertThat("123", "123");
    }

    @Test
    public void testRemove() {
        String removed = "test";
        set(PROPERTY, removed);
        remove(PROPERTY);
        asserter(PROPERTY).assertThat(format("Should have removed property %s", removed), "123", "123");
    }

    @Test
    public void testLoad() {
        load(propertiesWith("321"));

        asserter(PROPERTY).assertThat("321", null);
    }

    @Test
    public void testPropertyExceptionMessage() {
        set(PROPERTY, "test");
        assertEquals(format("Invalid property: \"%s\" = test", PROPERTY), new PropertyException(PROPERTY).getMessage());
    }

    private Properties propertiesWith(String value) {
        Properties updates = new Properties();
        updates.setProperty(PROPERTY, value);
        return updates;
    }

    public static class GlobalConfigAsserter {
        private static final String FAILURE_MESSAGE_TEMPLATE = "property: %s value for def: %s should be";
        private static final String DEFAULT_FAILURE_MESSAGE_TEMPLATE = "Property: %s default value def: %s should be";
        private final String property;

        private GlobalConfigAsserter(String property) {
            this.property = property;
        }

        public static GlobalConfigAsserter asserter(String property) {
            return new GlobalConfigAsserter(property);
        }

        public GlobalConfigAsserter assertThat(Object expected, Object def) {
            assertThat(failureMessage(def), expected, def);
            return this;
        }

        public GlobalConfigAsserter assertDefaultThat(Object expected, Object def) {
            assertThat(defaultFailureMessage(def), expected, def);
            return this;
        }

        public GlobalConfigAsserter assertThat(String message, Object expected, Object def) {
            if (def instanceof String) {
                assertEquals(message, expected, get(property, (String) def));
            } else if (def instanceof Boolean) {
                assertEquals(message, expected, get(property, (boolean) def));
            } else if (def instanceof Integer) {
                assertEquals(message, expected, get(property, (Integer) def));
            } else if (def instanceof Double) {
                assertEquals(message, (Double) expected, get(property, (Double) def), MIN_VALUE);
            }
            return this;
        }

        private String failureMessage(Object def) {
            return format(FAILURE_MESSAGE_TEMPLATE, property, def);
        }

        private String defaultFailureMessage(Object def) {
            return format(DEFAULT_FAILURE_MESSAGE_TEMPLATE, PROPERTY, def);
        }
    }
}
