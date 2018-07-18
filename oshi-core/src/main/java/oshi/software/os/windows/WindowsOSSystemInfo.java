/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
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
