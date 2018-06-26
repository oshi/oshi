/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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

import java.util.List;

/**
 * String utilities
 *
 * @author widdis[at]gmail[dot]com
 */
public class StringUtil {
    private StringUtil() {
    }

    /**
     * Returns a new String composed of copies of the array elements joined
     * together with a copy of the specified delimiter.
     * 
     * @param delimiter
     *            The character to separate the strings
     * @param stringArray
     *            The strings to join
     * @return A string containing the elements and specified delimiter
     */
    public static String join(String delimiter, String[] stringArray) {
        if (stringArray.length > 0) {
            StringBuilder sb = new StringBuilder(stringArray[0]);
            for (int i = 1; i < stringArray.length; i++) {
                sb.append(delimiter).append(stringArray[i]);
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
     * Returns a new String composed of copies of the list elements joined
     * together with a copy of the specified delimiter.
     * 
     * @param delimiter
     *            The character to separate the strings
     * @param stringList
     *            The strings to join
     * @return A string containing the elements and specified delimiter
     */
    public static String join(String delimiter, List<String> stringList) {
        return join(delimiter, stringList.toArray(new String[stringList.size()]));
    }
}