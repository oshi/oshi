/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdComputerSystem;
import oshi.util.Constants;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * JNA-backed FreeBSD computer system. {@code dmidecode} and {@code lshal} parsing live on
 * {@link FreeBsdComputerSystem}; only the {@code kern.hostuuid} sysctl fallback is JNA-specific.
 */
@Immutable
public class FreeBsdComputerSystemJNA extends FreeBsdComputerSystem {

    @Override
    protected String queryHostUuid() {
        return BsdSysctlUtil.sysctl("kern.hostuuid", Constants.UNKNOWN);
    }
}
