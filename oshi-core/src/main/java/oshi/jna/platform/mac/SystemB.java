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
package oshi.jna.platform.mac;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.mac.IOKit.MachPort;
import com.sun.jna.platform.mac.SystemBFunctionMapper;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.unix.CLibrary;

/**
 * System class. This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 */
public interface SystemB extends CLibrary, com.sun.jna.platform.mac.SystemB {

    Map<String, Object> OPTIONS = Collections.unmodifiableMap(new HashMap<String, Object>() {
        private static final long serialVersionUID = 1L; // we're not serializing it
        {
            put(OPTION_FUNCTION_MAPPER, new SystemBFunctionMapper());
        }
    });

    SystemB INSTANCE = Native.load("System", SystemB.class, OPTIONS);

    /**
     * The mach_host_self system call returns the calling thread's host name port.
     * It has an effect equivalent to receiving a send right for the host port.
     *
     * @return the host's name port
     */
    MachPort mach_host_self_ptr();

    /**
     * The mach_task_self system call returns the calling thread's task_self port.
     * It has an effect equivalent to receiving a send right for the task's kernel
     * port.
     *
     * @return the task's kernel port
     */
    MachPort mach_task_self_ptr();

    /**
     * The host_page_size function returns the page size for the given host.
     *
     * @param machPort
     *            The name (or control) port for the host for which the page size is
     *            desired.
     * @param pPageSize
     *            The host's page size (in bytes), set on success.
     * @return 0 on success; sets errno on failure
     */
    int host_page_size_ptr(MachPort machPort, LongByReference pPageSize);

    /**
     * The host_statistics function returns scheduling and virtual memory statistics
     * concerning the host as specified by hostStat.
     *
     * @param machPort
     *            The control port for the host for which information is to be
     *            obtained.
     * @param hostStat
     *            The type of statistics desired (HOST_LOAD_INFO, HOST_VM_INFO, or
     *            HOST_CPU_LOAD_INFO)
     * @param stats
     *            Statistics about the specified host.
     * @param count
     *            On input, the maximum size of the buffer; on output, the size
     *            returned (in natural-sized units).
     * @return 0 on success; sets errno on failure
     */
    int host_statistics_ptr(MachPort machPort, int hostStat, Structure stats, IntByReference count);

    /**
     * The host_statistics64 function returns 64-bit virtual memory statistics
     * concerning the host as specified by hostStat.
     *
     * @param machPort
     *            The control port for the host for which information is to be
     *            obtained.
     * @param hostStat
     *            The type of statistics desired (HOST_VM_INFO64)
     * @param stats
     *            Statistics about the specified host.
     * @param count
     *            On input, the maximum size of the buffer; on output, the size
     *            returned (in natural-sized units).
     * @return 0 on success; sets errno on failure
     */
    int host_statistics64_ptr(MachPort machPort, int hostStat, Structure stats, IntByReference count);

    /**
     * The host_processor_info function returns information about processors.
     *
     * @param machPort
     *            The control port for the host for which information is to be
     *            obtained.
     * @param flavor
     *            The type of information requested.
     * @param procCount
     *            Pointer to the number of processors
     * @param procInfo
     *            Pointer to the structure corresponding to the requested flavor
     * @param procInfoCount
     *            Pointer to number of elements in the returned structure
     * @return 0 on success; sets errno on failure
     */
    int host_processor_info_ptr(MachPort machPort, int flavor, IntByReference procCount, PointerByReference procInfo,
            IntByReference procInfoCount);
}
