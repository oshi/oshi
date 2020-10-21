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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test general utility methods
 */
public class UtilTest {

    @Test
    public void testSleep() {
        // Windows counters may be up to 1/64 second (16ms) off
        long now = System.nanoTime();
        Util.sleep(100);
        assertTrue(System.nanoTime() - now >= 84_375_000);
    }

    @Test
    public void testWildcardMatch() {
        assertFalse("Test should not match est", Util.wildcardMatch("Test", "est"));
        assertTrue("Test should match ^est", Util.wildcardMatch("Test", "^est"));
        assertFalse("Test should not match ^^est", Util.wildcardMatch("Test", "^^est"));
        assertTrue("Test should match ?est", Util.wildcardMatch("Test", "?est"));
        assertFalse("Test should not match ^?est", Util.wildcardMatch("Test", "^?est"));
        assertTrue("Test should match *est", Util.wildcardMatch("Test", "*est"));
        assertFalse("Test should not match ^*est", Util.wildcardMatch("Test", "^*est"));

        assertFalse("Test should not match T?t", Util.wildcardMatch("Test", "T?t"));
        assertTrue("Test should match T??t", Util.wildcardMatch("Test", "T??t"));
        assertTrue("Test should match T*t", Util.wildcardMatch("Test", "T*t"));

        assertFalse("Test should not match Tes", Util.wildcardMatch("Test", "Tes"));
        assertTrue("Test should match Tes?", Util.wildcardMatch("Test", "Tes?"));
        assertTrue("Test should match Tes*", Util.wildcardMatch("Test", "Tes*"));

        assertFalse("Test should not match Te?", Util.wildcardMatch("Test", "Te?"));
        assertTrue("Test should match Te*", Util.wildcardMatch("Test", "Te*"));
    }

    @Test
    public void testIsBlank() {
        assertTrue("\"\" should be Blank", Util.isBlank(""));
        assertTrue("null should be Blank", Util.isBlank(null));
        assertFalse("\"Not blank\" should not be Blank", Util.isBlank("Not blank"));
    }
}
