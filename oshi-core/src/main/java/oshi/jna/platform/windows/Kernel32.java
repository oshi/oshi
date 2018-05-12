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
package oshi.jna.platform.windows;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.Structure;

/**
 * Windows Kernel32
 *
 * @author widdis[at]gmail[dot]com
 */
public interface Kernel32 extends com.sun.jna.platform.win32.Kernel32 {
    Kernel32 INSTANCE = Native.loadLibrary("Kernel32", Kernel32.class);

    class IO_COUNTERS extends Structure {
        public long ReadOperationCount;
        public long WriteOperationCount;
        public long OtherOperationCount;
        public long ReadTransferCount;
        public long WriteTransferCount;
        public long OtherTransferCount;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "ReadOperationCount", "WriteOperationCount", "OtherOperationCount",
                    "ReadTransferCount", "WriteTransferCount", "OtherTransferCount" });
        }
    }

    /**
     * Retrieves timing information for the specified process.
     * 
     * @param hProcess
     *            A handle to the process whose timing information is sought.
     *            The handle must have the PROCESS_QUERY_INFORMATION or
     *            PROCESS_QUERY_LIMITED_INFORMATION access right.
     * @param lpCreationTime
     *            A pointer to a FILETIME structure that receives the creation
     *            time of the process.
     * @param lpExitTime
     *            A pointer to a FILETIME structure that receives the exit time
     *            of the process. If the process has not exited, the content of
     *            this structure is undefined.
     * @param lpKernelTime
     *            A pointer to a FILETIME structure that receives the amount of
     *            time that the process has executed in kernel mode. The time
     *            that each of the threads of the process has executed in kernel
     *            mode is determined, and then all of those times are summed
     *            together to obtain this value.
     * @param lpUserTime
     *            A pointer to a FILETIME structure that receives the amount of
     *            time that the process has executed in user mode. The time that
     *            each of the threads of the process has executed in user mode
     *            is determined, and then all of those times are summed together
     *            to obtain this value. Note that this value can exceed the
     *            amount of real time elapsed (between lpCreationTime and
     *            lpExitTime) if the process executes across multiple CPU cores.
     * @return If the function succeeds, the return value is nonzero. If the
     *         function fails, the return value is zero. To get extended error
     *         information, call GetLastError.
     */
    boolean GetProcessTimes(HANDLE hProcess, FILETIME lpCreationTime, FILETIME lpExitTime, FILETIME lpKernelTime,
            FILETIME lpUserTime);

    /**
     * Retrieves accounting information for all I/O operations performed by the
     * specified process.
     * 
     * @param hProcess
     *            A handle to the process. The handle must have the
     *            PROCESS_QUERY_INFORMATION or PROCESS_QUERY_LIMITED_INFORMATION
     *            access right.
     * @param lpIoCounters
     *            A pointer to an IO_COUNTERS structure that receives the I/O
     *            accounting information for the process.
     * @return If the function succeeds, the return value is nonzero. If the
     *         function fails, the return value is zero. To get extended error
     *         information, call GetLastError.
     */
    boolean GetProcessIoCounters(HANDLE hProcess, IO_COUNTERS lpIoCounters);
}
