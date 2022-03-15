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
package oshi.driver.windows.perfmon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.GlobalConfig;
import oshi.util.Util;

/**
 * Tests whether performance counters are disabled
 */
@ThreadSafe
public final class PerfmonDisabled {

    private static final Logger LOG = LoggerFactory.getLogger(PerfmonDisabled.class);

    static final boolean PERF_OS_DISABLED = isDisabled(GlobalConfig.OSHI_OS_WINDOWS_PERFOS_DIABLED, "PerfOS");
    static final boolean PERF_PROC_DISABLED = isDisabled(GlobalConfig.OSHI_OS_WINDOWS_PERFPROC_DIABLED, "PerfProc");
    static final boolean PERF_DISK_DISABLED = isDisabled(GlobalConfig.OSHI_OS_WINDOWS_PERFDISK_DIABLED, "PerfDisk");

    /**
     * Everything in this class is static, never instantiate it
     */
    private PerfmonDisabled() {
        throw new AssertionError();
    }

    private static boolean isDisabled(String config, String service) {
        String perfDisabled = GlobalConfig.get(config);
        // If null or empty, check registry
        if (Util.isBlank(perfDisabled)) {
            String key = String.format("SYSTEM\\CurrentControlSet\\Services\\%s\\Performance", service);
            String value = "Disable Performance Counters";
            // If disabled in registry, log warning and return
            if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, key, value)
                    && Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, key, value) > 0) {
                LOG.warn("{} counters are disabled and won't return data: {}\\\\{}\\\\{} > 0.", service,
                        "HKEY_LOCAL_MACHINE", key, value);
                return true;
            }
            return false;
        }
        // If not null or empty, parse as boolean
        return Boolean.parseBoolean(perfDisabled);
    }
}
