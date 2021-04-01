/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.unix.aix;

import java.util.HashMap;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Utility to query ls
 */
@ThreadSafe
public final class Ls {

    private Ls() {
    }

    /**
     * Query {@code ls} to get parition info
     *
     * @return A map of device name to a major-minor pair
     */
    public static Map<String, Pair<Integer, Integer>> queryDeviceMajorMinor() {
        // Map major and minor from ls
        /*-
         $ ls -l /dev
        brw-rw----  1 root system 10,  5 Sep 12  2017 hd2
        brw-------  1 root system 20,  0 Jun 28  1970 hdisk0
         */
        Map<String, Pair<Integer, Integer>> majMinMap = new HashMap<>();
        for (String s : ExecutingCommand.runNative("ls -l /dev")) {
            // Filter to block devices
            if (!s.isEmpty() && s.charAt(0) == 'b') {
                // Device name is last space-delim string
                int idx = s.lastIndexOf(' ');
                if (idx > 0 && idx < s.length()) {
                    String device = s.substring(idx + 1);
                    int major = ParseUtil.getNthIntValue(s, 2);
                    int minor = ParseUtil.getNthIntValue(s, 3);
                    majMinMap.put(device, new Pair<>(major, minor));
                }
            }
        }
        return majMinMap;
    }
}
