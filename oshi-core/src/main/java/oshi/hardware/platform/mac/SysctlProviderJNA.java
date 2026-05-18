/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import oshi.hardware.common.platform.mac.SysctlProvider;
import oshi.util.platform.mac.SysctlUtil;

/**
 * JNA-based {@link SysctlProvider} implementation.
 */
final class SysctlProviderJNA implements SysctlProvider {

    static final SysctlProviderJNA INSTANCE = new SysctlProviderJNA();

    private SysctlProviderJNA() {
    }

    @Override
    public int sysctlInt(String name, int defaultValue) {
        return SysctlUtil.sysctl(name, defaultValue);
    }

    @Override
    public int sysctlIntNoWarn(String name, int defaultValue) {
        return SysctlUtil.sysctl(name, defaultValue, false);
    }

    @Override
    public long sysctlLong(String name, long defaultValue) {
        return SysctlUtil.sysctl(name, defaultValue);
    }

    @Override
    public String sysctlString(String name, String defaultValue) {
        return SysctlUtil.sysctl(name, defaultValue);
    }

    @Override
    public String sysctlStringNoWarn(String name, String defaultValue) {
        return SysctlUtil.sysctl(name, defaultValue, false);
    }
}
