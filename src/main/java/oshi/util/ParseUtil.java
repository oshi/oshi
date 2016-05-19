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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util;

import java.io.StringWriter;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * String parsing utility.
 * 
 * @author alessio.fachechi[at]gmail[dot]com
 */
public abstract class ParseUtil {

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
    final private static Pattern HERTZ = Pattern.compile("(\\d+(.\\d+)?) ?([kMGT]?Hz)");

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
            try {
                Double value = Double.valueOf(matcher.group(1));
                String unit = matcher.group(3);

                if (multipliers.containsKey(unit)) {
                    value = value * multipliers.get(unit);
                    return value.longValue();
                }
            } catch (NumberFormatException e) {
                LOG.trace("", e);
            }
        }

        return -1L;
    }

    /**
     * Parse the last element of a string to a value
     * 
     * @param s
     *            The string to parse
     * @param i
     *            Default integer if not parsable
     * @return {@link Integer} value or the given default if not parsable
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
     * @return a byte array with each pair of characters converted to a byte
     */
    public static byte[] hexStringToByteArray(String digits) {
        // Check if string is valid hex
        if (!VALID_HEX.matcher(digits).matches()) {
            LOG.error("Invalid hexadecimal string: {}", digits);
            return null;
        }
        int len = digits.length();
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

    /**
     * Pretty print a JSON string.
     * 
     * @param json
     *            A JSON object
     * @return String representing the object with added whitespace, new lines,
     *         indentation
     */
    public static String jsonPrettyPrint(JsonObject json) {
        // Pretty printing using JsonWriterFactory
        // Output stream
        StringWriter stringWriter = new StringWriter();
        // Config
        Map<String, Boolean> config = new HashMap<String, Boolean>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(config);
        // Writer
        JsonWriter jsonWriter = writerFactory.createWriter(stringWriter);
        jsonWriter.write(json);
        jsonWriter.close();
        // Return
        return stringWriter.toString();
    }

    /*
     * Format for parsing DATETIME originally 20160513072950.782000-420,
     * modified to 20160513072950.782-0700
     */
    private static final SimpleDateFormat cimDateFormat = new SimpleDateFormat("yyyyMMddHHmmss.SSSX");

    /**
     * Parses a CIM_DateTime format (from WMI) to a Java Date. See
     * https://msdn.microsoft.com/en-us/library/aa387237(v=vs.85).aspx
     * 
     * @param cimDate
     *            A string containing the CIM_DateTime
     * @return The corresponding date
     */
    public static Date cimDateTimeToDate(String cimDate) {
        // Keep first 18 digits; ignore next 3
        // Keep + or - sign of timezone
        // Parse last 3 digits from minutes to HH:mm
        int tzMinutes = Integer.parseInt(cimDate.substring(22));
        return cimDateFormat.parse(String.format("%s%c%02d%02d", cimDate.substring(0, 18), cimDate.charAt(21),
                tzMinutes / 60, tzMinutes % 60), new ParsePosition(0));
    }
}
