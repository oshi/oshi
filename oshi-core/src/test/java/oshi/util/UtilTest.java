/*
 * Copyright 2016-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
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

    @Test
    void testIsSessionValid() {
        assertThat("Session is invalid because user is empty", Util.isSessionValid("", "device", (long) 0), is(false));
        assertThat("Session is invalid because device is empty", Util.isSessionValid("user", "", (long) 0), is(false));
        assertThat("Session is invalid because loginTime is greater than current system time",
                Util.isSessionValid("user", "device", Long.MAX_VALUE), is(false));
        assertThat("Session is invalid because loginTime is lesser than zero",
                Util.isSessionValid("user", "device", Long.MIN_VALUE), is(false));
        assertThat("Session is valid because all the arguments are appropriate",
                Util.isSessionValid("user", "device", (long) 999999999), is(true));
    }
}
