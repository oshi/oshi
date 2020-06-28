/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.jna.platform.unix.aix;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Function;
import com.sun.jna.Library; // NOSONAR squid:S1191
import com.sun.jna.NativeLibrary;

/**
 * The perfstat API uses the perfstat kernel extension to extract various AIX®
 * performance metrics.
 *
 * System component information is also retrieved from the Object Data Manager
 * (ODM) and returned with the performance metrics.
 *
 * The perfstat API is thread–safe, and does not require root authority.
 */
public class PerfstatUtil {
    private static final int RTLD_MEMBER = 0x00040000;
    private static final int RTLD_GLOBAL = 0x00010000;
    private static final int RTLD_LAZY = 0x00000004;
    private static final Map<String, Object> PERFSTAT_OPTIONS;
    static {
        HashMap<String, Object> options = new HashMap<String, Object>();
        options.put(Library.OPTION_OPEN_FLAGS, RTLD_MEMBER | RTLD_GLOBAL | RTLD_LAZY);
        PERFSTAT_OPTIONS = Collections.unmodifiableMap(options);
    }

    private static final NativeLibrary PERF = NativeLibrary.getInstance("/usr/lib/libperfstat.a(shr.o)",
            PERFSTAT_OPTIONS);

    public static int perfstat_cpu() {
        Function perfstat_cpu = PERF.getFunction("perfstat_cpu", Function.THROW_LAST_ERROR);
        Object[] params = new Object[4];
        params[0] = null;
        params[1] = null;
        params[2] = 1024;
        params[3] = 0;
        return perfstat_cpu.invokeInt(params);
    }
}
