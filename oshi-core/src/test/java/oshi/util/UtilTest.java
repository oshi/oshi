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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

/**
 * Test general utility methods
 */
class UtilTest {

    @Test
    void testSleep() {
        // Windows counters may be up to 1/64 second (16ms) off
        long now = System.nanoTime();
        Util.sleep(100);
        assertThat(System.nanoTime() - now, is(greaterThan(84_375_000L)));
    }

    @Test
    void testWildcardMatch() {
        assertThat("Test should not match est", Util.wildcardMatch("Test", "est"), is(false));
        assertThat("Test should match ^est", Util.wildcardMatch("Test", "^est"), is(true));
        assertThat("Test should not match ^^est", Util.wildcardMatch("Test", "^^est"), is(false));
        assertThat("Test should match ?est", Util.wildcardMatch("Test", "?est"), is(true));
        assertThat("Test should not match ^?est", Util.wildcardMatch("Test", "^?est"), is(false));
        assertThat("Test should match *est", Util.wildcardMatch("Test", "*est"), is(true));
        assertThat("Test should not match ^*est", Util.wildcardMatch("Test", "^*est"), is(false));

        assertThat("Test should not match T?t", Util.wildcardMatch("Test", "T?t"), is(false));
        assertThat("Test should match T??t", Util.wildcardMatch("Test", "T??t"), is(true));
        assertThat("Test should match T*t", Util.wildcardMatch("Test", "T*t"), is(true));

        assertThat("Test should not match Tes", Util.wildcardMatch("Test", "Tes"), is(false));
        assertThat("Test should match Tes?", Util.wildcardMatch("Test", "Tes?"), is(true));
        assertThat("Test should match Tes*", Util.wildcardMatch("Test", "Tes*"), is(true));

        assertThat("Test should not match Te?", Util.wildcardMatch("Test", "Te?"), is(false));
        assertThat("Test should match Te*", Util.wildcardMatch("Test", "Te*"), is(true));
    }

    @Test
    void testIsBlank() {
        assertThat("\"\" should be Blank", Util.isBlank(""), is(true));
        assertThat("null should be Blank", Util.isBlank(null), is(true));
        assertThat("\"Not blank\" should not be Blank", Util.isBlank("Not blank"), is(false));
    }

    @Test
    void testIsBlankOrUnknown() {
        assertThat("\"\" should be Blank", Util.isBlankOrUnknown(""), is(true));
        assertThat("null should be Blank", Util.isBlankOrUnknown(null), is(true));
        assertThat("unknown should be unknown", Util.isBlankOrUnknown(Constants.UNKNOWN), is(true));
        assertThat("\"Not blank\" should not be Blank", Util.isBlankOrUnknown("Not blank"), is(false));
    }
}
