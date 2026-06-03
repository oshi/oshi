/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdComputerSystem;
import oshi.util.Constants;

/**
 * FFM-backed FreeBSD computer system. {@code dmidecode} and {@code lshal} parsing live on
 * {@link FreeBsdComputerSystem}; only the {@code kern.hostuuid} sysctl fallback is FFM-specific.
 */
@Immutable
public class FreeBsdComputerSystemFFM extends FreeBsdComputerSystem {

    @Override
    protected String queryHostUuid() {
        return BsdSysctlUtilFFM.sysctl("kern.hostuuid", Constants.UNKNOWN);
    }
}
