/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Process;
import oshi.driver.common.windows.wmi.Win32Process.CommandLineProperty;
import oshi.driver.common.windows.wmi.Win32Process.ProcessXPProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryExecutorFFM;

@ThreadSafe
public final class Win32ProcessFFM extends Win32Process {
    private Win32ProcessFFM() {
    }

    /**
     * Queries the command lines of the given processes.
     *
     * @param pidsToQuery the process IDs to query, or {@code null} to query all processes
     * @return a {@link WmiResult} of the command line for each matching process
     */
    public static WmiResult<CommandLineProperty> queryCommandLines(@Nullable Set<Integer> pidsToQuery) {
        return Win32Process.queryCommandLines(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()),
                pidsToQuery);
    }

    public static WmiResult<ProcessXPProperty> queryProcesses(@Nullable Collection<Integer> pids) {
        return Win32Process.queryProcesses(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()), pids);
    }
}
