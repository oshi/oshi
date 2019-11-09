/**
 * MIT License
 *
 * Copyright (c) 2010-2019 The OSHI project team
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
package oshi.jna.platform.linux;

import com.sun.jna.Native;
import com.sun.jna.platform.linux.LibC;
import com.sun.jna.ptr.NativeLongByReference;

import oshi.jna.platform.unix.CLibrary;

/**
 * Linux C Library. This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 */
public interface LinuxLibc extends LibC, CLibrary {

    LinuxLibc INSTANCE = Native.load("c", LinuxLibc.class);

    /**
     * Writes the affinity mask of the process whose ID is pid into the cpu_set_t
     * structure pointed to by {@code pMask}.
     *
     * @param pid
     *            The process id whose mask to return. If {@code pid} is zero, then
     *            the mask of the calling process is returned.
     * @param cpusetsize
     *            specifies the size (in bytes) of the mask.
     * @param pMask
     *            On return, writes the affinity mask of the process whose ID is
     *            {@code pid} into the bitmask pointed to by {@code pMask}.
     * @return On success, returns 0. On error, -1 is returned, and errno is set
     *         appropriately.
     */
    int sched_getaffinity(int pid, int cpusetsize, NativeLongByReference pMask);
}
