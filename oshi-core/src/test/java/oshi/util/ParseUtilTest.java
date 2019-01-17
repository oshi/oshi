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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/**
 * The Class ParseUtilTest.
 */
public class ParseUtilTest {
    /**
     * Test parse hertz.
     */
    @Test
    public void testParseHertz() {
        assertEquals(-1L, ParseUtil.parseHertz("OneHz"));
        assertEquals(-1L, ParseUtil.parseHertz("NotEvenAHertz"));
        assertEquals(Long.MAX_VALUE, ParseUtil.parseHertz("10000000000000000000 Hz"));
        assertEquals(1L, ParseUtil.parseHertz("1Hz"));
        assertEquals(500L, ParseUtil.parseHertz("500 Hz"));
        assertEquals(1_000L, ParseUtil.parseHertz("1kHz"));
        assertEquals(1_000_000L, ParseUtil.parseHertz("1MHz"));
        assertEquals(1_000_000_000L, ParseUtil.parseHertz("1GHz"));
        assertEquals(1_500_000_000L, ParseUtil.parseHertz("1.5GHz"));
        assertEquals(1_000_000_000_000L, ParseUtil.parseHertz("1THz"));
        // GHz exceeds max double
    }

    /**
     * Test parse string.
     */
    @Test
    public void testParseLastInt() {
        assertEquals(-1, ParseUtil.parseLastInt("foo : bar", -1));
        assertEquals(1, ParseUtil.parseLastInt("foo : 1", 0));
        assertEquals(2, ParseUtil.parseLastInt("foo", 2));
        assertEquals(3, ParseUtil.parseLastInt("max_int plus one is 2147483648", 3));

        assertEquals(-1L, ParseUtil.parseLastLong("foo : bar", -1L));
        assertEquals(1L, ParseUtil.parseLastLong("foo : 1", 0L));
        assertEquals(2L, ParseUtil.parseLastLong("foo", 2L));
        assertEquals(2147483648L, ParseUtil.parseLastLong("max_int plus one is 2147483648", 3L));
    }

    /**
     * Test parse string.
     */
    @Test
    public void testParseLastString() {
        assertEquals("bar", ParseUtil.parseLastString("foo : bar"));
        assertEquals("foo", ParseUtil.parseLastString("foo"));
        assertEquals("", ParseUtil.parseLastString(""));
    }

    /**
     * Test hex string to byte array (and back).
     */
    @Test
    public void testHexStringToByteArray() {
        byte[] temp = { (byte) 0x12, (byte) 0xaf };
        assertTrue(Arrays.equals(temp, ParseUtil.hexStringToByteArray("12af")));
        assertEquals("12AF", ParseUtil.byteArrayToHexString(temp));
        temp = new byte[0];
        assertTrue(Arrays.equals(temp, ParseUtil.hexStringToByteArray("expected error abcde")));
        assertTrue(Arrays.equals(temp, ParseUtil.hexStringToByteArray("abcde")));
    }

    /**
     * Test string to byte array.
     */
    @Test
    public void testStringToByteArray() {
        byte[] temp = { (byte) '1', (byte) '2', (byte) 'a', (byte) 'f', (byte) 0 };
        assertTrue(Arrays.equals(temp, ParseUtil.stringToByteArray("12af", 5)));
    }

    /**
     * Test long to byte array.
     */
    @Test
    public void testLongToByteArray() {
        byte[] temp = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0 };
        assertTrue(Arrays.equals(temp, ParseUtil.longToByteArray(0x12345678, 4, 5)));
    }

    /**
     * Test string and byte array to long.
     */
    @Test
    public void testStringAndByteArrayToLong() {
        byte[] temp = { (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e' };
        long abcde = (long) temp[0] << 32 | temp[1] << 24 | temp[2] << 16 | temp[3] << 8 | temp[4];
        // Test string
        assertEquals(abcde, ParseUtil.strToLong("abcde", 5));
        // Test byte array
        assertEquals(abcde, ParseUtil.byteArrayToLong(temp, 5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByteArrayToLongSizeTooBig() {
        ParseUtil.byteArrayToLong(new byte[10], 9);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByteArrayToLongSizeBigger() {
        ParseUtil.byteArrayToLong(new byte[7], 8);
    }

    /**
     * Test byte arry to float
     */
    @Test
    public void testByteArrayToFloat() {
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
    public void testUnsignedIntToLong() {
        assertEquals(0L, ParseUtil.unsignedIntToLong(0));
        assertEquals(123L, ParseUtil.unsignedIntToLong(123));
        assertEquals(4294967295L, ParseUtil.unsignedIntToLong(0xffffffff));
    }

    /**
     * Test unsigned long to signed long
     */
    @Test
    public void testUnsignedLongToSignedLong() {
        assertEquals(1L, ParseUtil.unsignedLongToSignedLong(Long.MAX_VALUE + 2));
        assertEquals(123L, ParseUtil.unsignedLongToSignedLong(123));
        assertEquals(9223372036854775807L, ParseUtil.unsignedLongToSignedLong(9223372036854775807L));
    }

    /**
     * Test hex string to string
     */
    @Test
    public void testHexStringToString() {
        assertEquals("ABC", ParseUtil.hexStringToString("414243"));
        assertEquals("ab00cd", ParseUtil.hexStringToString("ab00cd"));
        assertEquals("ab88cd", ParseUtil.hexStringToString("ab88cd"));
        assertEquals("notHex", ParseUtil.hexStringToString("notHex"));
        assertEquals("320", ParseUtil.hexStringToString("320"));
        assertEquals("0", ParseUtil.hexStringToString("0"));
    }

    /**
     * Test parse int
     */
    @Test
    public void testParseIntOrDefault() {
        assertEquals(123, ParseUtil.parseIntOrDefault("123", 45));
        assertEquals(45, ParseUtil.parseIntOrDefault("123X", 45));
    }

    /**
     * Test parse long
     */
    @Test
    public void testParseLongOrDefault() {
        assertEquals(123L, ParseUtil.parseLongOrDefault("123", 45L));
        assertEquals(45L, ParseUtil.parseLongOrDefault("123L", 45L));
    }

    /**
     * Test parse long
     */
    @Test
    public void testParseUnsignedLongOrDefault() {
        assertEquals(9223372036854775807L, ParseUtil.parseUnsignedLongOrDefault("9223372036854775807", 123L));
        assertEquals(-9223372036854775808L, ParseUtil.parseUnsignedLongOrDefault("9223372036854775808", 45L));
        assertEquals(-1L, ParseUtil.parseUnsignedLongOrDefault("18446744073709551615", 123L));
        assertEquals(0L, ParseUtil.parseUnsignedLongOrDefault("18446744073709551616", 45L));
        assertEquals(123L, ParseUtil.parseUnsignedLongOrDefault("9223372036854775808L", 123L));
    }

    /**
     * Test parse double
     */
    @Test
    public void testParseDoubleOrDefault() {
        assertEquals(1.23d, ParseUtil.parseDoubleOrDefault("1.23", 4.5d), Double.MIN_VALUE);
        assertEquals(4.5d, ParseUtil.parseDoubleOrDefault("one.twentythree", 4.5d), Double.MIN_VALUE);
    }

    /**
     * Test parse DHMS
     */
    @Test
    public void testParseDHMSOrDefault() {
        assertEquals(93784050L, ParseUtil.parseDHMSOrDefault("1-02:03:04.05", 0L));
        assertEquals(93784000L, ParseUtil.parseDHMSOrDefault("1-02:03:04", 0L));
        assertEquals(7384000L, ParseUtil.parseDHMSOrDefault("02:03:04", 0L));
        assertEquals(184050L, ParseUtil.parseDHMSOrDefault("03:04.05", 0L));
        assertEquals(184000L, ParseUtil.parseDHMSOrDefault("03:04", 0L));
        assertEquals(0L, ParseUtil.parseDHMSOrDefault("04", 0L));
    }

    /**
     * Test parse UUID
     */
    @Test
    public void testParseUuidOrDefault() {
        assertEquals("123e4567-e89b-12d3-a456-426655440000",
                ParseUtil.parseUuidOrDefault("123e4567-e89b-12d3-a456-426655440000", "default"));
        assertEquals("123e4567-e89b-12d3-a456-426655440000",
                ParseUtil.parseUuidOrDefault("The UUID is 123E4567-E89B-12D3-A456-426655440000!", "default"));
        assertEquals("default", ParseUtil.parseUuidOrDefault("foo", "default"));
    }

    /**
     * Test parse SingleQuoteString
     */
    @Test
    public void testGetSingleQuoteStringValue() {
        assertEquals("bar", ParseUtil.getSingleQuoteStringValue("foo = 'bar' (string)"));
        assertEquals("", ParseUtil.getSingleQuoteStringValue("foo = bar (string)"));
    }

    @Test
    public void testGetDoubleQuoteStringValue() {
        assertEquals("bar", ParseUtil.getDoubleQuoteStringValue("foo = \"bar\" (string)"));
        assertEquals("", ParseUtil.getDoubleQuoteStringValue("hello"));
    }

    /**
     * Test parse SingleQuoteBetweenMultipleQuotes
     */
    @Test
    public void testGetStringBetweenMultipleQuotes() {
        assertEquals("hello $ is", ParseUtil.getStringBetween("hello = $hello $ is $", '$'));
        assertEquals("Realtek AC'97 Audio", ParseUtil.getStringBetween("pci.device = 'Realtek AC'97 Audio'", '\''));
    }

    /**
     * Test parse FirstIntValue
     */
    @Test
    public void testGetFirstIntValue() {
        assertEquals(42, ParseUtil.getFirstIntValue("foo = 42 (0x2a) (int)"));
        assertEquals(0, ParseUtil.getFirstIntValue("foo = 0x2a (int)"));
        assertEquals(42, ParseUtil.getFirstIntValue("42"));
        assertEquals(10, ParseUtil.getFirstIntValue("10.12.2"));
    }

    /**
     * Test parse NthIntValue
     */
    @Test
    public void testGetNthIntValue() {
        assertEquals(2, ParseUtil.getNthIntValue("foo = 42 (0x2a) (int)", 3));
        assertEquals(0, ParseUtil.getNthIntValue("foo = 0x2a (int)", 3));
        assertEquals(12, ParseUtil.getNthIntValue("10.12.2", 2));
    }

    /**
     * Test parse removeMatchingString
     */
    @Test
    public void testRemoveMatchingString() {
        assertEquals("foo = 42 () (int)", ParseUtil.removeMatchingString("foo = 42 (0x2a) (int)", "0x2a"));
        assertEquals("foo = 0x2a (int)", ParseUtil.removeMatchingString("foo = 0x2a (int)", "qqq"));
        assertEquals("10.1.", ParseUtil.removeMatchingString("10.12.2", "2"));
        assertEquals("", ParseUtil.removeMatchingString("10.12.2", "10.12.2"));
        assertEquals("", ParseUtil.removeMatchingString("", "10.12.2"));
        assertEquals(null, ParseUtil.removeMatchingString(null, "10.12.2"));
        assertEquals("2", ParseUtil.removeMatchingString("10.12.2", "10.12."));
    }

    /**
     * Test parse string to array
     */
    @Test
    public void testParseStringToLongArray() {
        int[] indices = { 1, 3 };
        long now = System.currentTimeMillis();

        String foo = String.format("The numbers are %d %d %d %d", 123, 456, 789, now);
        long[] result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertEquals(456L, result[0]);
        assertEquals(now, result[1]);

        foo = String.format("The numbers are %d -%d %d +%d", 123, 456, 789, now);
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertEquals(-456L, result[0]);
        assertEquals(now, result[1]);

        foo = String.format("Invalid character %d %s %d %d", 123, "4v6", 789, now);
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertEquals(0, result[1]);

        foo = String.format("Exceeds max long %d %d %d %d0", 123, 456, 789, Long.MAX_VALUE);
        result = ParseUtil.parseStringToLongArray(foo, indices, 4, ' ');
        assertEquals(0, result[1]);

        foo = String.format("String too short %d %d %d %d", 123, 456, 789, now);
        result = ParseUtil.parseStringToLongArray(foo, indices, 9, ' ');
        assertEquals(0, result[1]);

        foo = String.format("Array too short %d %d %d %d", 123, 456, 789, now);
        result = ParseUtil.parseStringToLongArray(foo, indices, 2, ' ');
        assertEquals(0, result[1]);
    }

    @Test
    public void testTextBetween() {
        String text = "foo bar baz";
        String before = "foo";
        String after = "baz";
        assertEquals(" bar ", ParseUtil.getTextBetweenStrings(text, before, after));

        before = "";
        assertEquals("foo bar ", ParseUtil.getTextBetweenStrings(text, before, after));

        before = "food";
        assertEquals("", ParseUtil.getTextBetweenStrings(text, before, after));

        before = "foo";
        after = "qux";
        assertEquals("", ParseUtil.getTextBetweenStrings(text, before, after));

    }

    @Test
    public void testFiletimeToMs() {
        assertEquals(1172163600306L, ParseUtil.filetimeToUtcMs(128166372003061629L, false));
    }
}
