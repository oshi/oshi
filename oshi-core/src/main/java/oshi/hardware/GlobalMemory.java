/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * The GlobalMemory class tracks information about the use of a computer's physical memory (RAM) as well as any
 * available virtual memory.
 */
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
