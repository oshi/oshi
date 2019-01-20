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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test general utility methods
 */
public class UtilTest {

    @Test
    public void testSleep() {
        // Windows counters may be up to 10ms off
        long now = System.nanoTime();
        Util.sleep(100);
        assertTrue(System.nanoTime() - now >= 90_000_000);

        now = System.nanoTime();
        long then = System.currentTimeMillis() + 100;
        Util.sleepAfter(then, 100);
        assertTrue(System.nanoTime() - now >= 190_000_000);

        now = System.nanoTime();
        then = System.currentTimeMillis() - 550;
        Util.sleepAfter(then, 500);
        assertTrue(System.nanoTime() - now < 500_000_000);
    }

    @Test
    public void testWildcardMatch() {
        assertFalse(Util.wildcardMatch("Test", "est"));
        assertTrue(Util.wildcardMatch("Test", "^est"));
        assertFalse(Util.wildcardMatch("Test", "^^est"));
        assertTrue(Util.wildcardMatch("Test", "?est"));
        assertFalse(Util.wildcardMatch("Test", "^?est"));
        assertTrue(Util.wildcardMatch("Test", "*est"));
        assertFalse(Util.wildcardMatch("Test", "^*est"));

        assertFalse(Util.wildcardMatch("Test", "T?t"));
        assertTrue(Util.wildcardMatch("Test", "T??t"));
        assertTrue(Util.wildcardMatch("Test", "T*t"));

        assertFalse(Util.wildcardMatch("Test", "Tes"));
        assertTrue(Util.wildcardMatch("Test", "Tes?"));
        assertTrue(Util.wildcardMatch("Test", "Tes*"));

        assertFalse(Util.wildcardMatch("Test", "Te?"));
        assertTrue(Util.wildcardMatch("Test", "Te*"));
    }
}
