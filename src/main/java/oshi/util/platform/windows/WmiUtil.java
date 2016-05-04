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
package oshi.util.platform.windows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.util.ExecutingCommand;

/**
 * Provides access to WMI queries
 * 
 * @author widdis[at]gmail[dot]com
 */
public class WmiUtil {

    /**
     * Get an integer value from WMI
     * 
     * @param path
     *            Path for the query
     * @param property
     *            Property to retrieve value
     * @return Integer value of the property if successful; -1 if failed
     */
    public static int getIntValue(String path, String property) {
        int value = -1;
        // TODO: Replace wmic command line with COM call
        ArrayList<String> data = ExecutingCommand.runNative(String.format("wmic %s get %s", path, property));
        for (String checkLine : data) {
            if (checkLine.length() == 0 || checkLine.toLowerCase().contains(property.toLowerCase())) {
                continue;
            } else {
                // If successful this line is the desired value
                try {
                    value = Integer.parseInt(checkLine.trim());
                } catch (NumberFormatException e) {
                    // If we failed to parse, give up
                    value = -1;
                }
                break;
            }
        }
        return value;
    }

    /**
     * Get a string value from WMI
     * 
     * @param path
     *            Path for the query
     * @param properties
     *            Property to retrieve value
     * @return A list of string values for the property
     */
    public static List<String> getStringValue(String path, String property) {
        Map<String, List<String>> map = getStrValues(path, property);
        if (map.containsKey(property)) {
            return map.get(property);
        }
        return new ArrayList<String>();
    }

    /**
     * Get String values from WMI
     * 
     * @param path
     *            Path for the query
     * @param properties
     *            Comma-delimited properties to retrieve value
     * @return Map with a key for each property and value a List of string
     *         values of the property. All lists will contain the same elements
     */
    public static Map<String, List<String>> getStrValues(String path, String properties) {
        // Set up map
        Map<String, List<String>> values = new HashMap<>();
        String[] props = properties.split(",");
        // If only one property, no header row is returned so we manually add it
        if (props.length == 1) {
            values.put(props[0], new ArrayList<String>());
        }
        // Get query
        // TODO: Replace wmic command line with COM call
        ArrayList<String> data = ExecutingCommand.runNative(String.format("wmic %s get %s", path, properties));
        String[] headers = new String[props.length];
        for (String checkLine : data) {
            if (checkLine.length() == 0) {
                continue;
            }
            String[] vals = checkLine.split("\\s\\s+");
            // First row is header
            if (headers[0] == null) {
                for (int i = 0; i < headers.length && i < vals.length; i++) {
                    headers[i] = vals[i];
                    values.put(headers[i], new ArrayList<String>());
                }
                continue;
            }
            // Values are in same order as headers
            for (int i = 0; i < headers.length && i < vals.length; i++) {
                values.get(headers[i]).add(vals[i].trim());
            }
        }
        return values;
    }

    /**
     * Get an array of String values from WMI when property matches a key
     * 
     * @param path
     *            Path for the query
     * @param key
     *            Key to match against the first property
     * @param properties
     *            Comma-delimited string; the first property will be checked
     *            against the key
     * @return String value of the second property if the first property matches
     */
    public static String[] getStrValuesForKey(String path, String key, String properties) {
        List<String> values = new ArrayList<String>();
        // TODO: Replace wmic command line with COM call
        ArrayList<String> data = ExecutingCommand.runNative(String.format("wmic %s get %s", path, properties));
        for (String checkLine : data) {
            String[] keyValue = checkLine.split("\\s+");
            if (keyValue.length < 2) {
                continue;
            }
            if (keyValue[0].equals(key)) {
                values.add(keyValue[1]);
            }
        }
        return values.toArray(new String[values.size()]);
    }

    /**
     * Get an array of double values from WMI when properties match keys
     * 
     * @param path
     *            Path for the query
     * @param key0
     *            Key to match against the first property
     * @param key1
     *            Key to match against the second property
     * @param properties
     *            Comma-delimited string; the first and second properties will
     *            be checked against the keys
     * @return Array of doubles in the third property if the first two
     *         properties match the keys
     */
    public static double[] wmiGetDoubleValuesForKeys(String path, String key0, String key1, String properties) {
        List<Double> values = new ArrayList<Double>();
        // TODO: Replace wmic command line with COM call
        ArrayList<String> data = ExecutingCommand.runNative(String.format("wmic %s get %s", path, properties));
        for (String checkLine : data) {
            double value;
            String[] keyValue = checkLine.split("\\s+");
            if (keyValue.length < 3) {
                continue;
            }
            if ((key0 == null || keyValue[0].equals(key0)) && keyValue[1].equals(key1)) {
                // If successful this line is the desired value
                try {
                    value = Double.parseDouble(keyValue[2]);
                } catch (NumberFormatException e) {
                    // If we failed to parse, do nothing
                    continue;
                }
                values.add(value);
            }
        }
        double[] valueArray = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            valueArray[i] = values.get(i);
        }
        return valueArray;
    }
}