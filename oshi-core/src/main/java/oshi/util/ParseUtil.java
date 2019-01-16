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

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        multipliers.put(KHZ, 1_000L);
        multipliers.put(MHZ, 1_000_000L);
        multipliers.put(GHZ, 1_000_000_000L);
        multipliers.put(THZ, 1_000_000_000_000L);
        multipliers.put(PHZ, 1_000_000_000_000_000L);
    }

    // Fast decimal exponentiation: pow(10,y) --> POWERS_OF_10[y]
    private static final long[] POWERS_OF_TEN = { 1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L, 10_000_000L,
            100_000_000L, 1_000_000_000L, 10_000_000_000L, 100_000_000_000L, 1_000_000_000_000L, 10_000_000_000_000L,
            100_000_000_000_000L, 1_000_000_000_000_000L, 10_000_000_000_000_000L, 100_000_000_000_000_000L,
            1_000_000_000_000_000_000L };

    // Fast hex character lookup
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

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
            double value = Double.valueOf(matcher.group(1)) * multipliers.getOrDefault(matcher.group(3), -1L);
            if (value >= 0d) {
                return (long) value;
            }
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
     * Parse the last element of a space-delimited string to a value
     *
     * @param s
     *            The string to parse
     * @param li
     *            Default long integer if not parsable
     * @return value or the given default if not parsable
     */
    public static long parseLastLong(String s, long li) {
        try {
            return Long.parseLong(parseLastString(s));
        } catch (NumberFormatException e) {
            LOG.trace(DEFAULT_LOG_MSG, s, e);
            return li;
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
     * Parse a byte aray into a string of hexadecimal digits including leading
     * zeros
     *
     * @param bytes
     *            The byte array to represent
     * @return A string of hex characters corresponding to the bytes. The string
     *         is upper case.
     */
    public static String byteArrayToHexString(byte[] bytes) {
        // Solution copied from https://stackoverflow.com/questions/9655181
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
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
     *            A human readable ASCII string
     * @param size
     *            Number of characters to convert to the long. May not exceed 8.
     * @return An integer representing the string where each character is
     *         treated as a byte
     */
    public static long strToLong(String str, int size) {
        return byteArrayToLong(str.getBytes(StandardCharsets.US_ASCII), size);
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
     * Convert an unsigned integer to a long value. The method assumes that all
     * bits in the specified integer value are 'data' bits, including the
     * most-significant bit which Java normally considers a sign bit. The method
     * must be used only when it is certain that the integer value represents an
     * unsigned integer, for example when the integer is returned by JNA library
     * in a structure which holds unsigned integers.
     *
     * @param unsignedValue
     *            The unsigned integer value to convert.
     * @return The unsigned integer value widened to a long.
     */
    public static long unsignedIntToLong(int unsignedValue) {
        // use standard Java widening conversion to long which does
        // sign-extension,
        // then drop any copies of the sign bit, to prevent the value being
        // considered a negative one by Java if it is set
        long longValue = unsignedValue;
        return longValue & 0xffffffffL;
    }

    /**
     * Convert an unsigned long to a signed long value by stripping the sign
     * bit. This method "rolls over" long values greater than the max value but
     * ensures the result is never negative.
     * 
     * @param unsignedValue
     *            The unsigned long value to convert.
     * @return The signed long value.
     */
    public static long unsignedLongToSignedLong(long unsignedValue) {
        return unsignedValue & 0x7fffffff_ffffffffL;
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
            return new BigInteger(s).longValue();
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
            milliseconds += (long) (1000 * parseDoubleOrDefault("0." + m.group(5), 0d));
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
        return getStringBetween(line, '\'');
    }

    /**
     * Parse a string key = "value" (string)
     * 
     * @param line
     *            the entire string
     * @return the value contained between double tick marks
     */
    public static String getDoubleQuoteStringValue(String line) {
        return getStringBetween(line, '"');
    }

    /**
     * Gets a value between two characters having multiple same characters
     * between them. <b>Examples : </b>
     * <ul>
     * <li>"name = 'James Gosling's Java'" returns "James Gosling's Java"</li>
     * <li>"pci.name = 'Realtek AC'97 Audio Device'" returns "Realtek AC'97
     * Audio Device"</li>
     * </ul>
     *
     * @param line
     *            The "key-value" pair line.
     * @param c
     *            The Trailing And Leading characters of the string line
     * @return : The value having the characters between them.
     */
    public static String getStringBetween(String line, char c) {
        int firstOcc = line.indexOf(c);
        if (firstOcc < 0) {
            return "";
        }
        return line.substring(firstOcc + 1, line.lastIndexOf(c)).trim();
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
     * Removes all matching sub strings from the string. More efficient than
     * regexp.
     *
     * @param original
     *            source String to remove from
     * @param toRemove
     *            the sub string to be removed
     * @return The string with all matching substrings removed
     */
    public static String removeMatchingString(final String original, final String toRemove) {
        if (original == null || original.isEmpty() || toRemove == null || toRemove.isEmpty()) {
            return original;
        }

        int matchIndex = original.indexOf(toRemove, 0);
        if (matchIndex == -1) {
            return original;
        }

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

    /**
     * Parses a delimited string to an array of longs. Optimized for processing
     * predictable-length arrays such as outputs of reliably formatted Linux
     * proc or sys filesystem, minimizing new object creation. Users should
     * perform other sanity checks of data.
     *
     * The indices parameters are referenced assuming the length as specified,
     * and leading characters are ignored. For example, if the string is "foo 12
     * 34 5" and the length is 3, then index 0 is 12, index 1 is 34, and index 2
     * is 5.
     *
     * @param s
     *            The string to parse
     * @param indices
     *            An array indicating which indexes should be populated in the
     *            final array; other values will be skipped. This idex is
     *            zero-referenced assuming the rightmost delimited fields of the
     *            string contain the array.
     * @param length
     *            The total number of elements in the string array. It is
     *            permissible for the string to have more elements than this;
     *            leading elements will be ignored.
     * @param delimiter
     *            The character to delimit by
     * @return If successful, an array of parsed longs. If parsing errors
     *         occurred, will be an array of zeros.
     */
    public static long[] parseStringToLongArray(String s, int[] indices, int length, char delimiter) {
        long[] parsed = new long[indices.length];
        // Iterate from right-to-left of String
        // Fill right to left of result array using index array
        int charIndex = s.length() - 1;
        int parsedIndex = indices.length - 1;
        int stringIndex = length - 1;

        int power = 0;
        int c;
        boolean delimCurrent = false;
        while (charIndex > 0 && parsedIndex >= 0) {
            c = s.charAt(charIndex--);
            if (c == delimiter) {
                if (!delimCurrent) {
                    power = 0;
                    if (indices[parsedIndex] == stringIndex--) {
                        parsedIndex--;
                    }
                    delimCurrent = true;
                }
            } else if (indices[parsedIndex] != stringIndex || c == '+') {
                // Doesn't impact parsing, ignore
                delimCurrent = false;
            } else if (c >= '0' && c <= '9') {
                if (power > 18) {
                    LOG.error("Number is too big for a long parsing string '{}' to long array", s);
                    return new long[indices.length];
                }
                parsed[parsedIndex] += (c - '0') * ParseUtil.POWERS_OF_TEN[power++];
                delimCurrent = false;
            } else if (c == '-') {
                parsed[parsedIndex] *= -1L;
                delimCurrent = false;
            } else {
                // error on everything else
                LOG.error("Illegal character parsing string '{}' to long array: {}", s, s.charAt(charIndex));
                return new long[indices.length];
            }
        }
        if (parsedIndex > 0) {
            LOG.error("Not enough fields in string '{}' parsing to long array: {}", s, indices.length - parsedIndex);
            return new long[indices.length];
        }
        return parsed;
    }

    /**
     * Get a String in a line of text between two marker strings
     *
     * @param text
     *            Text to search for match
     * @param before
     *            Start matching after this text
     * @param after
     *            End matching before this text
     * @return Text between the strings before and after, or empty string if
     *         either marker does not exist
     */
    public static String getTextBetweenStrings(String text, String before, String after) {

        String result = "";

        if (text.indexOf(before) >= 0 && text.indexOf(after) >= 0) {
            result = text.substring(text.indexOf(before) + before.length(), text.length());
            result = result.substring(0, result.indexOf(after));
        }
        return result;
    }
}
