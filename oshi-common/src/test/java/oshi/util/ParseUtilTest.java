/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * The Class ParseUtilTest.
 */
class ParseUtilTest {

    private static final double EPSILON = Double.MIN_VALUE;

    private enum TestEnum {
        FOO, BAR, BAZ;
    }

    /**
     * Verifies that UTF-16LE null-terminated binary data is decoded into a String.
     */
    @Test
    void testDecodeUtf16LE() {
        byte[] bytes = { 'A', 0, 'B', 0, 'C', 0, 0, 0 };
        assertEquals("ABC", ParseUtil.decodeBinaryToString(bytes));
    }

    @Test
    void testDecodeNullOrEmptyBinary() {
        assertThat(ParseUtil.decodeBinaryToString(null), is(nullValue()));
        assertThat(ParseUtil.decodeBinaryToString(new byte[0]), is(nullValue()));
    }

    /**
     * Verifies that Windows-1252 null-terminated binary data is decoded into a String.
     */
    @Test
    void testDecodeCp1252() {
        byte[] bytes = { 0x41, 0x42, 0x43, 0x00 };
        assertEquals("ABC", ParseUtil.decodeBinaryToString(bytes));
    }

    /**
     * Verifies that non-text binary data falls back to a hex string representation.
     */
    @Test
    void testDecodeHexFallback() {
        byte[] bytes = { 0x01, 0x02, 0x0A, (byte) 0xFF };
        assertEquals("01 02 0A FF", ParseUtil.decodeBinaryToString(bytes));
    }

    /**
     * Test parse hertz.
     */
    @Test
    void testParseSpeed() {
        assertThat("parse OneMT/s", ParseUtil.parseSpeed("OneMT/s"), is(-1L));
        assertThat("parse NotEvenAMegaTransferPerSec", ParseUtil.parseSpeed("NotEvenAMegaTransferPerSec"), is(-1L));
        assertThat("parse 10000000000000 MT/s", ParseUtil.parseSpeed("10000000000000 MT/s"), is(Long.MAX_VALUE));
        assertThat("parse 1MT/s", ParseUtil.parseSpeed("1MT/s"), is(1_000_000L));
        assertThat("parse 1.5 MT/s", ParseUtil.parseSpeed("1.5 MT/s"), is(1_500_000L));
        // fallback to Hz
        assertThat("parse 1Hz", ParseUtil.parseSpeed("1Hz"), is(1L));
        assertThat("parse 500 Hz", ParseUtil.parseSpeed("500 Hz"), is(500L));
        assertThat("parse 1kHz", ParseUtil.parseSpeed("1kHz"), is(1_000L));
        assertThat("parse 1MHz", ParseUtil.parseSpeed("1MHz"), is(1_000_000L));
        assertThat("parse 1GHz", ParseUtil.parseSpeed("1GHz"), is(1_000_000_000L));
        assertThat("parse 1.5GHz", ParseUtil.parseSpeed("1.5GHz"), is(1_500_000_000L));
        assertThat("parse 1THz", ParseUtil.parseSpeed("1THz"), is(1_000_000_000_000L));
    }

    /**
     * Test parse hertz.
     */
    @Test
    void testParseHertz() {
        assertThat("parse OneHz", ParseUtil.parseHertz("OneHz"), is(-1L));
        assertThat("parse NotEvenAHertz", ParseUtil.parseHertz("NotEvenAHertz"), is(-1L));
        assertThat("parse 10000000000000000000 Hz", ParseUtil.parseHertz("10000000000000000000 Hz"),
                is(Long.MAX_VALUE));
        assertThat("parse 1Hz", ParseUtil.parseHertz("1Hz"), is(1L));
        assertThat("parse 500 Hz", ParseUtil.parseHertz("500 Hz"), is(500L));
        assertThat("parse 1kHz", ParseUtil.parseHertz("1kHz"), is(1_000L));
        assertThat("parse 1MHz", ParseUtil.parseHertz("1MHz"), is(1_000_000L));
        assertThat("parse 1GHz", ParseUtil.parseHertz("1GHz"), is(1_000_000_000L));
        assertThat("parse 1.5GHz", ParseUtil.parseHertz("1.5GHz"), is(1_500_000_000L));
        assertThat("parse 1THz", ParseUtil.parseHertz("1THz"), is(1_000_000_000_000L));
        // GHz exceeds max double
    }

    /**
     * A number whose decimals are introduced by something other than a decimal point is not parseable, and the
     * documented -1 is the answer. This used to throw NumberFormatException instead.
     * <p>
     * These cases also pin the pattern's deliberately unescaped {@code .}, which matches any separator and so captures
     * the malformed number whole for the parser to reject. Escaping it would let the match re-anchor on a fragment and
     * report "2 40GHz" as a 40 GHz processor.
     */
    @Test
    void testParseHertzNonDecimalSeparator() {
        assertThat("comma decimal separator", ParseUtil.parseHertz("2,40GHz"), is(-1L));
        assertThat("comma separator in a CPU name", ParseUtil.parseHertz("Intel Core i7 @ 2,40GHz"), is(-1L));
        assertThat("space separator", ParseUtil.parseHertz("2 40GHz"), is(-1L));
        assertThat("letter separator", ParseUtil.parseHertz("2X00MHz"), is(-1L));
        assertThat("still parses a decimal point", ParseUtil.parseHertz("Intel Core i7 @ 2.40GHz"), is(2_400_000_000L));
        assertThat("still parses without a decimal", ParseUtil.parseHertz("Some CPU @ 3GHz"), is(3_000_000_000L));
    }

    /**
     * Test parse string.
     */
    @Test
    void testParseLastInt() {
        assertThat("parse def -1", ParseUtil.parseLastInt("foo : bar", -1), is(-1));
        assertThat("parse 1", ParseUtil.parseLastInt("foo : 1", 0), is(1));
        assertThat("parse def 2", ParseUtil.parseLastInt("foo", 2), is(2));
        assertThat("parse maxInt+1", ParseUtil.parseLastInt("max_int plus one is 2147483648", 3), is(3));
        assertThat("parse 0xff", ParseUtil.parseLastInt("0xff", 4), is(255));

        assertThat("parse def -1 as long", ParseUtil.parseLastLong("foo : bar", -1L), is(-1L));
        assertThat("parse 1 as long", ParseUtil.parseLastLong("foo : 1", 0L), is(1L));
        assertThat("parse def 2 as long", ParseUtil.parseLastLong("foo", 2L), is(2L));
        assertThat("parse maxInt+1 as long", ParseUtil.parseLastLong("max_int plus one is" + " 2147483648", 3L),
                is(2147483648L));
        assertThat("parse 0xff as long", ParseUtil.parseLastLong("0xff", 0L), is(255L));

        assertThat("parse def -1 as double", ParseUtil.parseLastDouble("foo : bar", -1d), is(closeTo(-1d, EPSILON)));
        assertThat("parse 1.0 as double", ParseUtil.parseLastDouble("foo : 1.0", 0d), is(closeTo(1d, EPSILON)));
        assertThat("parse def 2 as double", ParseUtil.parseLastDouble("foo", 2d), is(closeTo(2d, EPSILON)));
    }

    /**
     * Test parse string.
     */
    @Test
    void testParseLastString() {
        assertThat("parse bar", ParseUtil.parseLastString("foo : bar"), is("bar"));
        assertThat("parse foo", ParseUtil.parseLastString("foo"), is("foo"));
        assertThat("parse \"\"", ParseUtil.parseLastString(""), is(emptyString()));
    }

    /**
     * Test hex string to byte array (and back).
     */
    @Test
    void testHexStringToByteArray() {
        byte[] temp = { (byte) 0x12, (byte) 0xaf };
        assertThat(Arrays.equals(temp, ParseUtil.hexStringToByteArray("12af")), is(true));
        assertThat("parse 12AF", ParseUtil.byteArrayToHexString(temp), is("12AF"));
        temp = new byte[0];
        assertThat(Arrays.equals(temp, ParseUtil.hexStringToByteArray("expected error abcde")), is(true));
        assertThat(Arrays.equals(temp, ParseUtil.hexStringToByteArray("abcde")), is(true));
    }

    /**
     * Test string to byte array.
     */
    @Test
    void testStringToByteArray() {
        byte[] temp = { (byte) '1', (byte) '2', (byte) 'a', (byte) 'f', (byte) 0 };
        assertThat(Arrays.equals(temp, ParseUtil.asciiStringToByteArray("12af", 5)), is(true));
    }

    /**
     * Test long to byte array.
     */
    @Test
    void testLongToByteArray() {
        byte[] temp = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0 };
        assertThat(Arrays.equals(temp, ParseUtil.longToByteArray(0x12345678, 4, 5)), is(true));
    }

    /**
     * Test string and byte array to long.
     */
    @Test
    void testStringAndByteArrayToLong() {
        byte[] temp = { (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e' };
        long abcde = (long) temp[0] << 32 | temp[1] << 24 | temp[2] << 16 | temp[3] << 8 | temp[4];
        long edcba = (long) temp[4] << 32 | temp[3] << 24 | temp[2] << 16 | temp[1] << 8 | temp[0];
        // Test string
        assertThat("parse \"abcde\"", ParseUtil.strToLong("abcde", 5), is(abcde));
        // Test byte array
        assertThat("Incorrect parsing of " + abcde, ParseUtil.byteArrayToLong(temp, 5), is(abcde));
        assertThat("Incorrect parsing of " + abcde + " BE", ParseUtil.byteArrayToLong(temp, 5, true), is(abcde));
        assertThat("Incorrect parsing of " + edcba + " LE", ParseUtil.byteArrayToLong(temp, 5, false), is(edcba));
    }

    @Test
    void testByteArrayToLongSizeTooBig() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParseUtil.byteArrayToLong(new byte[10], 9);
        });
    }

    @Test
    void testByteArrayToLongSizeBigger() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParseUtil.byteArrayToLong(new byte[7], 8);
        });
    }

    /**
     * Test byte array to float
     */
    @Test
    void testByteArrayToFloat() {
        byte[] temp = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a };
        float f = (temp[0] << 22 | temp[1] << 14 | temp[2] << 6 | temp[3] >>> 2) + (float) (temp[3] & 0x3) / 0x4;
        assertEquals(f, ParseUtil.byteArrayToFloat(temp, 4, 2), Float.MIN_VALUE);
        f = 0x12345 + (float) 0x6 / 0x10;
        assertEquals(f, ParseUtil.byteArrayToFloat(temp, 3, 4), Float.MIN_VALUE);
        f = 0x123 + (float) 0x4 / 0x10;
        assertEquals(f, ParseUtil.byteArrayToFloat(temp, 2, 4), Float.MIN_VALUE);
    }

    /**
     * Test unsigned int to long
     */
    @Test
    void testUnsignedIntToLong() {
        assertThat("parse 0 as long", ParseUtil.unsignedIntToLong(0), is(0L));
        assertThat("parse 123 as long", ParseUtil.unsignedIntToLong(123), is(123L));
        assertThat("parse 4294967295L as long", ParseUtil.unsignedIntToLong(0xffffffff), is(4294967295L));
    }

    /**
     * Test unsigned long to signed long
     */
    @Test
    void testUnsignedLongToSignedLong() {
        assertThat("parse 1 as signed long", ParseUtil.unsignedLongToSignedLong(Long.MAX_VALUE + 2), is(1L));
        assertThat("parse 123 as signed long", ParseUtil.unsignedLongToSignedLong(123), is(123L));
        assertThat("parse 9223372036854775807 as signed long", ParseUtil.unsignedLongToSignedLong(9223372036854775807L),
                is(9223372036854775807L));
    }

    /**
     * Test hex string to string
     */
    @Test
    void testHexStringToString() {
        assertThat("parse ABC as string", ParseUtil.hexStringToString("414243"), is("ABC"));
        assertThat("parse ab00cd as string", ParseUtil.hexStringToString("ab00cd"), is("ab00cd"));
        assertThat("parse ab88cd as string", ParseUtil.hexStringToString("ab88cd"), is("ab88cd"));
        assertThat("parse notHex as string", ParseUtil.hexStringToString("notHex"), is("notHex"));
        assertThat("parse 320 as string", ParseUtil.hexStringToString("320"), is("320"));
        assertThat("parse 0 as string", ParseUtil.hexStringToString("0"), is("0"));
    }

    /**
     * Test parse int
     */
    @Test
    void testParseIntOrDefault() {
        assertThat("parse 123", ParseUtil.parseIntOrDefault("123", 45), is(123));
        assertThat("parse 45", ParseUtil.parseIntOrDefault("123X", 45), is(45));
        assertThat("parse null", ParseUtil.parseIntOrDefault(null, 45), is(45));
    }

    /**
     * Test parse long
     */
    @Test
    void testParseLongOrDefault() {
        assertThat("parse 123", ParseUtil.parseLongOrDefault("123", 45L), is(123L));
        assertThat("parse 45", ParseUtil.parseLongOrDefault("123L", 45L), is(45L));
        assertThat("parse null", ParseUtil.parseLongOrDefault(null, 45L), is(45L));
    }

    /**
     * Test parse long
     */
    @Test
    void testParseUnsignedLongOrDefault() {
        assertThat("parse 9223372036854775807L", ParseUtil.parseUnsignedLongOrDefault("9223372036854775807", 123L),
                is(9223372036854775807L));
        assertThat("parse 9223372036854775808L", ParseUtil.parseUnsignedLongOrDefault("9223372036854775808", 45L),
                is(-9223372036854775808L));
        assertThat("parse 1L", ParseUtil.parseUnsignedLongOrDefault("18446744073709551615", 123L), is(-1L));
        assertThat("parse 0L", ParseUtil.parseUnsignedLongOrDefault("18446744073709551616", 45L), is(0L));
        assertThat("parse 123L", ParseUtil.parseUnsignedLongOrDefault("9223372036854775808L", 123L), is(123L));
        assertThat("parse null", ParseUtil.parseUnsignedLongOrDefault(null, 123L), is(123L));
    }

    /**
     * Test parse double
     */
    @Test
    void testParseDoubleOrDefault() {
        assertThat("parse 1.23d", ParseUtil.parseDoubleOrDefault("1.23", 4.5d), is(closeTo(1.23d, EPSILON)));
        assertThat("parse 4.5d", ParseUtil.parseDoubleOrDefault("one.twentythree", 4.5d), is(closeTo(4.5d, EPSILON)));
        assertThat("parse null", ParseUtil.parseDoubleOrDefault(null, 4.5d), is(closeTo(4.5d, EPSILON)));
    }

    /**
     * Test parse DHMS
     */
    @Test
    void testParseDHMSOrDefault() {
        assertThat("parse 93784050L", ParseUtil.parseDHMSOrDefault("1-02:03:04.05", 0L), is(93784050L));
        assertThat("parse 93784000L", ParseUtil.parseDHMSOrDefault("1-02:03:04", 0L), is(93784000L));
        assertThat("parse 7384000L", ParseUtil.parseDHMSOrDefault("02:03:04", 0L), is(7384000L));
        assertThat("parse 184050L", ParseUtil.parseDHMSOrDefault("03:04.05", 0L), is(184050L));
        assertThat("parse 184000L", ParseUtil.parseDHMSOrDefault("03:04", 0L), is(184000L));
        assertThat("parse 4000L", ParseUtil.parseDHMSOrDefault("04", 0L), is(4000L));
        assertThat("parse 0L", ParseUtil.parseDHMSOrDefault("04:05-06", 0L), is(0L));
        assertThat("parse null", ParseUtil.parseDHMSOrDefault(null, 0L), is(0L));
        // DD-hh:mm:ss.ddd format with multi-digit days and 3-digit fractional seconds
        // 10*86400000 + 5*3600000 + 30*60000 + 45*1000 + 123 = 864000000 + 18000000 + 1800000 + 45000 + 123
        assertThat("parse 10-05:30:45.123", ParseUtil.parseDHMSOrDefault("10-05:30:45.123", 0L), is(883845123L));
    }

    /**
     * Test parse UUID
     */
    @Test
    void testParseUuidOrDefault() {
        assertThat("parse uuid", ParseUtil.parseUuidOrDefault("123e4567-e89b-12d3-a456-426655440000", "default"),
                is("123e4567-e89b-12d3-a456-426655440000"));
        assertThat("parse uuid in string",
                ParseUtil.parseUuidOrDefault("The UUID is 123E4567-E89B-12D3-A456-426655440000!", "default"),
                is("123e4567-e89b-12d3-a456-426655440000"));
        assertThat("parse foo or default", ParseUtil.parseUuidOrDefault("foo", "default"), is("default"));
        assertThat("parse null", ParseUtil.parseUuidOrDefault(null, "default"), is("default"));
    }

    /**
     * Test parse SingleQuoteString
     */
    @Test
    void testGetSingleQuoteStringValue() {
        assertThat("parse bar", ParseUtil.getSingleQuoteStringValue("foo = 'bar' (string)"), is("bar"));
        assertThat("parse empty string", ParseUtil.getSingleQuoteStringValue("foo = bar (string)"), is(""));
    }

    @Test
    void testGetDoubleQuoteStringValue() {
        assertThat("parse bar", ParseUtil.getDoubleQuoteStringValue("foo = \"bar\" (string)"), is("bar"));
        assertThat("parse empty string", ParseUtil.getDoubleQuoteStringValue("hello"), is(""));
    }

    /**
     * Test parse SingleQuoteBetweenMultipleQuotes
     */
    @Test
    void testGetStringBetweenMultipleQuotes() {
        assertThat("parse Single quotes between Multiple quotes",
                ParseUtil.getStringBetween("hello = $hello $ is $", '$'), is("hello $ is"));
        assertThat("parse Single quotes between Multiple quotes",
                ParseUtil.getStringBetween("pci.device = 'Realtek AC'97 Audio'", '\''), is("Realtek AC'97 Audio"));
        // A single, unmatched delimiter must yield "" rather than throwing StringIndexOutOfBoundsException
        assertThat("single unmatched delimiter", ParseUtil.getStringBetween("key = 'value", '\''), is(""));
        assertThat("no delimiter", ParseUtil.getStringBetween("key = value", '\''), is(""));
    }

    /**
     * Test getStringBefore
     */
    @Test
    void testGetStringBefore() {
        assertThat("delimiter present", ParseUtil.getStringBefore("Port-HDMI@1/DisplayPort", '/'), is("Port-HDMI@1"));
        assertThat("first of several delimiters", ParseUtil.getStringBefore("a/b/c", '/'), is("a"));
        assertThat("no delimiter returns whole string", ParseUtil.getStringBefore("disp0", ','), is("disp0"));
        assertThat("trailing delimiter", ParseUtil.getStringBefore("disp0,", ','), is("disp0"));
        assertThat("leading delimiter", ParseUtil.getStringBefore(",t6030", ','), is(""));
        assertThat("delimiter only", ParseUtil.getStringBefore("/", '/'), is(""));
        assertThat("empty", ParseUtil.getStringBefore("", '/'), is(""));
        assertThat("null", ParseUtil.getStringBefore(null, '/'), is(""));
        assertThat("whitespace is a value", ParseUtil.getStringBefore(" /x", '/'), is(" "));
    }

    /**
     * Test parse FirstIntValue
     */
    @Test
    void testGetFirstIntValue() {
        assertThat("parse FirstIntValue", ParseUtil.getFirstIntValue("foo = 42 (0x2a) (int)"), is(42));
        assertThat("parse FirstIntValue", ParseUtil.getFirstIntValue("foo = 0x2a (int)"), is(0));
        assertThat("parse FirstIntValue", ParseUtil.getFirstIntValue("42"), is(42));
        assertThat("parse FirstIntValue", ParseUtil.getFirstIntValue("10.12.2"), is(10));
    }

    /**
     * Test parse NthIntValue
     */
    @Test
    void testGetNthIntValue() {
        assertThat("parse NthIntValue", ParseUtil.getNthIntValue("foo = 42 (0x2a) (int)", 3), is(2));
        assertThat("parse NthIntValue", ParseUtil.getNthIntValue("foo = 0x2a (int)", 3), is(0));
        assertThat("parse NthIntValue", ParseUtil.getNthIntValue("10.12.2", 2), is(12));
    }

    /**
     * Test parse removeMatchingString
     */
    @Test
    void testRemoveMatchingString() {
        assertThat("parse removeMatchingString", ParseUtil.removeMatchingString("foo = 42 (0x2a) (int)", "0x2a"),
                is("foo = 42 () (int)"));
        assertThat("parse removeMatchingString", ParseUtil.removeMatchingString("foo = 0x2a (int)", "qqq"),
                is("foo = 0x2a (int)"));
        assertThat("parse removeMatchingString", ParseUtil.removeMatchingString("10.12.2", "2"), is("10.1."));
        assertThat("parse removeMatchingString", ParseUtil.removeMatchingString("10.12.2", "10.12.2"),
                is(emptyString()));
        assertThat("parse removeMatchingString", ParseUtil.removeMatchingString("", "10.12.2"), is(emptyString()));
        assertThat("parse removeMatchingString", ParseUtil.removeMatchingString(null, "10.12.2"), is(nullValue()));
        assertThat("parse removeMatchingString", ParseUtil.removeMatchingString("10.12.2", "10.12."), is("2"));
    }

    /**
     * Test parse string to array
     */
    @Test
    void testParseStringToLongArray() {
        int[] indices = { 1, 3 };
        long now = System.currentTimeMillis();

        String foo = String.format(Locale.ROOT, "The numbers are %d %d %d %d", 123, 456, 789, now);
        int count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 4 for \"" + foo + "\"", count, is(4));
        long[] result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertThat("result[0] should be 456 using parseStringToLongArray on \"" + foo + "\"", result[0], is(456L));
        assertThat("result[1] should be " + now + " using parseStringToLongArray on \"" + foo + "\"", result[1],
                is(now));

        foo = String.format(Locale.ROOT, "The numbers are %d %d %d %d %s", 123, 456, 789, now,
                "709af748-5f8e-41b3-b73a-b440ef4406c8");
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 4 for \"" + foo + "\"", count, is(4));
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertThat("result[0] should be 456 using parseStringToLongArray on \"" + foo + "\"", result[0], is(456L));
        assertThat("result[1] should be " + now + " using parseStringToLongArray on \"" + foo + "\"", result[1],
                is(now));

        foo = String.format(Locale.ROOT, "The numbers are %d -%d %d +%d", 123, 456, 789, now);
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 4 for \"" + foo + "\"", count, is(4));
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertThat("result[0] should be -4456 using parseStringToLongArray on \"" + foo + "\"", result[0], is(-456L));
        assertThat("result[1] index should be 456 using parseStringToLongArray on \"" + foo + "\"", result[1], is(now));

        foo = String.format(Locale.ROOT, "NOLOG: Invalid character %d %s %d %d", 123, "4v6", 789, now);
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 2 for \"" + foo + "\"", count, is(2));
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertThat("result[1] index should be 0 using parseStringToLongArray on \"" + foo + "\"", result[1], is(0L));

        foo = String.format(Locale.ROOT, "Exceeds max long %d %d %d 1%d", 123, 456, 789, Long.MAX_VALUE);
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertThat("result[1] index should be " + Long.MAX_VALUE
                + " (Long.MAX_VALUE) using parseStringToLongArray on \"" + foo + "\"", result[1], is(Long.MAX_VALUE));

        foo = String.format(Locale.ROOT, "NOLOG: String too short %d %d %d %d", 123, 456, 789, now);
        result = ParseUtil.parseStringToLongArray(foo, indices, 9, ' ');
        assertThat("result[1] index should be 0 using parseStringToLongArray on \"" + foo + "\"", result[1], is(0L));

        foo = String.format(Locale.ROOT, "NOLOG: Array too short %d %d %d %d", 123, 456, 789, now);
        result = ParseUtil.parseStringToLongArray(foo, indices, 2, ' ');
        assertThat("result[1] index should be 0 using parseStringToLongArray on \"" + foo + "\"", result[1], is(0L));

        foo = String.format(Locale.ROOT, "Invalid character %d %s %d %d", 123, "4v6", 789, now);
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertThat("result[1] index should be 0 using parseStringToLongArray on \"" + foo + "\"", result[1], is(0L));

        foo = String.format(Locale.ROOT, "String too short %d %d %d %d", 123, 456, 789, now);
        result = ParseUtil.parseStringToLongArray(foo, indices, 9, ' ');
        assertThat("result[1] index should be 0 using parseStringToLongArray on \"" + foo + "\"", result[1], is(0L));

        foo = String.format(Locale.ROOT, "%d %d %d %d", 123, 456, 789, now);
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 4 for \"" + foo + "\"", count, is(4));

        foo = String.format(Locale.ROOT, "%d %d %d %d nonNumeric", 123, 456, 789, now);
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 4 for \"" + foo + "\"", count, is(4));

        foo = String.format(Locale.ROOT, "%d %d %d %d 123-456", 123, 456, 789, now);
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 4 for \"" + foo + "\"", count, is(4));

        foo = String.format(Locale.ROOT, "%d %d %d %d", 123, 456, 789, now);
        indices = new int[] { 0 };
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertThat("result[0] should be 123 using parseStringToLongArray on \"" + foo + "\"", result[0], is(123L));

        // countStringToLongArray with empty string
        assertThat("countStringToLongArray should return 1 for empty string", ParseUtil.countStringToLongArray("", ' '),
                is(1));

        // countStringToLongArray with single number (no delimiter)
        assertThat("countStringToLongArray should return 1 for single number",
                ParseUtil.countStringToLongArray("42", ' '), is(1));

        // countStringToLongArray with UUID-like field at end (multiple dashes = non-numeric)
        foo = "123 456 789 a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 3 for string with UUID at end", count, is(3));

        // countStringToLongArray with leading + signs
        foo = "+100 +200 +300";
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 3 for string with leading + signs", count, is(3));

        // countStringToLongArray with negative numbers (single dash is a sign, not UUID)
        foo = "-100 200 -300";
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 3 for string with negative numbers", count, is(3));

        // parseStringToLongArray with negative numbers
        foo = "10 -20 30 -40";
        indices = new int[] { 0, 1, 2, 3 };
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertThat("result[0] should be 10", result[0], is(10L));
        assertThat("result[1] should be -20", result[1], is(-20L));
        assertThat("result[2] should be 30", result[2], is(30L));
        assertThat("result[3] should be -40", result[3], is(-40L));

        // parseStringToLongArray with Long.MAX_VALUE overflow (power > 18)
        foo = "1 12345678901234567890 3 4";
        indices = new int[] { 1 };
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertThat("result[0] should be Long.MAX_VALUE for overflow", result[0], is(Long.MAX_VALUE));

        // parseStringToLongArray with UUID-like non-numeric fields at end being ignored
        foo = "100 200 300 a1b2-c3d4-e5f6";
        indices = new int[] { 0, 1, 2 };
        result = ParseUtil.parseStringToLongArray(foo, indices, 3, ' ');
        assertThat("result[0] should be 100 with trailing UUID ignored", result[0], is(100L));
        assertThat("result[1] should be 200 with trailing UUID ignored", result[1], is(200L));
        assertThat("result[2] should be 300 with trailing UUID ignored", result[2], is(300L));
    }

    @Test
    void testTextBetween() {
        String text = "foo bar baz";
        String before = "foo";
        String after = "baz";
        assertThat(ParseUtil.getTextBetweenStrings(text, before, after), is(" bar "));

        before = "";
        assertThat(ParseUtil.getTextBetweenStrings(text, before, after), is("foo bar "));

        before = "food";
        assertThat(ParseUtil.getTextBetweenStrings(text, before, after), is(emptyString()));

        before = "foo";
        after = "qux";
        assertThat(ParseUtil.getTextBetweenStrings(text, before, after), is(emptyString()));

        // Both before and after markers absent
        assertThat(ParseUtil.getTextBetweenStrings(text, "xyz", "abc"), is(emptyString()));
    }

    @Test
    void testFiletimeToMs() {
        assertThat(ParseUtil.filetimeToUtcMs(128166372003061629L, false), is(1172163600306L));
    }

    @Test
    void testParseCimDateTimeToOffset() {
        String cimDateTime = "20160513072950.782000-420";
        OffsetDateTime parsedTime = ParseUtil.parseCimDateTimeToOffset(cimDateTime);
        assertNotNull(parsedTime);
        // 2016-05-13T07:29:50 == 1463124590
        // Add 420 minutes to get unix seconds
        Instant timeInst = Instant.ofEpochMilli(1463124590_782L + 60 * 420_000L);
        assertThat(parsedTime.toInstant(), is(timeInst));
        OffsetDateTime badParsingTime = ParseUtil.parseCimDateTimeToOffset("Not a datetime");
        assertNotNull(badParsingTime);
        assertThat(badParsingTime.toInstant(), is(Instant.EPOCH));
    }

    @Test
    void testFilePathStartsWith() {
        List<String> prefixList = List.of("/foo", "/bar");
        assertThat(ParseUtil.filePathStartsWith(prefixList, "/foo"), is(true));
        assertThat(ParseUtil.filePathStartsWith(prefixList, "/foo/bar"), is(true));
        assertThat(ParseUtil.filePathStartsWith(prefixList, "/foobar"), is(false));
        assertThat(ParseUtil.filePathStartsWith(prefixList, "/foo/baz"), is(true));
        assertThat(ParseUtil.filePathStartsWith(prefixList, "/baz/foo"), is(false));
    }

    @Test
    void testParseDecimalMemorySizeToBinary() {
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("Not a number"), is(0L));
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("1"), is(1L));
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("1 kB"), is(1024L));
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("1 KB"), is(1024L));
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("1 MB"), is(1_048_576L));
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("1MB"), is(1_048_576L));
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("1 GB"), is(1_073_741_824L));
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("1 TB"), is(1_099_511_627_776L));
        // Single-char suffixes (sysfs cache format)
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("32K"), is(32_768L));
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("1M"), is(1_048_576L));
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("2G"), is(2_147_483_648L));
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("1T"), is(1_099_511_627_776L));
        // Bare "B" suffix should not multiply
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("32B"), is(32L));
        // T suffix without space (multi-char suffix via regex path)
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("2TB"), is(2_199_023_255_552L));
        // Multi-digit number without space
        assertThat(ParseUtil.parseDecimalMemorySizeToBinary("4096MB"), is(4_294_967_296L));
    }

    @Test
    void testParseDeviceIdToVendorProductSerial() {
        Triplet<String, String, String> idsAndSerial = ParseUtil
                .parseDeviceIdToVendorProductSerial("PCI\\VEN_10DE&DEV_134B&SUBSYS_00081414&REV_A2\\4&25BACB6&0&00E0");
        assertNotNull(idsAndSerial, "VEN_ DEV_ deviceID failed to parse");
        assertThat("Vendor ID failed to parse", idsAndSerial.getA(), is("0x10de"));
        assertThat("Product ID failed to parse", idsAndSerial.getB(), is("0x134b"));
        assertThat("SerialNumber should not have parsed", idsAndSerial.getC(), is(emptyString()));

        idsAndSerial = ParseUtil.parseDeviceIdToVendorProductSerial("USB\\VID_045E&PID_07C6\\000001000000");
        assertNotNull(idsAndSerial, "VID_ PID_ serial deviceID failed to parse");
        assertThat("Vendor ID failed to parse", idsAndSerial.getA(), is("0x045e"));
        assertThat("Product ID failed to parse", idsAndSerial.getB(), is("0x07c6"));
        assertThat("SerialNumber failed to parse", idsAndSerial.getC(), is("000001000000"));

        idsAndSerial = ParseUtil.parseDeviceIdToVendorProductSerial("USB\\VID_045E&PID_07C6\\5&000001000000");
        assertNotNull(idsAndSerial, "VID_ PID_ nonserial deviceID failed to parse");
        assertThat("Vendor ID failed to parse", idsAndSerial.getA(), is("0x045e"));
        assertThat("Product ID failed to parse", idsAndSerial.getB(), is("0x07c6"));
        assertThat("SerialNumber should not have parsed", idsAndSerial.getC(), is(emptyString()));

        idsAndSerial = ParseUtil
                .parseDeviceIdToVendorProductSerial("PCI\\VEN_80286&DEV_19116&SUBSYS_00141414&REV_07\\3&11583659&0&10");
        assertThat("Vender and Product IDs should not have parsed", idsAndSerial, is(nullValue()));
    }

    @Test
    void testParseDeviceIdToVendorProductIds() {
        // Full PNPDeviceID with trailing instance — same inputs as the Serial test
        Pair<Integer, Integer> ids = ParseUtil
                .parseDeviceIdToVendorProductIds("PCI\\VEN_10DE&DEV_134B&SUBSYS_00081414&REV_A2\\4&25BACB6&0&00E0");
        assertNotNull(ids, "PCI full deviceID failed to parse");
        assertThat("Vendor ID", ids.getA(), is(0x10DE));
        assertThat("Product ID", ids.getB(), is(0x134B));

        // USB VID/PID with serial
        ids = ParseUtil.parseDeviceIdToVendorProductIds("USB\\VID_045E&PID_07C6\\000001000000");
        assertNotNull(ids, "USB VID/PID failed to parse");
        assertThat("Vendor ID", ids.getA(), is(0x045E));
        assertThat("Product ID", ids.getB(), is(0x07C6));

        // Bare MatchingDeviceId — no trailing backslash instance (the case VEN_DEV_PATTERN handled)
        ids = ParseUtil.parseDeviceIdToVendorProductIds("pci\\ven_8086&dev_56a0&subsys_00008086&rev_08");
        assertNotNull(ids, "Bare MatchingDeviceId failed to parse");
        assertThat("Vendor ID", ids.getA(), is(0x8086));
        assertThat("Product ID", ids.getB(), is(0x56A0));

        // Too-long hex groups — should not parse
        ids = ParseUtil
                .parseDeviceIdToVendorProductIds("PCI\\VEN_80286&DEV_19116&SUBSYS_00141414&REV_07\\3&11583659&0&10");
        assertThat("Over-length IDs should not have parsed", ids, is(nullValue()));

        // Null input
        assertThat("Null should return null", ParseUtil.parseDeviceIdToVendorProductIds(null), is(nullValue()));
    }

    @Test
    void testParseLshwResourceString() {
        assertThat(
                ParseUtil.parseLshwResourceString(
                        "irq:46 ioport:6000(size=32) memory:b0000000-bfffffff memory:e2000000-e200ffff"),
                is(268_435_456L + 65_536L));
        assertThat(
                ParseUtil.parseLshwResourceString(
                        "irq:46 ioport:6000(size=32) memory:b0000000-bfffffff memory:x2000000-e200ffff"),
                is(268_435_456L));
        assertThat(ParseUtil.parseLshwResourceString(
                "irq:46 ioport:6000(size=32) memory:x0000000-bfffffff memory:e2000000-e200ffff"), is(65_536L));
        assertThat(ParseUtil.parseLshwResourceString("some random string"), is(0L));
    }

    @Test
    void testParseLspciMachineReadable() {
        Pair<String, String> pair = ParseUtil.parseLspciMachineReadable("foo [bar]");
        assertNotNull(pair, "Well-formed lspci line failed to parse");
        assertThat("First element of pair mismatch.", pair.getA(), is("foo"));
        assertThat("Second element of pair mismatch.", pair.getB(), is("bar"));
        assertThat(ParseUtil.parseLspciMachineReadable("Bad format"), is(nullValue()));
    }

    @Test
    void testParseLspciMemorySize() {
        assertThat(ParseUtil.parseLspciMemorySize("Doesn't parse"), is(0L));
        assertThat(ParseUtil.parseLspciMemorySize("Foo [size=64K]"), is(64L * 1024L));
        assertThat(ParseUtil.parseLspciMemorySize("Foo [size=256M]"), is(256L * 1024L * 1024L));
    }

    @Test
    void testParseHyphenatedIntList() {
        String s = "1";
        List<Integer> parsed = ParseUtil.parseHyphenatedIntList(s);
        assertThat(parsed, not(hasItems(0)));
        assertThat(parsed, contains(1));

        s = "0 2-5 7";
        parsed = ParseUtil.parseHyphenatedIntList(s);
        assertThat(parsed, contains(0, 2, 3, 4, 5, 7));
        assertThat(parsed, not(hasItems(1)));
        assertThat(parsed, not(hasItems(6)));

        s = "0, 2-5, 7-8, 9";
        parsed = ParseUtil.parseHyphenatedIntList(s);
        assertThat(parsed, contains(0, 2, 3, 4, 5, 7, 8, 9));
        assertThat(parsed, not(hasItems(1)));
        assertThat(parsed, not(hasItems(6)));
    }

    @Test
    void testParseMmDdYyyyToYyyyMmDD() {
        assertThat("Unable to parse MM-DD-YYYY date string into YYYY-MM-DD date string",
                ParseUtil.parseMmDdYyyyToYyyyMmDD("00-11-2222"), is("2222-00-11"));
        assertThat("Date string should not be parsed", ParseUtil.parseMmDdYyyyToYyyyMmDD("badstr"), is("badstr"));
    }

    @Test
    void testParseIntToIP() {
        // IP addresses are big endian
        int ip = 1 | 2 << 8 | 3 << 16 | 4 << 24;
        byte[] ipb = { (byte) 1, (byte) 2, (byte) 3, (byte) 4 };
        assertThat("IP did not parse properly", ParseUtil.parseIntToIP(ip), is(ipb));
    }

    @Test
    void testParseIntArrayToIP() {
        // IP addresses are big endian
        int[] ip = new int[4];
        ip[0] = 1 | 2 << 8 | 3 << 16 | 4 << 24;
        ip[1] = 5 | 6 << 8 | 7 << 16 | 8 << 24;
        ip[2] = 9 | 10 << 8 | 11 << 16 | 12 << 24;
        ip[3] = 13 | 14 << 8 | 15 << 16 | 16 << 24;
        byte[] ipb = { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9,
                (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 16 };
        assertThat("IP array did not parse properly", ParseUtil.parseIntArrayToIP(ip), is(ipb));
    }

    @Test
    void testBigEndian16ToLittleEndian() {
        assertThat("Port 80 did not convert properly", ParseUtil.bigEndian16ToLittleEndian(0x5000), is(80));
        assertThat("Port 443 did not convert properly", ParseUtil.bigEndian16ToLittleEndian(0xBB01), is(443));
    }

    @Test
    void testParseUtAddrV6toIP() {
        int[] zero = { 0, 0, 0, 0 };
        int[] loopback = { 0, 0, 0, 1 };
        String v6test = "2001:db8:85a3::8a2e:370:7334";
        int[] v6 = new int[4];
        v6[0] = (int) ParseUtil.hexStringToLong("20010db8", 0L);
        v6[1] = (int) ParseUtil.hexStringToLong("85a30000", 0L);
        v6[2] = (int) ParseUtil.hexStringToLong("00008a2e", 0L);
        v6[3] = (int) ParseUtil.hexStringToLong("03707334", 0L);
        String v4test = "127.0.0.1";
        int[] v4 = new int[4];
        v4[0] = (127 << 24) + 1;
        int[] invalid = { 0, 0, 0 };

        assertThat("Unspecified address failed", ParseUtil.parseUtAddrV6toIP(zero), is("::"));
        assertThat("Loopback address failed", ParseUtil.parseUtAddrV6toIP(loopback), is("::1"));
        assertThat("V6 parsing failed", ParseUtil.parseUtAddrV6toIP(v6), is(v6test));
        assertThat("V4 parsing failed", ParseUtil.parseUtAddrV6toIP(v4), is(v4test));
        assertThrows(IllegalArgumentException.class, () -> {
            ParseUtil.parseUtAddrV6toIP(invalid);
        });
    }

    @Test
    void testParseIPv6BytesToIntArray() {
        // WTS_CLIENT_ADDRESS Address member: 2 bytes of padding, then 16 bytes of big-endian IPv6, then trailing bytes
        byte[] address = new byte[20];
        byte[] v6Bytes = { 0x20, 0x01, 0x0d, (byte) 0xb8, (byte) 0x85, (byte) 0xa3, 0, 0, 0, 0, (byte) 0x8a, 0x2e, 0x03,
                0x70, 0x73, 0x34 };
        System.arraycopy(v6Bytes, 0, address, 2, 16);
        int[] parsed = ParseUtil.parseIPv6BytesToIntArray(address);
        assertThat("Should decode to 4 ints", parsed.length, is(4));
        assertThat("Round trip through parseUtAddrV6toIP failed", ParseUtil.parseUtAddrV6toIP(parsed),
                is("2001:db8:85a3::8a2e:370:7334"));
        // All zeros
        assertThat("Zero address failed", ParseUtil.parseUtAddrV6toIP(ParseUtil.parseIPv6BytesToIntArray(new byte[20])),
                is("::"));
        // Exactly 18 bytes is sufficient
        assertThat("18-byte array should parse", ParseUtil.parseIPv6BytesToIntArray(new byte[18]).length, is(4));
        // Too short, and null
        assertThrows(IllegalArgumentException.class, () -> ParseUtil.parseIPv6BytesToIntArray(new byte[17]));
        assertThrows(IllegalArgumentException.class, () -> ParseUtil.parseIPv6BytesToIntArray(null));
    }

    @Test
    void testHexStringToInt() {
        assertThat("Parsing ff failed", ParseUtil.hexStringToInt("ff", 0), is(255));
        assertThat("Parsing 830f53a0 failed", ParseUtil.hexStringToInt("830f53a0", 0), is(-2096147552));
        assertThat("Parsing pqwe failed", ParseUtil.hexStringToInt("pqwe", 0), is(0));
        assertThat("Parsing 0xff failed", ParseUtil.hexStringToInt("0xff", 0), is(255));
        assertThat("Parsing 0x830f53a0 failed", ParseUtil.hexStringToInt("0x830f53a0", 0), is(-2096147552));
        assertThat("Parsing 0xpqwe failed", ParseUtil.hexStringToInt("0xpqwe", 0), is(0));
    }

    @Test
    void testHexStringToLong() {
        assertThat("Parsing ff failed", ParseUtil.hexStringToLong("ff", 0L), is(255L));
        assertThat("Parsing 830f53a0 failed", ParseUtil.hexStringToLong("ffffffff830f53a0", 0L), is(-2096147552L));
        assertThat("Parsing pqwe failed", ParseUtil.hexStringToLong("pqwe", 0L), is(0L));
        assertThat("Parsing 0xff failed", ParseUtil.hexStringToLong("0xff", 0L), is(255L));
        assertThat("Parsing 0x830f53a0 failed", ParseUtil.hexStringToLong("0xffffffff830f53a0", 0L), is(-2096147552L));
        assertThat("Parsing 0xpqwe failed", ParseUtil.hexStringToLong("0xpqwe", 0L), is(0L));
    }

    @Test
    void testRemoveLeadingDots() {
        assertThat(ParseUtil.removeLeadingDots("foo"), is("foo"));
        assertThat(ParseUtil.removeLeadingDots("...bar"), is("bar"));
        assertThat(ParseUtil.removeLeadingDots("..."), is(""));
    }

    @Test
    void testGetTextAfterString() {
        assertThat(ParseUtil.getTextAfterString("Serial Number: ABC123", "Serial Number:"), is(" ABC123"));
        // Marker at the start, as the startsWith-guarded call sites have it
        assertThat(ParseUtil.getTextAfterString("modelname       IBM,9114-275", "modelname"),
                is("       IBM,9114-275"));
        // Marker absent, at the very end, and repeated
        assertThat(ParseUtil.getTextAfterString("no marker here", "uuid:"), is(""));
        assertThat(ParseUtil.getTextAfterString("label:", "label:"), is(""));
        assertThat(ParseUtil.getTextAfterString("a: b: c", "a: "), is("b: c"));
        // Regex metacharacters in the marker are matched literally, unlike split()
        assertThat(ParseUtil.getTextAfterString("Rev. 2.1 build", "Rev. "), is("2.1 build"));
        assertThat(ParseUtil.getTextAfterString("x(1)y", "(1)"), is("y"));
    }

    @Test
    void testTrimLeadingWhitespace() {
        assertThat(ParseUtil.trimLeadingWhitespace("no leading space"), is("no leading space"));
        assertThat(ParseUtil.trimLeadingWhitespace("   Node 0x1"), is("Node 0x1"));
        assertThat(ParseUtil.trimLeadingWhitespace("\t \tgateway: 10.0.0.1"), is("gateway: 10.0.0.1"));
        // Trailing whitespace is left alone
        assertThat(ParseUtil.trimLeadingWhitespace("  value  "), is("value  "));
        assertThat(ParseUtil.trimLeadingWhitespace("   "), is(""));
        assertThat(ParseUtil.trimLeadingWhitespace(""), is(""));
    }

    @Test
    void testParseMultipliedToLongs() {
        assertThat(ParseUtil.parseMultipliedToLongs("Not a number"), is(0L));
        assertThat(ParseUtil.parseMultipliedToLongs("1"), is(1L));
        assertThat(ParseUtil.parseMultipliedToLongs("1.2"), is(1L));
        assertThat(ParseUtil.parseMultipliedToLongs("1 k"), is(1_000L));
        assertThat(ParseUtil.parseMultipliedToLongs("1 M"), is(1_000_000L));
        assertThat(ParseUtil.parseMultipliedToLongs("1MB"), is(1_000_000L));
        assertThat(ParseUtil.parseMultipliedToLongs("1MC"), is(1_000_000L));
        assertThat(ParseUtil.parseMultipliedToLongs("1 T"), is(1_000_000_000_000L));
        assertThat(ParseUtil.parseMultipliedToLongs("1073M"), is(1073000000L));
        assertThat(ParseUtil.parseMultipliedToLongs("1073 G"), is(1073000000000L));
        assertThat(ParseUtil.parseMultipliedToLongs("12K"), is(12000L));
        // T multiplier without space
        assertThat(ParseUtil.parseMultipliedToLongs("2T"), is(2_000_000_000_000L));
        // k (lowercase) multiplier
        assertThat(ParseUtil.parseMultipliedToLongs("5k"), is(5_000L));
        assertThat(ParseUtil.parseMultipliedToLongs("5 k"), is(5_000L));
    }

    @Test
    void parseByteArrayToStrings() {
        byte[] bytes = "foo bar".getBytes(StandardCharsets.US_ASCII);
        bytes[3] = 0;
        List<String> list = ParseUtil.parseByteArrayToStrings(bytes);
        assertThat(list, contains("foo", "bar"));

        bytes[4] = 0;
        list = ParseUtil.parseByteArrayToStrings(bytes);
        assertThat(list, contains("foo"));

        bytes[0] = 0;
        list = ParseUtil.parseByteArrayToStrings(bytes);
        assertThat(list, empty());

        bytes = new byte[0];
        list = ParseUtil.parseByteArrayToStrings(bytes);
        assertThat(list, empty());
    }

    @Test
    void parseByteArrayToStringMap() {
        byte[] bytes = "foo=1 bar=2".getBytes(StandardCharsets.US_ASCII);
        bytes[5] = 0;
        Map<String, String> map = ParseUtil.parseByteArrayToStringMap(bytes);
        assertThat(map.keySet(), containsInAnyOrder("foo", "bar"));
        assertThat(map.values(), containsInAnyOrder("1", "2"));
        assertThat(map.get("foo"), is("1"));
        assertThat(map.get("bar"), is("2"));

        bytes[10] = 0;
        map = ParseUtil.parseByteArrayToStringMap(bytes);
        assertThat(map.keySet(), containsInAnyOrder("foo", "bar"));
        assertThat(map.values(), containsInAnyOrder("1", ""));
        assertThat(map.get("foo"), is("1"));
        assertThat(map.get("bar"), is(""));

        bytes = "foo=1 bar=2".getBytes(StandardCharsets.US_ASCII);
        bytes[5] = 0;
        bytes[6] = 0;
        map = ParseUtil.parseByteArrayToStringMap(bytes);
        assertThat(map.keySet(), contains("foo"));
        assertThat(map.values(), contains("1"));
        assertThat(map.get("foo"), is("1"));

        bytes[0] = 0;
        map = ParseUtil.parseByteArrayToStringMap(bytes);
        assertThat(map, anEmptyMap());

        bytes = new byte[0];
        map = ParseUtil.parseByteArrayToStringMap(bytes);
        assertThat(map, anEmptyMap());
    }

    @Test
    void parseCharArrayToStringMap() {
        char[] chars = "foo=1 bar=2".toCharArray();
        chars[5] = 0;
        Map<String, String> map = ParseUtil.parseCharArrayToStringMap(chars);
        assertThat(map.keySet(), containsInAnyOrder("foo", "bar"));
        assertThat(map.values(), containsInAnyOrder("1", "2"));
        assertThat(map.get("foo"), is("1"));
        assertThat(map.get("bar"), is("2"));

        chars[10] = 0;
        map = ParseUtil.parseCharArrayToStringMap(chars);
        assertThat(map.keySet(), containsInAnyOrder("foo", "bar"));
        assertThat(map.values(), containsInAnyOrder("1", ""));
        assertThat(map.get("foo"), is("1"));
        assertThat(map.get("bar"), is(""));

        chars = "foo=1 bar=2".toCharArray();
        chars[5] = 0;
        chars[6] = 0;
        map = ParseUtil.parseCharArrayToStringMap(chars);
        assertThat(map.keySet(), contains("foo"));
        assertThat(map.values(), contains("1"));
        assertThat(map.get("foo"), is("1"));

        chars[0] = 0;
        map = ParseUtil.parseCharArrayToStringMap(chars);
        assertThat(map, anEmptyMap());

        chars = new char[0];
        map = ParseUtil.parseCharArrayToStringMap(chars);
        assertThat(map, anEmptyMap());
    }

    @Test
    void teststringToEnumMap() {
        String two = "one,two";
        Map<TestEnum, String> map = ParseUtil.stringToEnumMap(TestEnum.class, two, ',');
        assertThat(map.get(TestEnum.FOO), is("one"));
        assertThat(map.get(TestEnum.BAR), is("two"));
        assertThat(map.containsKey(TestEnum.BAZ), is(false));

        String three = "one,,two,three";
        map = ParseUtil.stringToEnumMap(TestEnum.class, three, ',');
        assertThat(map.get(TestEnum.FOO), is("one"));
        assertThat(map.get(TestEnum.BAR), is("two"));
        assertThat(map.get(TestEnum.BAZ), is("three"));

        String four = "one,two,three,four";
        map = ParseUtil.stringToEnumMap(TestEnum.class, four, ',');
        assertThat(map.get(TestEnum.FOO), is("one"));
        assertThat(map.get(TestEnum.BAR), is("two"));
        assertThat(map.get(TestEnum.BAZ), is("three,four"));

        String empty = "";
        map = ParseUtil.stringToEnumMap(TestEnum.class, empty, ',');
        assertThat(map.get(TestEnum.FOO), is(""));
    }

    @Test
    void teststringToEnumMapWithKeys() {
        // Explicit key order independent of ordinal order; last key slurps the remainder
        List<TestEnum> keys = List.of(TestEnum.BAZ, TestEnum.FOO);
        Map<TestEnum, String> map = ParseUtil.stringToEnumMap(TestEnum.class, keys, "one two,three four", ' ');
        assertThat(map.get(TestEnum.BAZ), is("one"));
        assertThat(map.get(TestEnum.FOO), is("two,three four"));
        assertThat(map.containsKey(TestEnum.BAR), is(false));

        // Fewer values than keys: later keys are not mapped
        map = ParseUtil.stringToEnumMap(TestEnum.class, keys, "only", ' ');
        assertThat(map.get(TestEnum.BAZ), is("only"));
        assertThat(map.containsKey(TestEnum.FOO), is(false));

        // Consecutive delimiters are treated as one
        List<TestEnum> three = List.of(TestEnum.FOO, TestEnum.BAR, TestEnum.BAZ);
        map = ParseUtil.stringToEnumMap(TestEnum.class, three, "one,,two,three", ',');
        assertThat(map.get(TestEnum.FOO), is("one"));
        assertThat(map.get(TestEnum.BAR), is("two"));
        assertThat(map.get(TestEnum.BAZ), is("three"));
    }

    @Test
    void testgetValueOrUnknown() {
        String key = "key";
        Map<String, String> map = new HashMap<>();
        assertThat(ParseUtil.getValueOrUnknown(map, key), is(Constants.UNKNOWN));

        map.put("key", "value");
        assertThat(ParseUtil.getValueOrUnknown(map, key), is("value"));
    }

    @Test
    void testGetValueOrUnknownWithObjectKey() {
        Object key = 1;
        Map<Object, String> map = new HashMap<>();
        assertThat(ParseUtil.getValueOrUnknown(map, key), is(Constants.UNKNOWN));

        map.put(key, "value");
        assertThat(ParseUtil.getValueOrUnknown(map, key), is("value"));
    }

    @Test
    void testGetStringValueOrUnknown() {
        assertThat("null should be unknown", ParseUtil.getStringValueOrUnknown(null), is(Constants.UNKNOWN));
        assertThat("empty should be unknown", ParseUtil.getStringValueOrUnknown(""), is(Constants.UNKNOWN));
        assertThat("whitespace should be unchanged", ParseUtil.getStringValueOrUnknown(" "), is(" "));
        assertThat("value should be unchanged", ParseUtil.getStringValueOrUnknown("value"), is("value"));
    }

    @Test
    void testGetStringValueOrEmpty() {
        assertThat("null should be empty", ParseUtil.getStringValueOrEmpty(null), is(""));
        assertThat("empty should be empty", ParseUtil.getStringValueOrEmpty(""), is(""));
        assertThat("whitespace should be unchanged", ParseUtil.getStringValueOrEmpty(" "), is(" "));
        assertThat("value should be unchanged", ParseUtil.getStringValueOrEmpty("value"), is("value"));
        assertThat("unknown should be unchanged", ParseUtil.getStringValueOrEmpty(Constants.UNKNOWN),
                is(Constants.UNKNOWN));
    }

    @Test
    void testParseDateToEpoch() {
        assertThat("Parse yyyyMMdd", ParseUtil.parseDateToEpoch("20240101", "yyyyMMdd"), is(
                LocalDate.of(2024, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()));

        assertThat("Parse dd/MM/yy, HH:mm", ParseUtil.parseDateToEpoch("01/01/24, 12:30", "dd/MM/yy, HH:mm"),
                is(LocalDateTime.of(2024, Month.JANUARY, 1, 12, 30).atZone(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli()));

        assertThat("Parse YYYY-'W'ww-e ISO week date", ParseUtil.parseDateToEpoch("2025-W25-2", "YYYY-'W'ww-e"),
                is(LocalDate.of(2025, Month.JUNE, 16).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()));

        assertThat("Parse EEE MMM dd HH:mm:ss yyyy",
                ParseUtil.parseDateToEpoch("Fri Sep 18 15:53:11 2020", "EEE MMM dd HH:mm:ss yyyy"),
                is(LocalDateTime.of(2020, Month.SEPTEMBER, 18, 15, 53, 11).atZone(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli()));

        assertThat("Parse empty date string", ParseUtil.parseDateToEpoch("", "yyyyMMdd"), is(0L));

        assertThat("Parse empty pattern", ParseUtil.parseDateToEpoch("20240101", ""), is(0L));

        assertThat("Parse UNKNOWN constant", ParseUtil.parseDateToEpoch(Constants.UNKNOWN, "yyyyMMdd"), is(0L));

        assertThat("Parse invalid date format", ParseUtil.parseDateToEpoch("invalid-date", "yyyyMMdd"), is(0L));
    }

    @Test
    void testParseYearlessDateToEpoch() {
        LocalDateTime now = LocalDateTime.of(2026, Month.AUGUST, 2, 16, 3);

        // Earlier in the same year, so the current year applies
        assertThat("Parse MMM d HH:mm earlier this year",
                ParseUtil.parseYearlessDateToEpoch("Feb 13 23:31", "MMM d HH:mm", now),
                is(LocalDateTime.of(2026, Month.FEBRUARY, 13, 23, 31).atZone(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli()));

        // Later in the year than now, so it cannot be this year and must be last year's
        assertThat("Parse MMM d HH:mm later in the year",
                ParseUtil.parseYearlessDateToEpoch("Nov 30 04:05", "MMM d HH:mm", now), is(LocalDateTime
                        .of(2025, Month.NOVEMBER, 30, 4, 5).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));

        // Fields the pattern does not supply default to zero rather than failing
        assertThat("Parse MMM d with no time", ParseUtil.parseYearlessDateToEpoch("Mar 9", "MMM d", now),
                is(LocalDate.of(2026, Month.MARCH, 9).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()));

        assertThat("Parse empty date string", ParseUtil.parseYearlessDateToEpoch("", "MMM d HH:mm", now), is(0L));

        // A leap day does not exist in a non-leap current year, so defaulting the year fails outright rather than
        // landing in the future. 2025 is not a leap year and 2024 is, so it resolves against the previous year.
        assertThat("Parse Feb 29 in a non-leap current year",
                ParseUtil.parseYearlessDateToEpoch("Feb 29 12:00", "MMM d HH:mm",
                        LocalDateTime.of(2025, Month.JUNE, 15, 10, 0)),
                is(LocalDateTime.of(2024, Month.FEBRUARY, 29, 12, 0).atZone(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli()));

        // Neither 2027 nor 2026 is a leap year, so the timestamp cannot be resolved at all
        assertThat("Parse Feb 29 when neither candidate year is a leap year", ParseUtil.parseYearlessDateToEpoch(
                "Feb 29 12:00", "MMM d HH:mm", LocalDateTime.of(2027, Month.JUNE, 15, 10, 0)), is(0L));

        // A real leap day earlier in a leap year keeps its date rather than being shifted to the 28th
        assertThat("Parse Feb 29 in a leap current year",
                ParseUtil.parseYearlessDateToEpoch("Feb 29 12:00", "MMM d HH:mm",
                        LocalDateTime.of(2024, Month.JUNE, 15, 10, 0)),
                is(LocalDateTime.of(2024, Month.FEBRUARY, 29, 12, 0).atZone(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli()));
        // April has 30 days in every year, so neither candidate can resolve it. The default resolver style would
        // have normalized this to April 30 instead of rejecting it.
        assertThat("Parse a day-of-month that exists in no year",
                ParseUtil.parseYearlessDateToEpoch("Apr 31 12:00", "MMM d HH:mm", now), is(0L));

        assertThat("Parse empty pattern", ParseUtil.parseYearlessDateToEpoch("Feb 13 23:31", "", now), is(0L));
        assertThat("Parse null date string", ParseUtil.parseYearlessDateToEpoch(null, "MMM d HH:mm", now), is(0L));
        assertThat("Parse unmatched format", ParseUtil.parseYearlessDateToEpoch("not a date", "MMM d HH:mm", now),
                is(0L));
    }

    @Test
    void testDecodeIntOrDefault() {
        assertThat(ParseUtil.decodeIntOrDefault("0x1A", -1), is(26));
        assertThat(ParseUtil.decodeIntOrDefault("26", -1), is(26));
        assertThat(ParseUtil.decodeIntOrDefault("032", -1), is(26));
        assertThat(ParseUtil.decodeIntOrDefault(null, -1), is(-1));
        assertThat(ParseUtil.decodeIntOrDefault("notanumber", -1), is(-1));
    }

    @Test
    void testDecodeLongOrDefault() {
        assertThat(ParseUtil.decodeLongOrDefault("0x1A", -1L), is(26L));
        assertThat(ParseUtil.decodeLongOrDefault("26", -1L), is(26L));
        assertThat(ParseUtil.decodeLongOrDefault("032", -1L), is(26L));
        assertThat(ParseUtil.decodeLongOrDefault(null, -1L), is(-1L));
        assertThat(ParseUtil.decodeLongOrDefault("notanumber", -1L), is(-1L));
        assertThat(ParseUtil.decodeLongOrDefault("0x7FFFFFFFFFFFFFFF", -1L), is(Long.MAX_VALUE));
    }

    @Test
    void testDecodeNulTerminated() {
        // Single NUL-terminated string
        assertThat(
                ParseUtil.decodeNulTerminated("Apple Inc.\0".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
                is("Apple Inc."));
        // NUL-padded
        assertThat(ParseUtil.decodeNulTerminated("Apple Inc.\0\0\0".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8), is("Apple Inc."));
        // Multi-string (IORegistry "compatible" style): stops at first NUL
        assertThat(ParseUtil.decodeNulTerminated("apple,t8103\0arm,v8\0".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8), is("apple,t8103"));
        // No NUL — decodes entire array
        assertThat(ParseUtil.decodeNulTerminated("hello".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
                is("hello"));
        // Leading NUL → empty
        assertThat(ParseUtil.decodeNulTerminated("\0trailing".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
                is(""));
        // Null and empty arrays
        assertThat(ParseUtil.decodeNulTerminated(null, StandardCharsets.UTF_8), is(""));
        assertThat(ParseUtil.decodeNulTerminated(new byte[0], StandardCharsets.UTF_8), is(""));
        // US_ASCII variant
        assertThat(ParseUtil.decodeNulTerminated("test\0data".getBytes(StandardCharsets.US_ASCII),
                StandardCharsets.US_ASCII), is("test"));
    }

    /**
     * Test parseIpv4AddressToBytes
     */
    @Test
    void testParseIpv4AddressToBytes() {
        assertThat(ParseUtil.parseIpv4AddressToBytes("10.0.0.1"), is(new byte[] { 10, 0, 0, 1 }));
        assertThat(ParseUtil.parseIpv4AddressToBytes("0.0.0.0"), is(new byte[] { 0, 0, 0, 0 }));
        assertThat(ParseUtil.parseIpv4AddressToBytes("255.255.255.255"), is(new byte[] { -1, -1, -1, -1 }));
        // Abbreviated networks, as UNIX routing tables print them
        assertThat(ParseUtil.parseIpv4AddressToBytes("10"), is(new byte[] { 10, 0, 0, 0 }));
        assertThat(ParseUtil.parseIpv4AddressToBytes("127"), is(new byte[] { 127, 0, 0, 0 }));
        assertThat(ParseUtil.parseIpv4AddressToBytes("10.1"), is(new byte[] { 10, 1, 0, 0 }));
        assertThat(ParseUtil.parseIpv4AddressToBytes("140.211.9"), is(new byte[] { -116, -45, 9, 0 }));
        // Not addresses
        assertThat(ParseUtil.parseIpv4AddressToBytes(""), is(new byte[0]));
        assertThat(ParseUtil.parseIpv4AddressToBytes("link#14"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv4AddressToBytes("Destination"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv4AddressToBytes("--------------------"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv4AddressToBytes("10.0.0.256"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv4AddressToBytes("1.2.3.4.5"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv4AddressToBytes("10."), is(new byte[0]));
        assertThat(ParseUtil.parseIpv4AddressToBytes("10..1"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv4AddressToBytes("-1.2.3.4"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv4AddressToBytes("0:11:32:c5:e:9b"), is(new byte[0]));
    }

    /**
     * Test parseIpv6AddressToBytes
     */
    @Test
    void testParseIpv6AddressToBytes() {
        assertThat(ParseUtil.parseIpv6AddressToBytes("::"), is(new byte[16]));
        assertThat(ParseUtil.parseIpv6AddressToBytes("::1"),
                is(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }));
        assertThat(ParseUtil.parseIpv6AddressToBytes("fe80::"),
                is(new byte[] { -2, -128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }));
        assertThat(ParseUtil.parseIpv6AddressToBytes("fe80::420f:c1ff:fecb:2a97"),
                is(new byte[] { -2, -128, 0, 0, 0, 0, 0, 0, 66, 15, -63, -1, -2, -53, 42, -105 }));
        // Uncompressed, all eight groups present
        assertThat(ParseUtil.parseIpv6AddressToBytes("2601:601:d47c:3090:211:32ff:fec5:e9b"),
                is(new byte[] { 38, 1, 6, 1, -44, 124, 48, -112, 2, 17, 50, -1, -2, -59, 14, -101 }));
        // A trailing dotted quad stays sixteen bytes rather than collapsing to an IPv4 address
        assertThat(ParseUtil.parseIpv6AddressToBytes("::ffff:1.2.3.4"),
                is(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 1, 2, 3, 4 }));
        // Zone suffixes are stripped, numeric as AIX prints them and named as macOS does
        assertThat(ParseUtil.parseIpv6AddressToBytes("::1%1"), is(ParseUtil.parseIpv6AddressToBytes("::1")));
        assertThat(ParseUtil.parseIpv6AddressToBytes("fe80::%utun0"), is(ParseUtil.parseIpv6AddressToBytes("fe80::")));
        assertThat(ParseUtil.parseIpv6AddressToBytes("fe80::420f:c1ff:fecb:2a97%en0"),
                is(ParseUtil.parseIpv6AddressToBytes("fe80::420f:c1ff:fecb:2a97")));
        // Unpadded MAC addresses from a macOS gateway column must not parse as IPv6
        assertThat(ParseUtil.parseIpv6AddressToBytes("0:11:32:c5:e:9b"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv6AddressToBytes("40:f:c1:cb:2a:97"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv6AddressToBytes("d4:80:8b:1e:6b:b9"), is(new byte[0]));
        // Other non-addresses
        assertThat(ParseUtil.parseIpv6AddressToBytes(""), is(new byte[0]));
        assertThat(ParseUtil.parseIpv6AddressToBytes("link#14"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv6AddressToBytes("10.0.0.1"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv6AddressToBytes("1:2:3:4:5:6:7:8:9"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv6AddressToBytes("1::2::3"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv6AddressToBytes("fe80:::1"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv6AddressToBytes("fffff::1"), is(new byte[0]));
        assertThat(ParseUtil.parseIpv6AddressToBytes("::ffff:1.2.3"), is(new byte[0]));
        // A trailing separator leaves an empty group
        assertThat(ParseUtil.parseIpv6AddressToBytes("1:2:3:4:5:6:7:"), is(new byte[0]));
        // A non-hex character in a group
        assertThat(ParseUtil.parseIpv6AddressToBytes("fe80::zzzz"), is(new byte[0]));
        // The "::" must stand for at least one all-zero group, so eight explicit groups around it do not fit
        assertThat(ParseUtil.parseIpv6AddressToBytes("1:2:3:4:5:6:7::8"), is(new byte[0]));
    }

    /**
     * Test parseRouteDestination
     */
    @Test
    void testParseRouteDestination() {
        // The literal default, whose family the caller supplies
        assertThat(ParseUtil.parseRouteDestination("default", false).getA(), is(new byte[] { 0, 0, 0, 0 }));
        assertThat(ParseUtil.parseRouteDestination("default", false).getB(), is(0));
        assertThat(ParseUtil.parseRouteDestination("default", true).getA(), is(new byte[16]));
        assertThat(ParseUtil.parseRouteDestination("default", true).getB(), is(0));
        // Abbreviated network with an explicit prefix, as macOS and AIX print
        assertThat(ParseUtil.parseRouteDestination("10/24", false).getA(), is(new byte[] { 10, 0, 0, 0 }));
        assertThat(ParseUtil.parseRouteDestination("10/24", false).getB(), is(24));
        assertThat(ParseUtil.parseRouteDestination("127/8", false).getA(), is(new byte[] { 127, 0, 0, 0 }));
        assertThat(ParseUtil.parseRouteDestination("127/8", false).getB(), is(8));
        assertThat(ParseUtil.parseRouteDestination("10.1/23", false).getA(), is(new byte[] { 10, 1, 0, 0 }));
        assertThat(ParseUtil.parseRouteDestination("10.1/23", false).getB(), is(23));
        assertThat(ParseUtil.parseRouteDestination("140.211.9/24", false).getA(), is(new byte[] { -116, -45, 9, 0 }));
        assertThat(ParseUtil.parseRouteDestination("140.211.9/24", false).getB(), is(24));
        // An abbreviated network with no explicit prefix states it through the octet count, as DragonFly BSD prints
        assertThat(ParseUtil.parseRouteDestination("192.168.122", false).getA(), is(new byte[] { -64, -88, 122, 0 }));
        assertThat(ParseUtil.parseRouteDestination("192.168.122", false).getB(), is(24));
        assertThat(ParseUtil.parseRouteDestination("10.1", false).getB(), is(16));
        assertThat(ParseUtil.parseRouteDestination("10", false).getB(), is(8));
        // A bare four-octet address states no prefix; the caller decides from the flags or a netmask column. Solaris
        // prints whole network addresses this way and supplies the mask separately, so inferring /32 would be wrong.
        assertThat(ParseUtil.parseRouteDestination("10.0.0.1", false).getA(), is(new byte[] { 10, 0, 0, 1 }));
        assertThat(ParseUtil.parseRouteDestination("10.0.0.1", false).getB(), is(-1));
        assertThat(ParseUtil.parseRouteDestination("129.70.163.176", false).getB(), is(-1));
        assertThat(ParseUtil.parseRouteDestination("::1%1", true).getB(), is(-1));
        // IPv6 with an inline prefix
        assertThat(ParseUtil.parseRouteDestination("fe80::/10", true).getB(), is(10));
        assertThat(ParseUtil.parseRouteDestination("2601:601:d47c:3090::/64", true).getB(), is(64));
        // A prefix wider than the address is clamped
        assertThat(ParseUtil.parseRouteDestination("10.0.0.1/99", false).getB(), is(32));
    }

    /**
     * Test parseRouteDestination keeping a stated but unparseable prefix unknown rather than inferring one
     */
    @Test
    void testParseRouteDestinationMalformedPrefix() {
        assertThat(ParseUtil.parseRouteDestination("10/foo", false).getA(), is(new byte[] { 10, 0, 0, 0 }));
        assertThat(ParseUtil.parseRouteDestination("10/foo", false).getB(), is(-1));
        assertThat(ParseUtil.parseRouteDestination("10/-1", false).getB(), is(-1));
        assertThat(ParseUtil.parseRouteDestination("192.168.122/", false).getB(), is(-1));
    }

    /**
     * Test parseRouteDestination rejecting the header, banner and separator lines it is used to skip
     */
    @Test
    void testParseRouteDestinationRejectsNonAddresses() {
        assertThat(ParseUtil.parseRouteDestination("Destination", false).getA(), is(new byte[0]));
        assertThat(ParseUtil.parseRouteDestination("Destination", false).getB(), is(-1));
        assertThat(ParseUtil.parseRouteDestination("Destination/Mask", true).getA(), is(new byte[0]));
        assertThat(ParseUtil.parseRouteDestination("--------------------", false).getA(), is(new byte[0]));
        assertThat(ParseUtil.parseRouteDestination("Routing", false).getA(), is(new byte[0]));
        assertThat(ParseUtil.parseRouteDestination("IRE", false).getA(), is(new byte[0]));
        assertThat(ParseUtil.parseRouteDestination("Table:", false).getA(), is(new byte[0]));
        assertThat(ParseUtil.parseRouteDestination("", false).getA(), is(new byte[0]));
    }

    /**
     * Test netmaskToPrefixLength
     */
    @Test
    void testNetmaskToPrefixLength() {
        assertThat(ParseUtil.netmaskToPrefixLength("255.255.255.255"), is(32));
        assertThat(ParseUtil.netmaskToPrefixLength("255.255.255.248"), is(29));
        assertThat(ParseUtil.netmaskToPrefixLength("255.255.255.0"), is(24));
        assertThat(ParseUtil.netmaskToPrefixLength("255.255.254.0"), is(23));
        assertThat(ParseUtil.netmaskToPrefixLength("255.0.0.0"), is(8));
        assertThat(ParseUtil.netmaskToPrefixLength("0.0.0.0"), is(0));
        // A discontiguous mask is not a prefix
        assertThat(ParseUtil.netmaskToPrefixLength("255.0.255.0"), is(-1));
        assertThat(ParseUtil.netmaskToPrefixLength("255.255.255.1"), is(-1));
        // An IPv6 mask in text form takes the same path
        assertThat(ParseUtil.netmaskToPrefixLength("ffff:ffff::"), is(32));
        assertThat(ParseUtil.netmaskToPrefixLength("ffff:ffff:8000::"), is(33));
        assertThat(ParseUtil.netmaskToPrefixLength("::"), is(0));
        // Not a mask at all
        assertThat(ParseUtil.netmaskToPrefixLength(""), is(-1));
        assertThat(ParseUtil.netmaskToPrefixLength("Mask"), is(-1));
        assertThat(ParseUtil.netmaskToPrefixLength(new byte[0]), is(-1));
        // Byte form, as Linux /proc/net/route supplies after hex decoding
        assertThat(ParseUtil.netmaskToPrefixLength(new byte[] { -1, -1, -1, 0 }), is(24));
        assertThat(ParseUtil.netmaskToPrefixLength(new byte[16]), is(0));
    }

    /**
     * Test isRouteFlags
     */
    @Test
    void testIsRouteFlags() {
        // Real flag fields from macOS, AIX and Solaris output
        assertThat(ParseUtil.isRouteFlags("U"), is(true));
        assertThat(ParseUtil.isRouteFlags("UG"), is(true));
        assertThat(ParseUtil.isRouteFlags("UH"), is(true));
        assertThat(ParseUtil.isRouteFlags("UHSb"), is(true));
        assertThat(ParseUtil.isRouteFlags("UGHS"), is(true));
        assertThat(ParseUtil.isRouteFlags("UGScg"), is(true));
        assertThat(ParseUtil.isRouteFlags("UHLWIir"), is(true));
        // Columns a right-to-left scan passes on its way to the flags
        assertThat(ParseUtil.isRouteFlags("0"), is(false));
        assertThat(ParseUtil.isRouteFlags("1500"), is(false));
        assertThat(ParseUtil.isRouteFlags("5666596"), is(false));
        assertThat(ParseUtil.isRouteFlags("lo0"), is(false));
        assertThat(ParseUtil.isRouteFlags("net0"), is(false));
        assertThat(ParseUtil.isRouteFlags("en1"), is(false));
        // Trailing decoration and separators
        assertThat(ParseUtil.isRouteFlags(""), is(false));
        assertThat(ParseUtil.isRouteFlags("-"), is(false));
        assertThat(ParseUtil.isRouteFlags("=>"), is(false));
        assertThat(ParseUtil.isRouteFlags("!"), is(false));
        assertThat(ParseUtil.isRouteFlags("129.70.163.177"), is(false));
        assertThat(ParseUtil.isRouteFlags("abcdefghijklm"), is(false));
    }
}
