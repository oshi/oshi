/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import java.util.List;

import oshi.annotation.PublicApi;
import oshi.annotation.concurrent.ThreadSafe;

/**
 * The GlobalMemory class tracks information about the use of a computer's physical memory (RAM) as well as any
 * available virtual memory.
 * <p>
 * Example: checking available memory:
 *
 * <pre>{@code
 * GlobalMemory mem = si.getHardware().getMemory();
 * long totalBytes = mem.getTotal();
 * long availBytes = mem.getAvailable();
 * long usedBytes = totalBytes - availBytes;
 * System.out.printf("Memory: %s used / %s total%n", FormatUtil.formatBytes(usedBytes),
 *         FormatUtil.formatBytes(totalBytes));
 * }</pre>
 *
 * For swap/virtual memory information, see {@link #getVirtualMemory()}.
 */
@PublicApi
@ThreadSafe
public interface GlobalMemory {
    /**
     * The amount of actual physical memory, in bytes.
     *
     * @return Total number of bytes.
     */
    long getTotal();

    /**
     * The amount of physical memory currently available, in bytes.
     *
     * @return Available number of bytes.
     */
    long getAvailable();

    /**
     * The number of bytes in a memory page
     *
     * @return Page size in bytes.
     */
    long getPageSize();

    /**
     * Virtual memory, such as a swap file.
     * <p>
     * Operating systems differ significantly in how they manage virtual memory. See the {@link VirtualMemory} class
     * documentation for details on Windows commit-charge, Linux overcommit, and macOS compressed-memory models.
     *
     * @return A VirtualMemory object.
     */
    VirtualMemory getVirtualMemory();

    /**
     * Physical memory, such as banks of memory.
     * <p>
     * On Linux, requires elevated permissions. On FreeBSD and Solaris, requires installation of dmidecode.
     *
     * @return A list of PhysicalMemory objects.
     */
    List<PhysicalMemory> getPhysicalMemory();
}
