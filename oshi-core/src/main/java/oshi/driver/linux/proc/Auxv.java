/*
 * MIT License
 *
 * Copyright (c) 2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.driver.linux.proc;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;
import oshi.util.platform.linux.ProcPath;

/**
 * Utility to read auxiliary vector from {@code /proc/self/auxv}
 */
@ThreadSafe
public final class Auxv {

    private Auxv() {
    }

    public static final int AT_PAGESZ = 6; // system page size
    public static final int AT_HWCAP = 16; // arch dependent hints at CPU capabilities
    public static final int AT_CLKTCK = 17; // frequency at which times() increments

    /**
     * Retrieve the auxiliary vector for the current process
     *
     * @return A map of auxiliary vector keys to their respective values
     * @see <a href=
     *      "https://github.com/torvalds/linux/blob/v3.19/include/uapi/linux/auxvec.h">auxvec.h</a>
     */
    public static Map<Integer, Long> queryAuxv() {
        ByteBuffer buff = FileUtil.readAllBytesAsBuffer(ProcPath.AUXV);
        Map<Integer, Long> auxvMap = new HashMap<>();
        int key;
        do {
            key = FileUtil.readNativeLongFromBuffer(buff).intValue();
            if (key > 0) {
                auxvMap.put(key, FileUtil.readNativeLongFromBuffer(buff).longValue());
            }
        } while (key > 0);
        return auxvMap;

    }
}
