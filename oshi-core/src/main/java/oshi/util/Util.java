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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * General utility methods
 */
@ThreadSafe
public final class Util {
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    private Util() {
    }

    /**
     * Sleeps for the specified number of milliseconds.
     *
     * @param ms
     *            How long to sleep
     */
    public static void sleep(long ms) {
        try {
            LOG.trace("Sleeping for {} ms", ms);
            Thread.sleep(ms);
        } catch (InterruptedException e) { // NOSONAR squid:S2142
            LOG.warn("Interrupted while sleeping for {} ms: {}", ms, e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Tests if a String matches another String with a wildcard pattern.
     *
     * @param text
     *            The String to test
     * @param pattern
     *            The String containing a wildcard pattern where ? represents a
     *            single character and * represents any number of characters. If the
     *            first character of the pattern is a carat (^) the test is
     *            performed against the remaining characters and the result of the
     *            test is the opposite.
     * @return True if the String matches or if the first character is ^ and the
     *         remainder of the String does not match.
     */
    public static boolean wildcardMatch(String text, String pattern) {
        if (pattern.length() > 0 && pattern.charAt(0) == '^') {
            return !wildcardMatch(text, pattern.substring(1));
        }
        return text.matches(pattern.replace("?", ".?").replace("*", ".*?"));
    }

    /**
     * Tests if a String is either null or empty.
     *
     * @param s
     *            The string to test
     * @return True if the String is either null or empty.
     */
    public static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Tests if a String is either null or empty or the unknown constant.
     *
     * @param s
     *            The string to test
     * @return True if the String is either null or empty or the unknown constant.
     */
    public static boolean isBlankOrUnknown(String s) {
        return isBlank(s) || Constants.UNKNOWN.equals(s);
    }
}
