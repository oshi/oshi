/*
 * MIT License
 *
 * Copyright (c) 2016-2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
import static org.hamcrest.Matchers.is;

import java.text.DecimalFormatSymbols;

import org.junit.jupiter.api.Test;

/**
 * The Class FormatUtilTest.
 */
class FormatUtilTest {

    private static char DECIMAL_SEPARATOR = new DecimalFormatSymbols().getDecimalSeparator();

    /**
     * Test format bytes.
     */
    @Test
    void testFormatBytes() {
        assertThat("format 0 bytes", FormatUtil.formatBytes(0), is("0 bytes"));
        assertThat("format byte", FormatUtil.formatBytes(1), is("1 byte"));
        assertThat("format bytes", FormatUtil.formatBytes(532), is("532 bytes"));
        assertThat("format KiByte", FormatUtil.formatBytes(1024), is("1 KiB"));
        assertThat("format GiByte", FormatUtil.formatBytes(1024 * 1024 * 1024), is("1 GiB"));
        assertThat("format TiByte", FormatUtil.formatBytes(1099511627776L), is("1 TiB"));
    }

    /**
     * Test format bytes with decimal separator.
     */
    @Test
    void testFormatBytesWithDecimalSeparator() {
        String expected1 = "1" + DECIMAL_SEPARATOR + "3 KiB";
        String expected2 = "2" + DECIMAL_SEPARATOR + "3 MiB";
        String expected3 = "2" + DECIMAL_SEPARATOR + "2 GiB";
        String expected4 = "1" + DECIMAL_SEPARATOR + "1 TiB";
        String expected5 = "1" + DECIMAL_SEPARATOR + "1 PiB";
        String expected6 = "1" + DECIMAL_SEPARATOR + "1 EiB";
        assertThat("format KiBytes with decimal separator", FormatUtil.formatBytes(1340), is(expected1));
        assertThat("format MiBytes with decimal separator", FormatUtil.formatBytes(2400016), is(expected2));
        assertThat("format GiBytes with decimal separator", FormatUtil.formatBytes(2400000000L), is(expected3));
        assertThat("format TiBytes with decimal separator", FormatUtil.formatBytes(1099511627776L + 109951162777L),
                is(expected4));
        assertThat("format PiBytes with decimal separator",
                FormatUtil.formatBytes(1125899906842624L + 112589990684262L), is(expected5));
        assertThat("format EiBytes with decimal separator",
                FormatUtil.formatBytes(1152921504606846976L + 115292150460684698L), is(expected6));
    }

    /**
     * Test format decimal bytes.
     */
    @Test
    void testFormatBytesDecimal() {
        assertThat("format 0 bytesDecimal", FormatUtil.formatBytesDecimal(0), is("0 bytes"));
        assertThat("format byteDecimal", FormatUtil.formatBytesDecimal(1), is("1 byte"));
        assertThat("format bytesDecimal", FormatUtil.formatBytesDecimal(532), is("532 bytes"));
        assertThat("format KbytesDecimal", FormatUtil.formatBytesDecimal(1000), is("1 KB"));
        assertThat("format GbytesDecimal", FormatUtil.formatBytesDecimal(1000 * 1000 * 1000), is("1 GB"));
        assertThat("format TbytesDecimal", FormatUtil.formatBytesDecimal(1000000000000L), is("1 TB"));
    }

    /**
     * Test format decimal bytes with decimal separator.
     */
    @Test
    void testFormatBytesDecimalWithDecimalSeparator() {
        String expected1 = "1" + DECIMAL_SEPARATOR + "3 KB";
        String expected2 = "2" + DECIMAL_SEPARATOR + "3 MB";
        String expected3 = "2" + DECIMAL_SEPARATOR + "2 GB";
        String expected4 = "1" + DECIMAL_SEPARATOR + "1 TB";
        String expected5 = "3" + DECIMAL_SEPARATOR + "4 PB";
        String expected6 = "5" + DECIMAL_SEPARATOR + "6 EB";
        assertThat("format KBytes with decimal separator", FormatUtil.formatBytesDecimal(1300), is(expected1));
        assertThat("format MBytes with decimal separator", FormatUtil.formatBytesDecimal(2300000), is(expected2));
        assertThat("format GBytes with decimal separator", FormatUtil.formatBytesDecimal(2200000000L), is(expected3));
        assertThat("format TBytes with decimal separator", FormatUtil.formatBytesDecimal(1100000000000L),
                is(expected4));
        assertThat("format PBytes with decimal separator", FormatUtil.formatBytesDecimal(3400000000000000L),
                is(expected5));
        assertThat("format EBytes with decimal separator", FormatUtil.formatBytesDecimal(5600000000000000000L),
                is(expected6));
    }

    /**
     * Test format hertz.
     */
    @Test
    void testFormatHertz() {
        assertThat("format zero Hertz", FormatUtil.formatHertz(0), is("0 Hz"));
        assertThat("format one Hertz", FormatUtil.formatHertz(1), is("1 Hz"));
        assertThat("format many Hertz", FormatUtil.formatHertz(999), is("999 Hz"));
        assertThat("format KHertz", FormatUtil.formatHertz(1000), is("1 KHz"));
        assertThat("format MHertz", FormatUtil.formatHertz(1000 * 1000), is("1 MHz"));
        assertThat("format GHertz", FormatUtil.formatHertz(1000 * 1000 * 1000), is("1 GHz"));
        assertThat("format THertz", FormatUtil.formatHertz(1000L * 1000L * 1000L * 1000L), is("1 THz"));
    }

    /**
     * Test format elapsed secs
     */
    @Test
    void testFormatElapsedSecs() {
        assertThat("format 0 elapsed seconds", FormatUtil.formatElapsedSecs(0), is("0 days, 00:00:00"));
        assertThat("format many elapsed seconds", FormatUtil.formatElapsedSecs(12345), is("0 days, 03:25:45"));
        assertThat("format elapsed day in seconds", FormatUtil.formatElapsedSecs(123456), is("1 days, 10:17:36"));
        assertThat("format elapsed days in seconds", FormatUtil.formatElapsedSecs(1234567), is("14 days, 06:56:07"));
    }

    /**
     * Test unsigned int to long.
     */
    @Test
    void testGetUnsignedInt() {
        assertThat("unsigned int", FormatUtil.getUnsignedInt(-1), is(4294967295L));
    }

    /**
     * Test unsigned string
     */
    @Test
    void testToUnsignedString() {
        assertThat("Integer to unsigned string", FormatUtil.toUnsignedString(0x00000001), is("1"));
        assertThat("Big Integer to unsigned string", FormatUtil.toUnsignedString(0x80000000), is("2147483648"));
        assertThat("INT_MAX to unsigned string", FormatUtil.toUnsignedString(0xffffffff), is("4294967295"));

        assertThat("Long to unsigned string", FormatUtil.toUnsignedString(0x0000000000000001L), is("1"));
        assertThat("Big Long to unsigned string", FormatUtil.toUnsignedString(0x8000000000000000L),
                is("9223372036854775808"));
        assertThat("LONG_MAX to unsigned string", FormatUtil.toUnsignedString(0xffffffffffffffffL),
                is("18446744073709551615"));
    }

    /**
     * Test format error
     */
    @Test
    void testFormatError() {
        assertThat("Format error code", FormatUtil.formatError(-1234567000), is("0xB66A00A8"));
    }

    /**
     * Test round to int
     */
    @Test
    void testRoundToInt() {
        assertThat("Improper rounding pi", FormatUtil.roundToInt(Math.PI), is(3));
        assertThat("Improper rounding e", FormatUtil.roundToInt(Math.E), is(3));
        assertThat("Improper rounding 0", FormatUtil.roundToInt(0d), is(0));
        assertThat("Improper rounding 1", FormatUtil.roundToInt(1d), is(1));
    }

}
