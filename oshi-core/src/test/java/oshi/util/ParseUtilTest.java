/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
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
        assertEquals(1000L, ParseUtil.parseHertz("1kHz"));
        assertEquals(1000000L, ParseUtil.parseHertz("1MHz"));
        assertEquals(1000000000L, ParseUtil.parseHertz("1GHz"));
        assertEquals(1500000000L, ParseUtil.parseHertz("1.5GHz"));
        assertEquals(1000000000000L, ParseUtil.parseHertz("1THz"));
    }

    /**
     * Test parse string.
     */
    @Test
    public void testParseLastInt() {
        assertEquals(-1, ParseUtil.parseLastInt("foo : bar", -1));
        assertEquals(1, ParseUtil.parseLastInt("foo : 1", 0));
        assertEquals(2, ParseUtil.parseLastInt("foo", 2));
    }

    /**
     * Test hex string to byte array.
     */
    @Test
    public void testHexStringToByteArray() {
        byte[] temp = { (byte) 0x12, (byte) 0xaf };
        assertTrue(Arrays.equals(temp, ParseUtil.hexStringToByteArray("12af")));
        assertEquals(null, ParseUtil.hexStringToByteArray("expected error abcde"));
        assertEquals(null, ParseUtil.hexStringToByteArray("abcde"));
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
     * Test CIM date to long.
     */
    @Test
    public void testCimDateTimeToDate() {
        assertEquals(1463149790782L, ParseUtil.cimDateTimeToMillis("20160513072950.782000-420"));
        assertEquals(0L, ParseUtil.cimDateTimeToMillis("20160513072950.782000"));
        assertEquals(0L, ParseUtil.cimDateTimeToMillis("20160513072950.782000-ABC"));
        assertEquals(0L, ParseUtil.cimDateTimeToMillis("NotAValidDateString00-000"));
    }

    /**
     * Test hex string to string
     */
    @Test
    public void testHexStringToString() {
        assertEquals("ABC", ParseUtil.hexStringToString("414243"));
        assertEquals("not hex", ParseUtil.hexStringToString("not hex"));
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

}
