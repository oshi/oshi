/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.WINDOWS)
class RegistryUtilTest {

    @Test
    void testRegistryValueToLongNull() {
        assertThat(RegistryUtil.registryValueToLong(null), is(0L));
    }

    @Test
    void testRegistryValueToLongInteger() {
        // Small integer, not in timestamp range
        assertThat(RegistryUtil.registryValueToLong(42), is(42L));
    }

    @Test
    void testRegistryValueToLongIntegerTimestamp() {
        // Value in plausible Unix timestamp range should be converted to millis
        int recentTimestamp = (int) (System.currentTimeMillis() / 1000L - 86400); // yesterday
        assertThat(RegistryUtil.registryValueToLong(recentTimestamp), is((long) recentTimestamp * 1000L));
    }

    @Test
    void testRegistryValueToLongDateString() {
        assertThat(RegistryUtil.registryValueToLong("20200101"), is(1577836800000L));
    }

    @Test
    void testRegistryValueToLongUnknownType() {
        assertThat(RegistryUtil.registryValueToLong(3.14), is(0L));
    }

    @Test
    void testRegistryValueToStringNull() {
        assertThat(RegistryUtil.registryValueToString(null), is(nullValue()));
    }

    @Test
    void testRegistryValueToStringString() {
        assertThat(RegistryUtil.registryValueToString("  hello  "), is("hello"));
    }

    @Test
    void testRegistryValueToStringByteArray() {
        // Windows-1252 with null terminator
        byte[] cp1252 = new byte[] { 't', 'e', 's', 't', 0x00 };
        assertThat(RegistryUtil.registryValueToString(cp1252), is("test"));
    }

    @Test
    void testRegistryValueToStringByteArrayUtf16() {
        // UTF-16LE with double-null terminator
        byte[] utf16 = new byte[] { 't', 0, 'e', 0, 's', 0, 't', 0, 0, 0 };
        assertThat(RegistryUtil.registryValueToString(utf16), is("test"));
    }
}
