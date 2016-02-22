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
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.JsonObject;

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
     * Parse hertz from a string, eg. "2.00MHz" in 2000000L.
     * 
     * @param hertz
     *            Hertz size.
     * @return {@link Long} Hertz value or -1 if not parsable.
     */
    public static long parseHertz(String hertz) {
        Pattern pattern = Pattern.compile("(\\d+(.\\d+)?) ?([kMGT]?Hz)");
        Matcher matcher = pattern.matcher(hertz.trim());

        if (matcher.find() && (matcher.groupCount() == 3)) {
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
    public static int parseString(String s, int i) {
        String[] ss = s.split("\\s+");
        if (ss.length < 2) {
            return i;
        } else {
            return Integer.valueOf(ss[ss.length - 1]);
        }
    }

    /**
     * Parse a string representation of a hex array into a byte array
     * 
     * @param s
     *            The string to be parsed
     * @return a byte array with each pair of characters converted to a byte
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Prints an array of JSON objects
     * 
     * @param objArray
     *            an array of JsonObjects
     * @return compact JSON string for the array
     */
    public static String toJsonArray(JsonObject[] objArray) {
        StringBuilder sb = new StringBuilder("[");
        String separator = "";
        for (JsonObject obj : objArray) {
            sb.append(separator).append(obj.toJSON());
            separator = ",";
        }
        return sb.append("]").toString();
    }

    /**
     * Escapes special characters in a string for use with JSON
     * 
     * @param s
     *            A string which possibly contains special characters
     * @return The string with JSON special characters escaped, wrapped in
     *         double quotes
     */
    public static String jsonQuote(String s) {
        if (s == null || s.length() == 0) {
            return "\"\"";
        }
        char c = 0;
        int i;
        int len = s.length();
        StringBuilder sb = new StringBuilder(len + 4);
        String t;

        sb.append('"');
        for (i = 0; i < len; i++) {
            c = s.charAt(i);
            switch (c) {
            case '\\':
            case '"':
            case '/':
                sb.append('\\');
                sb.append(c);
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\t':
                sb.append("\\t");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\r':
                sb.append("\\r");
                break;
            default:
                if (c < ' ') {
                    t = "000" + Integer.toHexString(c);
                    sb.append("\\u" + t.substring(t.length() - 4));
                } else {
                    sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Pretty print a JSON string.
     * 
     * @param json
     *            A JSON string
     * @return String with added whitespace, new lines, indentation
     */
    public static String jsonPrettyPrint(String json) {
        int indent = 0;
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            // TODO: Handle backslash escapes
            // If " toggle inQuotes.
            if (c == '"') {
                inQuotes = !inQuotes;
            }
            // If inside quotes ignore other formatting
            if (inQuotes) {
                sb.append(c);
                continue;
            }
            switch (c) {
            // If { or [ add new line and increase indent
            case '{':
            case '[':
                sb.append(c);
                indent++;
                sb.append(newLineWithIndent(indent));
                break;
            // If } or ] add new line and decrease indent
            case '}':
            case ']':
                indent--;
                sb.append(newLineWithIndent(indent));
                sb.append(c);
                break;
            // If , add new line
            case ',':
                sb.append(c);
                sb.append(newLineWithIndent(indent));
                break;
            // If :, add space
            case ':':
                sb.append(c).append(" ");
                break;
            // Otherwise just display
            default:
                sb.append(c);
            }
        }
        // Remove whitespace between brackets if needed
        return sb.toString().replaceAll("\\[\\s+\\]", "[]").replaceAll("\\{\\s+\\}", "{}");
    }

    private static String newLineWithIndent(int indent) {
        StringBuilder sb = new StringBuilder("\n");
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }
}
