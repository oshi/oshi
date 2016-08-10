/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.jna.platform.windows;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD.SIZE_T;
import com.sun.jna.platform.win32.WinDef.DWORD;

/**
 * Windows PSAPI. This class should be considered non-API as it may be removed
 * if/when its code is incorporated public into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface Psapi extends com.sun.jna.platform.win32.Psapi {
    Psapi INSTANCE = (Psapi) Native.loadLibrary("Psapi", Psapi.class);

    // TODO: Submit this change to JNA Psapi class
    class PERFORMANCE_INFORMATION extends Structure {
        public DWORD cb;
        public SIZE_T CommitTotal;
        public SIZE_T CommitLimit;
        public SIZE_T CommitPeak;
        public SIZE_T PhysicalTotal;
        public SIZE_T PhysicalAvailable;
        public SIZE_T SystemCache;
        public SIZE_T KernelTotal;
        public SIZE_T KernelPaged;
        public SIZE_T KernelNonpaged;
        public SIZE_T PageSize;
        public DWORD HandleCount;
        public DWORD ProcessCount;
        public DWORD ThreadCount;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "cb", "CommitTotal", "CommitLimit", "CommitPeak", "PhysicalTotal",
                    "PhysicalAvailable", "SystemCache", "KernelTotal", "KernelPaged", "KernelNonpaged", "PageSize",
                    "HandleCount", "ProcessCount", "ThreadCount" });
        }
    }

    /**
     * Retrieves the performance values contained in the
     * {@link PERFORMANCE_INFORMATION} structure.
     *
     * @param pPerformanceInformation
     *            A pointer to a {@link PERFORMANCE_INFORMATION} structure that
     *            receives the performance information.
     * @param cb
     *            The size of the {@link PERFORMANCE_INFORMATION} structure, in
     *            bytes.
     * @return If the function succeeds, the return value is TRUE. If the
     *         function fails, the return value is FALSE. To get extended error
     *         information, call GetLastError.
     */
    boolean GetPerformanceInfo(PERFORMANCE_INFORMATION pPerformanceInformation, int cb);
}
