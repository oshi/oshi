/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

/**
 * Constants for the Linux auxiliary vector ({@code /proc/self/auxv}).
 *
 * @see <a href="https://github.com/torvalds/linux/blob/v3.19/include/uapi/linux/auxvec.h">auxvec.h</a>
 */
public final class Auxv {

    private Auxv() {
    }

    /** end of vector */
    public static final int AT_NULL = 0;
    /** system page size */
    public static final int AT_PAGESZ = 6;
    /** arch dependent hints at CPU capabilities */
    public static final int AT_HWCAP = 16;
    /** frequency at which times() increments */
    public static final int AT_CLKTCK = 17;
}
