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
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Native; // NOSONAR squid:S1191

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

    public static Map<Integer, Long> queryAuxv() {
        byte[] auxv = FileUtil.readAllBytes(ProcPath.AUXV);
        if (auxv.length > 0) {
            ByteBuffer buff = ByteBuffer.allocate(auxv.length);
            buff.order(auxv[0] > 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
            for (byte b : auxv) {
                buff.put(b);
            }
            buff.flip();

            int key = 0;
            Map<Integer, Long> auxvMap = new HashMap<>();
            while (buff.position() <= buff.limit() - 2 * Native.LONG_SIZE) {
                key = Native.LONG_SIZE == 4 ? buff.getInt() : (int) buff.getLong();
                if (key > 0) {
                    auxvMap.put(key, Native.LONG_SIZE == 4 ? buff.getInt() : buff.getLong());
                }
            }
            return auxvMap;
        }
        return Collections.emptyMap();
    }
}
