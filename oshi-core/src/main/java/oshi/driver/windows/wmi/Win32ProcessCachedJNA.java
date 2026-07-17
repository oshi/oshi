/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Process.CommandLineProperty;
import oshi.driver.common.windows.wmi.Win32ProcessCached;
import oshi.driver.common.windows.wmi.WmiResult;

/**
 * Utility to query WMI class {@code Win32_Process} using cache (JNA).
 */
@ThreadSafe
public final class Win32ProcessCachedJNA extends Win32ProcessCached {

    private static final Supplier<Win32ProcessCachedJNA> INSTANCE = memoize(Win32ProcessCachedJNA::createInstance);

    private Win32ProcessCachedJNA() {
    }

    /**
     * Get the singleton instance of this class.
     *
     * @return the singleton instance
     */
    public static Win32ProcessCachedJNA getInstance() {
        return INSTANCE.get();
    }

    private static Win32ProcessCachedJNA createInstance() {
        return new Win32ProcessCachedJNA();
    }

    @Override
    protected WmiResult<CommandLineProperty> queryCommandLines() {
        return Win32ProcessJNA.queryCommandLines(null);
    }
}
