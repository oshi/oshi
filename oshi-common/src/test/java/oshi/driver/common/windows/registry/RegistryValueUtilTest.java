/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.registry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class RegistryValueUtilTest {

    @Test
    void testRegistryValueToLongNull() {
        assertThat(RegistryValueUtil.registryValueToLong(null), is(0L));
    }

    @Test
    void testRegistryValueToLongInteger() {
        // Small integer, not in timestamp range
        assertThat(RegistryValueUtil.registryValueToLong(42), is(42L));
    }

    @Test
    void testRegistryValueToLongIntegerTimestamp() {
        // Value in plausible Unix timestamp range should be converted to millis
        int recentTimestamp = (int) (System.currentTimeMillis() / 1000L - 86400); // yesterday
        assertThat(RegistryValueUtil.registryValueToLong(recentTimestamp), is((long) recentTimestamp * 1000L));
    }

    @Test
    void testRegistryValueToLongDateString() {
        // yyyyMMdd is parsed at midnight in the system default zone
        long expected = LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                .toEpochMilli();
        assertThat(RegistryValueUtil.registryValueToLong("20200101"), is(expected));
    }

    @Test
    void testRegistryValueToLongUnknownType() {
        assertThat(RegistryValueUtil.registryValueToLong(3.14), is(0L));
    }

    @Test
    void testRegistryValueToStringNull() {
        assertThat(RegistryValueUtil.registryValueToString(null), is(nullValue()));
    }

    @Test
    void testRegistryValueToStringString() {
        assertThat(RegistryValueUtil.registryValueToString("  hello  "), is("hello"));
    }

    @Test
    void testRegistryValueToStringInteger() {
        // REG_DWORD is rendered as its decimal string
        assertThat(RegistryValueUtil.registryValueToString(42), is("42"));
    }

    @Test
    void testRegistryValueToStringIntegerHighBit() {
        // REG_DWORD is unsigned: 0xFFFFFFFF renders as 4294967295, not -1
        assertThat(RegistryValueUtil.registryValueToString(-1), is("4294967295"));
    }

    @Test
    void testRegistryValueToLongIntegerHighBit() {
        // A high-bit REG_DWORD is treated as unsigned; outside the sane timestamp range it returns the unsigned value
        assertThat(RegistryValueUtil.registryValueToLong(-1), is(4294967295L));
    }

    @Test
    void testRegistryValueToStringByteArray() {
        // Windows-1252 with null terminator
        byte[] cp1252 = new byte[] { 't', 'e', 's', 't', 0x00 };
        assertThat(RegistryValueUtil.registryValueToString(cp1252), is("test"));
    }

    @Test
    void testRegistryValueToStringByteArrayUtf16() {
        // UTF-16LE with double-null terminator
        byte[] utf16 = new byte[] { 't', 0, 'e', 0, 's', 0, 't', 0, 0, 0 };
        assertThat(RegistryValueUtil.registryValueToString(utf16), is("test"));
    }
}
