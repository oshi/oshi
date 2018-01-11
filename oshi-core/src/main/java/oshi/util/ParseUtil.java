/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
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
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.util;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;

/**
 * String parsing utility.
 *
 * @author alessio.fachechi[at]gmail[dot]com
 */
public class ParseUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ParseUtil.class);

    private static final String DEFAULT_LOG_MSG = "{} didn't parse. Returning default. {}";
    /*
     * Used for matching
     */
    private static final Pattern HERTZ_PATTERN = Pattern.compile("(\\d+(.\\d+)?) ?([kMGT]?Hz).*");

    /*
     * Used to check validity of a hexadecimal string
     */
    private static final Pattern VALID_HEX = Pattern.compile("[0-9a-fA-F]+");

    /*
     * Format for parsing DATETIME originally 20160513072950.782000-420,
     * modified to 20160513072950.782000-07:00
     */
    private static final DateTimeFormatter CIM_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSSSSSZZZZZ",
            Locale.US);

    /*
     * Pattern for [dd-[hh:[mm:ss]]]
     */
    private static final Pattern DHMS = Pattern.compile("(?:(\\d+)-)?(?:(\\d+):)?(\\d+):(\\d+)(?:\\.(\\d+))?");

    /*
     * Pattern for a UUID
     */
    private static final Pattern UUID_PATTERN = Pattern
            .compile(".*([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}).*");

    /*
     * Hertz related variables.
     */
    private static final String HZ = "Hz";
    private static final String KHZ = "kHz";
    private static final String MHZ = "MHz";
    private static final String GHZ = "GHz";
    private static final String THZ = "THz";
    private static final String PHZ = "PHz";

    private static final Map<String, Long> multipliers;

    public static final Pattern whitespacesColonWhitespace = Pattern.compile("\\s+:\\s");

    public static final Pattern whitespaces = Pattern.compile("\\s+");

    public static final Pattern notDigits = Pattern.compile("[^0-9]+");

    public static final Pattern startWithNotDigits = Pattern.compile("^[^0-9]*");

    static {
        multipliers = new HashMap<>();
        multipliers.put(HZ, 1L);
        multipliers.put(KHZ, 1000L);
        multipliers.put(MHZ, 1000000L);
        multipliers.put(GHZ, 1000000000L);
        multipliers.put(THZ, 1000000000000L);
        multipliers.put(PHZ, 1000000000000000L);
    }

    private ParseUtil() {
    }

    /**
     * Parse hertz from a string, eg. "2.00MHz" in 2000000L.
     *
     * @param hertz
     *            Hertz size.
     * @return {@link Long} Hertz value or -1 if not parsable.
     */
    public static long parseHertz(String hertz) {
        Matcher matcher = HERTZ_PATTERN.matcher(hertz.trim());
        if (matcher.find() && matcher.groupCount() == 3) {
            // Regexp enforces #(.#) format so no test for NFE required
            Double value = Double.valueOf(matcher.group(1)) * MapUtil.getOrDefault(multipliers, matcher.group(3), -1L);
            return value < 0d ? -1L : value.longValue();
        }
        return -1L;
    }

    /**
     * Parse the last element of a space-delimited string to a value
     *
     * @param s
     *            The string to parse
     * @param i
     *            Default integer if not parsable
     * @return value or the given default if not parsable
     */
    public static int parseLastInt(String s, int i) {
        try {
            return Integer.parseInt(parseLastString(s));
        } catch (NumberFormatException e) {
            LOG.trace(DEFAULT_LOG_MSG, s, e);
            return i;
        }
    }

    /**
     * Parse the last element of a space-delimited string to a string
     *
     * @param s
     *            The string to parse
     * @return last space-delimited element
     */
    public static String parseLastString(String s) {
        String[] ss = whitespaces.split(s);
        if (ss.length < 1) {
            return s;
        } else {
            return ss[ss.length - 1];
        }
    }

    /**
     * Parse a string of hexadecimal digits into a byte array
     *
     * @param digits
     *            The string to be parsed
     * @return a byte array with each pair of characters converted to a byte, or
     *         empty array if the string is not valid hex
     */
    public static byte[] hexStringToByteArray(String digits) {
        int len = digits.length();
        // Check if string is valid hex
        if (!VALID_HEX.matcher(digits).matches() || (len & 0x1) != 0) {
            LOG.warn("Invalid hexadecimal string: {}", digits);
            return new byte[0];
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) (Character.digit(digits.charAt(i), 16) << 4
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
        long val = value;
        // Convert the long to 8-byte BE representation
        byte[] b = new byte[8];
        for (int i = 7; i >= 0 && val != 0L; i--) {
            b[i] = (byte) val;
            val >>>= 8;
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
            total = total << 8 | bytes[i] & 0xff;
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

    /**
     * Convert an unsigned integer to a long value.
     * The method assumes that all bits in the specified integer value are 'data' bits, including the most-significant bit which
     * Java normally considers a sign bit. The method must be used only when it is certain that the integer value
     * represents an unsigned integer, for example when the integer is returned by JNA library in a structure
     * which holds unsigned integers.
     *
     * @param unsignedValue The unsigned integer value to convert.
     * @return The unsigned integer value widened to a long.
     */
    public static long unsignedIntToLong(int unsignedValue) {
        //use standard Java widening conversion to long which does sign-extension,
        //then drop any copies of the sign bit, to prevent the value being considered a negative one by Java if it is set
        long longValue = (long) unsignedValue;
        return longValue & 0xffffffffL;
    }

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
            LOG.trace(DEFAULT_LOG_MSG, cimDate, e);
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
        // Odd length strings won't parse, return
        if (hexString.length() % 2 > 0) {
            return hexString;
        }
        int charAsInt;
        StringBuilder sb = new StringBuilder();
        try {
            for (int pos = 0; pos < hexString.length(); pos += 2) {
                charAsInt = Integer.parseInt(hexString.substring(pos, pos + 2), 16);
                if (charAsInt < 32 || charAsInt > 127) {
                    return hexString;
                }
                sb.append((char) charAsInt);
            }
        } catch (NumberFormatException e) {
            LOG.trace(DEFAULT_LOG_MSG, hexString, e);
            // Hex failed to parse, just return the existing string
            return hexString;
        }
        return sb.toString();
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
            LOG.trace(DEFAULT_LOG_MSG, s, e);
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
            LOG.trace(DEFAULT_LOG_MSG, s, e);
            return defaultLong;
        }
    }

    /**
     * Attempts to parse a string to an "unsigned" long. If it fails, returns
     * the default
     *
     * @param s
     *            The string to parse
     * @param defaultLong
     *            The value to return if parsing fails
     * @return The parsed long containing the same 64 bits that an unsigned long
     *         would contain (which may produce a negative value)
     */
    public static long parseUnsignedLongOrDefault(String s, long defaultLong) {
        try {
            BigInteger bi = new BigInteger(s);
            return bi.longValue();
        } catch (NumberFormatException e) {
            LOG.trace(DEFAULT_LOG_MSG, s, e);
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
            LOG.trace(DEFAULT_LOG_MSG, s, e);
            return defaultDouble;
        }
    }

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
        if (m.matches()) {
            long milliseconds = 0L;
            if (m.group(1) != null) {
                milliseconds += parseLongOrDefault(m.group(1), 0L) * 86400000L;
            }
            if (m.group(2) != null) {
                milliseconds += parseLongOrDefault(m.group(2), 0L) * 3600000L;
            }
            milliseconds += parseLongOrDefault(m.group(3), 0L) * 60000L;
            milliseconds += parseLongOrDefault(m.group(4), 0L) * 1000L;
            milliseconds += 1000 * parseDoubleOrDefault("0." + m.group(5), 0d);
            return milliseconds;
        }
        return defaultLong;
    }

    /**
     * Attempts to parse a UUID. If it fails, returns the default.
     *
     * @param s
     *            The string to parse
     * @param defaultStr
     *            The value to return if parsing fails
     * @return The parsed UUID, or the default if parsing fails
     */
    public static String parseUuidOrDefault(String s, String defaultStr) {
        Matcher m = UUID_PATTERN.matcher(s.toLowerCase());
        if (m.matches()) {
            return m.group(1);
        }
        return defaultStr;
    }

    /**
     * Parses a string key = 'value' (string)
     *
     * @param line
     *            The entire string
     * @return the value contained between single tick marks
     */
    public static String getSingleQuoteStringValue(String line) {
        String[] split = line.split("'");
        if (split.length < 2) {
            return "";
        }
        return split[1];
    }

    /**
     * Parses a string such as "10.12.2" or "key = 1 (0x1) (int)" to find the
     * integer value of the first set of one or more consecutive digits
     *
     * @param line
     *            The entire string
     * @return the value of first integer if any; 0 otherwise
     */
    public static int getFirstIntValue(String line) {
        return getNthIntValue(line, 1);
    }

    /**
     * Parses a string such as "10.12.2" or "key = 1 (0x1) (int)" to find the
     * integer value of the nth set of one or more consecutive digits
     *
     * @param line
     *            The entire string
     * @param n
     *            Which set of integers to return
     * @return the value of nth integer if any; 0 otherwise
     */
    public static int getNthIntValue(String line, int n) {
        // Split the string by non-digits,
        String[] split = notDigits.split(startWithNotDigits.matcher(line).replaceFirst(""));
        if (split.length >= n) {
            return parseIntOrDefault(split[n - 1], 0);
        }
        return 0;
    }

    /**
     * Removes all sub strings from the string
     * @param original source String to remove from
     * @param toRemove the sub string to be removed
     * @return
     */
    public static String removeMatchingString(final String original, final String toRemove) {
        if (original == null || original.isEmpty() || toRemove == null || toRemove.isEmpty())
            return original;

        int matchIndex = original.indexOf(toRemove, 0);
        if (matchIndex == -1)
            return original;

        StringBuilder buffer = new StringBuilder(original.length() - toRemove.length());
        int currIndex = 0;
        do {
            buffer.append(original.substring(currIndex, matchIndex));
            currIndex = matchIndex + toRemove.length();
            matchIndex = original.indexOf(toRemove, currIndex);
        } while (matchIndex != -1);

        buffer.append(original.substring(currIndex));
        return buffer.toString();
    }

}
