/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import oshi.ffm.util.platform.mac.SysctlUtilFFM;
import oshi.hardware.common.platform.mac.SysctlProvider;

/**
 * FFM-based {@link SysctlProvider} implementation.
 */
final class SysctlProviderFFM implements SysctlProvider {

    static final SysctlProviderFFM INSTANCE = new SysctlProviderFFM();

    private SysctlProviderFFM() {
    }

    @Override
    public int sysctlInt(String name, int defaultValue) {
        return SysctlUtilFFM.sysctl(name, defaultValue);
    }

    @Override
    public int sysctlIntNoWarn(String name, int defaultValue) {
        return SysctlUtilFFM.sysctl(name, defaultValue, false);
    }

    @Override
    public long sysctlLong(String name, long defaultValue) {
        return SysctlUtilFFM.sysctl(name, defaultValue);
    }

    @Override
    public String sysctlString(String name, String defaultValue) {
        return SysctlUtilFFM.sysctl(name, defaultValue);
    }

    @Override
    public String sysctlStringNoWarn(String name, String defaultValue) {
        return SysctlUtilFFM.sysctl(name, defaultValue, false);
    }
}
