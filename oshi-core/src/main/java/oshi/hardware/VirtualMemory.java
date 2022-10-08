/*
 * Copyright 2019-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * The VirtuallMemory class tracks information about the use of a computer's virtual memory (swap file) which
 * temporarily moves rarely accessed information to a disk or other storage device.
 */
@ThreadSafe
public interface VirtualMemory {

    /**
     * The current size of the paging/swap file(s), in bytes. If the paging/swap file can be extended, this is a soft
     * limit.
     *
     * @return Total swap in bytes.
     */
    long getSwapTotal();

    /**
     * The current memory committed to the paging/swap file(s), in bytes
     *
     * @return Swap used in bytes
     */
    long getSwapUsed();

    /**
     * The maximum memory that can be committed by the system without extending the paging file(s), in bytes. Also
     * called the Commit Limit. If the paging/swap file can be extended, this is a soft limit. This is generally equal
     * to the sum of the sizes of physical memory and paging/swap file(s).
     * <p>
     * On Linux, represents the total amount of memory currently available to be allocated on the system based on the
     * overcommit ratio, identified as {@code CommitLimit}. This may be higher or lower than the total size of physical
     * and swap memory depending on system configuration.
     *
     * @return Max Virtual Memory in bytes
     */
    long getVirtualMax();

    /**
     * The memory currently committed by the system, in bytes. Also called the Commit Total. This is generally equal to
     * the sum of the bytes used of physical memory and paging/swap file(s).
     * <p>
     * On Windows, committing pages changes this value immediately; however, the physical memory is not charged until
     * the pages are accessed, so this value may exceed the sum of used physical and paging/swap file memory.
     *
     * @return Swap used in bytes
     */
    long getVirtualInUse();

    /**
     * Number of pages read from paging/swap file(s) to resolve hard page faults. (Hard page faults occur when a process
     * requires code or data that is not in its working set or elsewhere in physical memory, and must be retrieved from
     * disk.) This property was designed as a primary indicator of the kinds of faults that cause system-wide delays. It
     * includes pages retrieved to satisfy faults in the file system cache (usually requested by applications) and in
     * non-cached mapped memory files.
     *
     * @return Pages swapped in
     */
    long getSwapPagesIn();

    /**
     * Number of pages written to paging/swap file(s) to free up space in physical memory. Pages are written back to
     * disk only if they are changed in physical memory, so they are likely to hold data, not code. A high rate of pages
     * output might indicate a memory shortage. The operating system writes more pages back to disk to free up space
     * when physical memory is in short supply.
     *
     * @return Pages swapped out
     */
    long getSwapPagesOut();
}
