/*
 * Copyright 2026 The OSHI Project Contributors
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
 * Utility to query WMI class {@code Win32_Process} using cache (FFM).
 */
@ThreadSafe
public final class Win32ProcessCachedFFM extends Win32ProcessCached {

    private static final Supplier<Win32ProcessCachedFFM> INSTANCE = memoize(Win32ProcessCachedFFM::createInstance);

    private Win32ProcessCachedFFM() {
    }

    /**
     * Get the singleton instance of this class.
     *
     * @return the singleton instance
     */
    public static Win32ProcessCachedFFM getInstance() {
        return INSTANCE.get();
    }

    private static Win32ProcessCachedFFM createInstance() {
        return new Win32ProcessCachedFFM();
    }

    @Override
    protected WmiResult<CommandLineProperty> queryCommandLines() {
        return Win32ProcessFFM.queryCommandLines(null);
    }
}
