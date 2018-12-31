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
package oshi.software.os.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

/**
 * Windows OS native system information.
 *
 * @author dblock[at]dblock[dot]org
 */
public class WindowsOSSystemInfo {
    private static final Logger LOG = LoggerFactory.getLogger(WindowsOSSystemInfo.class);

    private SYSTEM_INFO _si = null;

    public WindowsOSSystemInfo() {
        init();
    }

    public WindowsOSSystemInfo(SYSTEM_INFO si) {
        this._si = si;
    }

    private void init() {
        SYSTEM_INFO si = new SYSTEM_INFO();
        Kernel32.INSTANCE.GetSystemInfo(si);

        try {
            IntByReference isWow64 = new IntByReference();
            HANDLE hProcess = Kernel32.INSTANCE.GetCurrentProcess();
            if (Kernel32.INSTANCE.IsWow64Process(hProcess, isWow64) && isWow64.getValue() > 0) {
                Kernel32.INSTANCE.GetNativeSystemInfo(si);
            }
        } catch (UnsatisfiedLinkError e) {
            // no WOW64 support
            LOG.trace("No WOW64 support: {}", e);
        }

        this._si = si;
        LOG.debug("Initialized OSNativeSystemInfo");
    }

    /**
     * Number of processors.
     *
     * @return Integer.
     */
    public int getNumberOfProcessors() {
        return this._si.dwNumberOfProcessors.intValue();
    }
}
