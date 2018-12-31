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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads from lsof into a map
 *
 * @author widdis[at]gmail[dot]com
 */
public class LsofUtil {

    private LsofUtil() {
    }

    public static Map<Integer, String> getCwdMap(int pid) {
        List<String> lsof = ExecutingCommand.runNative("lsof -Fn -d cwd" + (pid < 0 ? "" : " -p " + pid));
        Map<Integer, String> cwdMap = new HashMap<>();
        Integer key = -1;
        for (String line : lsof) {
            if (line.isEmpty()) {
                continue;
            }
            switch (line.charAt(0)) {
            case 'p':
                key = ParseUtil.parseIntOrDefault(line.substring(1), -1);
                break;
            case 'n':
                cwdMap.put(key, line.substring(1));
                break;
            case 'f':
                // ignore the 'cwd' file descriptor
            default:
                break;
            }
        }
        return cwdMap;
    }
}
