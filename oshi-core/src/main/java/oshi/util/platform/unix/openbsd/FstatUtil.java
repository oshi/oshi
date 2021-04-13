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
package oshi.util.platform.unix.openbsd;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Reads from fstat.
 */
@ThreadSafe
public final class FstatUtil {
    private FstatUtil() {
    }

    /**
     * Gets current working directory info (using {@code ps} actually).
     *
     * @param pid
     *            a process ID
     * @return the current working directory for that process.
     */
    public static String getCwd(int pid) {
        List<String> ps = ExecutingCommand.runNative("ps -axwwo cwd -p " + pid);
        if (!ps.isEmpty()) {
            return ps.get(1);
        }
        return "";
    }

    /**
     * Gets open number of files.
     *
     * @param pid
     *            The process ID
     * @return the number of open files.
     */
    public static long getOpenFiles(int pid) {
        long fd = 0L;
        List<String> fstat = ExecutingCommand.runNative("fstat -sp " + pid);
        for (String line : fstat) {
            String[] split = ParseUtil.whitespaces.split(line.trim(), 11);
            if (split.length == 11 && !"pipe".contains(split[4]) && !"unix".contains(split[4])) {
                fd++;
            }
        }
        // subtract 1 for header row
        return fd - 1;
    }
}
