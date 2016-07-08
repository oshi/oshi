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

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * String parsing utility.
 * 
 * @author alessio.fachechi[at]gmail[dot]com
 */
public class ParseUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ParseUtil.class);

    /**
     * Hertz related variables.
     */
    final private static String Hertz = "Hz";

    final private static String kiloHertz = "k" + Hertz;

    final private static String megaHertz = "M" + Hertz;

    final private static String gigaHertz = "G" + Hertz;

    final private static String teraHertz = "T" + Hertz;

    final private static String petaHertz = "P" + Hertz;

    final private static Map<String, Long> multipliers;

    static {
        multipliers = new HashMap<>();
        multipliers.put(Hertz, 1L);
        multipliers.put(kiloHertz, 1000L);
        multipliers.put(megaHertz, 1000000L);
        multipliers.put(gigaHertz, 1000000000L);
        multipliers.put(teraHertz, 1000000000000L);
        multipliers.put(petaHertz, 1000000000000000L);
    }

    /**
     * Used for matching
     */
    final private static Pattern HERTZ = Pattern.compile("(\\d+(.\\d+)?) ?([kMGT]?Hz).*");

    /**
     * Used to check validity of a hexadecimal string
     */
    final private static Pattern VALID_HEX = Pattern.compile("[0-9a-fA-F]+");

    /**
     * Parse hertz from a string, eg. "2.00MHz" in 2000000L.
     * 
     * @param hertz
     *            Hertz size.
     * @return {@link Long} Hertz value or -1 if not parsable.
     */
    public static long parseHertz(String hertz) {
        Matcher matcher = HERTZ.matcher(hertz.trim());
        if (matcher.find() && matcher.groupCount() == 3) {
            // Regexp enforces #(.#) format so no test for NFE required
            Double value = Double.valueOf(matcher.group(1)) * multipliers.getOrDefault(matcher.group(3), -1L);
            return value < 0d ? -1L : value.longValue();
        }
        return -1L;
    }

    /**
     * Parse the last element of a space-delimited string to a value if there
     * are at least two elements
     * 
     * @param s
     *            The string to parse
     * @param i
     *            Default integer if not parsable or less than two elements
     * @return value or the given default if not parsable
     */
    public static int parseLastInt(String s, int i) {
        String[] ss = s.split("\\s+");
        if (ss.length < 2) {
            return i;
        } else {
            try {
                return Integer.parseInt(ss[ss.length - 1]);
            } catch (NumberFormatException nfe) {
                return i;
            }
        }
    }

    /**
     * Parse a string of hexadecimal digits into a byte array
     * 
     * @param digits
     *            The string to be parsed
     * @return a byte array with each pair of characters converted to a byte, or
     *         null if the string is not valid hex
     */
    public static byte[] hexStringToByteArray(String digits) {
        int len = digits.length();
        // Check if string is valid hex
        if (!VALID_HEX.matcher(digits).matches() || (len & 0x1) != 0) {
            LOG.warn("Invalid hexadecimal string: {}", digits);
            return null;
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(digits.charAt(i), 16) << 4)
                    | Character.digit(digits.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Parse a human readable string into a byte array, truncating or padding
     * with zeros (if necessary) so the array has the specified length.
     * 
     * @param text
     *            The string to be parsed
     * @param length
     *            Length of the returned array.
     * @return A byte array of specified length, with each of the first length
     *         characters converted to a byte. If length is longer than the
     *         provided string length, will be filled with zeroes.
     */
    public static byte[] stringToByteArray(String text, int length) {
        return Arrays.copyOf(text.getBytes(), length);
    }

    /**
     * Convert a long value to a byte array using Big Endian, truncating or
     * padding with zeros (if necessary) so the array has the specified length.
     * 
     * @param value
     *            The value to be converted
     * @param valueSize
     *            Number of bytes representing the value
     * @param length
     *            Number of bytes to return
     * @return A byte array of specified length representing the long in the
     *         first valueSize bytes
     */
    public static byte[] longToByteArray(long value, int valueSize, int length) {
        // Convert the long to 8-byte BE representation
        byte[] b = new byte[8];
        for (int i = 7; i >= 0 && value != 0L; i--) {
            b[i] = (byte) value;
            value >>>= 8;
        }
        // Then copy the rightmost valueSize bytes
        // e.g., for an integer we want rightmost 4 bytes
        return Arrays.copyOfRange(b, 8 - valueSize, 8 + length - valueSize);
    }

    /**
     * Convert a string to an integer representation.
     * 
     * @param str
     *            A human readable string
     * @param size
     *            Number of characters to convert to the long. May not exceed 8.
     * @return An integer representing the string where each character is
     *         treated as a byte
     */
    public static long strToLong(String str, int size) {
        return byteArrayToLong(str.getBytes(), size);
    }

    /**
     * Convert a byte array to its integer representation.
     * 
     * @param bytes
     *            An array of bytes no smaller than the size to be converted
     * @param size
     *            Number of bytes to convert to the long. May not exceed 8.
     * @return An integer representing the byte array as a 64-bit number
     */
    public static long byteArrayToLong(byte[] bytes, int size) {
        if (size > 8) {
            throw new IllegalArgumentException("Can't convert more than 8 bytes.");
        }
        if (size > bytes.length) {
            throw new IllegalArgumentException("Size can't be larger than array length.");
        }
        long total = 0L;
        for (int i = 0; i < size; i++) {
            total = (total << 8) | (bytes[i] & 0xff);
        }
        return total;
    }

    /**
     * Convert a byte array to its floating point representation.
     * 
     * @param bytes
     *            An array of bytes no smaller than the size to be converted
     * @param size
     *            Number of bytes to convert to the float. May not exceed 8.
     * @param fpBits
     *            Number of bits representing the decimal
     * @return A float; the integer portion representing the byte array as an
     *         integer shifted by the bits specified in fpBits; with the
     *         remaining bits used as a decimal
     */
    public static float byteArrayToFloat(byte[] bytes, int size, int fpBits) {
        return byteArrayToLong(bytes, size) / (float) (1 << fpBits);
    }

    /*
     * Format for parsing DATETIME originally 20160513072950.782000-420,
     * modified to 20160513072950.782000-07:00
     */
    private static DateTimeFormatter CIM_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSSSSSZZZZZ", Locale.US);

    /**
     * Parses a CIM_DateTime format (from WMI) to milliseconds since the epoch.
     * See https://msdn.microsoft.com/en-us/library/aa387237(v=vs.85).aspx
     * 
     * @param cimDate
     *            A string containing the CIM_DateTime
     * @return The corresponding DateTime as a number of milliseconds since the
     *         epoch
     */
    public static long cimDateTimeToMillis(String cimDate) {
        // Keep first 22 characters: digits, decimal, and + or - sign of
        // time zone. Parse last 3 digits from minutes to HH:mm
        try {
            int tzInMinutes = Integer.parseInt(cimDate.substring(22));
            LocalTime offsetAsLocalTime = LocalTime.MIN.plusMinutes(tzInMinutes);
            OffsetDateTime dateTime = OffsetDateTime.parse(
                    cimDate.substring(0, 22) + offsetAsLocalTime.format(DateTimeFormatter.ISO_LOCAL_TIME), CIM_FORMAT);
            return dateTime.toInstant().toEpochMilli();
        } catch (IndexOutOfBoundsException // if cimDate not 22+ chars
                | NumberFormatException // if TZ minutes doesn't parse
                | DateTimeParseException e) {
            return 0L;
        }
    }

    /**
     * Parses a string of hex digits to a string where each pair of hex digits
     * represents an ASCII character
     * 
     * @param hexString
     *            A sequence of hex digits
     * @return The corresponding string if valid hex; otherwise the original
     *         hexString
     */
    public static String hexStringToString(String hexString) {
        try {
            return new String(DatatypeConverter.parseHexBinary(hexString));
        } catch (IllegalArgumentException e) {
            // Hex failed to parse, just return the existing string
            return hexString;
        }
    }

    /**
     * Attempts to parse a string to an int. If it fails, returns the default
     * 
     * @param s
     *            The string to parse
     * @param defaultInt
     *            The value to return if parsing fails
     * @return The parsed int, or the default if parsing failed
     */
    public static int parseIntOrDefault(String s, int defaultInt) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultInt;
        }
    }

    /**
     * Attempts to parse a string to a long. If it fails, returns the default
     * 
     * @param s
     *            The string to parse
     * @param defaultLong
     *            The value to return if parsing fails
     * @return The parsed long, or the default if parsing failed
     */
    public static long parseLongOrDefault(String s, long defaultLong) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return defaultLong;
        }
    }

    /**
     * Attempts to parse a string to a double. If it fails, returns the default
     * 
     * @param s
     *            The string to parse
     * @param defaultDouble
     *            The value to return if parsing fails
     * @return The parsed double, or the default if parsing failed
     */
    public static double parseDoubleOrDefault(String s, double defaultDouble) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return defaultDouble;
        }
    }

    /*
     * Pattern for [dd-[hh:[mm:ss]]]
     */
    private static final Pattern DHMS = Pattern.compile("(?:(\\d+)-)?(?:(\\d+):)?(\\d+):(\\d+)(?:\\.(\\d+))?");

    /**
     * Attempts to parse a string of the form [DD-[hh:]]mm:ss[.ddd] to a number
     * of milliseconds. If it fails, returns the default.
     * 
     * @param s
     *            The string to parse
     * @param defaultLong
     *            The value to return if parsing fails
     * @return The parsed number of seconds, or the default if parsing fails
     */
    public static long parseDHMSOrDefault(String s, long defaultLong) {
        Matcher m = DHMS.matcher(s);
        long milliseconds = 0L;
        if (m.matches()) {
            if (m.group(1) != null) {
                milliseconds += parseLongOrDefault(m.group(1), 0L) * 86400000L;
            }
            if (m.group(2) != null) {
                milliseconds += parseLongOrDefault(m.group(2), 0L) * 3600000L;
            }
            milliseconds += parseLongOrDefault(m.group(3), 0L) * 60000L;
            milliseconds += parseLongOrDefault(m.group(4), 0L) * 1000L;
            milliseconds += 1000 * parseDoubleOrDefault("0." + m.group(5), 0d);
        }
        return milliseconds;
    }

}
