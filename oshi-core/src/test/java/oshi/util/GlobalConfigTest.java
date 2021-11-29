/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static oshi.util.GlobalConfig.clear;
import static oshi.util.GlobalConfig.get;
import static oshi.util.GlobalConfig.load;
import static oshi.util.GlobalConfig.remove;
import static oshi.util.GlobalConfig.set;
import static oshi.util.GlobalConfigTest.GlobalConfigAsserter.asserter;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

import oshi.util.GlobalConfig.PropertyException;

@Execution(SAME_THREAD)
class GlobalConfigTest {
    private static final String PROPERTY = "oshi.test.property";
    private static final double EPSILON = Double.MIN_VALUE;

    @BeforeEach
    void setUp() {
        clear();
    }

    @Test
    void testGetString() {
        asserter(PROPERTY).assertDefaultThat(null, null);
        set(PROPERTY, "test");
        asserter(PROPERTY).assertThat("test", null);
        set(PROPERTY, 123);

        asserter(PROPERTY).assertThat("123", null);
    }

    @Test
    void testGetInteger() {
        asserter(PROPERTY).assertDefaultThat(0, 0);
        set(PROPERTY, 123);
        asserter(PROPERTY).assertThat(123, 0).assertThat("123", null);

        // Invalid integer
        set(PROPERTY, "1.23");
        asserter(PROPERTY).assertThat(0, 0);
    }

    @Test
    void testGetDouble() {
        asserter(PROPERTY).assertDefaultThat(0.0, 0.0);
        set(PROPERTY, 1.23d);
        asserter(PROPERTY).assertThat(1.23, 0.0).assertThat("1.23", null);

        // Invalid double
        set(PROPERTY, "1.2.3");
        asserter(PROPERTY).assertThat(0.0, 0.0);
    }

    @Test
    void testGetBoolean() {
        asserter(PROPERTY).assertDefaultThat(false, false);
        set(PROPERTY, true);
        asserter(PROPERTY).assertThat(true, false).assertThat("true", null);
    }

    @Test
    void testSetNull() {
        set(PROPERTY, "test");
        set(PROPERTY, null);
        asserter(PROPERTY).assertThat("123", "123");
    }

    @Test
    void testRemove() {
        String removed = "test";
        set(PROPERTY, removed);
        remove(PROPERTY);
        asserter(PROPERTY).assertThat(format("Should have removed property %s", removed), "123", "123");
    }

    @Test
    void testLoad() {
        load(propertiesWith("321"));

        asserter(PROPERTY).assertThat("321", null);
    }

    @Test
    void testPropertyExceptionMessage() {
        set(PROPERTY, "test");
        assertThat(new PropertyException(PROPERTY).getMessage(),
                is(format("Invalid property: \"%s\" = test", PROPERTY)));
    }

    private Properties propertiesWith(String value) {
        Properties updates = new Properties();
        updates.setProperty(PROPERTY, value);
        return updates;
    }

    static final class GlobalConfigAsserter {
        private static final String FAILURE_MESSAGE_TEMPLATE = "property: %s value for def: %s should be";
        private static final String DEFAULT_FAILURE_MESSAGE_TEMPLATE = "Property: %s default value def: %s should be";
        private final String property;

        private GlobalConfigAsserter(String property) {
            this.property = property;
        }

        static GlobalConfigAsserter asserter(String property) {
            return new GlobalConfigAsserter(property);
        }

        GlobalConfigAsserter assertThat(Object expected, Object def) {
            assertThat(failureMessage(def), expected, def);
            return this;
        }

        GlobalConfigAsserter assertThat(String message, Object expected, Object def) {
            if (def instanceof String) {
                assertThat(message, get(property, (String) def), is(expected));
            } else if (def instanceof Boolean) {
                assertThat(message, get(property, (boolean) def), is(expected));
            } else if (def instanceof Integer) {
                assertThat(message, get(property, (Integer) def), is(expected));
            } else if (def instanceof Double) {
                assertThat(message, get(property, (Double) def), is(closeTo((Double) expected, EPSILON)));
            }
            return this;
        }

        GlobalConfigAsserter assertDefaultThat(Object expected, Object def) {
            assertThat(defaultFailureMessage(def), expected, def);
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
