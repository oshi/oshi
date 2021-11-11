/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors:
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
     * Returns a new String composed of copies of the array elements joined together
     * with a copy of the specified delimiter.
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
     * Returns a new String composed of copies of the list elements joined together
     * with a copy of the specified delimiter.
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