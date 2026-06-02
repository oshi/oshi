/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import oshi.software.common.AbstractOSProcess;

public abstract class FreeBsdOSProcess extends AbstractOSProcess {

    protected FreeBsdOSProcess(int pid) {
        super(pid);
    }
}
