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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
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
    }

    /**
     * Test parse long
     */
    @Test
    void testParseLongOrDefault() {
        assertThat("parse 123", ParseUtil.parseLongOrDefault("123", 45L), is(123L));
        assertThat("parse 45", ParseUtil.parseLongOrDefault("123L", 45L), is(45L));
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
    }

    /**
     * Test parse double
     */
    @Test
    void testParseDoubleOrDefault() {
        assertThat("parse 1.23d", ParseUtil.parseDoubleOrDefault("1.23", 4.5d), is(closeTo(1.23d, EPSILON)));
        assertThat("parse 4.5d", ParseUtil.parseDoubleOrDefault("one.twentythree", 4.5d), is(closeTo(4.5d, EPSILON)));
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

        String foo = String.format("The numbers are %d %d %d %d", 123, 456, 789, now);
        int count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 4 for \"" + foo + "\"", count, is(4));
        long[] result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertThat("result[0] should be 456 using parseStringToLongArray on \"" + foo + "\"", result[0], is(456L));
        assertThat("result[1] should be " + now + " using parseStringToLongArray on \"" + foo + "\"", result[1],
                is(now));

        foo = String.format("The numbers are %d %d %d %d %s", 123, 456, 789, now,
                "709af748-5f8e-41b3-b73a-b440ef4406c8");
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 4 for \"" + foo + "\"", count, is(4));
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertThat("result[0] should be 456 using parseStringToLongArray on \"" + foo + "\"", result[0], is(456L));
        assertThat("result[1] should be " + now + " using parseStringToLongArray on \"" + foo + "\"", result[1],
                is(now));

        foo = String.format("The numbers are %d -%d %d +%d", 123, 456, 789, now);
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 4 for \"" + foo + "\"", count, is(4));
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertThat("result[0] should be -4456 using parseStringToLongArray on \"" + foo + "\"", result[0], is(-456L));
        assertThat("result[1] index should be 456 using parseStringToLongArray on \"" + foo + "\"", result[1], is(now));

        foo = String.format("NOLOG: Invalid character %d %s %d %d", 123, "4v6", 789, now);
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 2 for \"" + foo + "\"", count, is(2));
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertThat("result[1] index should be 0 using parseStringToLongArray on \"" + foo + "\"", result[1], is(0L));

        foo = String.format("Exceeds max long %d %d %d 1%d", 123, 456, 789, Long.MAX_VALUE);
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertThat("result[1] index should be " + Long.MAX_VALUE
                + " (Long.MAX_VALUE) using parseStringToLongArray on \"" + foo + "\"", result[1], is(Long.MAX_VALUE));

        foo = String.format("NOLOG: String too short %d %d %d %d", 123, 456, 789, now);
        result = ParseUtil.parseStringToLongArray(foo, indices, 9, ' ');
        assertThat("result[1] index should be 0 using parseStringToLongArray on \"" + foo + "\"", result[1], is(0L));

        foo = String.format("NOLOG: Array too short %d %d %d %d", 123, 456, 789, now);
        result = ParseUtil.parseStringToLongArray(foo, indices, 2, ' ');
        assertThat("result[1] index should be 0 using parseStringToLongArray on \"" + foo + "\"", result[1], is(0L));

        foo = String.format("%d %d %d %d", 123, 456, 789, now);
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 4 for \"" + foo + "\"", count, is(4));

        foo = String.format("%d %d %d %d nonNumeric", 123, 456, 789, now);
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 4 for \"" + foo + "\"", count, is(4));

        foo = String.format("%d %d %d %d 123-456", 123, 456, 789, now);
        count = ParseUtil.countStringToLongArray(foo, ' ');
        assertThat("countStringToLongArray should return 4 for \"" + foo + "\"", count, is(4));
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
    }

    @Test
    void testFiletimeToMs() {
        assertThat(ParseUtil.filetimeToUtcMs(128166372003061629L, false), is(1172163600306L));
    }

    @Test
    void testParseCimDateTimeToOffset() {
        String cimDateTime = "20160513072950.782000-420";
        // 2016-05-13T07:29:50 == 1463124590
        // Add 420 minutes to get unix seconds
        Instant timeInst = Instant.ofEpochMilli(1463124590_782L + 60 * 420_000L);
        assertThat(ParseUtil.parseCimDateTimeToOffset(cimDateTime).toInstant(), is(timeInst));
        assertThat(ParseUtil.parseCimDateTimeToOffset("Not a datetime").toInstant(), is(Instant.EPOCH));
    }

    @Test
    void testFilePathStartsWith() {
        List<String> prefixList = Arrays.asList("/foo", "/bar");
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
    }

    @Test
    void testParseDeviceIdToVendorProductSerial() {
        Triplet<String, String, String> idsAndSerial = ParseUtil
                .parseDeviceIdToVendorProductSerial("PCI\\VEN_10DE&DEV_134B&SUBSYS_00081414&REV_A2\\4&25BACB6&0&00E0");
        assertThat("VEN_ DEV_ deviceID failed to parse", idsAndSerial, is(notNullValue()));
        assertThat("Vendor ID failed to parse", idsAndSerial.getA(), is("0x10de"));
        assertThat("Product ID failed to parse", idsAndSerial.getB(), is("0x134b"));
        assertThat("SerialNumber should not have parsed", idsAndSerial.getC(), is(emptyString()));

        idsAndSerial = ParseUtil.parseDeviceIdToVendorProductSerial("USB\\VID_045E&PID_07C6\\000001000000");
        assertThat("VID_ PID_ serial deviceID failed to parse", idsAndSerial, is(notNullValue()));
        assertThat("Vendor ID failed to parse", idsAndSerial.getA(), is("0x045e"));
        assertThat("Product ID failed to parse", idsAndSerial.getB(), is("0x07c6"));
        assertThat("SerialNumber failed to parse", idsAndSerial.getC(), is("000001000000"));

        idsAndSerial = ParseUtil.parseDeviceIdToVendorProductSerial("USB\\VID_045E&PID_07C6\\5&000001000000");
        assertThat("VID_ PID_ nonserial deviceID failed to parse", idsAndSerial, is(notNullValue()));
        assertThat("Vendor ID failed to parse", idsAndSerial.getA(), is("0x045e"));
        assertThat("Product ID failed to parse", idsAndSerial.getB(), is("0x07c6"));
        assertThat("SerialNumber should not have parsed", idsAndSerial.getC(), is(emptyString()));

        idsAndSerial = ParseUtil
                .parseDeviceIdToVendorProductSerial("PCI\\VEN_80286&DEV_19116&SUBSYS_00141414&REV_07\\3&11583659&0&10");
        assertThat("Vender and Product IDs should not have parsed", idsAndSerial, is(nullValue()));
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
        v6[0] = Integer.parseUnsignedInt("20010db8", 16);
        v6[1] = Integer.parseUnsignedInt("85a30000", 16);
        v6[2] = Integer.parseUnsignedInt("00008a2e", 16);
        v6[3] = Integer.parseUnsignedInt("03707334", 16);
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
    }

    @Test
    void parseByteArrayToStrings() {
        byte[] bytes = "foo bar".getBytes();
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
        byte[] bytes = "foo=1 bar=2".getBytes();
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

        bytes = "foo=1 bar=2".getBytes();
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
}
