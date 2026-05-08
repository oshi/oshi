/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux.nativefree;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.os.linux.LinuxOSProcess;
import oshi.software.common.os.linux.LinuxOperatingSystem;

/**
 * Native-free Linux OS process implementation. Extends {@link LinuxOSProcess}, providing implementations that require
 * no native access.
 */
@ThreadSafe
public class LinuxOSProcessNF extends LinuxOSProcess {

    /**
     * Creates a new native-free Linux OS process.
     *
     * @param pid the process ID
     * @param os  the operating system instance
     */
    public LinuxOSProcessNF(int pid, LinuxOperatingSystem os) {
        super(pid, os);
    }

    @Override
    protected long queryRlimitSoft() {
        return getProcessOpenFileLimit(getProcessID(), 1);
    }

    @Override
    protected long queryRlimitHard() {
        return getProcessOpenFileLimit(getProcessID(), 2);
    }
}
