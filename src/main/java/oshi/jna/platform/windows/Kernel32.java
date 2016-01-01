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
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.jna.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinBase;

/**
 * Windows Kernel32. This class should be considered non-API as it may be
 * removed if/when its code is incorporated into the JNA project.
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface Kernel32 extends com.sun.jna.platform.win32.Kernel32 {
    Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("Kernel32", Kernel32.class);

    // TODO: Submit this change to JNA Kernel32 class
    /**
     * Retrieves system timing information. On a multiprocessor system, the
     * values returned are the sum of the designated times across all
     * processors.
     * 
     * @param lpIdleTime
     *            A pointer to a FILETIME structure that receives the amount of
     *            time that the system has been idle.
     * @param lpKernelTime
     *            A pointer to a FILETIME structure that receives the amount of
     *            time that the system has spent executing in Kernel mode
     *            (including all threads in all processes, on all processors).
     *            This time value also includes the amount of time the system
     *            has been idle.
     * @param lpUserTime
     *            A pointer to a FILETIME structure that receives the amount of
     *            time that the system has spent executing in User mode
     *            (including all threads in all processes, on all processors).
     * @return If the function succeeds, the return value is nonzero. If the
     *         function fails, the return value is zero and errno is set.
     */
    boolean GetSystemTimes(WinBase.FILETIME lpIdleTime, WinBase.FILETIME lpKernelTime, WinBase.FILETIME lpUserTime);

}
