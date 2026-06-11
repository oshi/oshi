/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.unix.solaris;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.unix.solaris.SolarisOSThread;

/**
 * JNA-backed Solaris OSThread.
 */
@ThreadSafe
public final class SolarisOSThreadJNA extends SolarisOSThread {

    public SolarisOSThreadJNA(int pid, int lwpid) {
        super(pid, lwpid);
    }
}
