/*
 * MIT License
 *
 * Copyright (c) 2020-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.unix.solaris.disk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Utility to query lshal
 */
@ThreadSafe
public final class Lshal {

    private static final String LSHAL_CMD = "lshal";

    private Lshal() {
    }

    /**
     * Query lshal to get device major
     *
     * @return A map with disk names as the key and block device major as the value
     *         if lshal is installed; empty map otherwise
     */
    public static Map<String, Integer> queryDiskToMajorMap() {
        Map<String, Integer> majorMap = new HashMap<>();
        List<String> lshal = ExecutingCommand.runNative(LSHAL_CMD);
        String diskName = null;
        for (String line : lshal) {
            if (line.startsWith("udi ")) {
                String udi = ParseUtil.getSingleQuoteStringValue(line);
                diskName = udi.substring(udi.lastIndexOf('/') + 1);
            } else {
                line = line.trim();
                if (line.startsWith("block.major") && diskName != null) {
                    majorMap.put(diskName, ParseUtil.getFirstIntValue(line));
                }
            }
        }
        return majorMap;
    }
}
